import * as crypto from "crypto";
import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {FieldPath, FieldValue, Timestamp} from "firebase-admin/firestore";

const REGION = "us-central1";
const RECENT_AUTH_WINDOW_SECONDS = 5 * 60;
const EXPORT_URL_LIFETIME_MS = 15 * 60 * 1000;
const EXPORT_OBJECT_LIFETIME_MS = 24 * 60 * 60 * 1000;
const DELETION_RECEIPT_LIFETIME_DAYS = 30;

/**
 * Increment whenever the approved retention policy changes. The mobile clients
 * send this value back with destructive requests so stale copy cannot silently
 * authorize a materially different deletion policy.
 */
export const ACCOUNT_LIFECYCLE_POLICY_VERSION = "pilot-2026-07-21-draft";

type CallableContext = functions.https.CallableContext;

type ExportRecord = {
  path: string;
  data: admin.firestore.DocumentData;
};

const requireRecentlyAuthenticatedUser = (context: CallableContext): string => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Sign in before managing your account data."
    );
  }

  const rawAuthTime = context.auth?.token.auth_time;
  const authTime = typeof rawAuthTime === "number" ?
    rawAuthTime : Number(rawAuthTime);
  const ageSeconds = Math.floor(Date.now() / 1000) - authTime;
  if (!Number.isFinite(authTime) || ageSeconds < 0 ||
      ageSeconds > RECENT_AUTH_WINDOW_SECONDS) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Reauthenticate before managing your account data.",
      {reason: "REAUTHENTICATION_REQUIRED"}
    );
  }

  return uid;
};

const requirePolicyVersion = (raw: unknown): void => {
  if (raw !== ACCOUNT_LIFECYCLE_POLICY_VERSION) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Review the current account deletion and retention policy before continuing.",
      {
        reason: "POLICY_VERSION_MISMATCH",
        policyVersion: ACCOUNT_LIFECYCLE_POLICY_VERSION,
      }
    );
  }
};

const collectDocumentTree = async (
  root: admin.firestore.DocumentReference
): Promise<ExportRecord[]> => {
  const records: ExportRecord[] = [];

  const visitDocument = async (
    reference: admin.firestore.DocumentReference
  ): Promise<void> => {
    const snapshot = await reference.get();
    if (snapshot.exists) {
      records.push({path: snapshot.ref.path, data: snapshot.data() ?? {}});
    }
    const children = await reference.listCollections();
    for (const collection of children) {
      const childSnapshots = await collection.get();
      for (const child of childSnapshots.docs) {
        await visitDocument(child.ref);
      }
    }
  };

  await visitDocument(root);
  return records;
};

const collectQuery = async (
  query: admin.firestore.Query
): Promise<ExportRecord[]> => {
  const snapshot = await query.get();
  return snapshot.docs.map((document) => ({
    path: document.ref.path,
    data: document.data(),
  }));
};

const storagePathFromDrop = (
  data: admin.firestore.DocumentData
): string | null => {
  const storedPath = data.mediaStoragePath;
  if (typeof storedPath === "string" && storedPath.trim()) {
    const normalized = storedPath.trim().replace(/^\/+/, "");
    return normalized.startsWith("drops/") ? normalized : null;
  }

  const rawUrl = data.mediaUrl;
  if (typeof rawUrl !== "string" || !rawUrl.trim()) return null;
  try {
    const url = new URL(rawUrl);
    const match = url.pathname.match(/\/o\/([^/]+)$/);
    if (!match) return null;
    const decoded = decodeURIComponent(match[1]).replace(/^\/+/, "");
    return decoded.startsWith("drops/") ? decoded : null;
  } catch (_) {
    return null;
  }
};

const deleteOwnedInventoryCopies = async (
  dropIds: string[]
): Promise<number> => {
  const firestore = admin.firestore();
  let deleted = 0;
  for (let offset = 0; offset < dropIds.length; offset += 10) {
    const ids = dropIds.slice(offset, offset + 10);
    if (ids.length === 0) continue;
    const snapshot = await firestore
      .collectionGroup("inventory")
      .where("id", "in", ids)
      .get();
    const writer = firestore.bulkWriter();
    snapshot.docs.forEach((document) => writer.delete(document.ref));
    await writer.close();
    deleted += snapshot.size;
  }
  return deleted;
};

const scrubUserFromDropMaps = async (uid: string): Promise<number> => {
  const firestore = admin.firestore();
  const fields = ["likedBy", "dislikedBy", "reportedBy", "collectedBy"];
  const documents = new Map<string, admin.firestore.DocumentReference>();

  for (const field of fields) {
    const snapshot = await firestore.collection("drops")
      .where(new FieldPath(field, uid), "!=", null)
      .get();
    snapshot.docs.forEach((document) => {
      documents.set(document.ref.path, document.ref);
    });
  }

  for (const reference of documents.values()) {
    const updates: Array<string | FieldValue | FieldPath> = [];
    fields.forEach((field) => {
      updates.push(new FieldPath(field, uid));
      updates.push(FieldValue.delete());
    });
    await reference.update(
      updates[0] as FieldPath,
      updates[1],
      ...updates.slice(2)
    );
  }
  return documents.size;
};

const anonymizeSubmittedReports = async (
  uid: string,
  deletionPseudonym: string
): Promise<number> => {
  const firestore = admin.firestore();
  const snapshot = await firestore.collection("reports")
    .where("reportedBy", "==", uid)
    .get();
  const writer = firestore.bulkWriter();
  snapshot.docs.forEach((document) => writer.update(document.ref, {
    reportedBy: deletionPseudonym,
    reporterDeletedAt: FieldValue.serverTimestamp(),
  }));
  await writer.close();
  return snapshot.size;
};

export const requestAccountExport = functions
  .region(REGION)
  .runWith({
    enforceAppCheck: true,
    timeoutSeconds: 120,
    memory: "1GB",
  })
  .https.onCall(async (data: {policyVersion?: unknown}, context) => {
    const uid = requireRecentlyAuthenticatedUser(context);
    requirePolicyVersion(data?.policyVersion);

    const firestore = admin.firestore();
    const userRef = firestore.collection("users").doc(uid);
    const authUser = await admin.auth().getUser(uid);
    const profile = await userRef.get();
    const username = profile.get("username");

    const [userDocuments, ownedDrops, submittedReports] = await Promise.all([
      collectDocumentTree(userRef),
      collectQuery(firestore.collection("drops").where("createdBy", "==", uid)),
      collectQuery(firestore.collection("reports").where("reportedBy", "==", uid)),
    ]);

    let usernameRecord: ExportRecord | null = null;
    if (typeof username === "string" && username.trim()) {
      const usernameSnapshot = await firestore
        .collection("usernames")
        .doc(username.trim())
        .get();
      if (usernameSnapshot.exists && usernameSnapshot.get("userId") === uid) {
        usernameRecord = {
          path: usernameSnapshot.ref.path,
          data: usernameSnapshot.data() ?? {},
        };
      }
    }

    const generatedAt = new Date();
    const payload = {
      schemaVersion: 1,
      policyVersion: ACCOUNT_LIFECYCLE_POLICY_VERSION,
      generatedAt: generatedAt.toISOString(),
      account: {
        uid,
        email: authUser.email ?? null,
        displayName: authUser.displayName ?? null,
        disabled: authUser.disabled,
        creationTime: authUser.metadata.creationTime,
        lastSignInTime: authUser.metadata.lastSignInTime,
        providers: authUser.providerData.map((provider) => provider.providerId),
      },
      userDocuments,
      usernameRecord,
      ownedDrops,
      submittedReports,
      retentionNotice: {
        deletionReceiptsDays: DELETION_RECEIPT_LIFETIME_DAYS,
        safetyReports: "Reporter identity is removed on deletion; the report may be retained under the approved safety-retention policy.",
        backups: "Deletion from provider backups follows the published backup-retention window.",
      },
    };

    const requestId = crypto.randomUUID();
    const objectPath = `account-exports/${uid}/${requestId}.json`;
    const file = admin.storage().bucket().file(objectPath);
    await file.save(JSON.stringify(payload, null, 2), {
      resumable: false,
      contentType: "application/json; charset=utf-8",
      metadata: {
        cacheControl: "private, no-store, max-age=0",
        metadata: {
          ownerId: uid,
          purpose: "account-export",
          deleteAfter: new Date(
            generatedAt.getTime() + EXPORT_OBJECT_LIFETIME_MS
          ).toISOString(),
        },
      },
    });
    const expiresAt = new Date(Date.now() + EXPORT_URL_LIFETIME_MS);
    const storageEmulatorHost = process.env.FIREBASE_STORAGE_EMULATOR_HOST;
    let downloadUrl: string;
    if (storageEmulatorHost) {
      downloadUrl = `http://${storageEmulatorHost}/v0/b/` +
        `${encodeURIComponent(file.bucket.name)}/o/` +
        `${encodeURIComponent(objectPath)}?alt=media`;
    } else {
      [downloadUrl] = await file.getSignedUrl({
        action: "read",
        expires: expiresAt,
      });
    }

    return {
      requestId,
      downloadUrl,
      expiresAt: expiresAt.toISOString(),
      policyVersion: ACCOUNT_LIFECYCLE_POLICY_VERSION,
      emulator: Boolean(storageEmulatorHost),
    };
  });

export const deleteAccount = functions
  .region(REGION)
  .runWith({
    enforceAppCheck: true,
    timeoutSeconds: 540,
    memory: "1GB",
  })
  .https.onCall(async (
    data: {confirmation?: unknown; policyVersion?: unknown},
    context
  ) => {
    const uid = requireRecentlyAuthenticatedUser(context);
    requirePolicyVersion(data?.policyVersion);
    if (data?.confirmation !== "DELETE") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Type DELETE to confirm permanent account deletion.",
        {reason: "EXPLICIT_CONFIRMATION_REQUIRED"}
      );
    }

    const firestore = admin.firestore();
    const userRef = firestore.collection("users").doc(uid);
    const profile = await userRef.get();
    const username = profile.get("username");
    const ownedDropsSnapshot = await firestore.collection("drops")
      .where("createdBy", "==", uid)
      .get();
    const dropIds = ownedDropsSnapshot.docs.map((document) => document.id);
    const mediaPaths = Array.from(new Set(ownedDropsSnapshot.docs
      .map((document) => storagePathFromDrop(document.data()))
      .filter((path): path is string => Boolean(path))));

    const receiptRef = firestore.collection("accountDeletionReceipts").doc();
    const deletionPseudonym = `deleted:${receiptRef.id}`;
    const [anonymizedReports, scrubbedDrops, deletedInventoryCopies] =
      await Promise.all([
        anonymizeSubmittedReports(uid, deletionPseudonym),
        scrubUserFromDropMaps(uid),
        deleteOwnedInventoryCopies(dropIds),
      ]);

    const bucket = admin.storage().bucket();
    await Promise.all(mediaPaths.map((path) =>
      bucket.file(path).delete({ignoreNotFound: true})
    ));

    for (const document of ownedDropsSnapshot.docs) {
      await firestore.recursiveDelete(document.ref);
    }

    if (typeof username === "string" && username.trim()) {
      const usernameRef = firestore.collection("usernames").doc(username.trim());
      await firestore.runTransaction(async (transaction) => {
        const current = await transaction.get(usernameRef);
        if (current.exists && current.get("userId") === uid) {
          transaction.delete(usernameRef);
        }
      });
    }

    await firestore.recursiveDelete(userRef);
    const completedAt = new Date();
    const expiresAt = new Date(
      completedAt.getTime() +
      DELETION_RECEIPT_LIFETIME_DAYS * 24 * 60 * 60 * 1000
    );
    const uidDigest = crypto.createHash("sha256").update(uid).digest("hex");
    const receipt = {
      receiptId: receiptRef.id,
      status: "completed",
      policyVersion: ACCOUNT_LIFECYCLE_POLICY_VERSION,
      completedAt: completedAt.toISOString(),
      counts: {
        drops: dropIds.length,
        mediaObjects: mediaPaths.length,
        inventoryCopies: deletedInventoryCopies,
        anonymizedReports,
        scrubbedDrops,
      },
    };
    await receiptRef.set({
      ...receipt,
      uidDigest,
      expiresAt: Timestamp.fromDate(expiresAt),
    });

    // Authentication is deleted last so a retry remains possible if any prior
    // data operation fails. The callable response still returns to the client.
    await admin.auth().deleteUser(uid);
    return receipt;
  });

export const purgeExpiredAccountExports = functions
  .region(REGION)
  .pubsub.schedule("every 24 hours")
  .onRun(async () => {
    const bucket = admin.storage().bucket();
    const [files] = await bucket.getFiles({prefix: "account-exports/"});
    const now = Date.now();
    let deleted = 0;
    for (const file of files) {
      const [metadata] = await file.getMetadata();
      const deleteAfter = metadata.metadata?.deleteAfter;
      const parsed = typeof deleteAfter === "string" ?
        Date.parse(deleteAfter) : NaN;
      const created = metadata.timeCreated ? Date.parse(metadata.timeCreated) : NaN;
      if ((Number.isFinite(parsed) && parsed <= now) ||
          (!Number.isFinite(parsed) && Number.isFinite(created) &&
            created + EXPORT_OBJECT_LIFETIME_MS <= now)) {
        await file.delete({ignoreNotFound: true});
        deleted += 1;
      }
    }
    console.log(`Purged ${deleted} expired account export objects.`);
  });

export const purgeExpiredDeletionReceipts = functions
  .region(REGION)
  .pubsub.schedule("every 24 hours")
  .onRun(async () => {
    const firestore = admin.firestore();
    const expired = await firestore.collection("accountDeletionReceipts")
      .where("expiresAt", "<=", Timestamp.now())
      .limit(500)
      .get();
    const writer = firestore.bulkWriter();
    expired.docs.forEach((document) => writer.delete(document.ref));
    await writer.close();
    console.log(`Purged ${expired.size} expired account deletion receipts.`);
  });
