"use strict";

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

const safePath = "drops/safe-owner/safe.jpg";
const unsafePath = "drops/unsafe-owner/unsafe.jpg";
const groupPath = "drops/group-owner/group.jpg";

const seed = async () => {
  const batch = db.batch();
  batch.set(db.collection("users").doc("pilot-user"), {
    nsfwEnabled: true,
    nsfwEnabledAt: 123,
  });
  batch.set(db.collection("drops").doc("safe-drop"), {
    createdBy: "safe-owner",
    contentType: "PHOTO",
    mediaStoragePath: safePath,
    mediaUrl: `https://legacy.invalid/o/${encodeURIComponent(safePath)}?token=legacy`,
  });
  batch.set(db.collection("drops").doc("unsafe-drop"), {
    createdBy: "unsafe-owner",
    contentType: "PHOTO",
    mediaStoragePath: unsafePath,
    mediaUrl: `https://legacy.invalid/o/${encodeURIComponent(unsafePath)}?token=legacy`,
    isNsfw: true,
  });
  batch.set(db.collection("drops").doc("group-drop"), {
    createdBy: "group-owner",
    groupCode: "P0GROUP",
    contentType: "PHOTO",
    mediaStoragePath: groupPath,
    mediaUrl: `https://legacy.invalid/o/${encodeURIComponent(groupPath)}?token=legacy`,
  });
  batch.set(db.collection("drops").doc("text-drop"), {
    createdBy: "text-owner",
    contentType: "TEXT",
  });
  await batch.commit();
  await Promise.all([
    bucket.file(safePath).save(Buffer.from("safe"), {
      metadata: {metadata: {ownerId: "safe-owner", dropId: "safe-drop"}},
    }),
    bucket.file(unsafePath).save(Buffer.from("unsafe"), {
      metadata: {metadata: {ownerId: "unsafe-owner", dropId: "unsafe-drop"}},
    }),
    bucket.file(groupPath).save(Buffer.from("group"), {
      metadata: {metadata: {ownerId: "group-owner", dropId: "group-drop"}},
    }),
  ]);
};

const runMigration = () => {
  const script = path.join(__dirname, "backfill-launch-fields.js");
  const result = spawnSync(process.execPath, [
    script,
    "--apply",
    "--quarantine-group-media",
    "--quarantine-unsafe-media",
  ], {
    cwd: path.resolve(__dirname, ".."),
    env: {
      ...process.env,
      GCLOUD_PROJECT: projectId,
      FIREBASE_CONFIG: JSON.stringify({projectId, storageBucket: bucketName}),
    },
    encoding: "utf8",
  });
  if (result.stdout) process.stdout.write(result.stdout);
  if (result.stderr) process.stderr.write(result.stderr);
  if (result.status !== 0) throw new Error(`Migration exited ${result.status}`);
};

const verify = async () => {
  const failures = [];
  const [safe, unsafe, group, text, user] = await Promise.all([
    db.collection("drops").doc("safe-drop").get(),
    db.collection("drops").doc("unsafe-drop").get(),
    db.collection("drops").doc("group-drop").get(),
    db.collection("drops").doc("text-drop").get(),
    db.collection("users").doc("pilot-user").get(),
  ]);
  [safe, unsafe, group, text].forEach((document) => {
    const data = document.data() || {};
    if (typeof data.isDeleted !== "boolean" || typeof data.isNsfw !== "boolean" ||
        !["PUBLIC", "GROUP"].includes(data.visibility)) {
      failures.push(`${document.id} lacks canonical launch fields`);
    }
  });
  if (safe.get("isDeleted") || safe.get("isNsfw") || safe.get("visibility") !== "PUBLIC") {
    failures.push("Safe public drop was not preserved");
  }
  if (!unsafe.get("isDeleted") || unsafe.get("mediaStoragePath") != null) {
    failures.push("Unsafe drop was not quarantined");
  }
  if (!group.get("isDeleted") || group.get("mediaStoragePath") != null) {
    failures.push("Group media was not quarantined");
  }
  if (user.get("nsfwEnabled") !== false || user.get("nsfwEnabledAt") != null) {
    failures.push("Pilot user mature-content preference remains enabled");
  }
  const [safeExists] = await bucket.file(safePath).exists();
  const [unsafeExists] = await bucket.file(unsafePath).exists();
  const [groupExists] = await bucket.file(groupPath).exists();
  if (!safeExists || unsafeExists || groupExists) failures.push("Media quarantine mismatch");
  if (safeExists) {
    const [metadata] = await bucket.file(safePath).getMetadata();
    const custom = metadata.metadata || {};
    if (custom.ownerId !== "safe-owner" || custom.dropId !== "safe-drop" ||
        custom.accessLevel !== "PUBLIC" || custom.safetyStatus !== "SAFE" ||
        custom.firebaseStorageDownloadTokens) {
      failures.push("Safe media metadata was not hardened");
    }
  }
  if (failures.length > 0) throw new Error(failures.join("; "));
};

const main = async () => {
  await seed();
  runMigration();
  await verify();
  console.log(JSON.stringify({passed: true, projectId}, null, 2));
};

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
