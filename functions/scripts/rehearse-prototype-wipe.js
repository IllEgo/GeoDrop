"use strict";

/**
 * Emulator rehearsal for task 1.4 (wipe-prototype-data.js).
 *
 * Proves, without touching production:
 *   1. the dry run mutates nothing,
 *   2. --apply refuses without a matching --confirm-project,
 *   3. --apply refuses when an unclassified root collection exists,
 *   4. content is deleted and accounts are preserved,
 *   5. a backup exists on disk before deletion, and
 *   6. the script is idempotent (a second run is a clean no-op).
 */

const fs = require("fs");
const os = require("os");
const path = require("path");
const {spawnSync} = require("child_process");
const admin = require("firebase-admin");

if (!process.env.FIRESTORE_EMULATOR_HOST ||
    !process.env.FIREBASE_STORAGE_EMULATOR_HOST) {
  throw new Error("Firestore and Storage emulators are required; refusing production.");
}

const projectId = process.env.GCLOUD_PROJECT || "geodrop-ci";
const bucketName = `${projectId}.appspot.com`;
admin.initializeApp({projectId, storageBucket: bucketName});
const db = admin.firestore();
const bucket = admin.storage().bucket();

const backupRoot = fs.mkdtempSync(path.join(os.tmpdir(), "geodrop-wipe-"));
const mediaPath = "drops/photos/rehearsal.jpg";

// CI runs this after three other rehearsals against the same emulator, so start
// from a known-empty state instead of inheriting their fixtures. Emulator-only:
// the env guard above has already refused to run against production.
const clearEmulator = async () => {
  const collections = await db.listCollections();
  for (const collection of collections) {
    await db.recursiveDelete(collection);
  }
  await bucket.deleteFiles({force: true});
};

const seed = async () => {
  const batch = db.batch();
  // Content — must all be gone afterwards.
  batch.set(db.collection("drops").doc("drop-1"), {createdBy: "owner", contentType: "PHOTO", mediaStoragePath: mediaPath});
  batch.set(db.collection("drops").doc("drop-2"), {createdBy: "owner", contentType: "TEXT"});
  batch.set(db.collection("groups").doc("group-1"), {ownerId: "owner"});
  batch.set(db.collection("reports").doc("report-1"), {dropId: "drop-1", status: "pending"});
  batch.set(db.collection("huntChains").doc("hunt-1"), {createdBy: "owner"});
  batch.set(db.collection("moderationCases").doc("case-1"), {state: "OPEN"});
  // Accounts — must all survive.
  batch.set(db.collection("users").doc("owner"), {role: "EXPLORER", displayName: "Owner"});
  batch.set(db.collection("usernames").doc("owner.name"), {userId: "owner"});
  batch.set(db.collection("accountDeletionReceipts").doc("receipt-1"), {uid: "gone"});
  batch.set(db.collection("users").doc("owner").collection("legalAcceptances").doc("v1"), {policyVersion: "v1"});
  batch.set(db.collection("users").doc("owner").collection("groups").doc("group-1"), {code: "group-1"});
  batch.set(db.collection("users").doc("owner").collection("inventory").doc("drop-1"), {id: "drop-1"});
  batch.set(db.collection("users").doc("owner").collection("huntProgress").doc("hunt-1"), {huntId: "hunt-1"});
  batch.set(db.collection("users").doc("owner").collection("notificationTokens").doc("device"), {token: "keep-me"});
  batch.set(db.collection("users").doc("owner").collection("blockedCreators").doc("someone"), {creatorId: "someone"});
  await batch.commit();
  await bucket.file(mediaPath).save(Buffer.from("rehearsal media"), {
    metadata: {metadata: {ownerId: "owner", dropId: "drop-1"}},
  });
};

const runWipe = (args, {expectFailure = false} = {}) => {
  const script = path.join(__dirname, "wipe-prototype-data.js");
  const result = spawnSync(process.execPath, [script, ...args], {
    cwd: path.resolve(__dirname, ".."),
    env: {
      ...process.env,
      GCLOUD_PROJECT: projectId,
      WIPE_STORAGE_BUCKET: bucketName,
      FIREBASE_CONFIG: JSON.stringify({projectId, storageBucket: bucketName}),
    },
    encoding: "utf8",
  });
  const failed = result.status !== 0;
  if (failed !== expectFailure) {
    if (result.stdout) process.stdout.write(result.stdout);
    if (result.stderr) process.stderr.write(result.stderr);
    throw new Error(
      `wipe ${args.join(" ")} expected ${expectFailure ? "failure" : "success"}, ` +
        `got exit ${result.status}`
    );
  }
  return result;
};

const countAll = async () => {
  const [drops, groups, reports, hunts, cases, users, usernames, receipts] = await Promise.all([
    db.collection("drops").get(),
    db.collection("groups").get(),
    db.collection("reports").get(),
    db.collection("huntChains").get(),
    db.collection("moderationCases").get(),
    db.collection("users").get(),
    db.collection("usernames").get(),
    db.collection("accountDeletionReceipts").get(),
  ]);
  const owner = db.collection("users").doc("owner");
  const [memberships, inventory, progress, tokens, blocked, legal] = await Promise.all([
    owner.collection("groups").get(),
    owner.collection("inventory").get(),
    owner.collection("huntProgress").get(),
    owner.collection("notificationTokens").get(),
    owner.collection("blockedCreators").get(),
    owner.collection("legalAcceptances").get(),
  ]);
  const [mediaExists] = await bucket.file(mediaPath).exists();
  return {
    drops: drops.size, groups: groups.size, reports: reports.size,
    huntChains: hunts.size, moderationCases: cases.size,
    users: users.size, usernames: usernames.size, receipts: receipts.size,
    memberships: memberships.size, inventory: inventory.size,
    huntProgress: progress.size, tokens: tokens.size, blocked: blocked.size,
    legal: legal.size,
    mediaExists,
  };
};

const main = async () => {
  const failures = [];
  await clearEmulator();
  await seed();
  const seeded = await countAll();

  // 1. Dry run must not mutate anything.
  const dry = runWipe([`--backup-dir=${path.join(backupRoot, "dry")}`, "--json"]);
  const afterDry = await countAll();
  if (JSON.stringify(seeded) !== JSON.stringify(afterDry)) {
    failures.push("dry run mutated data");
  }
  const dryReport = JSON.parse(dry.stdout);
  if (dryReport.mode !== "DRY RUN") failures.push("dry run did not report DRY RUN mode");
  if (dryReport.willDelete.storageObjects !== 1) failures.push("dry run missed the storage object");
  if (dryReport.willDelete.byRootCollection.drops !== 2) failures.push("dry run miscounted drops");
  if (dryReport.willDelete.byUserSubcollection.inventory !== 1) failures.push("dry run missed users/*/inventory");
  if (fs.existsSync(path.join(backupRoot, "dry"))) failures.push("dry run wrote a backup directory");

  // 2. --apply without a matching --confirm-project must refuse.
  runWipe(["--apply"], {expectFailure: true});
  runWipe(["--apply", "--confirm-project=some-other-project"], {expectFailure: true});
  const afterRefusals = await countAll();
  if (JSON.stringify(seeded) !== JSON.stringify(afterRefusals)) {
    failures.push("a refused --apply still mutated data");
  }

  // 3. An unclassified root collection must block --apply.
  await db.collection("mysteryCollection").doc("x").set({unexpected: true});
  runWipe(["--apply", `--confirm-project=${projectId}`], {expectFailure: true});
  if ((await db.collection("drops").get()).size !== seeded.drops) {
    failures.push("unclassified collection did not block the wipe");
  }
  await db.collection("mysteryCollection").doc("x").delete();

  // 4. The real run: content deleted, accounts preserved, backup on disk.
  const backupDir = path.join(backupRoot, "live");
  runWipe(["--apply", `--confirm-project=${projectId}`, `--backup-dir=${backupDir}`, "--json"]);
  const afterApply = await countAll();

  const emptied = ["drops", "groups", "reports", "huntChains", "moderationCases",
    "memberships", "inventory", "huntProgress"];
  emptied.forEach((key) => {
    if (afterApply[key] !== 0) failures.push(`${key} survived the wipe (${afterApply[key]})`);
  });
  if (afterApply.mediaExists) failures.push("storage media survived the wipe");
  if (afterApply.users !== seeded.users) failures.push("user profiles were deleted");
  if (afterApply.usernames !== seeded.usernames) failures.push("usernames were deleted");
  if (afterApply.tokens !== seeded.tokens) failures.push("notification tokens were deleted");
  if (afterApply.blocked !== seeded.blocked) failures.push("blocked creators were deleted");
  if (afterApply.receipts !== seeded.receipts) failures.push("account deletion receipts were deleted");
  if (afterApply.legal !== seeded.legal) failures.push("legal acceptances were deleted");

  const backedUp = JSON.parse(
    fs.readFileSync(path.join(backupDir, "firestore-documents.json"), "utf8")
  );
  if (backedUp.documentCount < 1) failures.push("backup contains no documents");
  if (!backedUp.documents.some((d) => d.path === "drops/drop-1")) {
    failures.push("backup is missing a deleted drop");
  }
  if (!fs.existsSync(path.join(backupDir, "storage", mediaPath))) {
    failures.push("backup is missing the downloaded media bytes");
  }

  // 5. Idempotent: a second run finds nothing and still succeeds.
  const second = runWipe([
    "--apply", `--confirm-project=${projectId}`,
    `--backup-dir=${path.join(backupRoot, "second")}`, "--json",
  ]);
  const secondReport = JSON.parse(second.stdout);
  if (secondReport.deleted.firestoreDocuments !== 0 ||
      secondReport.deleted.storageObjects !== 0) {
    failures.push("second run was not a no-op");
  }

  if (failures.length > 0) throw new Error(failures.join("; "));
  console.log(JSON.stringify({passed: true, projectId, backupRoot}, null, 2));
};

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
