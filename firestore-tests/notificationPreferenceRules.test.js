const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// Task 4.5 — the experience-alert opt-out.
//
// Membership decides who may be notified; this document decides who still wants
// to be, and it lives server-side because a local toggle cannot stop a Cloud
// Function. Absent means opted in: joining an experience is the opt-in.
//
// It is a settings document about one person, so it is readable and writable by
// that person alone, with a shape tight enough that it cannot become a place to
// stash arbitrary data under a user profile.
// ---------------------------------------------------------------------------

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';
const OWNER = 'owner-uid';
const STRANGER = 'stranger-uid';
const PATH = `users/${OWNER}/notificationSettings/preferences`;

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

  const preference = (enabled) => ({
    experienceAlertsEnabled: enabled,
    updatedAt: 1786000000000,
  });

  try {
    await seed(env, {
      [`users/${OWNER}`]: {role: 'EXPLORER'},
      [`users/${STRANGER}`]: {role: 'EXPLORER'},
    });

    const owner = env.authenticatedContext(OWNER).firestore();
    const stranger = env.authenticatedContext(STRANGER).firestore();
    const signedOut = env.unauthenticatedContext().firestore();

    currentCase = 'the owner opts out';
    await assertSucceeds(owner.doc(PATH).set(preference(false)));

    currentCase = 'the owner reads their own preference back';
    await assertSucceeds(owner.doc(PATH).get());

    currentCase = 'the owner opts back in';
    await assertSucceeds(owner.doc(PATH).set(preference(true)));

    currentCase = 'the owner clears the preference entirely';
    await assertSucceeds(owner.doc(PATH).delete());
    await assertSucceeds(owner.doc(PATH).set(preference(true)));

    // --- nobody else, in either direction ---------------------------------

    currentCase = 'a stranger cannot read it';
    await assertFails(stranger.doc(PATH).get());

    currentCase = 'a stranger cannot write it';
    await assertFails(stranger.doc(PATH).set(preference(false)));

    currentCase = 'a signed-out client cannot read it';
    await assertFails(signedOut.doc(PATH).get());

    // --- the shape stays a preference, not a storage bucket ---------------

    currentCase = 'unknown fields are refused';
    await assertFails(
      owner.doc(PATH).set({...preference(true), smuggled: 'anything'})
    );

    currentCase = 'a non-boolean preference is refused';
    await assertFails(
      owner.doc(PATH).set({experienceAlertsEnabled: 'yes', updatedAt: 1786000000000})
    );

    currentCase = 'a missing timestamp is refused';
    await assertFails(owner.doc(PATH).set({experienceAlertsEnabled: true}));

    currentCase = 'a sibling settings document is refused';
    await assertFails(
      owner.doc(`users/${OWNER}/notificationSettings/somethingElse`).set(preference(true))
    );

    console.log('All notification-preference rule tests passed.');
  } catch (err) {
    console.error(`Notification-preference rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
