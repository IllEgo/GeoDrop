const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

// ---------------------------------------------------------------------------
// Task 3.5 — Unlock receipts, not location history.
//
// The 3.1 audit found no persisted location trail anywhere: the only coordinates
// written to Firestore are the drops' own, and 3.3 records a successful unlock
// as a drop id. So 3.5 is not a removal — there was nothing to remove. What it
// owes is proof that the absence is *enforced* rather than merely observed,
// which is what this suite pins.
//
// The invariant: a user-scoped document may record WHICH drop was unlocked and
// WHEN, never WHERE the user was.
//
// The one deliberate exception is users/{uid}/inventory/{dropId}, which stores
// lat/lng — those are the collected drop's coordinates, i.e. a copy of content
// the user already unlocked, not a position reading. It is asserted as allowed
// on purpose so a future tightening cannot silently break collecting.
// ---------------------------------------------------------------------------

const PROJECT_ID = process.env.GCLOUD_PROJECT || 'geodrop-ci';

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
    const me = env.authenticatedContext('me');

    // --- the profile cannot become a location record ----------------------

    currentCase = 'profile rejects position fields';
    await env.clearFirestore();
    await seed(env, {
      'users/me': {role: 'EXPLORER', displayName: 'Me', createdAt: 1},
    });

    for (const positionShaped of [
      {lat: 19.7, lng: -155.08},
      {lastKnownLocation: {lat: 19.7, lng: -155.08}},
      {locationHistory: [{lat: 19.7, lng: -155.08, at: 1}]},
      {lastSeenAt: 1, lastSeenLat: 19.7},
      {currentLatitude: 19.7},
    ]) {
      await assertFails(me.firestore().doc('users/me').update(positionShaped));
    }

    // hasOnlyAllowedUserFields() is what refuses these: the profile has a closed
    // field list, so no position key can be introduced by any client.

    currentCase = 'profile still accepts its own allowed fields';
    await assertSucceeds(me.firestore().doc('users/me').update({displayName: 'Me II'}));

    // --- no trail subcollection can be created ----------------------------

    currentCase = 'location-trail subcollections are unwritable';
    await env.clearFirestore();
    await seed(env, {'users/me': {role: 'EXPLORER', createdAt: 1}});

    for (const trailPath of [
      'users/me/locationHistory/point-1',
      'users/me/locations/point-1',
      'users/me/positions/point-1',
      'users/me/breadcrumbs/point-1',
      'users/me/visits/visit-1',
    ]) {
      await assertFails(
        me.firestore().doc(trailPath).set({lat: 19.7, lng: -155.08, at: 1})
      );
    }

    // These paths have no match block, so Firestore denies by default. The
    // assertion exists so that adding a permissive wildcard later fails here
    // rather than silently opening a trail.

    currentCase = 'a root-level trail collection is unwritable too';
    await assertFails(
      me.firestore().doc('locationHistory/me').set({lat: 19.7, lng: -155.08})
    );

    // --- the unlock receipt records ids, not positions ---------------------

    currentCase = 'hunt progress records step ids only';
    await env.clearFirestore();
    await seed(env, {'users/me': {role: 'EXPLORER', createdAt: 1}});

    await assertSucceeds(
      me.firestore().doc('users/me/huntProgress/hunt-1').set({
        huntId: 'hunt-1',
        currentStepIndex: 1,
        completedStepIds: ['drop-1'],
        startedAt: 1,
      })
    );

    // The same receipt, with where the user was standing when they unlocked it.
    await assertFails(
      me.firestore().doc('users/me/huntProgress/hunt-2').set({
        huntId: 'hunt-2',
        currentStepIndex: 1,
        completedStepIds: ['drop-1'],
        startedAt: 1,
        unlockedAtLat: 19.7,
        unlockedAtLng: -155.08,
      })
    );

    // --- the inventory exception, asserted deliberately --------------------

    currentCase = 'inventory stores the drop coordinates, and that is allowed';
    await env.clearFirestore();
    await seed(env, {'users/me': {role: 'EXPLORER', createdAt: 1}});

    await assertSucceeds(
      me.firestore().doc('users/me/inventory/drop-1').set({
        id: 'drop-1',
        text: 'A collected drop',
        contentType: 'TEXT',
        collectedAt: 1,
        lat: 19.7,
        lng: -155.08,
      })
    );

    // ...but the inventory field list is closed, so it cannot be extended into
    // a record of the collector's own position.
    currentCase = 'inventory cannot be extended with collector position';
    await assertFails(
      me.firestore().doc('users/me/inventory/drop-2').set({
        id: 'drop-2',
        contentType: 'TEXT',
        collectedAt: 1,
        collectorLat: 19.7,
        collectorLng: -155.08,
      })
    );

    // --- no user's position is exposed to another user ---------------------

    currentCase = 'a user cannot write a position onto anyone else';
    await env.clearFirestore();
    await seed(env, {
      'users/me': {role: 'EXPLORER', createdAt: 1},
      'users/other': {role: 'EXPLORER', createdAt: 1},
    });

    await assertFails(
      me.firestore().doc('users/other').update({lat: 19.7, lng: -155.08})
    );
    await assertFails(
      me.firestore().doc('users/other/locationHistory/point-1').set({lat: 19.7})
    );

    console.log('All location-privacy rule tests passed.');
  } catch (err) {
    console.error(`Location-privacy rule tests failed during ${currentCase}:`, err);
    process.exitCode = 1;
  } finally {
    await env.cleanup();
  }
})();
