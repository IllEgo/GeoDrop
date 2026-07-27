"use strict";

const crypto = require("crypto");
const fs = require("fs");
const admin = require("firebase-admin");

const args = process.argv.slice(2);
const command = args[0];
const valueFor = (name) => {
  const index = args.indexOf(name);
  return index >= 0 ? args[index + 1] : undefined;
};
const uid = (valueFor("--uid") || process.env.GEODROP_ACCOUNT_UID || "").trim();

if (!uid) throw new Error("Provide --uid or GEODROP_ACCOUNT_UID.");
if (!admin.apps.length) admin.initializeApp();

const db = admin.firestore();
const bucket = admin.storage().bucket();

const storagePathFromDrop = (data) => {
  if (typeof data.mediaStoragePath === "string" && data.mediaStoragePath.trim()) {
    return data.mediaStoragePath.trim().replace(/^\/+/, "");
  }
  if (typeof data.mediaUrl !== "string") return null;
  try {
    const url = new URL(data.mediaUrl);
    const match = url.pathname.match(/\/o\/([^/]+)$/);
    return match ? decodeURIComponent(match[1]).replace(/^\/+/, "") : null;
  } catch (_) {
    return null;
  }
};

const collectTreePaths = async (root) => {
  const paths = [];
  const visit = async (reference) => {
    const snapshot = await reference.get();
    if (snapshot.exists) paths.push(reference.path);
    const collections = await reference.listCollections();
    for (const collection of collections) {
      const children = await collection.get();
      for (const child of children.docs) await visit(child.ref);
    }
  };
  await visit(root);
  return paths;
};

const buildManifest = async () => {
  const userRef = db.collection("users").doc(uid);
  const [profile, ownedDrops, reports, inventory] = await Promise.all([
    userRef.get(),
    db.collection("drops").where("createdBy", "==", uid).get(),
    db.collection("reports").where("reportedBy", "==", uid).get(),
    db.collectionGroup("inventory").get(),
  ]);
  const dropIds = new Set(ownedDrops.docs.map((document) => document.id));
  const inventoryPaths = inventory.docs
    .filter((document) => dropIds.has(document.id) || dropIds.has(document.get("id")))
    .map((document) => document.ref.path);
  let authExists = true;
  try {
    await admin.auth().getUser(uid);
  } catch (error) {
    if (error.code === "auth/user-not-found") authExists = false;
    else throw error;
  }
  return {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    projectId: process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT || null,
    uid,
    uidDigest: crypto.createHash("sha256").update(uid).digest("hex"),
    authExists,
    username: profile.get("username") || null,
    userDocumentPaths: await collectTreePaths(userRef),
    ownedDrops: ownedDrops.docs.map((document) => ({
      id: document.id,
      path: document.ref.path,
      mediaStoragePath: storagePathFromDrop(document.data()),
    })),
    submittedReportPaths: reports.docs.map((document) => document.ref.path),
    inventoryPaths,
  };
};

const verifyManifest = async (manifest) => {
  const failures = [];
  try {
    await admin.auth().getUser(manifest.uid);
    failures.push("Firebase Authentication user still exists");
  } catch (error) {
    if (error.code !== "auth/user-not-found") throw error;
  }

  for (const path of manifest.userDocumentPaths || []) {
    if ((await db.doc(path).get()).exists) failures.push(`User data remains: ${path}`);
  }
  if (manifest.username) {
    const username = await db.collection("usernames").doc(manifest.username).get();
    if (username.exists && username.get("userId") === manifest.uid) {
      failures.push(`Username remains owned: ${manifest.username}`);
    }
  }
  for (const drop of manifest.ownedDrops || []) {
    if ((await db.doc(drop.path).get()).exists) failures.push(`Owned drop remains: ${drop.path}`);
    if (drop.mediaStoragePath) {
      const [exists] = await bucket.file(drop.mediaStoragePath).exists();
      if (exists) failures.push(`Owned media remains: ${drop.mediaStoragePath}`);
    }
  }
  for (const path of manifest.inventoryPaths || []) {
    if ((await db.doc(path).get()).exists) failures.push(`Inventory copy remains: ${path}`);
  }
  for (const path of manifest.submittedReportPaths || []) {
    const report = await db.doc(path).get();
    if (report.exists && report.get("reportedBy") === manifest.uid) {
      failures.push(`Report was not pseudonymized: ${path}`);
    }
  }

  const drops = await db.collection("drops").get();
  const mapFields = ["likedBy", "reportedBy", "collectedBy"];
  drops.docs.forEach((document) => {
    mapFields.forEach((field) => {
      const value = document.get(field);
      if (value && typeof value === "object" && value[manifest.uid] != null) {
        failures.push(`Identifier remains in ${document.ref.path}.${field}`);
      }
    });
  });

  const receipts = await db.collection("accountDeletionReceipts")
    .where("uidDigest", "==", manifest.uidDigest)
    .limit(1)
    .get();
  if (receipts.empty) failures.push("Pseudonymous completion receipt is missing");

  return {
    checkedAt: new Date().toISOString(),
    passed: failures.length === 0,
    failures,
  };
};

const main = async () => {
  if (command === "preflight") {
    const manifest = await buildManifest();
    const output = valueFor("--output");
    if (output) {
      fs.writeFileSync(output, JSON.stringify(manifest, null, 2), {
        encoding: "utf8",
        mode: 0o600,
      });
      console.log(JSON.stringify({written: output, summary: {
        userDocuments: manifest.userDocumentPaths.length,
        ownedDrops: manifest.ownedDrops.length,
        submittedReports: manifest.submittedReportPaths.length,
        inventoryCopies: manifest.inventoryPaths.length,
      }}, null, 2));
    } else {
      console.log(JSON.stringify(manifest, null, 2));
    }
    return;
  }
  if (command === "verify") {
    const input = valueFor("--manifest");
    if (!input) throw new Error("Verification requires --manifest PATH.");
    const manifest = JSON.parse(fs.readFileSync(input, "utf8"));
    if (manifest.uid !== uid) throw new Error("Manifest UID does not match --uid.");
    const result = await verifyManifest(manifest);
    console.log(JSON.stringify(result, null, 2));
    if (!result.passed) process.exitCode = 2;
    return;
  }
  throw new Error(
    "Usage: account-lifecycle-audit.js preflight --uid UID [--output FILE] | " +
    "verify --uid UID --manifest FILE"
  );
};

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
