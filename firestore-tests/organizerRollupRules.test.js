const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// Task 4.4 (ADR P7) — the organiser rollup.
//
// groups/{code}/analytics/summary is written only by the Admin SDK — the
// rollUpExperienceActivity trigger and the daily reconcile — and is readable
// only by the experience owner. It carries aggregates and no attendee identity.
//
// This suite exists because every real defect found this week was caught by a
// test asserting the happy path, and every one that hid did so because no such
// test existed. So it asserts the owner CAN read, not merely that others cannot.
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
    const member = env.authenticatedContext('member');
    const guest = env.unauthenticatedContext();

    const summary = {
      groupCode: 'PILOT1',
      collects: 12,
      redemptions: 3,
      drops: 5,
      updatedAt: Date.now(),
    };

    currentCase = 'the experience owner can read the rollup';
    await env.clearFirestore();
    await seed(env, {
      'groups/PILOT1': {ownerId: 'owner', code: 'PILOT1'},
      'groups/PILOT1/analytics/summary': summary,
      'users/member/groups/PILOT1': {code: 'PILOT1', role: 'SUBSCRIBER', ownerId: 'owner'},
    });
    await assertSucceeds(owner.firestore().doc('groups/PILOT1/analytics/summary').get());

    currentCase = 'a member who is not the owner cannot read it';
    await assertFails(member.firestore().doc('groups/PILOT1/analytics/summary').get());

    currentCase = 'a signed-out visitor cannot read it';
    await assertFails(guest.firestore().doc('groups/PILOT1/analytics/summary').get());

    // --- no client may write, not even the owner --------------------------

    currentCase = 'the owner cannot create the rollup';
    await env.clearFirestore();
    await seed(env, {'groups/PILOT1': {ownerId: 'owner', code: 'PILOT1'}});
    await assertFails(
      owner.firestore().doc('groups/PILOT1/analytics/summary').set(summary)
    );

    currentCase = 'the owner cannot inflate their own numbers';
    await env.clearFirestore();
    await seed(env, {
      'groups/PILOT1': {ownerId: 'owner', code: 'PILOT1'},
      'groups/PILOT1/analytics/summary': summary,
    });
    await assertFails(
      owner.firestore().doc('groups/PILOT1/analytics/summary').update({collects: 9999})
    );

    currentCase = 'the owner cannot delete the rollup';
    await assertFails(
      owner.firestore().doc('groups/PILOT1/analytics/summary').delete()
    );

    currentCase = 'a stranger cannot write it either';
    await assertFails(
      member.firestore().doc('groups/PILOT1/analytics/summary').set(summary)
    );

    // --- ownership is proven against the parent group ----------------------

    currentCase = 'ownership of a different group grants nothing';
    await env.clearFirestore();
    await seed(env, {
      'groups/PILOT1': {ownerId: 'someone-else', code: 'PILOT1'},
      'groups/PILOT1/analytics/summary': summary,
      'groups/OTHER': {ownerId: 'owner', code: 'OTHER'},
    });
    await assertFails(owner.firestore().doc('groups/PILOT1/analytics/summary').get());

    console.log('All organizer-rollup rule tests passed.');
  } catch (err) {
    console.error(`Organizer-rollup rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
