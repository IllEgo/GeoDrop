const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// Task 2.5 — Direct messaging.
//
// 0.1 classified DMs "D·absent": not implemented on either client, nothing to
// remove. Re-confirmed at 2.5 across app/src, ios/GeoDropIOS and functions/src
// — there is no DM UI, no thread model, no conversation collection, and no DM
// notification handler. The client routes exactly two push events,
// DROP_COLLECTED and REPORT_STATUS_UPDATED, and 1.3 already denied every
// DM-shaped path at the rules layer.
//
// So this suite is 2.5's acceptance evidence: "notification routing still works
// for the scoped notifications that remain." It pins the two data paths those
// notifications depend on. reportStatuses had no coverage before this task.
// ---------------------------------------------------------------------------

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';

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
    const owner = env.authenticatedContext('owner');
    const other = env.authenticatedContext('other');
    const guest = env.unauthenticatedContext();

    // --- DROP_COLLECTED delivery: notification tokens ---------------------

    currentCase = 'device token lifecycle for push delivery';
    await env.clearFirestore();
    const tokenPath = 'users/owner/notificationTokens/device-1';
    await assertSucceeds(
      owner.firestore().doc(tokenPath).set({
        token: 'fcm-token', platform: 'android', updatedAt: 1,
      })
    );
    // Token refresh must keep working, or delivery silently dies.
    await assertSucceeds(
      owner.firestore().doc(tokenPath).set({
        token: 'fcm-token-rotated', platform: 'android', updatedAt: 2,
      })
    );
    await assertSucceeds(owner.firestore().doc(tokenPath).get());
    // Sign-out / uninstall cleanup.
    await assertSucceeds(owner.firestore().doc(tokenPath).delete());

    currentCase = 'device tokens stay private';
    await env.clearFirestore();
    await seed(env, {
      'users/owner/notificationTokens/device-1': {
        token: 'fcm-token', platform: 'android', updatedAt: 1,
      },
    });
    await assertFails(other.firestore().doc('users/owner/notificationTokens/device-1').get());
    await assertFails(guest.firestore().doc('users/owner/notificationTokens/device-1').get());
    await assertFails(
      other.firestore().doc('users/owner/notificationTokens/stolen').set({
        token: 'attacker-token', platform: 'android', updatedAt: 1,
      })
    );
    // A token document cannot smuggle extra fields past the shape check.
    await assertFails(
      owner.firestore().doc('users/owner/notificationTokens/device-2').set({
        token: 'fcm-token', platform: 'android', updatedAt: 1, topic: 'all-users',
      })
    );

    // --- REPORT_STATUS_UPDATED payload: reportStatuses -------------------
    // Server-written, owner-readable. Previously untested.

    currentCase = 'report statuses are readable only by their owner';
    await env.clearFirestore();
    await seed(env, {
      'users/owner/reportStatuses/report-1': {status: 'ACTIONED', updatedAt: 1},
    });
    await assertSucceeds(owner.firestore().doc('users/owner/reportStatuses/report-1').get());
    await assertFails(other.firestore().doc('users/owner/reportStatuses/report-1').get());
    await assertFails(guest.firestore().doc('users/owner/reportStatuses/report-1').get());

    currentCase = 'report statuses are server-written only';
    // A client must not be able to fabricate or edit a moderation outcome.
    await assertFails(
      owner.firestore().doc('users/owner/reportStatuses/forged').set({
        status: 'ACTIONED', updatedAt: 1,
      })
    );
    await assertFails(
      owner.firestore().doc('users/owner/reportStatuses/report-1').update({status: 'DISMISSED'})
    );
    await assertFails(owner.firestore().doc('users/owner/reportStatuses/report-1').delete());

    // --- DM-shaped paths stay closed (smoke; full set is in 1.3) ---------

    currentCase = 'no DM path can be used to route notifications';
    await env.clearFirestore();
    await assertFails(
      owner.firestore().doc('conversations/owner-other').set({
        participants: ['owner', 'other'], lastMessage: 'hi',
      })
    );
    await assertFails(
      owner.firestore().doc('users/owner/messages/inbound').set({from: 'other', body: 'hi'})
    );

    console.log('All notification-routing rule tests passed.');
  } catch (err) {
    console.error(`Notification-routing rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
