const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// Task 4.2 — Collect / claim.
//
// Collecting was already confined to the collector's own key by
// hasOnlyUserCollectedChange. What it was not, was *one-way*: nothing stopped a
// user removing their own collectedBy entry and collecting again. That matters
// because a claim is the pilot's unit of value — the prize, the trail step, the
// organiser's collect counts that 4.4 will report on — and a reversible claim
// can be farmed.
//
// These tests pin: you may collect once, you may not un-collect, you may not
// touch anyone else's entry, and collecting stays idempotent.
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

    currentCase = 'a signed-in user may collect a drop';
    await env.clearFirestore();
    await seed(env, {'drops/d1': baseDrop({})});
    await assertSucceeds(
      me.firestore().doc('drops/d1').update({'collectedBy.me': true})
    );

    currentCase = 'collecting is idempotent';
    await assertSucceeds(
      me.firestore().doc('drops/d1').update({'collectedBy.me': true})
    );

    // --- a claim is one-way ------------------------------------------------

    currentCase = 'a collector cannot un-collect by nulling their entry';
    await env.clearFirestore();
    await seed(env, {'drops/d2': baseDrop({collectedBy: {me: true}})});
    await assertFails(
      me.firestore().doc('drops/d2').update({'collectedBy.me': null})
    );

    currentCase = 'a collector cannot un-collect by falsifying their entry';
    await env.clearFirestore();
    await seed(env, {'drops/d3': baseDrop({collectedBy: {me: true}})});
    await assertFails(
      me.firestore().doc('drops/d3').update({'collectedBy.me': false})
    );

    currentCase = 'a collector cannot un-collect by rewriting the whole map';
    await env.clearFirestore();
    await seed(env, {'drops/d4': baseDrop({collectedBy: {me: true}})});
    await assertFails(
      me.firestore().doc('drops/d4').update({collectedBy: {}})
    );

    // --- other people's claims are untouchable ------------------------------

    currentCase = 'a user cannot collect on behalf of someone else';
    await env.clearFirestore();
    await seed(env, {'drops/d5': baseDrop({})});
    await assertFails(
      me.firestore().doc('drops/d5').update({'collectedBy.someone': true})
    );

    currentCase = 'a user cannot remove another collector';
    await env.clearFirestore();
    await seed(env, {'drops/d6': baseDrop({collectedBy: {other: true}})});
    await assertFails(
      me.firestore().doc('drops/d6').update({collectedBy: {}})
    );

    currentCase = 'collecting alongside another collector leaves theirs intact';
    await env.clearFirestore();
    await seed(env, {'drops/d7': baseDrop({collectedBy: {other: true}})});
    await assertSucceeds(
      me.firestore().doc('drops/d7').update({'collectedBy.me': true})
    );

    // --- the inventory copy is the collector's own -------------------------

    currentCase = 'a user cannot write into another user inventory';
    await env.clearFirestore();
    await seed(env, {'users/other': {role: 'EXPLORER', createdAt: 1}});
    await assertFails(
      me.firestore().doc('users/other/inventory/d1').set({
        id: 'd1', contentType: 'TEXT', collectedAt: 1,
      })
    );

    // --- redeeming an offer (task 4.3 prerequisite) ------------------------
    //
    // No test asserted a *successful* redemption before, which is exactly why the
    // redemption branch running out of expression budget stayed hidden. These
    // assertions are the regression guard for that.

    currentCase = 'a signed-in user may redeem a coupon';
    await env.clearFirestore();
    await seed(env, {
      'drops/c1': baseDrop({
        dropType: 'RESTAURANT_COUPON',
        businessId: 'biz',
        redemptionCount: 0,
        redeemedBy: {},
      }),
    });
    await assertSucceeds(
      me.firestore().doc('drops/c1').update({
        'redeemedBy.me': Date.now(),
        redemptionCount: 1,
      })
    );

    currentCase = 'the same user cannot redeem twice';
    await assertFails(
      me.firestore().doc('drops/c1').update({
        'redeemedBy.me': Date.now(),
        redemptionCount: 2,
      })
    );

    currentCase = 'redemption cannot exceed the limit';
    await env.clearFirestore();
    await seed(env, {
      'drops/c2': baseDrop({
        dropType: 'RESTAURANT_COUPON',
        businessId: 'biz',
        redemptionCount: 5,
        redemptionLimit: 5,
        redeemedBy: {},
      }),
    });
    await assertFails(
      me.firestore().doc('drops/c2').update({
        'redeemedBy.me': Date.now(),
        redemptionCount: 6,
      })
    );

    currentCase = 'an expired coupon cannot be redeemed';
    await env.clearFirestore();
    await seed(env, {
      'drops/c3': baseDrop({
        dropType: 'RESTAURANT_COUPON',
        businessId: 'biz',
        createdAt: Date.now() - 3 * 86400000,
        decayDays: 1,
        redemptionCount: 0,
        redeemedBy: {},
      }),
    });
    await assertFails(
      me.firestore().doc('drops/c3').update({
        'redeemedBy.me': Date.now(),
        redemptionCount: 1,
      })
    );

    console.log('All collect/claim rule tests passed.');
  } catch (err) {
    console.error(`Collect/claim rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
