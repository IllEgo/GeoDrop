"use strict";

const admin = require("firebase-admin");

const requiredHosts = [
  "FIREBASE_AUTH_EMULATOR_HOST",
  "FIRESTORE_EMULATOR_HOST",
  "FIREBASE_STORAGE_EMULATOR_HOST",
];
requiredHosts.forEach((name) => {
  if (!process.env[name]) {
    throw new Error(`${name} is required; this rehearsal refuses production.`);
  }
});

const projectId = process.env.GCLOUD_PROJECT || "geodrop-ci";
const bucketName = `${projectId}.appspot.com`;
admin.initializeApp({projectId, storageBucket: bucketName});
const db = admin.firestore();
const auth = admin.auth();
const bucket = admin.storage().bucket();
const uid = "p0-account-lifecycle-user";
const otherUid = "p0-account-lifecycle-other";
const password = "GeoDrop-P0-Rehearsal-42!";
const mediaPath = `drops/${uid}/owned.jpg`;

const callable = async (name, idToken, data) => {
  const configured = process.env.GEODROP_FUNCTIONS_BASE_URL;
  const base = configured || `http://127.0.0.1:5001/${projectId}/us-central1`;
  const response = await fetch(`${base.replace(/\/$/, "")}/${name}`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${idToken}`,
      "Content-Type": "application/json",
      "X-Firebase-AppCheck": "emulator-rehearsal",
    },
    body: JSON.stringify({data}),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.error) {
    throw new Error(payload.error?.message || `${name} returned ${response.status}`);
  }
  return payload.result;
};

const signIn = async () => {
  const host = process.env.FIREBASE_AUTH_EMULATOR_HOST;
  const response = await fetch(
    `http://${host}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake`,
    {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({
        email: "p0-account-lifecycle@geodrop.invalid",
        password,
        returnSecureToken: true,
      }),
    }
  );
  const payload = await response.json();
  if (!response.ok || !payload.idToken) {
    throw new Error(`Auth emulator sign-in failed: ${JSON.stringify(payload)}`);
  }
  return payload.idToken;
};

const seed = async () => {
  await Promise.all([
    auth.createUser({
      uid,
      email: "p0-account-lifecycle@geodrop.invalid",
      password,
      displayName: "P0 Account Rehearsal",
    }),
    auth.createUser({
      uid: otherUid,
      email: "p0-account-other@geodrop.invalid",
      password,
    }),
  ]);
  const batch = db.batch();
  batch.set(db.collection("users").doc(uid), {
    username: "p0_account_rehearsal",
    displayName: "P0 Account Rehearsal",
  });
  batch.set(db.collection("usernames").doc("p0_account_rehearsal"), {userId: uid});
  batch.set(db.doc(`users/${uid}/notificationTokens/token-1`), {token: "fake-token"});
  batch.set(db.doc(`users/${uid}/groups/P0GROUP`), {
    code: "P0GROUP",
    role: "SUBSCRIBER",
    ownerId: otherUid,
  });
  batch.set(db.doc(`users/${uid}/inventory/owned-drop`), {
    id: "owned-drop",
    state: "COLLECTED",
  });
  batch.set(db.doc(`users/${otherUid}/inventory/owned-drop`), {
    id: "owned-drop",
    state: "COLLECTED",
  });
  batch.set(db.collection("drops").doc("owned-drop"), {
    createdBy: uid,
    contentType: "PHOTO",
    mediaStoragePath: mediaPath,
    visibility: "PUBLIC",
    isDeleted: false,
    isNsfw: false,
  });
  batch.set(db.collection("drops").doc("other-drop"), {
    createdBy: otherUid,
    visibility: "PUBLIC",
    isDeleted: false,
    isNsfw: false,
    likedBy: {[uid]: true},
    dislikedBy: {[uid]: true},
    reportedBy: {[uid]: true},
    collectedBy: {[uid]: true},
  });
  batch.set(db.collection("reports").doc("account-report"), {
    dropId: "other-drop",
    reportedBy: uid,
    reasonCodes: ["spam"],
    status: "pending",
  });
  await batch.commit();
  await bucket.file(mediaPath).save(Buffer.from("synthetic-photo"), {
    contentType: "image/jpeg",
    metadata: {metadata: {ownerId: uid, dropId: "owned-drop"}},
  });
};

const verify = async (receipt) => {
  const failures = [];
  try {
    await auth.getUser(uid);
    failures.push("Auth user remains");
  } catch (error) {
    if (error.code !== "auth/user-not-found") throw error;
  }
  const checks = await Promise.all([
    db.collection("users").doc(uid).get(),
    db.doc(`users/${uid}/notificationTokens/token-1`).get(),
    db.doc(`users/${uid}/groups/P0GROUP`).get(),
    db.doc(`users/${uid}/inventory/owned-drop`).get(),
    db.doc(`users/${otherUid}/inventory/owned-drop`).get(),
    db.collection("usernames").doc("p0_account_rehearsal").get(),
    db.collection("drops").doc("owned-drop").get(),
    db.collection("drops").doc("other-drop").get(),
    db.collection("reports").doc("account-report").get(),
    db.collection("accountDeletionReceipts").doc(receipt.receiptId).get(),
  ]);
  checks.slice(0, 7).forEach((snapshot, index) => {
    if (snapshot.exists) failures.push(`Deletion check ${index + 1} still exists`);
  });
  const otherDrop = checks[7].data() || {};
  ["likedBy", "dislikedBy", "reportedBy", "collectedBy"].forEach((field) => {
    if (otherDrop[field]?.[uid] != null) failures.push(`${field} still contains UID`);
  });
  if (checks[8].get("reportedBy") === uid) failures.push("Report identity was not removed");
  if (!checks[9].exists || checks[9].get("status") !== "completed") {
    failures.push("Completion receipt is missing");
  }
  const [mediaExists] = await bucket.file(mediaPath).exists();
  if (mediaExists) failures.push("Owned media remains");
  if (failures.length > 0) throw new Error(failures.join("; "));
};

const main = async () => {
  await seed();
  const idToken = await signIn();
  const exportResult = await callable("requestAccountExport", idToken, {
    policyVersion: "pilot-2026-07-21-draft",
  });
  if (!exportResult.requestId || !exportResult.downloadUrl || !exportResult.expiresAt) {
    throw new Error("Account export did not return its signed-link contract");
  }
  const [exportFiles] = await bucket.getFiles({prefix: `account-exports/${uid}/`});
  if (exportFiles.length !== 1) throw new Error("Private export object was not created");
  const [exportBytes] = await exportFiles[0].download();
  const exportPayload = JSON.parse(exportBytes.toString("utf8"));
  if (exportPayload.account?.uid !== uid || exportPayload.ownedDrops?.length !== 1) {
    throw new Error("Export payload omitted required account or owned-drop data");
  }
  const receipt = await callable("deleteAccount", idToken, {
    confirmation: "DELETE",
    policyVersion: "pilot-2026-07-21-draft",
  });
  await verify(receipt);
  console.log(JSON.stringify({
    passed: true,
    exportRequestId: exportResult.requestId,
    receiptId: receipt.receiptId,
    counts: receipt.counts,
  }, null, 2));
};

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
