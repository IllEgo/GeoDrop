const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'kithe-r5-p-rules';
const RULES_PATH = path.join(__dirname, '..', 'deployment', 'r5-p', 'firestore.rules');

async function seed(env, documents) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    for (const [documentPath, data] of Object.entries(documents)) {
      await db.doc(documentPath).set(data);
    }
  });
}

const activeExperience = (overrides = {}) => ({
  schemaVersion: 2,
  code: 'ACTIVE01',
  ownerId: 'fixture-owner',
  name: 'Kithe release rehearsal',
  description: 'Non-secret test content.',
  hostLabel: 'Kithe Test Host',
  startsAt: new Date(Date.now() - 60 * 60 * 1000),
  endsAt: new Date(Date.now() + 60 * 60 * 1000),
  timeZone: 'Pacific/Honolulu',
  defaultRadiusM: 25,
  state: 'PUBLISHED',
  createdAt: new Date(),
  publishedAt: new Date(),
  updatedAt: new Date(),
  ...overrides,
});

const safeDrop = (overrides = {}) => ({
  schemaVersion: 1,
  experienceCode: 'ACTIVE01',
  ownerId: 'fixture-owner',
  hostLabel: 'Kithe Test Host',
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
});

(async () => {
  const env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {rules: fs.readFileSync(RULES_PATH, 'utf8')},
  });

  try {
    const member = env.authenticatedContext('member', {
      firebase: {sign_in_provider: 'password'},
    }).firestore();
    const stranger = env.authenticatedContext('stranger', {
      firebase: {sign_in_provider: 'password'},
    }).firestore();
    const guest = env.authenticatedContext('guest', {
      firebase: {sign_in_provider: 'anonymous'},
    }).firestore();
    const owner = env.authenticatedContext('fixture-owner', {
      firebase: {sign_in_provider: 'password'},
    }).firestore();

    await seed(env, {
      'users/member': {role: 'EXPLORER', createdAt: new Date()},
      'users/stranger': {role: 'EXPLORER', createdAt: new Date()},
      'users/member/groups/ACTIVE01': {
        schemaVersion: 2,
        code: 'ACTIVE01',
        ownerId: 'fixture-owner',
        role: 'SUBSCRIBER',
        joinedAt: new Date(),
        updatedAt: new Date(),
      },
      'users/member/unlocks/drop-safe': {dropId: 'drop-safe'},
      'users/member/rewardReceipts/drop-safe': {dropId: 'drop-safe'},
      'users/member/trailProgress/trail-1': {experienceCode: 'ACTIVE01'},
      'users/member/blockedHosts/fixture-owner': {hostId: 'fixture-owner'},
      'users/member/legalAcceptances/policy-v1': {policyVersion: 'policy-v1'},
      'groups/ACTIVE01': activeExperience(),
      'groups/ENDED001': activeExperience({
        code: 'ENDED001',
        startsAt: new Date(Date.now() - 2 * 60 * 60 * 1000),
        endsAt: new Date(Date.now() - 60 * 60 * 1000),
      }),
      'experienceDrops/drop-safe': safeDrop(),
      'experienceDrops/drop-pending': safeDrop({moderationState: 'PENDING'}),
      'dropPayloads/drop-safe': {currentVersion: 1},
      'dropPayloads/drop-safe/versions/1': {title: 'private payload'},
      'analyticsEvents/event-1': {eventName: 'invite_link_opened'},
    });

    await assertSucceeds(member.doc('users/member').get());
    await assertFails(member.doc('users/stranger').get());
    await assertSucceeds(guest.doc('users/guest').set({
      role: 'EXPLORER',
      displayName: null,
      createdAt: new Date(),
    }));
    await assertFails(guest.doc('users/guest').set({role: 'BUSINESS'}, {merge: true}));
    await assertSucceeds(member.doc('users/member').set({displayName: 'Member'}, {merge: true}));
    await assertFails(member.doc('users/member').set({role: 'BUSINESS'}, {merge: true}));

    await assertSucceeds(member.doc('groups/ACTIVE01').get());
    await assertSucceeds(owner.doc('groups/ACTIVE01').get());
    await assertFails(stranger.doc('groups/ACTIVE01').get());
    await assertFails(member.collection('groups').get());

    await assertSucceeds(member.doc('experienceDrops/drop-safe').get());
    await assertFails(stranger.doc('experienceDrops/drop-safe').get());
    await assertFails(member.doc('experienceDrops/drop-pending').get());
    await assertSucceeds(
      member.collection('experienceDrops')
        .where('experienceCode', '==', 'ACTIVE01')
        .where('state', '==', 'PUBLISHED')
        .where('moderationState', '==', 'SAFE')
        .orderBy('publishedAt', 'desc')
        .get()
    );

    for (const readable of [
      'users/member/groups/ACTIVE01',
      'users/member/unlocks/drop-safe',
      'users/member/rewardReceipts/drop-safe',
      'users/member/trailProgress/trail-1',
      'users/member/blockedHosts/fixture-owner',
      'users/member/legalAcceptances/policy-v1',
    ]) {
      await assertSucceeds(member.doc(readable).get());
      await assertFails(stranger.doc(readable).get());
      await assertFails(member.doc(readable).set({tampered: true}, {merge: true}));
    }

    await assertFails(member.doc('dropPayloads/drop-safe').get());
    await assertFails(member.doc('dropPayloads/drop-safe/versions/1').get());
    await assertFails(member.doc('analyticsEvents/event-1').get());
    await assertFails(member.doc('messages/message-1').set({text: 'blocked'}));

    console.log('All R5-P deployment rule tests passed.');
  } finally {
    await env.cleanup();
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
