const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'geodrop-test';
const GROUP_PATH = 'groups/TEST-GROUP';
const MEMBER_ID = 'member';
const OWNER_ID = 'owner-123';
const MEMBERSHIP_PATH = `users/${MEMBER_ID}/groups/TEST-GROUP`;

async function seed(env, documents) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    for (const [documentPath, data] of Object.entries(documents)) {
      await db.doc(documentPath).set(data);
    }
  });
}

(async () => {
  const env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8'),
    },
  });

  try {
    const member = env.authenticatedContext(MEMBER_ID).firestore();
    const owner = env.authenticatedContext(OWNER_ID).firestore();
    const attacker = env.authenticatedContext('attacker').firestore();

    await env.clearFirestore();
    await seed(env, {
      [GROUP_PATH]: {
        ownerId: OWNER_ID,
        createdAt: 1,
        updatedAt: 1,
      },
      [MEMBERSHIP_PATH]: {
        code: 'TEST-GROUP',
        role: 'SUBSCRIBER',
        ownerId: OWNER_ID,
        updatedAt: 1,
      },
    });

    // Only an existing owner or member can resolve a known group code.
    await assertSucceeds(owner.doc(GROUP_PATH).get());
    await assertSucceeds(member.doc(GROUP_PATH).get());
    await assertFails(attacker.doc(GROUP_PATH).get());

    // Group-code enumeration is denied even to owners and members.
    await assertFails(owner.collection('groups').get());
    await assertFails(member.collection('groups').get());

    // Membership lifecycle is callable-only. A client cannot self-enroll,
    // elevate its role, remove the server record, or create a group directly.
    const attackerMembership = attacker.doc('users/attacker/groups/TEST-GROUP');
    await assertFails(attackerMembership.set({
      code: 'TEST-GROUP',
      role: 'SUBSCRIBER',
      ownerId: OWNER_ID,
      updatedAt: 2,
    }));
    await assertFails(member.doc(MEMBERSHIP_PATH).update({role: 'OWNER'}));
    await assertFails(member.doc(MEMBERSHIP_PATH).delete());
    await assertFails(attacker.doc('groups/ATTACKER-GROUP').set({
      ownerId: 'attacker',
      createdAt: 2,
      updatedAt: 2,
    }));

    // Users retain read access to only their own membership documents.
    await assertSucceeds(member.doc(MEMBERSHIP_PATH).get());
    await assertFails(attacker.doc(MEMBERSHIP_PATH).get());

    console.log('All group membership rule tests passed.');
  } catch (err) {
    console.error('Group membership rule tests failed:', err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
