const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const {deleteField} = require('firebase/firestore');

// ---------------------------------------------------------------------------
// Migration baseline (characterization) tests — Task 1.1.
//
// These assertions PIN THE CURRENT behavior of the exact rules that Phase 1.2
// and 1.3 will intentionally change. They deliberately assert that things the
// launch scope disallows are *still allowed today*, so that when a rule is
// edited the matching assertion FAILS LOUDLY and must be consciously flipped
// to `assertFails` as part of that task.
//
// When you edit a rule below, update the paired assertion here in the SAME PR:
//   * 1.2 — force isAnonymous == false on create  → flip ANON-TOGGLE case
//   * 1.3 — deny contentType == 'VIDEO' on create → flip VIDEO-DROP case
//   * 1.3 — remove drops/videos + video/* storage → flip VIDEO-UPLOAD case
//   * 1.3 — remove dislikedBy/dislikeCount writes  → flip DISLIKE case
//
// Do NOT weaken these into no-ops. A green run here means "current prototype
// behavior is intact"; a red run after a rule edit is the intended signal.
// ---------------------------------------------------------------------------

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';

function dropData(overrides = {}) {
  return {
    text: 'Baseline drop',
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

function uploadVideo(ref, uploaderUid) {
  return ref.putString('test video bytes', 'raw', {
    contentType: 'video/mp4',
    customMetadata: {
      ownerId: uploaderUid,
      accessLevel: 'PRIVATE',
      safetyStatus: 'PENDING',
    },
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
    const anonymous = env.unauthenticatedContext();
    const creator = env.authenticatedContext('creator');

    // ANON-TOGGLE (1.2 will flip the assertSucceeds below to assertFails).
    // Baseline: a signed-in explorer may currently create a drop that hides the
    // creator's display name via isAnonymous: true. Anonymous *auth* is already
    // blocked — an unauthenticated create fails today and must keep failing.
    currentCase = 'anonymous-posting toggle (1.2)';
    await env.clearFirestore();
    await seed(env, {'users/creator': {role: 'EXPLORER', nsfwEnabled: false}});
    await assertFails(
      anonymous.firestore().doc('drops/anon-unauth').set(dropData({isAnonymous: true}))
    );
    await assertSucceeds(
      creator.firestore().doc('drops/anon-toggle').set(dropData({isAnonymous: true}))
    );

    // VIDEO-DROP (1.3 will flip the assertSucceeds below to assertFails).
    // Baseline: a PUBLIC drop with contentType 'VIDEO' is accepted at create.
    currentCase = 'video drop create (1.3)';
    await env.clearFirestore();
    await seed(env, {'users/creator': {role: 'EXPLORER', nsfwEnabled: false}});
    await assertSucceeds(
      creator.firestore().doc('drops/video-drop').set(dropData({contentType: 'VIDEO'}))
    );

    // VIDEO-UPLOAD (1.3 will flip the assertSucceeds below to assertFails).
    // Baseline: an owner may upload a video/* object under drops/videos/.
    currentCase = 'video storage upload (1.3)';
    await assertSucceeds(
      uploadVideo(creator.storage().ref('drops/videos/clip.mp4'), 'creator')
    );

    // DISLIKE (1.3 will flip the assertSucceeds cases below to assertFails).
    // Baseline: any signed-in user may cast and retract a dislike, mirroring the
    // like transitions covered in voteRules.test.js.
    currentCase = 'dislike transitions (1.3)';
    await env.clearFirestore();
    await seed(env, {
      'drops/dislike-target': dropData({
        likedBy: {}, likeCount: 0, dislikedBy: {}, dislikeCount: 0,
      }),
    });
    const dropRef = creator.firestore().doc('drops/dislike-target');
    await assertSucceeds(
      dropRef.update({dislikeCount: 1, ['dislikedBy.creator']: true})
    );
    await assertSucceeds(
      dropRef.update({dislikeCount: 0, ['dislikedBy.creator']: deleteField()})
    );

    console.log('All migration baseline rule tests passed.');
  } catch (err) {
    console.error(`Migration baseline rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
