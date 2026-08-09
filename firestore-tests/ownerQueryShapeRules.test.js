const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// The owner-facing drop queries, asserted as query *shapes*.
//
// Firestore authorizes a list from the query alone: every condition the `list`
// rule names must be provable from the filters, whatever the collection holds.
// The drops rule requires the canonical isDeleted and isNsfw booleans, so a
// query missing either is refused outright — which is how the business
// dashboard and My Drops came to fail with PERMISSION_DENIED for every account
// while the browse path, which does filter isNsfw, kept working.
//
// These tests mirror the exact queries in FirestoreRepo. If someone drops a
// filter, this fails in CI instead of on a device.
// ---------------------------------------------------------------------------

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';
const BUSINESS = 'business-uid';
const EXPLORER = 'explorer-uid';

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

  const drop = (createdBy, extra = {}) => ({
    text: 'seeded',
    lat: 19.7,
    lng: -155.07,
    createdBy,
    createdAt: 1,
    isDeleted: false,
    isNsfw: false,
    visibility: 'PUBLIC',
    dropType: 'COMMUNITY',
    contentType: 'TEXT',
    ...extra,
  });

  try {
    await seed(env, {
      [`users/${BUSINESS}`]: {role: 'BUSINESS', businessName: 'eat za pizza'},
      [`users/${EXPLORER}`]: {role: 'EXPLORER'},
      'drops/business-1': drop(BUSINESS, {
        businessId: BUSINESS,
        businessName: 'eat za pizza',
        dropType: 'TOUR_STOP',
        visibility: 'GROUP',
        groupCode: 'EATZ',
      }),
      'drops/explorer-1': drop(EXPLORER),
      [`users/${BUSINESS}/groups/EATZ`]: {code: 'EATZ', role: 'OWNER', ownerId: BUSINESS},
      'groups/EATZ': {ownerId: BUSINESS},
    });

    const business = env.authenticatedContext(BUSINESS).firestore();
    const explorer = env.authenticatedContext(EXPLORER).firestore();

    // --- the shapes FirestoreRepo actually sends ---------------------------

    currentCase = 'getBusinessDrops as shipped';
    await assertSucceeds(
      business.collection('drops')
        .where('businessId', '==', BUSINESS)
        .where('createdBy', '==', BUSINESS)
        .where('isDeleted', '==', false)
        .where('isNsfw', '==', false)
        .get()
    );

    currentCase = 'getDropsForUser as shipped';
    await assertSucceeds(
      explorer.collection('drops')
        .where('createdBy', '==', EXPLORER)
        .where('isDeleted', '==', false)
        .where('isNsfw', '==', false)
        .get()
    );

    // --- the shapes that regressed ----------------------------------------

    currentCase = 'a business query without the isNsfw filter';
    await assertFails(
      business.collection('drops')
        .where('businessId', '==', BUSINESS)
        .where('createdBy', '==', BUSINESS)
        .where('isDeleted', '==', false)
        .get()
    );

    currentCase = 'an owner query without the isNsfw filter';
    await assertFails(
      explorer.collection('drops')
        .where('createdBy', '==', EXPLORER)
        .where('isDeleted', '==', false)
        .get()
    );

    currentCase = 'an owner query without the isDeleted filter';
    await assertFails(
      explorer.collection('drops')
        .where('createdBy', '==', EXPLORER)
        .where('isNsfw', '==', false)
        .get()
    );

    // --- authorship stays where it was ------------------------------------

    currentCase = 'a signed-in user cannot reassign createdBy';
    await assertFails(
      explorer.doc('drops/business-1').update({createdBy: EXPLORER})
    );

    console.log('All owner query-shape rule tests passed.');
  } catch (err) {
    console.error(`Owner query-shape rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
