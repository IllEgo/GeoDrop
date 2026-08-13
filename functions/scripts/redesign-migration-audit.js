"use strict";

/**
 * R2 / M0 — read-only redesign migration inventory and integrity manifest.
 *
 * This command has no apply mode. It requires an explicit project id and only
 * reads Firestore and Storage. Redirect stdout to retain the JSON gate artifact:
 *
 *   node scripts/redesign-migration-audit.js --project geodrop-project > audit.json
 */

const crypto = require("crypto");
const admin = require("firebase-admin");

const args = process.argv.slice(2);
const projectFlag = args.indexOf("--project");
const projectId = projectFlag >= 0 ? args[projectFlag + 1] : null;
if (!projectId || !/^[a-z0-9-]{4,80}$/.test(projectId)) {
  throw new Error("Pass the exact Firebase project id with --project.");
}
if (args.includes("--apply")) {
  throw new Error("This audit is read-only and has no --apply mode.");
}

admin.initializeApp({projectId, storageBucket: `${projectId}.appspot.com`});
const db = admin.firestore();
const bucket = admin.storage().bucket();

const stableValue = (value) => {
  if (value === null || value === undefined) return value ?? null;
  if (Array.isArray(value)) return value.map(stableValue);
  if (typeof value.toDate === "function") return value.toDate().toISOString();
  if (Buffer.isBuffer(value)) return value.toString("base64");
  if (typeof value === "object") {
    return Object.fromEntries(Object.keys(value).sort()
      .map((key) => [key, stableValue(value[key])]));
  }
  return value;
};

const digestDocuments = (documents) => crypto.createHash("sha256")
  .update(JSON.stringify(documents
    .map((document) => ({path: document.ref.path, data: stableValue(document.data())}))
    .sort((left, right) => left.path.localeCompare(right.path))))
  .digest("hex");

const hasText = (value) => typeof value === "string" && value.trim().length > 0;
const truthyKeys = (value) => value && typeof value === "object" ?
  Object.entries(value).filter(([, state]) => Boolean(state)).map(([key]) => key) : [];

const classifyGroup = (document) => {
  const data = document.data();
  const missing = [];
  if (!hasText(data.ownerId)) missing.push("ownerId");
  if (!hasText(data.name)) missing.push("name");
  if (!data.startsAt) missing.push("startsAt");
  if (!data.endsAt) missing.push("endsAt");
  return {
    path: document.ref.path,
    disposition: missing.length === 0 ? "MIGRATE" : "FLAG",
    reasons: missing.map((field) => `MISSING_${field.toUpperCase()}`),
  };
};

const classifyDrop = (document) => {
  const data = document.data();
  const contentKind = String(data.contentType || "TEXT").toUpperCase();
  const reasons = [];
  if (!hasText(data.createdBy)) reasons.push("MISSING_OWNER");
  if (!hasText(data.groupCode)) reasons.push("MISSING_EXPERIENCE");
  if (!["TEXT", "PHOTO"].includes(contentKind)) reasons.push("UNSUPPORTED_CONTENT");
  if (data.isNsfw === true) reasons.push("UNSAFE_CONTENT");
  if (data.isDeleted === true) reasons.push("DELETED_CONTENT");
  let disposition = "MIGRATE";
  if (reasons.includes("DELETED_CONTENT")) disposition = "ARCHIVE";
  else if (reasons.length > 0) disposition = "FLAG";
  return {
    path: document.ref.path,
    disposition,
    reasons,
    privacyInputs: {
      payloadFields: ["text", "description", "mediaUrl", "mediaStoragePath", "mediaData"]
        .filter((field) => data[field] !== undefined),
      likedByCount: truthyKeys(data.likedBy).length,
      collectedByCount: truthyKeys(data.collectedBy).length,
      migratedRadiusM: Number.isInteger(data.radiusM) ? data.radiusM : 30,
    },
  };
};

const main = async () => {
  const actualProject = admin.app().options.projectId;
  if (actualProject !== projectId) {
    throw new Error(`Project mismatch: requested ${projectId}, initialized ${actualProject}.`);
  }
  const roots = (await db.listCollections()).map((collection) => collection.id).sort();
  const [groups, drops, users, memberships, inventories, hunts, huntProgress, redemptions] =
    await Promise.all([
      db.collection("groups").get(),
      db.collection("drops").get(),
      db.collection("users").get(),
      db.collectionGroup("groups").get(),
      db.collectionGroup("inventory").get(),
      db.collection("huntChains").get(),
      db.collectionGroup("huntProgress").get(),
      db.collectionGroup("redemptions").get(),
    ]);
  let storageObjects = [];
  let storageError = null;
  try {
    [storageObjects] = await bucket.getFiles();
  } catch (error) {
    storageError = error.message;
  }
  const groupClassifications = groups.docs.map(classifyGroup);
  const dropClassifications = drops.docs.map(classifyDrop);
  const businessAccounts = users.docs
    .filter((document) => document.get("role") === "BUSINESS")
    .map((document) => ({
      path: document.ref.path,
      organizerAccessStatus: document.get("organizerAccessStatus") ?? null,
      disposition: document.get("organizerAccessStatus") === "APPROVED" ?
        "PRESERVE" : "FLAG",
      reason: document.get("organizerAccessStatus") === "APPROVED" ? null :
        "APPROVAL_EVIDENCE_REQUIRED",
    }));
  const classifications = [
    ...groupClassifications,
    ...dropClassifications,
    ...businessAccounts,
  ];
  const manifest = {
    schemaVersion: 1,
    mode: "READ_ONLY",
    generatedAt: new Date().toISOString(),
    projectId,
    rootCollections: roots,
    counts: {
      groups: groups.size,
      memberships: memberships.docs.filter((document) =>
        document.ref.parent.parent?.parent.id === "users").length,
      drops: drops.size,
      inventories: inventories.size,
      huntChains: hunts.size,
      huntProgress: huntProgress.size,
      legacyRewardReceipts: redemptions.size,
      users: users.size,
      businessAccounts: businessAccounts.length,
      storageObjects: storageObjects.length,
    },
    integrity: {
      groupsSha256: digestDocuments(groups.docs),
      dropsSha256: digestDocuments(drops.docs),
      usersSha256: digestDocuments(users.docs),
      membershipsSha256: digestDocuments(memberships.docs),
      inventoriesSha256: digestDocuments(inventories.docs),
      huntProgressSha256: digestDocuments(huntProgress.docs),
      redemptionsSha256: digestDocuments(redemptions.docs),
      storagePathsSha256: crypto.createHash("sha256")
        .update(storageObjects.map((file) => file.name).sort().join("\n"))
        .digest("hex"),
    },
    classifications,
    summary: {
      migrate: classifications.filter((item) => item.disposition === "MIGRATE").length,
      preserve: classifications.filter((item) => item.disposition === "PRESERVE").length,
      archive: classifications.filter((item) => item.disposition === "ARCHIVE").length,
      flag: classifications.filter((item) => item.disposition === "FLAG").length,
      unclassified: 0,
    },
    valuePromotionGuard: {
      inventoriesPromotedToUnlocks: 0,
      huntProgressPromotedToTrailProgress: 0,
      legacyRewardReceiptsTargetState: "ISSUED",
      legacyLikedByTarget: "users/{uid}/likes/{dropId}",
    },
    storageInspectionError: storageError,
  };
  console.log(JSON.stringify(manifest, null, 2));
};

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
