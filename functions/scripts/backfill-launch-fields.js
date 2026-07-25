"use strict";

const admin = require("firebase-admin");
const {FieldValue} = require("firebase-admin/firestore");

const args = new Set(process.argv.slice(2));
const shouldApply = args.has("--apply");
const shouldQuarantineGroupMedia = args.has("--quarantine-group-media");
const shouldQuarantineUnsafeMedia = args.has("--quarantine-unsafe-media");

if ((shouldQuarantineGroupMedia || shouldQuarantineUnsafeMedia) && !shouldApply) {
  throw new Error("quarantine flags require --apply");
}

admin.initializeApp();
const db = admin.firestore();

const hasGroupMedia = (data) => {
  const contentType = typeof data.contentType === "string" ? data.contentType : "TEXT";
  return contentType !== "TEXT" || [
    data.mediaUrl,
    data.mediaStoragePath,
    data.mediaData,
    data.mediaMimeType,
  ].some((value) => value != null && String(value).trim().length > 0);
};

const buildCanonicalUpdate = (data) => {
  const groupCode = typeof data.groupCode === "string" ? data.groupCode.trim() : "";
  const nsfwLabels = Array.isArray(data.nsfwLabels) ? data.nsfwLabels : [];
  return {
    isDeleted: data.isDeleted === true,
    isNsfw: data.isNsfw === true || data.nsfw === true || nsfwLabels.length > 0,
    visibility: groupCode ? "GROUP" : "PUBLIC",
  };
};

const storagePathFor = (data) => {
  if (typeof data.mediaStoragePath === "string" && data.mediaStoragePath.trim()) {
    return data.mediaStoragePath.trim().replace(/^\/+/, "");
  }
  if (typeof data.mediaUrl !== "string") return null;
  try {
    const url = new URL(data.mediaUrl);
    const match = url.pathname.match(/\/o\/([^/]+)$/);
    return match ? decodeURIComponent(match[1]) : null;
  } catch (_) {
    return null;
  }
};

const rulesCheckedMediaUrlFor = (bucketName, storagePath) =>
  `https://firebasestorage.googleapis.com/v0/b/${encodeURIComponent(bucketName)}` +
  `/o/${encodeURIComponent(storagePath)}?alt=media`;

const main = async () => {
  const [snapshot, usersSnapshot] = await Promise.all([
    db.collection("drops").get(),
    db.collection("users").get(),
  ]);
  const bucket = admin.storage().bucket();
  const planned = [];
  const plannedUsers = [];
  const mediaPathsToDelete = new Set();
  const mediaObjectsToHarden = [];
  let groupMediaCount = 0;
  let unsafeMediaCount = 0;
  let missingMediaPathCount = 0;
  let missingMediaOwnerCount = 0;
  let legacyBearerUrlCount = 0;

  snapshot.forEach((doc) => {
    const data = doc.data();
    const update = buildCanonicalUpdate(data);
    const isGroupMedia = update.visibility === "GROUP" && hasGroupMedia(data);
    const isUnsafeMedia = update.isNsfw && hasGroupMedia(data);
    const mediaPath = storagePathFor(data);
    if (isGroupMedia) {
      groupMediaCount += 1;
      if (shouldQuarantineGroupMedia) {
        update.isDeleted = true;
        update.deletedAt = Date.now();
      }
    }
    if (isUnsafeMedia) {
      unsafeMediaCount += 1;
      if (shouldQuarantineUnsafeMedia) {
        update.isDeleted = true;
        update.deletedAt = Date.now();
      }
    }
    if ((isGroupMedia && shouldQuarantineGroupMedia) ||
        (isUnsafeMedia && shouldQuarantineUnsafeMedia)) {
      if (mediaPath) mediaPathsToDelete.add(mediaPath);
      else missingMediaPathCount += 1;
      update.mediaUrl = FieldValue.delete();
      update.mediaStoragePath = FieldValue.delete();
      update.mediaData = FieldValue.delete();
    } else if (!update.isDeleted && update.visibility === "PUBLIC" &&
        !update.isNsfw && (mediaPath ||
          (typeof data.mediaUrl === "string" && data.mediaUrl.trim()))) {
      const ownerId = typeof data.createdBy === "string" ? data.createdBy.trim() : "";
      if (!mediaPath) {
        missingMediaPathCount += 1;
      } else if (!ownerId) {
        missingMediaOwnerCount += 1;
      } else {
        mediaObjectsToHarden.push({
          dropId: doc.id,
          ownerId,
          mediaPath,
        });
        const rulesUrl = rulesCheckedMediaUrlFor(bucket.name, mediaPath);
        if (data.mediaUrl !== rulesUrl) update.mediaUrl = rulesUrl;
      }
      if (typeof data.mediaUrl === "string" && /[?&]token=/.test(data.mediaUrl)) {
        legacyBearerUrlCount += 1;
      }
    }

    const changed = Object.entries(update).some(([key, value]) => data[key] !== value);
    if (changed) planned.push({ref: doc.ref, update});
  });

  usersSnapshot.forEach((doc) => {
    const data = doc.data();
    if (data.nsfwEnabled !== false || data.nsfwEnabledAt != null) {
      plannedUsers.push({
        ref: doc.ref,
        update: {
          nsfwEnabled: false,
          nsfwEnabledAt: FieldValue.delete(),
        },
      });
    }
  });

  console.log(JSON.stringify({
    mode: shouldApply ? "apply" : "dry-run",
    scannedDrops: snapshot.size,
    scannedUsers: usersSnapshot.size,
    documentsToUpdate: planned.length,
    userPreferencesToDisable: plannedUsers.length,
    groupMediaBlockers: groupMediaCount,
    unsafeMediaBlockers: unsafeMediaCount,
    mediaObjectsToDelete: mediaPathsToDelete.size,
    mediaObjectsToHarden: mediaObjectsToHarden.length,
    legacyBearerUrlsToReplace: legacyBearerUrlCount,
    missingMediaPaths: missingMediaPathCount,
    missingMediaOwners: missingMediaOwnerCount,
  }));

  if (groupMediaCount > 0 && !shouldQuarantineGroupMedia) {
    console.error(
      "Launch migration blocked: private group media must be removed or rerun with " +
      "--apply --quarantine-group-media after product approval."
    );
    process.exitCode = 2;
    return;
  }
  if (unsafeMediaCount > 0 && !shouldQuarantineUnsafeMedia) {
    console.error(
      "Launch migration blocked: unsafe media must be removed or rerun with " +
      "--apply --quarantine-unsafe-media after product approval."
    );
    process.exitCode = 2;
    return;
  }
  if (missingMediaPathCount > 0) {
    console.error(
      "Launch migration blocked: one or more quarantined media objects have no " +
      "resolvable Storage path and require manual deletion."
    );
    process.exitCode = 2;
    return;
  }
  if (missingMediaOwnerCount > 0) {
    console.error(
      "Launch migration blocked: one or more public media objects have no " +
      "canonical owner and require manual remediation."
    );
    process.exitCode = 2;
    return;
  }

  if (!shouldApply) return;

  const allUpdates = [...planned, ...plannedUsers];
  for (let offset = 0; offset < allUpdates.length; offset += 400) {
    const batch = db.batch();
    allUpdates.slice(offset, offset + 400).forEach(({ref, update}) => {
      batch.update(ref, update);
    });
    await batch.commit();
  }

  for (const mediaPath of mediaPathsToDelete) {
    await bucket.file(mediaPath).delete({ignoreNotFound: true});
  }

  for (const media of mediaObjectsToHarden) {
    const file = bucket.file(media.mediaPath);
    const [exists] = await file.exists();
    if (!exists) {
      throw new Error(`Missing Storage object ${media.mediaPath}`);
    }
    const [metadata] = await file.getMetadata();
    const existingOwnerId = metadata.metadata?.ownerId;
    const existingDropId = metadata.metadata?.dropId;
    if ((existingOwnerId && existingOwnerId !== media.ownerId) ||
        (existingDropId && existingDropId !== media.dropId)) {
      throw new Error(
        `Ownership mismatch for Storage object ${media.mediaPath}`
      );
    }
    const customMetadata = {
      ...(metadata.metadata || {}),
      ownerId: media.ownerId,
      dropId: media.dropId,
      accessLevel: "PUBLIC",
      safetyStatus: "SAFE",
      firebaseStorageDownloadTokens: null,
    };
    await file.setMetadata({metadata: customMetadata});
  }

  console.log(
    `Updated ${planned.length} drop documents and ` +
    `${plannedUsers.length} user profiles; deleted ` +
    `${mediaPathsToDelete.size} quarantined media objects; hardened ` +
    `${mediaObjectsToHarden.length} public media objects.`
  );
};

main().catch((error) => {
  console.error("Launch-field migration failed", error);
  process.exitCode = 1;
});
