"use strict";

/**
 * Task 4.6 — emulator rehearsal for `mergeGuestAccount`.
 *
 * The callable moves content between accounts with the Admin SDK, so rules cannot
 * be the thing that keeps it honest. What keeps it honest is that the guest uid
 * comes out of a verified ID token and never out of the request. This rehearsal
 * exists to prove that, and to prove the refusals stay refusals: every property
 * below is a way the callable could quietly become a way to steal another
 * account's drops.
 */

const admin = require("firebase-admin");

const requiredHosts = [
  "FIREBASE_AUTH_EMULATOR_HOST",
  "FIRESTORE_EMULATOR_HOST",
];
requiredHosts.forEach((name) => {
  if (!process.env[name]) {
    throw new Error(`${name} is required; this rehearsal refuses production.`);
  }
});

const projectId = process.env.GCLOUD_PROJECT || "geodrop-ci";
admin.initializeApp({projectId});
const db = admin.firestore();
const auth = admin.auth();

const destinationUid = "p0-merge-destination";
const strangerUid = "p0-merge-stranger";
const password = "GeoDrop-P0-Rehearsal-42!";
const destinationEmail = "p0-merge-destination@geodrop.invalid";
const strangerEmail = "p0-merge-stranger@geodrop.invalid";

const authHost = () => process.env.FIREBASE_AUTH_EMULATOR_HOST;

const identityToolkit = async (method, body) => {
  const response = await fetch(
    `http://${authHost()}/identitytoolkit.googleapis.com/v1/${method}?key=fake`,
    {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(body),
    }
  );
  const payload = await response.json();
  if (!response.ok || !payload.idToken) {
    throw new Error(`${method} failed: ${JSON.stringify(payload)}`);
  }
  return payload;
};

const signInWithPassword = async (email) =>
  (await identityToolkit("accounts:signInWithPassword", {
    email,
    password,
    returnSecureToken: true,
  })).idToken;

/** @return {Promise<Object>} A fresh anonymous session: its uid and ID token. */
const createGuestSession = async () => {
  const payload = await identityToolkit("accounts:signUp", {
    returnSecureToken: true,
  });
  return {uid: payload.localId, idToken: payload.idToken};
};

const callMerge = async (idToken, data) => {
  const configured = process.env.GEODROP_FUNCTIONS_BASE_URL;
  const base = configured || `http://127.0.0.1:5001/${projectId}/us-central1`;
  const response = await fetch(`${base.replace(/\/$/, "")}/mergeGuestAccount`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${idToken}`,
      "Content-Type": "application/json",
      "X-Firebase-AppCheck": "emulator-rehearsal",
    },
    body: JSON.stringify({data}),
  });
  const payload = await response.json().catch(() => ({}));
  return {ok: response.ok && !payload.error, payload};
};

const expectRefusal = async (label, idToken, data, expectedReason, failures) => {
  const {ok, payload} = await callMerge(idToken, data);
  if (ok) {
    failures.push(`${label}: the callable accepted it`);
    return;
  }
  const reason = payload.error?.details?.reason;
  if (expectedReason && reason !== expectedReason) {
    failures.push(`${label}: expected ${expectedReason}, got ${reason || "no reason"}`);
  }
};

const clearEmulator = async () => {
  await fetch(
    `http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/` +
    `${projectId}/databases/(default)/documents`,
    {method: "DELETE"}
  );
  await fetch(
    `http://${authHost()}/emulator/v1/projects/${projectId}/accounts`,
    {method: "DELETE"}
  );
};

/**
 * Seed a guest that has done everything the pilot loop can produce, so the
 * happy path proves each kind of content moves rather than just the easy one.
 *
 * @param {string} guestUid The anonymous session's uid.
 */
const seed = async (guestUid) => {
  await Promise.all([
    auth.createUser({uid: destinationUid, email: destinationEmail, password}),
    auth.createUser({uid: strangerUid, email: strangerEmail, password}),
  ]);

  const batch = db.batch();
  // The guest: a display name, a handle, and one of every content type.
  batch.set(db.collection("users").doc(guestUid), {
    displayName: "Guest Explorer",
    username: "guest_explorer",
    role: "EXPLORER",
    businessName: "Should Not Travel",
  });
  batch.set(db.collection("usernames").doc("guest_explorer"), {userId: guestUid});
  batch.set(db.doc(`users/${guestUid}/inventory/collected-drop`), {
    id: "collected-drop",
    state: "COLLECTED",
  });
  batch.set(db.doc(`users/${guestUid}/huntProgress/hunt-1`), {
    huntId: "hunt-1",
    currentStepIndex: 2,
  });
  batch.set(db.doc(`users/${guestUid}/groups/P0MERGE`), {
    code: "P0MERGE",
    role: "SUBSCRIBER",
    ownerId: strangerUid,
  });
  batch.set(db.doc(`users/${guestUid}/blockedCreators/${strangerUid}`), {
    creatorId: strangerUid,
  });
  // Must stay behind: consent belongs to the session that gave it, and a token
  // is device state that re-registers on the next launch.
  batch.set(db.doc(`users/${guestUid}/legalAcceptances/v1`), {policyVersion: "v1"});
  batch.set(db.doc(`users/${guestUid}/notificationTokens/guest-device`), {
    token: "guest-token",
  });

  // The destination already exists and already collected one drop.
  batch.set(db.collection("users").doc(destinationUid), {role: "EXPLORER"});

  batch.set(db.collection("drops").doc("guest-drop"), {
    createdBy: guestUid,
    createdByUsername: "guest_explorer",
    visibility: "PUBLIC",
    isDeleted: false,
    isNsfw: false,
  });
  batch.set(db.collection("drops").doc("collected-drop"), {
    createdBy: strangerUid,
    visibility: "PUBLIC",
    isDeleted: false,
    isNsfw: false,
    collectedBy: {[guestUid]: 111},
    likedBy: {[guestUid]: true},
  });
  // Both accounts collected this one. The destination's claim must survive: a
  // claim is one-way as of task 4.2, so a merge must not be a way to un-collect.
  batch.set(db.collection("drops").doc("shared-drop"), {
    createdBy: strangerUid,
    visibility: "PUBLIC",
    isDeleted: false,
    isNsfw: false,
    collectedBy: {[guestUid]: 111, [destinationUid]: 222},
  });
  await batch.commit();
};

const main = async () => {
  const failures = [];
  await clearEmulator();

  const guest = await createGuestSession();
  await seed(guest.uid);
  const destinationToken = await signInWithPassword(destinationEmail);

  // --- refusals, before anything has moved -------------------------------
  await expectRefusal(
    "naming a uid without a token",
    destinationToken,
    {guestUid: guest.uid},
    "GUEST_TOKEN_REQUIRED",
    failures
  );
  await expectRefusal(
    "a garbage token",
    destinationToken,
    {guestIdToken: "not-a-token"},
    "GUEST_TOKEN_INVALID",
    failures
  );
  await expectRefusal(
    "a real but non-anonymous token",
    destinationToken,
    {guestIdToken: await signInWithPassword(strangerEmail)},
    "GUEST_NOT_ANONYMOUS",
    failures
  );
  await expectRefusal(
    "the caller's own token",
    destinationToken,
    {guestIdToken: destinationToken},
    "SAME_ACCOUNT",
    failures
  );

  const guestStillThere = await db.collection("users").doc(guest.uid).get();
  if (!guestStillThere.exists) {
    failures.push("a refused merge still deleted the guest profile");
  }

  // --- the happy path -----------------------------------------------------
  const first = await callMerge(destinationToken, {guestIdToken: guest.idToken});
  if (!first.ok) {
    failures.push(`happy path failed: ${JSON.stringify(first.payload)}`);
  }

  const destination = db.collection("users").doc(destinationUid);
  const [
    guestProfile,
    destinationProfile,
    guestDrop,
    collectedDrop,
    sharedDrop,
    usernamePointer,
    inventory,
    progress,
    membership,
    blocked,
    legal,
    tokens,
  ] = await Promise.all([
    db.collection("users").doc(guest.uid).get(),
    destination.get(),
    db.collection("drops").doc("guest-drop").get(),
    db.collection("drops").doc("collected-drop").get(),
    db.collection("drops").doc("shared-drop").get(),
    db.collection("usernames").doc("guest_explorer").get(),
    destination.collection("inventory").doc("collected-drop").get(),
    destination.collection("huntProgress").doc("hunt-1").get(),
    destination.collection("groups").doc("P0MERGE").get(),
    destination.collection("blockedCreators").doc(strangerUid).get(),
    destination.collection("legalAcceptances").doc("v1").get(),
    destination.collection("notificationTokens").doc("guest-device").get(),
  ]);

  if (guestProfile.exists) failures.push("the guest profile survived the merge");
  if (guestDrop.get("createdBy") !== destinationUid) {
    failures.push("authored drop did not change hands");
  }
  if (guestDrop.get("createdByUsername") !== "guest_explorer") {
    failures.push("the reassigned drop lost its author handle");
  }
  if (collectedDrop.get(`collectedBy.${destinationUid}`) !== 111) {
    failures.push("the collect claim did not move");
  }
  if (collectedDrop.get(`collectedBy.${guest.uid}`) !== undefined) {
    failures.push("the guest collect key survived");
  }
  if (collectedDrop.get(`likedBy.${destinationUid}`) !== true) {
    failures.push("the like did not move");
  }
  if (sharedDrop.get(`collectedBy.${destinationUid}`) !== 222) {
    failures.push("a merge overwrote the destination's existing claim");
  }
  if (sharedDrop.get(`collectedBy.${guest.uid}`) !== undefined) {
    failures.push("the guest key survived on the shared drop");
  }
  if (usernamePointer.get("userId") !== destinationUid) {
    failures.push("the username pointer did not move");
  }
  if (destinationProfile.get("username") !== "guest_explorer") {
    failures.push("the username did not land on the destination profile");
  }
  if (destinationProfile.get("displayName") !== "Guest Explorer") {
    failures.push("the display name did not move into empty space");
  }
  if (destinationProfile.get("businessName") !== undefined) {
    failures.push("business metadata travelled with the merge");
  }
  if (!inventory.exists) failures.push("inventory did not move");
  if (progress.get("currentStepIndex") !== 2) failures.push("trail progress did not move");
  if (!membership.exists) failures.push("experience membership did not move");
  if (!blocked.exists) failures.push("the block list did not move");
  if (legal.exists) failures.push("legal acceptances travelled with the merge");
  if (tokens.exists) failures.push("a notification token travelled with the merge");

  try {
    await auth.getUser(guest.uid);
    failures.push("the guest auth user survived the merge");
  } catch (error) {
    if (error.code !== "auth/user-not-found") throw error;
  }

  const receipt = await db.collection("accountMergeReceipts")
    .doc(first.payload.result?.receiptId || "missing").get();
  if (!receipt.exists) {
    failures.push("no merge receipt was written");
  } else if (receipt.get("guestUidDigest") === guest.uid) {
    failures.push("the receipt stored a raw uid instead of a digest");
  }

  // --- idempotency --------------------------------------------------------
  // The guest account is gone now, so a retry cannot verify it. That must read
  // as "already done", not as a failure: the client would otherwise tell the
  // user their drops were lost about drops it had just kept.
  const second = await callMerge(destinationToken, {guestIdToken: guest.idToken});
  if (!second.ok) {
    failures.push(`a retry after completion failed: ${JSON.stringify(second.payload)}`);
  } else if (second.payload.result?.status !== "already-merged") {
    failures.push(`a retry reported ${second.payload.result?.status}, not already-merged`);
  }

  if (failures.length > 0) {
    failures.forEach((failure) => console.error(`FAIL ${failure}`));
    throw new Error(`${failures.length} guest-merge rehearsal failure(s)`);
  }
  console.log(
    "Guest merge rehearsal passed: 4 refusals held, every content type moved, " +
    "account-scoped state stayed behind, and a retry was a no-op."
  );
};

main().then(() => process.exit(0)).catch((error) => {
  console.error(error);
  process.exit(1);
});
