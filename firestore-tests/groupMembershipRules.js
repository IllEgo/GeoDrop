const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'geodrop-test';
const GROUP_PATH = 'groups/test-group';
const USER_ID = 'member';
const MEMBERSHIP_PATH = `users/${USER_ID}/groups/test-group`;

function baseMembershipData(overrides = {}) {
  return {
    code: 'test-group',
    role: 'SUBSCRIBER',
    ownerId: 'owner-123',
    updatedAt: 1,
    ...overrides,
  };
}

async function seedGroup(env, data) {
  await env.withSecurityRulesDisabled(async (context) => {
    await context.firestore().doc(GROUP_PATH).set(data);
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
    const authed = env.authenticatedContext(USER_ID);
    const membershipRef = authed.firestore().doc(MEMBERSHIP_PATH);

    // Subscribing to an existing group should succeed when the owner matches.
    await env.clearFirestore();
    await seedGroup(env, {
      ownerId: 'owner-123',
      createdAt: 1,
      updatedAt: 1,
    });
    await assertSucceeds(
      membershipRef.set(
        baseMembershipData({
          role: 'SUBSCRIBER',
          updatedAt: 10,
        })
      )
    );

    // Subscribing to a non-existent group should fail.
    await env.clearFirestore();
    await assertFails(
      membershipRef.set(
        baseMembershipData({
          updatedAt: 20,
        })
      )
    );

    // Subscribing with an owner that differs from the group document should fail.
    await env.clearFirestore();
    await seedGroup(env, {
      ownerId: 'different-owner',
      createdAt: 1,
      updatedAt: 1,
    });
    await assertFails(
      membershipRef.set(
        baseMembershipData({
          ownerId: 'owner-123',
          updatedAt: 30,
        })
      )
    );

    // Subscribing without providing an owner should fail when the group doesn't exist.
    await env.clearFirestore();
    const ownerlessMissingGroup = baseMembershipData({
      updatedAt: 35,
    });
    delete ownerlessMissingGroup.ownerId;
    await assertFails(membershipRef.set(ownerlessMissingGroup));

    // Subscribing without providing an owner should succeed when the group exists.
    await env.clearFirestore();
    await seedGroup(env, {
      ownerId: 'owner-123',
      createdAt: 1,
      updatedAt: 1,
    });
    const ownerlessMembership = baseMembershipData({
      updatedAt: 40,
    });
    delete ownerlessMembership.ownerId;
    await assertSucceeds(membershipRef.set(ownerlessMembership));

    console.log('All group membership rule tests passed.');
  } catch (err) {
    console.error('Group membership rule tests failed:', err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();