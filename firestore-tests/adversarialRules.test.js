const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'geodrop-adversarial-test';

function dropData(overrides = {}) {
  return {
    text: 'Public drop',
    lat: 1,
    lng: 1,
    createdBy: 'owner',
    createdAt: 1,
    isDeleted: false,
    visibility: 'PUBLIC',
    isNsfw: false,
    dropType: 'COMMUNITY',
    ...overrides,
  };
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
  });

  try {
    const anonymous = env.unauthenticatedContext().firestore();
    const viewer = env.authenticatedContext('viewer').firestore();
    const owner = env.authenticatedContext('owner').firestore();
    const attacker = env.authenticatedContext('attacker').firestore();

    // Public, active, safe drops remain readable without an account.
    currentCase = 'public direct read';
    await env.clearFirestore();
    await seed(env, {'drops/public': dropData()});
    await assertSucceeds(anonymous.doc('drops/public').get());

    // Deleted and NSFW content must not be directly readable anonymously.
    currentCase = 'anonymous visibility denial';
    await env.clearFirestore();
    await seed(env, {
      'drops/deleted': dropData({isDeleted: true}),
      'drops/nsfw': dropData({isNsfw: true}),
    });
    await assertFails(anonymous.doc('drops/deleted').get());
    await assertFails(anonymous.doc('drops/nsfw').get());

    // Collection listing must not bypass per-document visibility controls.
    await assertFails(anonymous.collection('drops').get());

    // An authenticated viewer without the NSFW preference cannot read NSFW content.
    currentCase = 'authenticated NSFW denial';
    await env.clearFirestore();
    await seed(env, {
      'users/viewer': {displayName: 'Viewer', role: 'EXPLORER', nsfwEnabled: false},
      'drops/nsfw': dropData({isNsfw: true}),
    });
    await assertFails(viewer.doc('drops/nsfw').get());

    // Authenticated collection reads must also prove that their query excludes
    currentCase = 'safe public query';
    // deleted and non-opted-in NSFW documents.
    await env.clearFirestore();
    await seed(env, {
      'users/viewer': {displayName: 'Viewer', role: 'EXPLORER', nsfwEnabled: false},
      'drops/public': dropData({isNsfw: false}),
      'drops/deleted': dropData({isDeleted: true, isNsfw: false}),
      'drops/nsfw': dropData({isNsfw: true}),
    });
    await assertFails(viewer.collection('drops').get());
    await assertSucceeds(
      viewer.collection('drops')
        .where('isDeleted', '==', false)
        .where('isNsfw', '==', false)
        .where('visibility', '==', 'PUBLIC')
        .get()
    );

    // Legacy opt-in values cannot bypass the pilot guard, and clients cannot
    // enable the preference or create new mature-content drops.
    currentCase = 'NSFW pilot guard';
    await env.clearFirestore();
    await seed(env, {
      'users/viewer': {displayName: 'Viewer', role: 'EXPLORER', nsfwEnabled: true},
      'drops/nsfw': dropData({isNsfw: true}),
      'drops/deleted': dropData({isDeleted: true}),
    });
    await assertFails(viewer.doc('drops/nsfw').get());
    await assertFails(viewer.doc('users/viewer').update({nsfwEnabled: true}));
    await assertSucceeds(viewer.doc('users/viewer').update({nsfwEnabled: false}));
    await assertFails(viewer.doc('drops/new-nsfw').set(dropData({
      createdBy: 'viewer',
      isNsfw: true,
    })));
    await assertFails(viewer.doc('drops/deleted').get());
    await assertSucceeds(owner.doc('drops/deleted').get());

    // A collector retains access to a deleted drop they previously collected.
    currentCase = 'collector deleted access';
    await env.clearFirestore();
    await seed(env, {
      'drops/collected-deleted': dropData({
        isDeleted: true,
        collectedBy: {viewer: true},
      }),
    });
    await assertSucceeds(viewer.doc('drops/collected-deleted').get());

    // Private/group-scoped content must not be readable by a non-member.
    currentCase = 'private group access';
    await env.clearFirestore();
    await seed(env, {
      'groups/private-group': {ownerId: 'owner', createdAt: 1, updatedAt: 1},
      'drops/private': dropData({visibility: 'GROUP', groupCode: 'private-group'}),
    });
    await assertFails(viewer.doc('drops/private').get());
    await assertFails(viewer.doc('groups/private-group').get());
    await assertSucceeds(owner.doc('groups/private-group').get());
    await assertFails(owner.collection('groups').get());
    await assertFails(
      attacker.doc('users/attacker/groups/private-group').set({
        code: 'private-group',
        role: 'SUBSCRIBER',
        ownerId: 'owner',
        updatedAt: 1,
      })
    );
    await seed(env, {
      'users/viewer/groups/private-group': {
        code: 'private-group',
        role: 'SUBSCRIBER',
        ownerId: 'owner',
        updatedAt: 1,
      },
    });
    await assertSucceeds(viewer.doc('groups/private-group').get());
    await assertSucceeds(viewer.doc('drops/private').get());
    await assertSucceeds(
      viewer.collection('drops')
        .where('isDeleted', '==', false)
        .where('isNsfw', '==', false)
        .where('visibility', '==', 'GROUP')
        .where('groupCode', '==', 'private-group')
        .get()
    );

    // New group drops require membership and remain text-only for the pilot.
    currentCase = 'private group creation';
    await env.clearFirestore();
    await seed(env, {
      'users/viewer': {displayName: 'Viewer', role: 'EXPLORER', nsfwEnabled: false},
      'users/attacker': {displayName: 'Attacker', role: 'EXPLORER', nsfwEnabled: false},
      'groups/private-group': {ownerId: 'owner', createdAt: 1, updatedAt: 1},
      'users/viewer/groups/private-group': {
        code: 'private-group',
        role: 'SUBSCRIBER',
        ownerId: 'owner',
        updatedAt: 1,
      },
    });
    await assertSucceeds(
      viewer.doc('drops/group-text').set(dropData({
        createdBy: 'viewer',
        visibility: 'GROUP',
        groupCode: 'private-group',
        contentType: 'TEXT',
      }))
    );
    await assertFails(
      attacker.doc('drops/group-nonmember').set(dropData({
        createdBy: 'attacker',
        visibility: 'GROUP',
        groupCode: 'private-group',
        contentType: 'TEXT',
      }))
    );
    await assertFails(
      viewer.doc('drops/group-media').set(dropData({
        createdBy: 'viewer',
        visibility: 'GROUP',
        groupCode: 'private-group',
        contentType: 'PHOTO',
        mediaUrl: 'https://example.invalid/private.jpg',
      }))
    );

    // A username may be claimed once, but an existing mapping cannot be taken over.
    currentCase = 'username ownership';
    await env.clearFirestore();
    await assertSucceeds(
      viewer.doc('usernames/new.viewer').set({userId: 'viewer'})
    );
    await env.clearFirestore();
    await seed(env, {'usernames/taken.name': {userId: 'owner'}});
    await assertFails(
      attacker.doc('usernames/taken.name').set({userId: 'attacker'})
    );

    // Users may edit benign profile data, but cannot grant themselves a new role.
    currentCase = 'role escalation';
    await env.clearFirestore();
    await seed(env, {
      'users/viewer': {displayName: 'Viewer', username: 'viewer', role: 'EXPLORER'},
    });
    await assertSucceeds(viewer.doc('users/viewer').update({displayName: 'New name'}));
    await assertFails(viewer.doc('users/viewer').update({role: 'BUSINESS'}));
    await assertFails(viewer.doc('users/viewer').update({role: 'ADMIN'}));

    // Reports cannot be anonymous or carry arbitrary privileged/spoofed fields.
    currentCase = 'report validation';
    await env.clearFirestore();
    await seed(env, {'drops/public': dropData()});
    await assertFails(
      anonymous.doc('reports/anonymous-report').set({
        dropId: 'public',
        reportedBy: 'viewer',
        reportedAt: 1,
        reasonCodes: ['spam'],
        status: 'pending',
      })
    );
    await assertSucceeds(
      viewer.doc('reports/valid-report').set({
        dropId: 'public',
        reportedBy: 'viewer',
        reportedAt: 1,
        reasonCodes: ['spam'],
        status: 'pending',
      })
    );
    await assertFails(
      viewer.doc('reports/spoofed-report').set({
        dropId: 'public',
        reportedBy: 'owner',
        reportedAt: 1,
        reasonCodes: ['spam'],
        status: 'pending',
        moderationStatus: 'APPROVED',
        secret: 'unexpected',
      })
    );
    await assertFails(
      viewer.doc('reports/valid-report').update({moderationStatus: 'APPROVED'})
    );

    // Notification tokens are private to their owning user document.
    currentCase = 'notification token privacy';
    await env.clearFirestore();
    await assertFails(
      attacker.doc('users/viewer/notificationTokens/device').set({
        token: 'stolen-token',
        platform: 'android',
        updatedAt: 1,
      })
    );
    await assertSucceeds(
      viewer.doc('users/viewer/notificationTokens/device').set({
        token: 'own-token',
        platform: 'android',
        updatedAt: 1,
      })
    );
    await assertFails(attacker.doc('users/viewer/notificationTokens/device').get());

    console.log('All adversarial Firestore rule tests passed.');
  } catch (err) {
    console.error(`Adversarial Firestore rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
