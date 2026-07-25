const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// Cross-service Storage -> Firestore lookups in the Emulator Suite resolve
// within the project selected by `firebase emulators:exec`. Keep this aligned
// with .firebaserc instead of using a synthetic per-test project ID.
const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geo-drop-eec7e';

function upload(ref, contentType, uploaderUid, metadataOverrides = {}) {
  const metadata = {contentType};
  if (uploaderUid !== undefined) {
    metadata.customMetadata = {
      ownerId: uploaderUid,
      accessLevel: 'PRIVATE',
      safetyStatus: 'PENDING',
      ...metadataOverrides,
    };
  }
  return ref.putString('test media bytes', 'raw', metadata);
}

async function seedObject(env, objectPath, customMetadata = {}) {
  await env.withSecurityRulesDisabled(async (context) => {
    await upload(
      context.storage().ref(objectPath),
      'image/jpeg',
      customMetadata.ownerId ?? 'owner',
      customMetadata
    );
  });
}

async function seedDrop(env, dropId, data) {
  await env.withSecurityRulesDisabled(async (context) => {
    await context.firestore().doc(`drops/${dropId}`).set(data);
  });
}

function publicDrop(mediaStoragePath, overrides = {}) {
  return {
    createdBy: 'owner',
    isDeleted: false,
    isNsfw: false,
    visibility: 'PUBLIC',
    mediaStoragePath,
    ...overrides,
  };
}

(async () => {
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
    const anonymous = env.unauthenticatedContext().storage();
    const owner = env.authenticatedContext('owner').storage();
    const attacker = env.authenticatedContext('attacker').storage();

    // Uploads are always owner-bound, private, and pending. Clients cannot
    // declare their own upload public or safe.
    await assertFails(
      upload(anonymous.ref('drops/photos/anonymous.jpg'), 'image/jpeg', 'owner')
    );
    await assertSucceeds(
      upload(owner.ref('drops/photos/owned.jpg'), 'image/jpeg', 'owner')
    );
    await assertFails(
      upload(owner.ref('drops/photos/wrong-type.jpg'), 'audio/mpeg', 'owner')
    );
    await assertFails(
      upload(owner.ref('drops/photos/no-owner.jpg'), 'image/jpeg')
    );
    await assertFails(
      upload(owner.ref('drops/photos/spoofed-owner.jpg'), 'image/jpeg', 'attacker')
    );
    await assertFails(
      upload(owner.ref('drops/photos/spoofed-public.jpg'), 'image/jpeg', 'owner', {
        accessLevel: 'PUBLIC',
        safetyStatus: 'SAFE',
        dropId: 'spoofed',
      })
    );

    // Pending/private media is readable only by its recorded owner.
    await seedObject(env, 'drops/photos/private.jpg', {
      ownerId: 'owner',
      accessLevel: 'PRIVATE',
      safetyStatus: 'PENDING',
    });
    await assertSucceeds(owner.ref('drops/photos/private.jpg').getMetadata());
    await assertFails(attacker.ref('drops/photos/private.jpg').getMetadata());
    await assertFails(anonymous.ref('drops/photos/private.jpg').getMetadata());

    // A trusted backend may publish media only when its metadata points to the
    // matching active, public, safe Firestore drop. Guest behavior is explicit.
    const publicPath = 'drops/photos/public.jpg';
    await seedObject(env, publicPath, {
      ownerId: 'owner',
      accessLevel: 'PUBLIC',
      safetyStatus: 'SAFE',
      dropId: 'public',
    });
    await seedDrop(env, 'public', publicDrop(publicPath));
    await assertSucceeds(anonymous.ref(publicPath).getMetadata());
    await assertSucceeds(attacker.ref(publicPath).getMetadata());

    // Forged safe metadata cannot expose group, unsafe, deleted, or mismatched
    // media because Storage Rules also check the canonical Firestore drop.
    const groupPath = 'drops/photos/group.jpg';
    await seedObject(env, groupPath, {
      ownerId: 'owner',
      accessLevel: 'PUBLIC',
      safetyStatus: 'SAFE',
      dropId: 'group',
    });
    await seedDrop(env, 'group', publicDrop(groupPath, {
      visibility: 'GROUP',
      groupCode: 'PRIVATE-GROUP',
    }));
    await assertFails(attacker.ref(groupPath).getMetadata());
    await assertFails(anonymous.ref(groupPath).getMetadata());
    await assertSucceeds(owner.ref(groupPath).getMetadata());

    const unsafePath = 'drops/photos/unsafe.jpg';
    await seedObject(env, unsafePath, {
      ownerId: 'owner',
      accessLevel: 'PUBLIC',
      safetyStatus: 'SAFE',
      dropId: 'unsafe',
    });
    await seedDrop(env, 'unsafe', publicDrop(unsafePath, {isNsfw: true}));
    await assertFails(attacker.ref(unsafePath).getMetadata());
    await assertFails(anonymous.ref(unsafePath).getMetadata());

    const deletedPath = 'drops/photos/deleted.jpg';
    await seedObject(env, deletedPath, {
      ownerId: 'owner',
      accessLevel: 'PUBLIC',
      safetyStatus: 'SAFE',
      dropId: 'deleted',
    });
    await seedDrop(env, 'deleted', publicDrop(deletedPath, {isDeleted: true}));
    await assertFails(attacker.ref(deletedPath).getMetadata());

    const mismatchPath = 'drops/photos/mismatch.jpg';
    await seedObject(env, mismatchPath, {
      ownerId: 'owner',
      accessLevel: 'PUBLIC',
      safetyStatus: 'SAFE',
      dropId: 'mismatch',
    });
    await seedDrop(env, 'mismatch', publicDrop('drops/photos/another.jpg'));
    await assertFails(attacker.ref(mismatchPath).getMetadata());

    // Existing objects are immutable and only their recorded owner may delete.
    await seedObject(env, 'drops/photos/delete-check.jpg', {
      ownerId: 'owner',
      accessLevel: 'PRIVATE',
      safetyStatus: 'PENDING',
    });
    await assertFails(
      upload(owner.ref('drops/photos/delete-check.jpg'), 'image/jpeg', 'owner')
    );
    await assertFails(attacker.ref('drops/photos/delete-check.jpg').delete());
    await assertSucceeds(owner.ref('drops/photos/delete-check.jpg').delete());

    // Unsupported paths are denied even to authenticated users.
    await assertFails(
      upload(owner.ref('avatars/owner.jpg'), 'image/jpeg', 'owner')
    );

    // Account exports are intentionally available only through short-lived
    // backend signed URLs, never through the Firebase client SDK.
    await env.withSecurityRulesDisabled(async (context) => {
      await context.storage().ref('account-exports/owner/export.json')
        .putString('{"export":true}', 'raw', {contentType: 'application/json'});
    });
    await assertFails(owner.ref('account-exports/owner/export.json').getMetadata());

    console.log('All adversarial Storage rule tests passed.');
  } catch (err) {
    console.error('Adversarial Storage rule tests failed:', err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
