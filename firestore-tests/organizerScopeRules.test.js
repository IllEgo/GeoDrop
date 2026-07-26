const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// Task 2.4 — Organizer/event scoping survives the group work.
//
// The plan's 2.4 line reads "remove group creation, membership, and
// group-scoped feeds", but 0.1 Decision A reclassified INVITE-ONLY groups as
// launch scope (they are the "experiences the user explicitly joined"
// mechanism, and NearbyDropRegistrar filters push notifications by exactly that
// membership). The deferred item is PUBLIC / discoverable groups, which were
// never built. The 0.3 ADR records the same: "KEEP invite-only; DELETE
// nothing."
//
// So this suite is the acceptance evidence for 2.4: organizer-scoped event
// drops work, invite-only group scoping works, and neither depends on any
// public-group surface. The matching denies (create/update/delete/list on
// `groups`, self-granted membership) are asserted in denyDeferredWrites.test.js
// from task 1.3 and are not duplicated here.
// ---------------------------------------------------------------------------

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';

function dropData(overrides = {}) {
  return {
    text: 'Drop',
    lat: 1,
    lng: 1,
    createdBy: 'organizer',
    createdAt: 1,
    isDeleted: false,
    visibility: 'PUBLIC',
    isNsfw: false,
    dropType: 'COMMUNITY',
    contentType: 'TEXT',
    ...overrides,
  };
}

async function seed(env, documents) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    for (const [documentPath, data] of Object.entries(documents)) {
      await db.doc(documentPath).set(data);
    }
  });
}

(async () => {
  let currentCase = 'initialize';
  const env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8'),
    },
  });

  try {
    const guest = env.unauthenticatedContext();
    const organizer = env.authenticatedContext('organizer');
    const explorer = env.authenticatedContext('explorer');
    const outsider = env.authenticatedContext('outsider');

    const accounts = {
      'users/organizer': {role: 'BUSINESS', nsfwEnabled: false, businessName: 'E3HI'},
      'users/explorer': {role: 'EXPLORER', nsfwEnabled: false},
      'users/outsider': {role: 'EXPLORER', nsfwEnabled: false},
    };

    // --- Organizer/event drops -------------------------------------------

    currentCase = 'organizer can create event drops';
    await env.clearFirestore();
    await seed(env, accounts);
    await assertSucceeds(
      organizer.firestore().doc('drops/tour-stop').set(dropData({
        dropType: 'TOUR_STOP',
        businessId: 'organizer',
        businessName: 'E3HI',
      }))
    );
    await assertSucceeds(
      organizer.firestore().doc('drops/coupon').set(dropData({
        dropType: 'RESTAURANT_COUPON',
        businessId: 'organizer',
        businessName: 'E3HI',
        redemptionCode: 'PILOT10',
        redemptionLimit: 50,
        redemptionCount: 0,
      }))
    );
    // An organizer may also place ordinary community drops.
    await assertSucceeds(
      organizer.firestore().doc('drops/organizer-community').set(dropData())
    );

    currentCase = 'organizer role separation holds';
    // An explorer cannot masquerade as a business.
    await assertFails(
      explorer.firestore().doc('drops/explorer-coupon').set(dropData({
        createdBy: 'explorer',
        dropType: 'RESTAURANT_COUPON',
        businessId: 'explorer',
      }))
    );
    // A business cannot attribute a drop to a different business.
    await assertFails(
      organizer.firestore().doc('drops/spoofed-business').set(dropData({
        dropType: 'TOUR_STOP',
        businessId: 'someone-else',
      }))
    );

    currentCase = 'guests can discover public organizer drops';
    await assertSucceeds(guest.firestore().doc('drops/tour-stop').get());
    await assertSucceeds(guest.firestore().doc('drops/coupon').get());
    // The canonical browse query still works for a guest.
    await assertSucceeds(
      guest.firestore().collection('drops')
        .where('isDeleted', '==', false)
        .where('isNsfw', '==', false)
        .where('visibility', '==', 'PUBLIC')
        .get()
    );

    // --- Invite-only group scoping (launch scope per 0.1 Decision A) ------

    currentCase = 'invite-only group scoping still works';
    await env.clearFirestore();
    await seed(env, {
      ...accounts,
      'groups/EVENT24': {ownerId: 'organizer', createdAt: 1, updatedAt: 1},
      'users/explorer/groups/EVENT24': {
        code: 'EVENT24', role: 'SUBSCRIBER', ownerId: 'organizer', updatedAt: 1,
      },
      'users/organizer/groups/EVENT24': {
        code: 'EVENT24', role: 'OWNER', ownerId: 'organizer', updatedAt: 1,
      },
    });
    // A member may place a group-scoped drop (text-only, per the rules).
    await assertSucceeds(
      explorer.firestore().doc('drops/group-drop').set(dropData({
        createdBy: 'explorer',
        visibility: 'GROUP',
        groupCode: 'EVENT24',
      }))
    );
    // An organizer may scope an event drop to their own group.
    await assertSucceeds(
      organizer.firestore().doc('drops/group-event').set(dropData({
        visibility: 'GROUP',
        groupCode: 'EVENT24',
        dropType: 'TOUR_STOP',
        businessId: 'organizer',
      }))
    );
    // Members read it; a non-member does not, and cannot join themselves.
    await assertSucceeds(explorer.firestore().doc('drops/group-event').get());
    await assertFails(outsider.firestore().doc('drops/group-event').get());
    await assertFails(
      outsider.firestore().doc('drops/outsider-group-drop').set(dropData({
        createdBy: 'outsider',
        visibility: 'GROUP',
        groupCode: 'EVENT24',
      }))
    );

    currentCase = 'organizer scoping does not depend on groups';
    // The public organizer path needs no membership at all — the two axes
    // (dropType/businessId vs visibility/groupCode) stay independent.
    await env.clearFirestore();
    await seed(env, accounts);
    await assertSucceeds(
      organizer.firestore().doc('drops/no-group-needed').set(dropData({
        dropType: 'TOUR_STOP',
        businessId: 'organizer',
      }))
    );

    console.log('All organizer-scope rule tests passed.');
  } catch (err) {
    console.error(`Organizer-scope rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
