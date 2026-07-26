const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');
const {deleteField} = require('firebase/firestore');

// ---------------------------------------------------------------------------
// Task 1.3 — Deny deferred-feature writes.
//
// One denied path per deferred feature: video content, NSFW-flagged content,
// DM threads, public group creation, and vote records. This task closes WRITES
// only — every case is paired with an assertion that existing documents remain
// readable and that the launch-scope neighbours (photo/audio drops, simple
// likes, invite-only group reads) still work.
// ---------------------------------------------------------------------------

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';

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
    contentType: 'TEXT',
    ...overrides,
  };
}

function upload(ref, contentType, ownerUid) {
  return ref.putString('media bytes', 'raw', {
    contentType,
    customMetadata: {
      ownerId: ownerUid,
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

async function seedObject(env, objectPath, contentType, customMetadata) {
  await env.withSecurityRulesDisabled(async (context) => {
    await context.storage().ref(objectPath).putString('media bytes', 'raw', {
      contentType,
      customMetadata,
    });
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
    const viewer = env.authenticatedContext('viewer');

    // --- 1. Video content ------------------------------------------------

    currentCase = 'video drop create denied';
    await env.clearFirestore();
    await seed(env, {'users/creator': {role: 'EXPLORER', nsfwEnabled: false}});
    await assertFails(
      creator.firestore().doc('drops/video').set(dropData({contentType: 'VIDEO'}))
    );
    // A video/* mime type is rejected even under a non-video contentType.
    await assertFails(
      creator.firestore().doc('drops/video-mime').set(dropData({
        contentType: 'PHOTO',
        mediaMimeType: 'video/mp4',
        mediaUrl: 'https://example.invalid/clip.mp4',
      }))
    );
    // Photo and audio drops are launch scope and must still be creatable.
    await assertSucceeds(
      creator.firestore().doc('drops/photo').set(dropData({
        contentType: 'PHOTO',
        mediaMimeType: 'image/jpeg',
        mediaUrl: 'https://example.invalid/photo.jpg',
      }))
    );
    await assertSucceeds(
      creator.firestore().doc('drops/audio').set(dropData({
        contentType: 'AUDIO',
        mediaMimeType: 'audio/mp4',
        mediaUrl: 'https://example.invalid/tour.m4a',
      }))
    );

    // Object names are unique to this file: the Storage emulator is shared
    // across the suite and uploads are create-only (resource == null).
    currentCase = 'video upload denied';
    await assertFails(
      upload(creator.storage().ref('drops/videos/deferred.mp4'), 'video/mp4', 'creator')
    );
    // The legacy drops/{uid}/ layout must not be a way back in.
    await assertFails(
      upload(creator.storage().ref('drops/creator/deferred.mp4'), 'video/mp4', 'creator')
    );
    await assertSucceeds(
      upload(creator.storage().ref('drops/photos/deferred.jpg'), 'image/jpeg', 'creator')
    );
    await assertSucceeds(
      upload(creator.storage().ref('drops/audio/deferred.m4a'), 'audio/mp4', 'creator')
    );

    currentCase = 'existing video content still readable';
    await env.clearFirestore();
    await seed(env, {
      'drops/legacy-video': dropData({
        contentType: 'VIDEO',
        mediaStoragePath: 'drops/videos/legacy.mp4',
      }),
    });
    await seedObject(env, 'drops/videos/legacy.mp4', 'video/mp4', {
      ownerId: 'creator',
      accessLevel: 'PUBLIC',
      safetyStatus: 'SAFE',
      dropId: 'legacy-video',
    });
    await assertSucceeds(guest.firestore().doc('drops/legacy-video').get());
    await assertSucceeds(guest.storage().ref('drops/videos/legacy.mp4').getMetadata());

    // --- 2. NSFW-flagged content -----------------------------------------

    currentCase = 'nsfw writes denied';
    await env.clearFirestore();
    await seed(env, {'users/creator': {role: 'EXPLORER', nsfwEnabled: false}});
    await assertFails(
      creator.firestore().doc('drops/nsfw').set(dropData({isNsfw: true}))
    );
    // The legacy mirror field cannot be used to smuggle the same state in.
    await assertFails(
      creator.firestore().doc('drops/nsfw-mirror').set(dropData({nsfw: true}))
    );
    await assertFails(
      creator.firestore().doc('drops/nsfw-labels').set(dropData({
        nsfwLabels: ['adult'],
      }))
    );
    // Clients cannot opt themselves into viewing mature content either.
    await assertFails(
      creator.firestore().doc('users/creator').update({nsfwEnabled: true})
    );
    // The shape the client actually writes (explicit false + empty labels).
    await assertSucceeds(
      creator.firestore().doc('drops/safe').set(dropData({
        nsfw: false,
        nsfwLabels: [],
      }))
    );

    // --- 3. DM threads ----------------------------------------------------

    currentCase = 'dm writes denied';
    await env.clearFirestore();
    const dmPaths = [
      'messages/thread-1',
      'threads/thread-1',
      'conversations/thread-1',
      'directMessages/thread-1',
      'dms/thread-1',
      'users/creator/messages/thread-1',
      'users/creator/threads/thread-1',
      'users/creator/conversations/thread-1',
    ];
    for (const dmPath of dmPaths) {
      currentCase = `dm write denied (${dmPath})`;
      await assertFails(
        creator.firestore().doc(dmPath).set({from: 'creator', body: 'hello'})
      );
    }
    // Reads are denied too, including for a document a backend created.
    currentCase = 'dm read denied';
    await seed(env, {'messages/seeded': {from: 'creator', body: 'hello'}});
    await assertFails(creator.firestore().doc('messages/seeded').get());

    // --- 4. Public group creation -----------------------------------------

    currentCase = 'group writes denied';
    await env.clearFirestore();
    await seed(env, {
      'groups/owned-group': {ownerId: 'creator', createdAt: 1, updatedAt: 1},
      'users/viewer/groups/owned-group': {
        code: 'owned-group',
        role: 'SUBSCRIBER',
        ownerId: 'creator',
        updatedAt: 1,
      },
    });
    await assertFails(
      creator.firestore().doc('groups/new-group').set({
        ownerId: 'creator',
        createdAt: 1,
        updatedAt: 1,
      })
    );
    await assertFails(
      creator.firestore().doc('groups/owned-group').update({name: 'Renamed'})
    );
    await assertFails(creator.firestore().doc('groups/owned-group').delete());
    // Enumeration stays denied, so no group is publicly discoverable.
    await assertFails(creator.firestore().collection('groups').get());
    // Membership cannot be self-granted; only the manageGroup callable writes it.
    await assertFails(
      viewer.firestore().doc('users/viewer/groups/self-join').set({
        code: 'self-join',
        role: 'SUBSCRIBER',
        ownerId: 'creator',
        updatedAt: 1,
      })
    );
    // Invite-only reads are launch scope and must survive.
    await assertSucceeds(creator.firestore().doc('groups/owned-group').get());
    await assertSucceeds(viewer.firestore().doc('groups/owned-group').get());

    // --- 5. Vote records --------------------------------------------------

    currentCase = 'dislike writes denied';
    await env.clearFirestore();
    await seed(env, {
      'drops/vote-target': dropData({
        likedBy: {}, likeCount: 0, dislikedBy: {}, dislikeCount: 0,
      }),
    });
    const voteTarget = viewer.firestore().doc('drops/vote-target');
    await assertFails(
      voteTarget.update({dislikeCount: 1, ['dislikedBy.viewer']: true})
    );
    // Inflating the counter on its own is denied as well.
    await assertFails(voteTarget.update({dislikeCount: 5}));
    // Simple likes are launch scope and keep working.
    await assertSucceeds(
      voteTarget.update({likeCount: 1, ['likedBy.viewer']: true})
    );

    currentCase = 'seeded votes denied at create';
    await env.clearFirestore();
    await seed(env, {'users/creator': {role: 'EXPLORER', nsfwEnabled: false}});
    await assertFails(
      creator.firestore().doc('drops/seeded-count').set(dropData({dislikeCount: 3}))
    );
    await assertFails(
      creator.firestore().doc('drops/seeded-map').set(dropData({
        dislikedBy: {viewer: true}, dislikeCount: 1,
      }))
    );
    await assertSucceeds(
      creator.firestore().doc('drops/zeroed').set(dropData({
        dislikedBy: {}, dislikeCount: 0,
      }))
    );

    // Retracting an existing dislike stays legal so clients can clear
    // prototype votes, and so a like still lands on a previously disliked drop.
    currentCase = 'dislike retraction still allowed';
    await env.clearFirestore();
    await seed(env, {
      'drops/retract': dropData({
        likedBy: {}, likeCount: 0, dislikedBy: {viewer: true}, dislikeCount: 1,
      }),
    });
    await assertSucceeds(
      viewer.firestore().doc('drops/retract').update({
        dislikeCount: 0,
        ['dislikedBy.viewer']: deleteField(),
      })
    );

    // Regression guard for FirestoreRepo.setDropLike, which always sends
    // dislikeCount and a dislikedBy delete alongside the like.
    currentCase = 'client like payload still allowed';
    await env.clearFirestore();
    await seed(env, {
      'drops/like-payload': dropData({likedBy: {}, likeCount: 0}),
    });
    await assertSucceeds(
      viewer.firestore().doc('drops/like-payload').update({
        likeCount: 1,
        ['likedBy.viewer']: true,
        dislikeCount: 0,
        ['dislikedBy.viewer']: deleteField(),
      })
    );

    console.log('All deny-deferred-writes rule tests passed.');
  } catch (err) {
    console.error(`Deny-deferred-writes rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
