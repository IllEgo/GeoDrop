const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// Task 2.7 — Collapse account types.
//
// The launch scope has exactly two account types: EXPLORER (the default) and
// BUSINESS (organizer). 0.1 Decision + the 0.3 ADR recorded that there is no
// extended permission matrix to remove — the other per-principal axes are
// group-scoped roles (users/{uid}/groups) and operational Auth claims
// (moderator/admin/suspended), neither of which is a client-writable field.
//
// So this suite is the acceptance evidence that the two-type model is enforced
// rather than merely conventional:
//   - a client can only ever seed EXPLORER, and can never change `role`;
//   - no third role value is accepted anywhere, at create or update;
//   - business metadata is server-authored, so a client cannot unlock the
//     business surface by writing itself a `businessName` (which is what the
//     removed client-side "infer BUSINESS from business metadata" branch did);
//   - moderation fields are server-authored but tolerated as present, so a
//     reinstated account can still edit its own profile.
//
// Role escalation to BUSINESS/ADMIN is also asserted in
// adversarialRules.test.js; the overlap is deliberate — that suite guards the
// attack, this one pins the account model.
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

function dropData(overrides = {}) {
  return {
    text: 'Drop',
    lat: 1,
    lng: 1,
    createdBy: 'business',
    createdAt: 1,
    isDeleted: false,
    visibility: 'PUBLIC',
    isNsfw: false,
    dropType: 'COMMUNITY',
    contentType: 'TEXT',
    ...overrides,
  };
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
    const explorer = env.authenticatedContext('explorer');
    const business = env.authenticatedContext('business');
    const newcomer = env.authenticatedContext('newcomer');

    // --- Creation: every account starts as an explorer -------------------

    currentCase = 'profile creation seeds EXPLORER';
    await env.clearFirestore();
    await assertSucceeds(
      newcomer.firestore().doc('users/newcomer').set({
        role: 'EXPLORER',
        displayName: 'Newcomer',
        nsfwEnabled: false,
        createdAt: 1,
      })
    );

    currentCase = 'profile creation cannot pick another type';
    await env.clearFirestore();
    await assertFails(
      newcomer.firestore().doc('users/newcomer').set({
        role: 'BUSINESS',
        displayName: 'Newcomer',
        nsfwEnabled: false,
      })
    );
    // No third type exists — not an invented one, not a legacy one, and not a
    // lower-cased spelling of a real one (the rules compare the string exactly,
    // so the clients must too).
    for (const role of ['ADMIN', 'MODERATOR', 'ORGANIZER', 'CREATOR', 'business', 'explorer', '']) {
      await assertFails(
        newcomer.firestore().doc('users/newcomer').set({
          role,
          displayName: 'Newcomer',
          nsfwEnabled: false,
        })
      );
    }
    // A profile with no role at all is refused too: the field is the account
    // type, and an absent one would leave the type ambiguous.
    await assertFails(
      newcomer.firestore().doc('users/newcomer').set({displayName: 'Newcomer', nsfwEnabled: false})
    );

    currentCase = 'profile creation cannot carry server-authored fields';
    await env.clearFirestore();
    // Business metadata at create would be a way around the callable, which is
    // where the verified-email requirement lives.
    await assertFails(
      newcomer.firestore().doc('users/newcomer').set({
        role: 'EXPLORER',
        displayName: 'Newcomer',
        businessName: 'Fake Co',
      })
    );
    await assertFails(
      newcomer.firestore().doc('users/newcomer').set({
        role: 'EXPLORER',
        displayName: 'Newcomer',
        businessCategories: ['FOOD_RESTAURANTS_CAFES'],
      })
    );
    // Nor may a new account declare itself already-adjudicated.
    await assertFails(
      newcomer.firestore().doc('users/newcomer').set({
        role: 'EXPLORER',
        displayName: 'Newcomer',
        moderationStatus: 'ACTIVE',
      })
    );

    // --- The role is immutable from the client ---------------------------

    currentCase = 'role is immutable';
    await env.clearFirestore();
    await seed(env, {
      'users/explorer': {role: 'EXPLORER', displayName: 'Explorer', nsfwEnabled: false},
      'users/business': {
        role: 'BUSINESS',
        displayName: 'Organizer',
        nsfwEnabled: false,
        businessName: 'E3HI',
        businessCategories: ['HOSPITALITY_TOUR_GUIDES_ATTRACTIONS'],
      },
    });
    await assertFails(explorer.firestore().doc('users/explorer').update({role: 'BUSINESS'}));
    await assertFails(explorer.firestore().doc('users/explorer').update({role: 'ADMIN'}));
    // A business cannot quietly demote itself either — the field is not the
    // client's to write in either direction.
    await assertFails(business.firestore().doc('users/business').update({role: 'EXPLORER'}));
    // Benign edits still work, on both types.
    await assertSucceeds(explorer.firestore().doc('users/explorer').update({displayName: 'Ex'}));
    await assertSucceeds(business.firestore().doc('users/business').update({displayName: 'E3HI HQ'}));

    currentCase = 'an off-model stored role locks the profile down';
    await env.clearFirestore();
    await seed(env, {
      'users/explorer': {role: 'ADMIN', displayName: 'Legacy', nsfwEnabled: false},
    });
    // Preserving a role that is not one of the two launch types is refused, so
    // a stray value cannot be carried forward by ordinary profile edits. Such a
    // document must be normalized server-side
    // (functions/scripts/normalize-account-roles.js).
    await assertFails(explorer.firestore().doc('users/explorer').update({displayName: 'Legacy 2'}));

    // --- Business metadata is server-authored ---------------------------

    currentCase = 'a client cannot write itself business metadata';
    await env.clearFirestore();
    await seed(env, {
      'users/explorer': {role: 'EXPLORER', displayName: 'Explorer', nsfwEnabled: false},
    });
    await assertFails(explorer.firestore().doc('users/explorer').update({businessName: 'Fake Co'}));
    await assertFails(
      explorer.firestore().doc('users/explorer').update({
        businessCategories: ['FOOD_RESTAURANTS_CAFES'],
      })
    );
    // Even an empty backfill counts as introducing the field.
    await assertFails(explorer.firestore().doc('users/explorer').update({businessCategories: []}));

    currentCase = 'a business cannot rewrite or drop its own metadata';
    await env.clearFirestore();
    await seed(env, {
      'users/business': {
        role: 'BUSINESS',
        displayName: 'Organizer',
        nsfwEnabled: false,
        businessName: 'E3HI',
        businessCategories: ['HOSPITALITY_TOUR_GUIDES_ATTRACTIONS'],
      },
    });
    await assertFails(business.firestore().doc('users/business').update({businessName: 'Someone Else'}));
    await assertFails(
      business.firestore().doc('users/business').update({
        businessCategories: ['MARKETING_GUERRILLA_BRANDS'],
      })
    );
    // Restating the same values is a no-op and stays legal, which is what the
    // clients' profile-load path does.
    await assertSucceeds(
      business.firestore().doc('users/business').update({
        businessName: 'E3HI',
        businessCategories: ['HOSPITALITY_TOUR_GUIDES_ATTRACTIONS'],
        displayName: 'Organizer 2',
      })
    );

    currentCase = 'business metadata alone does not grant the business axis';
    await env.clearFirestore();
    // The removed client-side inference treated business metadata as proof of
    // BUSINESS. The server never did: the drop rules read the stored role, so a
    // profile carrying metadata but still typed EXPLORER gets explorer access.
    await seed(env, {
      'users/explorer': {
        role: 'EXPLORER',
        displayName: 'Explorer',
        nsfwEnabled: false,
        businessName: 'Fake Co',
      },
    });
    await assertFails(
      explorer.firestore().doc('drops/coupon').set(dropData({
        createdBy: 'explorer',
        dropType: 'RESTAURANT_COUPON',
        businessId: 'explorer',
        businessName: 'Fake Co',
      }))
    );
    // And the stored-BUSINESS account still has it.
    await seed(env, {
      'users/business': {
        role: 'BUSINESS',
        displayName: 'Organizer',
        nsfwEnabled: false,
        businessName: 'E3HI',
      },
    });
    await assertSucceeds(
      business.firestore().doc('drops/real-coupon').set(dropData({
        dropType: 'RESTAURANT_COUPON',
        businessId: 'business',
        businessName: 'E3HI',
      }))
    );

    // --- Moderation state is server-authored, but not disabling ----------

    currentCase = 'a reinstated account can still edit its profile';
    await env.clearFirestore();
    // moderationOperations writes these fields with the Admin SDK and leaves
    // them behind after an overturned appeal. They must be tolerated as
    // present, or a reinstated user could never touch their profile again.
    await seed(env, {
      'users/explorer': {
        role: 'EXPLORER',
        displayName: 'Explorer',
        nsfwEnabled: false,
        moderationStatus: 'ACTIVE',
        reinstatedAt: 5,
      },
    });
    await assertSucceeds(explorer.firestore().doc('users/explorer').update({displayName: 'Back'}));

    currentCase = 'a client cannot author its own moderation state';
    await assertFails(explorer.firestore().doc('users/explorer').update({moderationStatus: 'CLEARED'}));
    await assertFails(explorer.firestore().doc('users/explorer').update({reinstatedAt: 99}));
    await assertFails(
      explorer.firestore().doc('users/explorer').update({
        suspendedBy: 'explorer',
        suspensionReason: 'none',
      })
    );

    currentCase = 'accepting the legal policies does not lock the profile';
    await env.clearFirestore();
    // acceptLegalPolicies stamps these two fields on the profile with the Admin
    // SDK. Same failure mode as the moderation fields, but this one is on the
    // path every single account takes.
    await seed(env, {
      'users/explorer': {
        role: 'EXPLORER',
        displayName: 'Explorer',
        nsfwEnabled: false,
        legalAcceptanceVersion: '2026-07-01',
        legalAcceptedAt: 7,
      },
    });
    await assertSucceeds(explorer.firestore().doc('users/explorer').update({displayName: 'Accepted'}));
    // But the acceptance record itself is not the client's to forge.
    await assertFails(
      explorer.firestore().doc('users/explorer').update({legalAcceptanceVersion: '2099-01-01'})
    );
    await assertFails(explorer.firestore().doc('users/explorer').update({legalAcceptedAt: 99}));

    currentCase = 'suspension still blocks profile writes';
    await env.clearFirestore();
    await seed(env, {
      'users/suspended': {
        role: 'EXPLORER',
        displayName: 'Suspended',
        nsfwEnabled: false,
        moderationStatus: 'SUSPENDED',
        suspendedAt: 5,
      },
    });
    const suspended = env.authenticatedContext('suspended', {suspended: true});
    await assertFails(suspended.firestore().doc('users/suspended').update({displayName: 'Nope'}));

    // Task 4.6 — the guest-merge audit trail is server-only in both directions.
    // Reading it would correlate a guest session with the account it became, and
    // writing it would let a client fabricate a merge that never happened. These
    // assertions exist so that a future permissive wildcard fails here rather
    // than silently exposing the trail.
    currentCase = 'account merge receipts are invisible and unwritable';
    await env.clearFirestore();
    await seed(env, {
      'users/explorer': {role: 'EXPLORER', displayName: 'Explorer', nsfwEnabled: false},
      'accountMergeReceipts/receipt-1': {status: 'completed', guestUidDigest: 'abc'},
    });
    const merger = env.authenticatedContext('explorer');
    await assertFails(merger.firestore().doc('accountMergeReceipts/receipt-1').get());
    await assertFails(merger.firestore().collection('accountMergeReceipts').get());
    await assertFails(
      merger.firestore().doc('accountMergeReceipts/forged').set({status: 'completed'})
    );
    await assertFails(merger.firestore().doc('accountMergeReceipts/receipt-1').delete());
    await assertFails(
      env.unauthenticatedContext().firestore().doc('accountMergeReceipts/receipt-1').get()
    );

    console.log('All account-role rule tests passed.');
  } catch (err) {
    console.error(`Account-role rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
