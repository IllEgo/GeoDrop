const fs = require('fs');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
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
    voteMap: {},
    upvoteCount: 0,
    downvoteCount: 0,
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
      rules: fs.readFileSync(require('path').join(__dirname, '..', 'firestore.rules'), 'utf8'),
    },
  });

  try {
    const authed = env.authenticatedContext('voter');
    const dropRef = authed.firestore().doc(DROP_PATH);

    // Removing a vote when the aggregate count is already 0 should clamp and succeed.
    await env.clearFirestore();
    await seedDrop(env, baseDropData({
      voteMap: { voter: 1 },
      upvoteCount: 0,
      downvoteCount: 0,
    }));
    await assertSucceeds(
      dropRef.set(
        baseDropData({
          voteMap: {},
          upvoteCount: 0,
          downvoteCount: 0,
        })
      )
    );

    // Flipping directly between upvote and downvote requires both counters to update.
    await env.clearFirestore();
    await seedDrop(env, baseDropData({
      voteMap: { voter: 1 },
      upvoteCount: 3,
      downvoteCount: 2,
    }));
    await assertSucceeds(
      dropRef.set(
        baseDropData({
          voteMap: { voter: -1 },
          upvoteCount: 2,
          downvoteCount: 3,
        })
      )
    );

    // Attempting to flip without adjusting counters must fail.
    await env.clearFirestore();
    await seedDrop(env, baseDropData({
      voteMap: { voter: 1 },
      upvoteCount: 3,
      downvoteCount: 2,
    }));
    await assertFails(
      dropRef.set(
        baseDropData({
          voteMap: { voter: -1 },
          upvoteCount: 3,
          downvoteCount: 2,
        })
      )
    );

    // Leaving the vote unchanged should succeed without touching counters.
    await env.clearFirestore();
    await seedDrop(env, baseDropData({
      voteMap: { voter: -1 },
      upvoteCount: 1,
      downvoteCount: 4,
    }));
    await assertSucceeds(
      dropRef.set(
        baseDropData({
          voteMap: { voter: -1 },
          upvoteCount: 1,
          downvoteCount: 4,
        })
      )
    );

    console.log('All vote rule tests passed.');
  } catch (err) {
    console.error('Vote rule tests failed:', err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();