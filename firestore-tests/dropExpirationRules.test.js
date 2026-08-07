const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// Task 4.1 — Drop expiration.
//
// Expiration existed only in the clients: `Drop.isExpired()` hid expired drops
// and refused to collect them, in 77 places across app/src. Nothing in the rules
// ever evaluated `decayDays`, so an expired drop was still collectable and still
// redeemable by anything that did not run that client check.
//
// These tests pin the enforcement: a drop with a positive decayDays stops being
// collectable and redeemable once createdAt + decayDays has passed, while drops
// without a decay never expire. Liking and reporting are deliberately unaffected
// — they run through the like/report-only branch, which expiration does not gate.
// ---------------------------------------------------------------------------

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';
const DAY_MS = 86400000;

async function seed(env, documents) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    for (const [documentPath, data] of Object.entries(documents)) {
      await db.doc(documentPath).set(data);
    }
  });
}

function baseDrop(overrides) {
  return Object.assign({
    createdBy: 'creator',
    createdAt: Date.now(),
    lat: 19.7,
    lng: -155.08,
    text: 'A drop',
    contentType: 'TEXT',
    dropType: 'COMMUNITY',
    visibility: 'PUBLIC',
    isDeleted: false,
    isNsfw: false,
    nsfwLabels: [],
    likeCount: 0,
    likedBy: {},
    reportCount: 0,
    reportedBy: {},
    collectedBy: {},
  }, overrides);
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
    const me = env.authenticatedContext('me');

    // --- an expired drop cannot be collected ------------------------------

    currentCase = 'expired drop cannot be collected';
    await env.clearFirestore();
    await seed(env, {
      'drops/expired': baseDrop({
        createdAt: Date.now() - 3 * DAY_MS,
        decayDays: 1,
      }),
    });

    await assertFails(
      me.firestore().doc('drops/expired').update({'collectedBy.me': true})
    );

    // --- an unexpired drop still collects ---------------------------------

    currentCase = 'unexpired drop still collects';
    await env.clearFirestore();
    await seed(env, {
      'drops/fresh': baseDrop({
        createdAt: Date.now() - 1 * DAY_MS,
        decayDays: 7,
      }),
    });

    await assertSucceeds(
      me.firestore().doc('drops/fresh').update({'collectedBy.me': true})
    );

    // --- a drop with no decay never expires -------------------------------

    currentCase = 'drop without decayDays never expires';
    await env.clearFirestore();
    await seed(env, {
      'drops/forever': baseDrop({createdAt: Date.now() - 3650 * DAY_MS}),
    });

    await assertSucceeds(
      me.firestore().doc('drops/forever').update({'collectedBy.me': true})
    );

    currentCase = 'decayDays of zero never expires';
    await env.clearFirestore();
    await seed(env, {
      'drops/zero': baseDrop({createdAt: Date.now() - 3650 * DAY_MS, decayDays: 0}),
    });

    await assertSucceeds(
      me.firestore().doc('drops/zero').update({'collectedBy.me': true})
    );

    // --- the boundary ------------------------------------------------------

    currentCase = 'a drop one hour short of expiry still collects';
    await env.clearFirestore();
    await seed(env, {
      'drops/almost': baseDrop({
        createdAt: Date.now() - (DAY_MS - 3600000),
        decayDays: 1,
      }),
    });

    await assertSucceeds(
      me.firestore().doc('drops/almost').update({'collectedBy.me': true})
    );

    currentCase = 'a drop one hour past expiry does not';
    await env.clearFirestore();
    await seed(env, {
      'drops/just-past': baseDrop({
        createdAt: Date.now() - (DAY_MS + 3600000),
        decayDays: 1,
      }),
    });

    await assertFails(
      me.firestore().doc('drops/just-past').update({'collectedBy.me': true})
    );

    // --- liking and reporting are deliberately unaffected ------------------

    currentCase = 'an expired drop can still be liked and reported';
    await env.clearFirestore();
    await seed(env, {
      'drops/expired-social': baseDrop({
        createdAt: Date.now() - 3 * DAY_MS,
        decayDays: 1,
      }),
    });

    await assertSucceeds(
      me.firestore().doc('drops/expired-social').update({
        'likedBy.me': true,
        likeCount: 1,
      })
    );

    // Reporting an expired drop must keep working — moderation cannot depend on
    // a drop still being live.
    await assertSucceeds(
      me.firestore().doc('drops/expired-social').update({
        // reportedBy values are timestamps/ints per isValidReportTransition.
        'reportedBy.me': Date.now(),
        reportCount: 1,
      })
    );

    // --- the owner can still delete an expired drop ------------------------

    currentCase = 'owner can still soft-delete an expired drop';
    await env.clearFirestore();
    await seed(env, {
      'drops/mine': baseDrop({
        createdBy: 'me',
        createdAt: Date.now() - 3 * DAY_MS,
        decayDays: 1,
      }),
    });

    await assertSucceeds(
      me.firestore().doc('drops/mine').update({isDeleted: true, deletedAt: Date.now()})
    );

    console.log('All drop-expiration rule tests passed.');
  } catch (err) {
    console.error(`Drop-expiration rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
