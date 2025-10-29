const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'geodrop-test';
const USER_ID = 'collector';
const INVENTORY_PATH = `users/${USER_ID}/inventory/test-drop`;

function baseInventoryData(overrides = {}) {
  return {
    id: 'test-drop',
    text: 'Example drop',
    dropType: 'COMMUNITY',
    collectedAt: 1,
    likeCount: 0,
    isLiked: false,
    dislikeCount: 0,
    isDisliked: false,
    state: 'COLLECTED',
    updatedAt: 1,
    ...overrides,
  };
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
    const inventoryRef = authed.firestore().doc(INVENTORY_PATH);

    // Writing dislike metadata with correct types should succeed.
    await env.clearFirestore();
    await assertSucceeds(inventoryRef.set(baseInventoryData()));

    // Writing a dislike count with the wrong type must fail.
    await env.clearFirestore();
    await assertFails(
      inventoryRef.set(
        baseInventoryData({
          dislikeCount: '0',
        })
      )
    );

    // Writing a dislike flag with the wrong type must fail.
    await env.clearFirestore();
    await assertFails(
      inventoryRef.set(
        baseInventoryData({
          isDisliked: 'no',
        })
      )
    );

    console.log('All inventory rule tests passed.');
  } catch (err) {
    console.error('Inventory rule tests failed:', err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();