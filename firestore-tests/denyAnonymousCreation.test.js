const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// Task 1.2 — Deny anonymous drop creation.
//
// Drop and media creation require an authenticated, NON-anonymous principal.
// Each rejection path is asserted, alongside the paths that must stay intact:
// a normal signed-in user can still create, and guests can still READ public
// drops (anonymous viewing remains legal per the direction doc).
// ---------------------------------------------------------------------------

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';

// A token whose firebase.sign_in_provider is 'anonymous' (Firebase Anonymous
// Auth). createMockUserToken spreads these options over its defaults.
const ANON_TOKEN = {firebase: {sign_in_provider: 'anonymous', identities: {}}};

function dropData(overrides = {}) {
  return {
    text: 'Drop',
    lat: 1,
    lng: 1,
    createdBy: 'creator',
    createdAt: 1,
    isDeleted: false,
    visibility: 'PUBLIC',
    isNsfw: false,
    dropType: 'COMMUNITY',
    ...overrides,
  };
}

function photoUpload(ref, ownerUid) {
  return ref.putString('bytes', 'raw', {
    contentType: 'image/jpeg',
    customMetadata: {ownerId: ownerUid, accessLevel: 'PRIVATE', safetyStatus: 'PENDING'},
  });
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
    storage: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'storage.rules'), 'utf8'),
    },
  });

  try {
    const guest = env.unauthenticatedContext();
    const creator = env.authenticatedContext('creator');
    // Anonymous-auth principal WITH a seeded EXPLORER profile, so the only thing
    // that can reject its create is the non-anonymous check itself.
    const anon = env.authenticatedContext('anon-user', ANON_TOKEN);

    // --- Rejection paths -------------------------------------------------

    currentCase = 'unauthenticated create rejected';
    await env.clearFirestore();
    await assertFails(
      guest.firestore().doc('drops/guest-create').set(dropData({createdBy: 'guest'}))
    );

    currentCase = 'anonymous-auth create rejected';
    await env.clearFirestore();
    await seed(env, {'users/anon-user': {role: 'EXPLORER', nsfwEnabled: false}});
    await assertFails(
      anon.firestore().doc('drops/anon-create').set(dropData({createdBy: 'anon-user'}))
    );

    currentCase = 'isAnonymous=true create rejected';
    await env.clearFirestore();
    await seed(env, {'users/creator': {role: 'EXPLORER', nsfwEnabled: false}});
    await assertFails(
      creator.firestore().doc('drops/anon-toggle').set(dropData({isAnonymous: true}))
    );

    currentCase = 'anonymous-auth media upload rejected';
    await assertFails(
      photoUpload(anon.storage().ref('drops/photos/anon.jpg'), 'anon-user')
    );

    // --- Preserved paths -------------------------------------------------

    currentCase = 'non-anonymous create allowed';
    await env.clearFirestore();
    await seed(env, {'users/creator': {role: 'EXPLORER', nsfwEnabled: false}});
    await assertSucceeds(
      creator.firestore().doc('drops/ok-implicit').set(dropData())
    );
    await assertSucceeds(
      creator.firestore().doc('drops/ok-explicit').set(dropData({isAnonymous: false}))
    );

    currentCase = 'non-anonymous media upload allowed';
    await assertSucceeds(
      photoUpload(creator.storage().ref('drops/photos/ok.jpg'), 'creator')
    );

    currentCase = 'guest read of public drop still allowed';
    await env.clearFirestore();
    await seed(env, {'drops/public': dropData()});
    await assertSucceeds(guest.firestore().doc('drops/public').get());

    console.log('All deny-anonymous-creation rule tests passed.');
  } catch (err) {
    console.error(`Deny-anonymous-creation rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
