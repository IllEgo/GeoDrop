"use strict";

/**
 * Emulator rehearsal for task 2.7 (normalize-account-roles.js).
 *
 * Production credentials are not required to review the migration path: this
 * proves, against the emulator, that
 *   1. the dry run mutates nothing and reports the exact writes it would make,
 *   2. --apply refuses without a matching --confirm-project,
 *   3. a mis-cased role is canonicalized,
 *   4. a missing role is backfilled — BUSINESS when business metadata is
 *      present, EXPLORER otherwise (the one-time server-side replacement for
 *      the deleted client-side inference),
 *   5. an off-model role is flagged and left alone,
 *   6. business metadata is never written or altered, and
 *   7. the script is idempotent (a second run is a clean no-op).
 */

const path = require("path");
const {spawnSync} = require("child_process");
const admin = require("firebase-admin");

// The script reconciles profiles against Auth, so both emulators are required.
if (!process.env.FIRESTORE_EMULATOR_HOST ||
    !process.env.FIREBASE_AUTH_EMULATOR_HOST) {
  throw new Error("Firestore and Auth emulators are required; refusing production.");
}

const projectId = process.env.GCLOUD_PROJECT || "geodrop-ci";
admin.initializeApp({projectId});
const db = admin.firestore();

// CI runs this alongside other rehearsals against a shared emulator, so start
// from a known-empty state. Emulator-only: the env guard above has already
// refused to run against production.
const clearEmulator = async () => {
  const collections = await db.listCollections();
  for (const collection of collections) {
    await db.recursiveDelete(collection);
  }
};

const FIXTURES = {
  "canonical-explorer": {role: "EXPLORER", displayName: "Explorer"},
  "canonical-business": {
    role: "BUSINESS",
    displayName: "Organizer",
    businessName: "E3HI",
    businessCategories: ["HOSPITALITY_TOUR_GUIDES_ATTRACTIONS"],
  },
  "miscased-business": {
    role: "business",
    displayName: "Legacy Organizer",
    businessName: "Legacy Co",
  },
  "padded-explorer": {role: " EXPLORER ", displayName: "Padded"},
  "roleless-with-metadata": {
    displayName: "Old Business",
    businessName: "Pre-role Co",
    businessCategories: ["FOOD_RESTAURANTS_CAFES"],
  },
  "roleless-with-categories-only": {
    displayName: "Categories Only",
    businessCategories: ["RETAIL_LOCAL_SHOPS"],
  },
  "roleless-plain": {displayName: "Plain"},
  "empty-role": {role: "", displayName: "Empty"},
  "offmodel-role": {role: "ADMIN", displayName: "Admin-ish"},
  "offmodel-type": {role: 7, displayName: "Numeric"},
};

const seed = async () => {
  const batch = db.batch();
  Object.entries(FIXTURES).forEach(([id, data]) => {
    batch.set(db.collection("users").doc(id), data);
  });
  await batch.commit();
};

const snapshotProfiles = async () => {
  const snapshot = await db.collection("users").get();
  const out = {};
  snapshot.docs.forEach((doc) => {
    out[doc.id] = doc.data();
  });
  return out;
};

const runScript = (args, {expectFailure = false} = {}) => {
  const script = path.join(__dirname, "normalize-account-roles.js");
  const result = spawnSync(process.execPath, [script, ...args], {
    cwd: path.resolve(__dirname, ".."),
    env: {
      ...process.env,
      GCLOUD_PROJECT: projectId,
      FIREBASE_CONFIG: JSON.stringify({projectId}),
    },
    encoding: "utf8",
  });
  const failed = result.status !== 0;
  if (failed !== expectFailure) {
    if (result.stdout) process.stdout.write(result.stdout);
    if (result.stderr) process.stderr.write(result.stderr);
    throw new Error(
      `normalize-account-roles ${args.join(" ")} expected ` +
        `${expectFailure ? "failure" : "success"}, got exit ${result.status}`
    );
  }
  return result;
};

const main = async () => {
  const failures = [];
  await clearEmulator();
  await seed();
  const seeded = await snapshotProfiles();

  // 1. Dry run mutates nothing, and plans exactly the writes we expect.
  const dry = JSON.parse(runScript(["--json"]).stdout);
  if (JSON.stringify(seeded) !== JSON.stringify(await snapshotProfiles())) {
    failures.push("dry run mutated data");
  }
  if (dry.mode !== "DRY RUN") failures.push("dry run did not report DRY RUN mode");
  if (dry.profiles !== Object.keys(FIXTURES).length) {
    failures.push(`dry run saw ${dry.profiles} profiles, expected ${Object.keys(FIXTURES).length}`);
  }

  const planned = new Map(dry.willWrite.map((entry) => [entry.id, entry]));
  const expectedWrites = {
    "miscased-business": "BUSINESS",
    "padded-explorer": "EXPLORER",
    "roleless-with-metadata": "BUSINESS",
    "roleless-with-categories-only": "BUSINESS",
    "roleless-plain": "EXPLORER",
    "empty-role": "EXPLORER",
  };
  Object.entries(expectedWrites).forEach(([id, to]) => {
    const entry = planned.get(id);
    if (!entry) {
      failures.push(`dry run did not plan a write for ${id}`);
    } else if (entry.to !== to) {
      failures.push(`dry run plans ${id} -> ${entry.to}, expected ${to}`);
    }
  });
  ["canonical-explorer", "canonical-business", "offmodel-role", "offmodel-type"].forEach((id) => {
    if (planned.has(id)) failures.push(`dry run planned an unwanted write for ${id}`);
  });
  if (planned.size !== Object.keys(expectedWrites).length) {
    failures.push(`dry run planned ${planned.size} writes, expected ${Object.keys(expectedWrites).length}`);
  }

  // 5a. Off-model roles are flagged rather than guessed at.
  const flagged = new Set(dry.flagged.map((entry) => entry.id));
  ["offmodel-role", "offmodel-type"].forEach((id) => {
    if (!flagged.has(id)) failures.push(`${id} was not flagged`);
  });
  if (flagged.size !== 2) failures.push(`${flagged.size} profiles flagged, expected 2`);

  // 2. --apply without a matching --confirm-project must refuse.
  runScript(["--apply"], {expectFailure: true});
  runScript(["--apply", "--confirm-project=some-other-project"], {expectFailure: true});
  if (JSON.stringify(seeded) !== JSON.stringify(await snapshotProfiles())) {
    failures.push("a refused --apply still mutated data");
  }

  // 3/4. The real run.
  runScript(["--apply", `--confirm-project=${projectId}`, "--json"]);
  const after = await snapshotProfiles();

  const expectedRoles = {
    "canonical-explorer": "EXPLORER",
    "canonical-business": "BUSINESS",
    "miscased-business": "BUSINESS",
    "padded-explorer": "EXPLORER",
    "roleless-with-metadata": "BUSINESS",
    "roleless-with-categories-only": "BUSINESS",
    "roleless-plain": "EXPLORER",
    "empty-role": "EXPLORER",
    // 5b. Left exactly as found.
    "offmodel-role": "ADMIN",
    "offmodel-type": 7,
  };
  Object.entries(expectedRoles).forEach(([id, role]) => {
    if (after[id].role !== role) {
      failures.push(`${id} has role ${JSON.stringify(after[id].role)}, expected ${JSON.stringify(role)}`);
    }
  });

  // 6. Nothing but `role` was touched — business metadata in particular.
  Object.keys(FIXTURES).forEach((id) => {
    const before = {...seeded[id]};
    const now = {...after[id]};
    delete before.role;
    delete now.role;
    if (JSON.stringify(before) !== JSON.stringify(now)) {
      failures.push(`${id} had fields other than role changed`);
    }
  });
  if (Object.keys(after).length !== Object.keys(FIXTURES).length) {
    failures.push("the profile count changed — the script must never create or delete");
  }

  // 7. Idempotent.
  const second = JSON.parse(runScript(["--apply", `--confirm-project=${projectId}`, "--json"]).stdout);
  if (second.wrote !== 0 || second.willWrite.length !== 0) {
    failures.push("second run was not a no-op");
  }
  if (second.counts.flagged !== 2) failures.push("second run lost the flagged profiles");

  if (failures.length > 0) throw new Error(failures.join("; "));
  console.log(JSON.stringify({passed: true, projectId, profiles: dry.profiles}, null, 2));
};

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
