"use strict";

/**
 * Seeds one owner-visible experience with known collect and redemption activity,
 * so the task 4.4 organiser dashboards can be reviewed against numbers that were
 * decided in advance rather than eyeballed for plausibility.
 *
 * The drops are written with the Admin SDK and the deployed `rollUpExperienceActivity`
 * trigger computes `groups/{code}/analytics/summary` from them — the rollup is never
 * written directly here, because the point of the review is to prove that pipeline
 * works end to end.
 *
 * Dry run:
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *     node scripts/seed-experience-activity.js --owner=<uid> --code=DEMO
 *
 * Apply, then verify what the trigger produced:
 *   ... node scripts/seed-experience-activity.js --owner=<uid> --code=DEMO --apply
 *   ... node scripts/seed-experience-activity.js --code=DEMO --verify
 *
 * Clean up when the review is signed off:
 *   ... node scripts/seed-experience-activity.js --code=DEMO --retire --apply
 */

const admin = require("firebase-admin");

const args = process.argv.slice(2);
const flags = new Set(args);
const valueFor = (flag) => {
  const prefix = `${flag}=`;
  const match = args.find((value) => value.startsWith(prefix));
  return match ? match.slice(prefix.length).trim() : "";
};

const shouldApply = flags.has("--apply");
const shouldVerify = flags.has("--verify");
const shouldRetire = flags.has("--retire");
const owner = valueFor("--owner");
const groupCode = valueFor("--code").toUpperCase();
const businessName = valueFor("--business-name") || "E3HI";

const usage = () => {
  console.error(
    "Usage: node scripts/seed-experience-activity.js --owner=<uid> --code=<CODE> [--business-name=<name>] [--apply]\n" +
    "       node scripts/seed-experience-activity.js --code=<CODE> --verify\n" +
    "       node scripts/seed-experience-activity.js --code=<CODE> --retire [--apply]"
  );
  process.exit(2);
};

if (!groupCode) usage();
if (!shouldVerify && !shouldRetire && !owner) usage();

admin.initializeApp();
const db = admin.firestore();

// Three synthetic attendees. Aggregates are what the dashboard shows; these ids
// exist only so collectedBy/redeemedBy have realistic distinct keys.
const ATTENDEES = ["seed-attendee-1", "seed-attendee-2", "seed-attendee-3"];

const BASE_LAT = 19.703995;
const BASE_LNG = -155.076800;

/**
 * 4 drops, 6 collects, 2 redemptions. The coupons carry the redemptions because
 * that is the transition the rollup counts; note they will not appear in the
 * per-drop dashboard while pilot_coupons_enabled is false, though they do count
 * toward the aggregate.
 */
const buildDrops = (now) => [
  {
    id: `seed-${groupCode}-tour-1`,
    text: "Start at the banyan tree",
    description: "Opening stop for the walking tour.",
    dropType: "TOUR_STOP",
    latOffset: 0,
    lngOffset: 0,
    collectedBy: [ATTENDEES[0], ATTENDEES[1]],
    redeemedBy: [],
  },
  {
    id: `seed-${groupCode}-tour-2`,
    text: "The mural on the side wall",
    description: "Second stop, one block makai.",
    dropType: "TOUR_STOP",
    latOffset: 0.0004,
    lngOffset: 0.0003,
    collectedBy: [ATTENDEES[2]],
    redeemedBy: [],
  },
  {
    id: `seed-${groupCode}-coupon-1`,
    text: "Free shave ice with any purchase",
    description: "Show this at the counter.",
    dropType: "RESTAURANT_COUPON",
    latOffset: -0.0003,
    lngOffset: 0.0005,
    collectedBy: [ATTENDEES[0], ATTENDEES[2]],
    redeemedBy: [ATTENDEES[0]],
    redemptionLimit: 25,
  },
  {
    id: `seed-${groupCode}-coupon-2`,
    text: "Two-for-one plate lunch",
    description: "Valid during the event only.",
    dropType: "RESTAURANT_COUPON",
    latOffset: 0.0002,
    lngOffset: -0.0004,
    collectedBy: [ATTENDEES[1]],
    redeemedBy: [ATTENDEES[1]],
    redemptionLimit: 25,
  },
].map((spec) => ({
  id: spec.id,
  document: {
    text: spec.text,
    description: spec.description,
    lat: Number((BASE_LAT + spec.latOffset).toFixed(6)),
    lng: Number((BASE_LNG + spec.lngOffset).toFixed(6)),
    createdBy: owner,
    createdAt: now,
    dropperUsername: businessName,
    isDeleted: false,
    isNsfw: false,
    nsfwLabels: [],
    visibility: "GROUP",
    groupCode,
    dropType: spec.dropType,
    businessId: owner,
    businessName,
    contentType: "TEXT",
    likeCount: 0,
    likedBy: {},
    reportCount: 0,
    reportedBy: {},
    redemptionLimit: spec.redemptionLimit ?? null,
    redemptionCount: spec.redeemedBy.length,
    redeemedBy: Object.fromEntries(spec.redeemedBy.map((uid) => [uid, now])),
    collectedBy: Object.fromEntries(spec.collectedBy.map((uid) => [uid, true])),
  },
}));

const expectedTotals = (drops) => ({
  drops: drops.length,
  collects: drops.reduce((sum, d) => sum + Object.keys(d.document.collectedBy).length, 0),
  redemptions: drops.reduce((sum, d) => sum + Object.keys(d.document.redeemedBy).length, 0),
});

const summaryRef = () => db.collection("groups").doc(groupCode).collection("analytics").doc("summary");

const verify = async () => {
  const [summary, dropsSnapshot] = await Promise.all([
    summaryRef().get(),
    db.collection("drops").where("groupCode", "==", groupCode).get(),
  ]);

  const live = dropsSnapshot.docs.filter((doc) => doc.get("isDeleted") !== true);
  const actualFromSource = {
    drops: live.length,
    collects: live.reduce((sum, doc) => sum + Object.keys(doc.get("collectedBy") || {}).length, 0),
    redemptions: live.reduce((sum, doc) => sum + Object.keys(doc.get("redeemedBy") || {}).length, 0),
  };

  const rollup = summary.exists ? summary.data() : null;
  const matches = rollup !== null &&
    rollup.drops === actualFromSource.drops &&
    rollup.collects === actualFromSource.collects &&
    rollup.redemptions === actualFromSource.redemptions;

  console.log(JSON.stringify({
    groupCode,
    computedFromDrops: actualFromSource,
    rollupDocument: rollup && {
      drops: rollup.drops,
      collects: rollup.collects,
      redemptions: rollup.redemptions,
      updatedAt: rollup.updatedAt ? new Date(rollup.updatedAt).toISOString() : null,
      reconciledAt: rollup.reconciledAt ? new Date(rollup.reconciledAt).toISOString() : null,
    },
    rollupMatchesSource: matches,
  }, null, 2));

  if (!matches) {
    console.error("\nRollup does not match the source drops yet. The trigger may still be running.");
    process.exitCode = 1;
  }
};

const retire = async () => {
  const dropsSnapshot = await db.collection("drops").where("groupCode", "==", groupCode).get();
  const seeded = dropsSnapshot.docs.filter((doc) => doc.id.startsWith(`seed-${groupCode}-`));

  console.log(JSON.stringify({
    mode: shouldApply ? "apply" : "dry-run",
    action: "retire",
    groupCode,
    seededDrops: seeded.map((doc) => doc.id),
  }, null, 2));

  if (!shouldApply) {
    console.log("\nDry run only. Re-run with --apply to soft-delete these drops.");
    return;
  }

  // Soft delete only, and only the drops this script created. The trigger
  // decrements the rollup as each one goes; the group and its membership stay.
  for (const doc of seeded) {
    await doc.ref.update({isDeleted: true, deletedAt: Date.now()});
  }
  console.log(`\nSoft-deleted ${seeded.length} seeded drops in ${groupCode}.`);
};

const seed = async () => {
  const now = Date.now();
  const drops = buildDrops(now);
  const groupRef = db.collection("groups").doc(groupCode);
  const membershipRef = db.collection("users").doc(owner).collection("groups").doc(groupCode);

  const [existingGroup, ownerDoc] = await Promise.all([groupRef.get(), db.collection("users").doc(owner).get()]);
  if (!ownerDoc.exists) {
    console.error(`No user document for owner ${owner}.`);
    process.exit(1);
  }
  if (existingGroup.exists && existingGroup.get("ownerId") !== owner) {
    console.error(
      `Group ${groupCode} already exists and is owned by ${existingGroup.get("ownerId")}. ` +
      "Pick an unused code rather than repointing someone else's experience."
    );
    process.exit(1);
  }

  console.log(JSON.stringify({
    mode: shouldApply ? "apply" : "dry-run",
    groupCode,
    owner,
    ownerRole: ownerDoc.get("role"),
    ownerBusinessName: ownerDoc.get("businessName") || null,
    groupAlreadyExists: existingGroup.exists,
    drops: drops.map((d) => ({
      id: d.id,
      dropType: d.document.dropType,
      collects: Object.keys(d.document.collectedBy).length,
      redemptions: Object.keys(d.document.redeemedBy).length,
    })),
    expectedRollup: expectedTotals(drops),
  }, null, 2));

  if (!shouldApply) {
    console.log("\nDry run only. Re-run with --apply to write the experience and its drops.");
    return;
  }

  await groupRef.set({
    ownerId: owner,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});

  await membershipRef.set({
    code: groupCode,
    role: "OWNER",
    ownerId: owner,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});

  for (const drop of drops) {
    await db.collection("drops").doc(drop.id).set(drop.document);
  }

  console.log(
    `\nSeeded experience ${groupCode} for ${owner} with ${drops.length} drops.\n` +
    "The rollup trigger runs asynchronously; re-run with --verify in a few seconds."
  );
};

const main = async () => {
  if (shouldVerify) return verify();
  if (shouldRetire) return retire();
  await seed();
};

main().catch((error) => {
  console.error("Failed to seed experience activity", error);
  process.exitCode = 1;
});
