"use strict";

/**
 * Seeds a single public drop for on-device proximity-unlock testing.
 *
 * The document is written with the Admin SDK, so it must satisfy by hand the
 * shape the client rules enforce at create: canonical booleans, PUBLIC
 * visibility, and no client-authored NSFW state. Only fields in the rules'
 * allowed-key list are written, so a seeded drop stays indistinguishable from
 * one the app created.
 *
 * The drop is deliberately owned by a synthetic uid rather than the tester's
 * account: FirestoreRepo hides a drop from the user who created it, so a drop
 * seeded under the test account would never appear on that device's map.
 *
 * Dry run:
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *     node scripts/seed-test-drop.js --lat=19.703995 --lng=-155.076800
 *
 * Apply:
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *     node scripts/seed-test-drop.js --lat=19.703995 --lng=-155.076800 --apply
 *
 * Clean up afterwards (soft delete, matching how the app retires a drop):
 *   node scripts/seed-test-drop.js --retire=<dropId> --apply
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
const retireId = valueFor("--retire");

const usage = () => {
  console.error(
    "Usage: node scripts/seed-test-drop.js --lat=<deg> --lng=<deg> " +
    "[--text=<message>] [--description=<text>] [--owner=<uid>] " +
    "[--username=<handle>] [--decay-days=<n>] [--id=<dropId>] [--apply]\n" +
    "       node scripts/seed-test-drop.js --retire=<dropId> [--apply]"
  );
  process.exit(2);
};

const parseCoordinate = (raw, name, limit) => {
  const value = Number(raw);
  if (!raw || !Number.isFinite(value) || Math.abs(value) > limit) {
    console.error(`Invalid --${name}: ${raw || "(missing)"}`);
    usage();
  }
  return value;
};

const parseDecayDays = (raw) => {
  if (!raw) return null;
  const value = Number(raw);
  if (!Number.isInteger(value) || value <= 0) {
    console.error(`Invalid --decay-days: ${raw}`);
    usage();
  }
  return value;
};

admin.initializeApp();
const db = admin.firestore();

const retire = async () => {
  const ref = db.collection("drops").doc(retireId);
  const snapshot = await ref.get();
  if (!snapshot.exists) {
    console.error(`No drop found with id ${retireId}`);
    process.exitCode = 1;
    return;
  }
  const data = snapshot.data();
  console.log(JSON.stringify({
    mode: shouldApply ? "apply" : "dry-run",
    action: "retire",
    dropId: retireId,
    text: data.text || null,
    createdBy: data.createdBy || null,
    isDeletedBefore: data.isDeleted === true,
  }, null, 2));

  if (!shouldApply) {
    console.log("\nDry run only. Re-run with --apply to soft-delete this drop.");
    return;
  }
  await ref.update({isDeleted: true, deletedAt: Date.now()});
  console.log(`\nDrop ${retireId} soft-deleted.`);
};

const seed = async () => {
  const lat = parseCoordinate(valueFor("--lat"), "lat", 90);
  const lng = parseCoordinate(valueFor("--lng"), "lng", 180);
  const decayDays = parseDecayDays(valueFor("--decay-days"));
  const owner = valueFor("--owner") || "seed-test-organizer";
  const username = valueFor("--username") || "GeoDropTest";
  const text = valueFor("--text") ||
    "Test drop — you unlocked it. Physical-device proximity check passed.";
  const description = valueFor("--description") ||
    "Seeded for device testing. Walk within 30 m to unlock.";
  const dropId = valueFor("--id") || `seed-${Date.now().toString(36)}`;

  // Only keys in the rules' hasOnlyAllowedDropFields list, with the canonical
  // booleans the list query filters on (isDeleted, isNsfw, visibility).
  const drop = {
    text,
    description,
    lat,
    lng,
    createdBy: owner,
    createdAt: Date.now(),
    dropperUsername: username,
    isDeleted: false,
    isNsfw: false,
    nsfwLabels: [],
    visibility: "PUBLIC",
    dropType: "COMMUNITY",
    contentType: "TEXT",
    likeCount: 0,
    likedBy: {},
    reportCount: 0,
    reportedBy: {},
    collectedBy: {},
  };
  if (decayDays) drop.decayDays = decayDays;

  const ref = db.collection("drops").doc(dropId);
  const existing = await ref.get();
  if (existing.exists) {
    console.error(`Drop ${dropId} already exists; pass a different --id.`);
    process.exitCode = 1;
    return;
  }

  console.log(JSON.stringify({
    mode: shouldApply ? "apply" : "dry-run",
    action: "seed",
    projectId: db.projectId || process.env.GOOGLE_CLOUD_PROJECT || null,
    dropId,
    drop,
  }, null, 2));

  if (!shouldApply) {
    console.log("\nDry run only. Re-run with --apply to write this drop.");
    return;
  }
  await ref.set(drop);
  console.log(
    `\nSeeded drop ${dropId} at ${lat}, ${lng}.\n` +
    "Unlock requires the device within 30 m AND a GPS accuracy of 30 m or better.\n" +
    `Retire it with: node scripts/seed-test-drop.js --retire=${dropId} --apply`
  );
};

const main = async () => {
  if (retireId) return retire();
  await seed();
};

main().catch((error) => {
  console.error("Failed to seed test drop", error);
  process.exitCode = 1;
});
