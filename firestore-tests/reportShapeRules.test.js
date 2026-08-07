// ---------------------------------------------------------------------------
// reportedBy / redeemedBy value shape.
//
// isValidReportTransition accepts int or timestamp. Android has always written
// integer milliseconds; iOS wrote a Firestore Timestamp for reports and a
// seconds-as-Double for redemptions, and decoded both as [String: TimeInterval]
// — so the redemption write was refused outright and the report map decoded
// empty, leaving iOS unable to tell which drops it had already reported.
//
// Both are now integer milliseconds on both clients. This pins the contract:
// integer millis is accepted, a bare fractional double is not.
// ---------------------------------------------------------------------------
const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';

const drop = {
  createdBy: 'creator', createdAt: Date.now(), lat: 19.7, lng: -155.08,
  text: 'A drop', contentType: 'TEXT', dropType: 'COMMUNITY', visibility: 'PUBLIC',
  isDeleted: false, isNsfw: false, nsfwLabels: [],
  likeCount: 0, likedBy: {}, reportCount: 0, reportedBy: {}, collectedBy: {},
};

(async () => {
  const env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8')},
  });
  try {
    const me = env.authenticatedContext('me');

    // Android's shape: integer milliseconds.
    await env.clearFirestore();
    await env.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('drops/a').set(drop);
    });
    await assertSucceeds(
      me.firestore().doc('drops/a').update({'reportedBy.me': Date.now(), reportCount: 1})
    );

    // iOS's shape: seconds as a fractional Double.
    await env.clearFirestore();
    await env.withSecurityRulesDisabled(async (c) => {
      await c.firestore().doc('drops/b').set(drop);
    });
    const fractional = Date.now() / 1000; // fractional -> stored as a double
    await assertFails(
      me.firestore().doc('drops/b').update({'reportedBy.me': fractional, reportCount: 1})
    );

    console.log('All report-shape rule tests passed.');
  } finally {
    await env.cleanup();
  }
})();
