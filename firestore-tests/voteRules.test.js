const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
  firestore,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'geodrop-test';
const DROP_PATH = 'drops/test-drop';

function baseDropData(overrides = {}) {
  return {
    text: 'Example',
    description: 'Example description',
    lat: 0,
    lng: 0,
    createdBy: 'owner',
    createdAt: 1,
    isDeleted: false,
    dropType: 'COMMUNITY',
    reportCount: 0,
    likedBy: {},
    likeCount: 0,
    ...overrides,
  };
}

async function seedDrop(env, data) {
  await env.withSecurityRulesDisabled(async (context) => {
    await context.firestore().doc(DROP_PATH).set(data);
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
    const authed = env.authenticatedContext('voter');
    const dropRef = authed.firestore().doc(DROP_PATH);

    // Recording a like should succeed when the counter increments.
    await env.clearFirestore();
    await seedDrop(env, baseDropData());
    await assertSucceeds(
      dropRef.update({
        likeCount: 1,
        ['likedBy.voter']: true,
      })
    );

    // Removing a like through a nested delete should succeed.
    await env.clearFirestore();
    await seedDrop(env, baseDropData({
      likedBy: { voter: true },
      likeCount: 1,
    }));
    await assertSucceeds(
      dropRef.update({
        likeCount: 0,
        ['likedBy.voter']: firestore.FieldValue.delete(),
      })
    );

    // Rewriting the entire document without the like should also succeed.
    await env.clearFirestore();
    await seedDrop(env, baseDropData({
      likedBy: { voter: true },
      likeCount: 1,
    }));
    await assertSucceeds(
      dropRef.set(
        baseDropData({
          likedBy: {},
          likeCount: 0,
        })
      )
    );

    // Attempting to like without updating the counter must fail.
    await env.clearFirestore();
    await seedDrop(env, baseDropData());
    await assertFails(
      dropRef.update({
        ['likedBy.voter']: true,
        likeCount: 0,
      })
    );

    // Likewise, removing a like without decrementing the counter must fail.
    await env.clearFirestore();
    await seedDrop(env, baseDropData({
      likedBy: { voter: true },
      likeCount: 3,
    }));
    await assertFails(
      dropRef.update({
        ['likedBy.voter']: firestore.FieldValue.delete(),
        likeCount: 3,
      })
    );

    // Leaving the like unchanged should succeed.
    await env.clearFirestore();
    await seedDrop(env, baseDropData({
      likedBy: { voter: true },
      likeCount: 5,
    }));
    await assertSucceeds(
      dropRef.set(
        baseDropData({
          likedBy: { voter: true },
          likeCount: 5,
        })
      )
    );

    console.log('All like rule tests passed.');
  } catch (err) {
    console.error('Like rule tests failed:', err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();