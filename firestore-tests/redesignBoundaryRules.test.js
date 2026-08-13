const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geo-drop-eec7e';

async function seed(env, documents) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    for (const [documentPath, data] of Object.entries(documents)) {
      await db.doc(documentPath).set(data);
    }
  });
}

function activeExperience(overrides = {}) {
  return {
    schemaVersion: 2,
    code: 'ACTIVE01',
    ownerId: 'owner',
    name: 'Active Experience',
    description: null,
    hostLabel: 'Approved Host',
    startsAt: new Date(Date.now() - 60 * 60 * 1000),
    endsAt: new Date(Date.now() + 60 * 60 * 1000),
    timeZone: 'Pacific/Honolulu',
    defaultRadiusM: 25,
    state: 'PUBLISHED',
    createdAt: new Date(),
    publishedAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

function targetDrop(overrides = {}) {
  return {
    schemaVersion: 1,
    experienceCode: 'ACTIVE01',
    ownerId: 'owner',
    hostLabel: 'Approved Host',
    state: 'PUBLISHED',
    moderationState: 'SAFE',
    lat: 19.7,
    lng: -155.1,
    radiusM: 25,
    contentKind: 'TEXT',
    dropKind: 'STANDARD',
    payloadVersion: 1,
    trailId: null,
    trailStepIndex: null,
    trailTotalSteps: null,
    likeCount: 0,
    createdAt: new Date(),
    publishedAt: new Date(),
    updatedAt: new Date(),
    editedAt: null,
    expiryMode: 'NONE',
    expiresAt: null,
    ...overrides,
  };
}

function stagingUpload(ref, uid, custom = {}) {
  return ref.putString('private staging image', 'raw', {
    contentType: 'image/jpeg',
    customMetadata: {
      ownerId: uid,
      purpose: 'DROP_STAGING',
      ...custom,
    },
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
    const signedOut = env.unauthenticatedContext();
    const guest = env.authenticatedContext('guest', {
      firebase: {sign_in_provider: 'anonymous'},
    });
    const member = env.authenticatedContext('member', {
      firebase: {sign_in_provider: 'password'},
    });
    const nonmember = env.authenticatedContext('nonmember', {
      firebase: {sign_in_provider: 'password'},
    });
    const owner = env.authenticatedContext('owner', {
      firebase: {sign_in_provider: 'password'},
    });
    const suspended = env.authenticatedContext('suspended', {
      suspended: true,
      firebase: {sign_in_provider: 'password'},
    });
    const admin = env.authenticatedContext('admin', {
      admin: true,
      firebase: {sign_in_provider: 'password'},
    });

    currentCase = 'seed target boundary';
    await seed(env, {
      'users/member': {
        role: 'EXPLORER',
        organizerAccessStatus: 'NONE',
        displayName: 'Member',
        nsfwEnabled: false,
      },
      'users/guest': {role: 'EXPLORER', displayName: 'Guest', nsfwEnabled: false},
      'users/owner': {
        role: 'BUSINESS',
        organizerAccessStatus: 'APPROVED',
        displayName: 'Owner',
        businessName: 'Approved Host',
        businessCategories: ['TOUR'],
        nsfwEnabled: false,
      },
      'users/suspended': {role: 'EXPLORER', displayName: 'Suspended'},
      'creatorProfiles/owner': {
        schemaVersion: 1,
        hostLabel: 'Approved Host',
        username: null,
        organizationName: 'Approved Host',
        updatedAt: new Date(),
      },
      'groups/ACTIVE01': activeExperience(),
      'users/member/groups/ACTIVE01': {
        schemaVersion: 2,
        code: 'ACTIVE01',
        ownerId: 'owner',
        role: 'SUBSCRIBER',
        joinedAt: new Date(),
        updatedAt: new Date(),
      },
      'users/guest/groups/ACTIVE01': {
        schemaVersion: 2,
        code: 'ACTIVE01',
        ownerId: 'owner',
        role: 'SUBSCRIBER',
        joinedAt: new Date(),
        updatedAt: new Date(),
      },
      'experienceDrops/safe': targetDrop(),
      'experienceDrops/pending': targetDrop({moderationState: 'PENDING'}),
      'dropPayloads/safe': {
        schemaVersion: 1,
        dropId: 'safe',
        experienceCode: 'ACTIVE01',
        ownerId: 'owner',
        currentVersion: 1,
      },
      'dropPayloads/safe/versions/1': {
        schemaVersion: 1,
        title: 'Private payload',
        body: 'Must never be directly visible',
      },
      'users/member/unlocks/safe': {
        schemaVersion: 1,
        receiptId: 'receipt',
        dropId: 'safe',
        experienceCode: 'ACTIVE01',
        payloadVersion: 1,
        source: 'SERVER_PROXIMITY_V1',
      },
      'users/member/rewardReceipts/safe': {
        schemaVersion: 1,
        dropId: 'safe',
        code: 'PRIVATE-CODE',
        state: 'ISSUED',
      },
      'users/member/trailProgress/trail': {
        schemaVersion: 1,
        experienceCode: 'ACTIVE01',
        trailId: 'trail',
      },
      'groups/ACTIVE01/trails/trail': {
        schemaVersion: 1,
        title: 'Main Trail',
        dropIds: ['safe'],
        isMain: true,
        state: 'ACTIVE',
        version: 1,
      },
      'groups/ACTIVE01/analytics/summary': {
        schemaVersion: 2,
        unlocks: 1,
      },
      'rewards/safe': {schemaVersion: 1, ownerId: 'owner'},
      'rewards/safe/codes/code': {code: 'PRIVATE-CODE', state: 'ISSUED'},
      'analyticsEvents/event': {eventName: 'unlock_succeeded'},
      'organizerApplications/member': {status: 'PENDING'},
      'organizerApplicationTokens/digest': {uid: 'member'},
      'safetyReports/report': {reporterId: 'member'},
      'callableRateLimits/private': {scope: 'submitReport', count: 1},
      'feedbackResponses/feedback': {actorKey: 'private'},
    });

    currentCase = 'safe public creator projection';
    await assertFails(signedOut.firestore().doc('creatorProfiles/owner').get());
    await assertSucceeds(member.firestore().doc('creatorProfiles/owner').get());
    await assertFails(member.firestore().collection('creatorProfiles').get());
    await assertFails(member.firestore().doc('creatorProfiles/member').set({hostLabel: 'Fake'}));

    currentCase = 'experience capability and membership';
    await assertSucceeds(owner.firestore().doc('groups/ACTIVE01').get());
    await assertSucceeds(member.firestore().doc('groups/ACTIVE01').get());
    await assertFails(nonmember.firestore().doc('groups/ACTIVE01').get());
    await assertFails(member.firestore().collection('groups').get());

    currentCase = 'target discovery visibility';
    await assertSucceeds(member.firestore().doc('experienceDrops/safe').get());
    await assertSucceeds(guest.firestore().doc('experienceDrops/safe').get());
    await assertSucceeds(owner.firestore().doc('experienceDrops/pending').get());
    await assertFails(member.firestore().doc('experienceDrops/pending').get());
    await assertFails(nonmember.firestore().doc('experienceDrops/safe').get());
    await assertFails(signedOut.firestore().doc('experienceDrops/safe').get());
    await assertFails(suspended.firestore().doc('experienceDrops/safe').get());
    await assertSucceeds(
      member.firestore().collection('experienceDrops')
        .where('experienceCode', '==', 'ACTIVE01')
        .where('state', '==', 'PUBLISHED')
        .where('moderationState', '==', 'SAFE')
        .get()
    );
    await assertFails(member.firestore().collection('experienceDrops').get());

    currentCase = 'server-owned target resources';
    const deniedWrites = [
      ['experienceDrops/forged', targetDrop()],
      ['dropPayloads/forged', {ownerId: 'member'}],
      ['users/member/unlocks/forged', {source: 'SERVER_PROXIMITY_V1'}],
      ['users/member/rewardReceipts/forged', {code: 'STOLEN'}],
      ['users/member/trailProgress/forged', {currentStepIndex: 99}],
      ['users/member/likes/forged', {likedAt: new Date()}],
      ['users/member/blockedHosts/owner', {hostId: 'owner'}],
      ['rewards/forged', {ownerId: 'member'}],
      ['analyticsEvents/forged', {eventName: 'unlock_succeeded'}],
      ['organizerApplications/member', {status: 'APPROVED'}],
      ['organizerApplicationTokens/forged', {uid: 'member'}],
      ['safetyReports/forged', {reporterId: 'member'}],
      ['callableRateLimits/forged', {scope: 'submitReport', count: 0}],
      ['feedbackResponses/forged', {actorKey: 'member'}],
    ];
    for (const [documentPath, value] of deniedWrites) {
      await assertFails(member.firestore().doc(documentPath).set(value));
      await assertFails(admin.firestore().doc(documentPath).set(value));
    }

    currentCase = 'private receipt reads';
    await assertSucceeds(member.firestore().doc('users/member/unlocks/safe').get());
    await assertSucceeds(member.firestore().doc('users/member/rewardReceipts/safe').get());
    await assertSucceeds(member.firestore().doc('users/member/trailProgress/trail').get());
    await assertFails(nonmember.firestore().doc('users/member/unlocks/safe').get());
    await assertFails(owner.firestore().doc('users/member/rewardReceipts/safe').get());
    await assertFails(member.firestore().doc('dropPayloads/safe/versions/1').get());
    await assertFails(owner.firestore().doc('dropPayloads/safe/versions/1').get());
    await assertFails(admin.firestore().doc('analyticsEvents/event').get());
    await assertFails(member.firestore().doc('callableRateLimits/private').get());
    await assertFails(admin.firestore().doc('callableRateLimits/private').get());

    currentCase = 'owner-only Trail and Results';
    await assertSucceeds(owner.firestore().doc('groups/ACTIVE01/trails/trail').get());
    await assertFails(member.firestore().doc('groups/ACTIVE01/trails/trail').get());
    await assertSucceeds(owner.firestore().doc('groups/ACTIVE01/analytics/summary').get());
    await assertFails(member.firestore().doc('groups/ACTIVE01/analytics/summary').get());

    currentCase = 'organizer workflow fields cannot self-promote';
    await assertFails(member.firestore().doc('users/member').update({
      role: 'BUSINESS',
      organizerAccessStatus: 'APPROVED',
    }));
    await assertFails(member.firestore().doc('users/member').update({
      organizerAccessStatus: 'APPROVED',
    }));
    await assertSucceeds(member.firestore().doc('users/member').update({displayName: 'Member 2'}));

    currentCase = 'private staging and payload storage';
    await assertSucceeds(stagingUpload(
      member.storage().ref('drop-upload-staging/member/upload-1'),
      'member'
    ));
    await assertFails(stagingUpload(
      guest.storage().ref('drop-upload-staging/guest/upload-1'),
      'guest'
    ));
    await assertFails(stagingUpload(
      member.storage().ref('drop-upload-staging/member/upload-2'),
      'owner'
    ));
    await assertFails(stagingUpload(
      member.storage().ref('drop-upload-staging/member/upload-3'),
      'member',
      {accessLevel: 'PUBLIC'}
    ));
    await env.withSecurityRulesDisabled(async (context) => {
      await context.storage().ref('drop-payloads/safe/1/asset')
        .putString('private payload image', 'raw', {contentType: 'image/jpeg'});
    });
    await assertFails(member.storage().ref('drop-payloads/safe/1/asset').getMetadata());
    await assertFails(owner.storage().ref('drop-payloads/safe/1/asset').getMetadata());

    console.log('All R2 redesign boundary rule tests passed.');
  } catch (error) {
    console.error(`R2 redesign boundary rule test failed at ${currentCase}:`, error);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
