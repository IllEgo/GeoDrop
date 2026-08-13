"use strict";

/**
 * R2 / M2 — deterministic emulator-only data migration rehearsal.
 *
 * This intentionally has no production override. The later point-of-no-return
 * gate must supply a separately reviewed production migration command.
 */

const admin = require("firebase-admin");

if (!process.env.FIRESTORE_EMULATOR_HOST) {
  throw new Error("Firestore emulator required; refusing production.");
}

const projectId = process.env.GCLOUD_PROJECT || "geodrop-redesign-rehearsal";
admin.initializeApp({projectId});
const db = admin.firestore();
const now = admin.firestore.Timestamp.now();

const discoveryAllowlist = new Set([
  "schemaVersion", "experienceCode", "ownerId", "hostLabel", "state",
  "moderationState", "lat", "lng", "radiusM", "contentKind", "dropKind",
  "payloadVersion", "trailId", "trailStepIndex", "trailTotalSteps", "likeCount",
  "createdAt", "publishedAt", "updatedAt", "editedAt", "expiryMode", "expiresAt",
]);

const seed = async () => {
  const batch = db.batch();
  batch.set(db.collection("users").doc("approved-owner"), {
    role: "BUSINESS",
    organizerAccessStatus: "APPROVED",
    businessName: "Migration Host",
  });
  batch.set(db.collection("users").doc("unverified-business"), {
    role: "BUSINESS",
    businessName: "Needs Review",
  });
  batch.set(db.collection("users").doc("participant"), {role: "EXPLORER"});
  batch.set(db.collection("groups").doc("MIGRATE1"), {
    ownerId: "approved-owner",
    name: "Legacy Experience",
    description: "Rehearsal fixture",
    startsAt: admin.firestore.Timestamp.fromMillis(Date.now() - 60000),
    endsAt: admin.firestore.Timestamp.fromMillis(Date.now() + 3600000),
    timeZone: "Pacific/Honolulu",
    createdAt: now,
  });
  batch.set(db.collection("users").doc("participant")
    .collection("groups").doc("MIGRATE1"), {
    code: "MIGRATE1",
    ownerId: "approved-owner",
    role: "SUBSCRIBER",
    updatedAt: now,
  });
  batch.set(db.collection("drops").doc("legacy-drop"), {
    createdBy: "approved-owner",
    groupCode: "MIGRATE1",
    createdByUsername: "Migration Host",
    text: "Private migrated payload",
    description: "Private migrated body",
    contentType: "TEXT",
    dropType: "COMMUNITY",
    lat: 19.7,
    lng: -155.1,
    isDeleted: false,
    isNsfw: false,
    createdAt: now,
    likedBy: {participant: true},
    collectedBy: {participant: true},
  });
  batch.set(db.collection("users").doc("participant")
    .collection("inventory").doc("legacy-drop"), {
    id: "legacy-drop",
    state: "COLLECTED",
  });
  batch.set(db.collection("users").doc("participant")
    .collection("huntProgress").doc("legacy-hunt"), {
    huntId: "legacy-hunt",
    currentStepIndex: 3,
    completedStepIds: ["legacy-drop"],
  });
  batch.set(db.collection("users").doc("participant")
    .collection("redemptions").doc("legacy-drop"), {
    dropId: "legacy-drop",
    code: "LEGACY-1234",
    redeemedAt: Date.now() - 1000,
  });
  await batch.commit();
};

const migrate = async () => {
  const groups = await db.collection("groups").get();
  for (const group of groups.docs) {
    if (group.get("schemaVersion") === 2) continue;
    const owner = await db.collection("users").doc(String(group.get("ownerId"))).get();
    if (owner.get("role") !== "BUSINESS" ||
        owner.get("organizerAccessStatus") !== "APPROVED") {
      await db.collection("redesignMigrationReview").doc(`group_${group.id}`).set({
        path: group.ref.path,
        reason: "OWNER_APPROVAL_EVIDENCE_REQUIRED",
      });
      continue;
    }
    await group.ref.set({
      schemaVersion: 2,
      code: group.id,
      ownerId: group.get("ownerId"),
      name: group.get("name"),
      description: group.get("description") ?? null,
      hostLabel: owner.get("businessName"),
      startsAt: group.get("startsAt"),
      endsAt: group.get("endsAt"),
      timeZone: group.get("timeZone") || "Pacific/Honolulu",
      defaultRadiusM: 25,
      state: "PUBLISHED",
      createdAt: group.get("createdAt") || now,
      publishedAt: group.get("createdAt") || now,
      updatedAt: now,
    });
  }

  const legacyDrops = await db.collection("drops").get();
  for (const drop of legacyDrops.docs) {
    const data = drop.data();
    if (data.isDeleted === true || data.isNsfw === true ||
        !["TEXT", "PHOTO"].includes(String(data.contentType || "TEXT"))) {
      await db.collection("redesignMigrationReview").doc(`drop_${drop.id}`).set({
        path: drop.ref.path,
        reason: "DROP_REQUIRES_DISPOSITION",
      });
      continue;
    }
    const group = await db.collection("groups").doc(String(data.groupCode)).get();
    if (!group.exists || group.get("schemaVersion") !== 2 ||
        group.get("ownerId") !== data.createdBy) {
      await db.collection("redesignMigrationReview").doc(`drop_${drop.id}`).set({
        path: drop.ref.path,
        reason: "EXPERIENCE_OR_OWNER_MISMATCH",
      });
      continue;
    }
    const discoveryRef = db.collection("experienceDrops").doc(drop.id);
    const existing = await discoveryRef.get();
    if (!existing.exists) {
      const contentKind = String(data.contentType || "TEXT");
      await db.runTransaction(async (transaction) => {
        transaction.create(discoveryRef, {
          schemaVersion: 1,
          experienceCode: group.id,
          ownerId: data.createdBy,
          hostLabel: group.get("hostLabel"),
          state: "PUBLISHED",
          moderationState: contentKind === "PHOTO" ? "PENDING" : "SAFE",
          lat: data.lat,
          lng: data.lng,
          radiusM: Number.isInteger(data.radiusM) ? data.radiusM : 30,
          contentKind,
          dropKind: data.dropType === "RESTAURANT_COUPON" ? "REWARD" : "STANDARD",
          payloadVersion: 1,
          trailId: null,
          trailStepIndex: null,
          trailTotalSteps: null,
          likeCount: Object.values(data.likedBy || {}).filter(Boolean).length,
          createdAt: data.createdAt || now,
          publishedAt: data.createdAt || now,
          updatedAt: now,
          editedAt: null,
          expiryMode: "NONE",
          expiresAt: null,
        });
        const payloadRef = db.collection("dropPayloads").doc(drop.id);
        transaction.create(payloadRef, {
          schemaVersion: 1,
          dropId: drop.id,
          experienceCode: group.id,
          ownerId: data.createdBy,
          currentVersion: 1,
          createdAt: data.createdAt || now,
          updatedAt: now,
        });
        transaction.create(payloadRef.collection("versions").doc("1"), {
          schemaVersion: 1,
          title: String(data.text || data.description || "Found drop").slice(0, 80),
          body: data.description || data.text || null,
          contentKind,
          mediaAssetId: null,
          mediaMimeType: null,
          mediaAltText: null,
          rewardPresentation: null,
          createdAt: data.createdAt || now,
        });
      });
    }
    for (const [uid, liked] of Object.entries(data.likedBy || {})) {
      if (liked) {
        await db.collection("users").doc(uid).collection("likes").doc(drop.id).set({
          schemaVersion: 1,
          dropId: drop.id,
          experienceCode: group.id,
          likedAt: data.createdAt || now,
        }, {merge: false});
      }
    }
  }

  const redemptions = await db.collectionGroup("redemptions").get();
  for (const redemption of redemptions.docs) {
    const uid = redemption.ref.parent.parent.id;
    const dropId = redemption.id;
    const receiptRef = db.collection("users").doc(uid)
      .collection("rewardReceipts").doc(dropId);
    if ((await receiptRef.get()).exists) continue;
    await receiptRef.create({
      schemaVersion: 1,
      receiptId: db.collection("receiptIds").doc().id,
      dropId,
      experienceCode: "MIGRATE1",
      code: redemption.get("code"),
      state: "ISSUED",
      issuedAt: now,
      usedAt: null,
      updatedAt: now,
    }, {merge: false});
  }

  const businesses = await db.collection("users").where("role", "==", "BUSINESS").get();
  for (const business of businesses.docs) {
    if (business.get("organizerAccessStatus") !== "APPROVED") {
      await db.collection("redesignMigrationReview").doc(`user_${business.id}`).set({
        path: business.ref.path,
        reason: "APPROVAL_EVIDENCE_REQUIRED",
      });
    }
  }
};

const verify = async () => {
  const [group, discovery, payload, version, like, unlock, trailProgress, receipt, review] =
    await Promise.all([
      db.collection("groups").doc("MIGRATE1").get(),
      db.collection("experienceDrops").doc("legacy-drop").get(),
      db.collection("dropPayloads").doc("legacy-drop").get(),
      db.collection("dropPayloads").doc("legacy-drop").collection("versions").doc("1").get(),
      db.collection("users").doc("participant").collection("likes").doc("legacy-drop").get(),
      db.collection("users").doc("participant").collection("unlocks").doc("legacy-drop").get(),
      db.collection("users").doc("participant").collection("trailProgress").doc("legacy-hunt").get(),
      db.collection("users").doc("participant").collection("rewardReceipts")
        .doc("legacy-drop").get(),
      db.collection("redesignMigrationReview").doc("user_unverified-business").get(),
    ]);
  const failures = [];
  if (group.get("schemaVersion") !== 2) failures.push("Experience facade missing");
  if (!discovery.exists || discovery.get("radiusM") !== 30) {
    failures.push("Discovery/default migrated radius mismatch");
  }
  const leaked = Object.keys(discovery.data() || {})
    .filter((key) => !discoveryAllowlist.has(key));
  if (leaked.length > 0) failures.push(`Discovery leaked fields: ${leaked.join(",")}`);
  if (!payload.exists || !version.exists || version.get("body") !== "Private migrated body") {
    failures.push("Private payload split mismatch");
  }
  if (!like.exists) failures.push("Legacy likedBy was not made private");
  if (unlock.exists) failures.push("Legacy inventory was promoted to an unlock receipt");
  if (trailProgress.exists) failures.push("Legacy hunt progress was promoted to Trail progress");
  if (!receipt.exists || receipt.get("state") !== "ISSUED" || receipt.get("usedAt") !== null) {
    failures.push("Legacy reward receipt was not migrated as ISSUED");
  }
  if (!review.exists) failures.push("Unverified BUSINESS account was not flagged");
  if (failures.length > 0) throw new Error(failures.join("; "));
};

const main = async () => {
  await seed();
  await migrate();
  await migrate();
  await verify();
  console.log(JSON.stringify({
    passed: true,
    projectId,
    productionWritesPossible: false,
    idempotentSecondPass: true,
    legacyInventoryPromoted: false,
    legacyHuntProgressPromoted: false,
    legacyRewardTargetState: "ISSUED",
  }, null, 2));
};

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
