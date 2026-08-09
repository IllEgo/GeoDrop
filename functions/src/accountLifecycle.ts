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

/**
 * Requires the `inventory.id` COLLECTION_GROUP index in
 * `firestore.indexes.json`. Default single-field indexing covers COLLECTION
 * scope only, so without it this query fails `FAILED_PRECONDITION` in
 * production while passing on the emulator, which creates indexes on demand.
 * It went out that way once: an account that had ever created a drop could not
 * be deleted, and failed here *after* report anonymisation had already run.
 *
 * @param {string[]} dropIds Drops whose collected copies should be removed.
 * @return {Promise<number>} How many inventory copies were deleted.
 */
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
  const fields = ["likedBy", "reportedBy", "collectedBy"];
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

/**
 * Task 4.6 — subcollections that follow a guest into their real account.
 *
 * `inventory` and `huntProgress` are what the pilot loop produces: the drops the
 * attendee collected and how far along the trail they are. `groups` is their
 * experience membership, without which the GROUP drops they were invited to stop
 * being visible at all. `blockedCreators` moves because losing a block list on
 * sign-in is a safety regression, not merely lost convenience.
 */
const MERGED_USER_SUBCOLLECTIONS = [
  "inventory",
  "huntProgress",
  "groups",
  "blockedCreators",
];

/**
 * Task 4.6 — subcollections that deliberately stay behind.
 *
 * `legalAcceptances` belongs to the account that accepted, and the destination
 * accepted separately. `notificationTokens` is device state that re-registers on
 * next launch, and `notificationSettings` is the destination's own choice.
 * `reportStatuses` is moderation correspondence addressed to the account that
 * filed the report. Listed rather than merely omitted so an unrecognised
 * subcollection is reported instead of silently dropped.
 */
const RETAINED_USER_SUBCOLLECTIONS = [
  "legalAcceptances",
  "notificationTokens",
  "notificationSettings",
  "reportStatuses",
];

/**
 * Drop maps whose per-user key follows the guest.
 *
 * `reportedBy` is excluded on purpose: a report is a safety record filed by a
 * session, and reassigning it would rewrite who reported what.
 */
const MERGED_DROP_MAPS = ["collectedBy", "likedBy"];

type MergeGuestAccountRequest = {
  guestIdToken?: unknown;
};

/**
 * Read the subject claim of a JWT **without verifying it**.
 *
 * The result is never authorization. It is used only to recognise a retry whose
 * account is already gone; every path that moves content goes through
 * `verifyIdToken` instead.
 *
 * @param {string} token A serialized JWT.
 * @return {string|null} The claimed uid, or null if the token is not readable.
 */
const unverifiedUidFromToken = (token: string): string | null => {
  const segments = token.split(".");
  if (segments.length < 2) return null;
  try {
    const claims = JSON.parse(
      Buffer.from(segments[1], "base64url").toString("utf8")
    );
    const uid = claims?.user_id ?? claims?.sub;
    return typeof uid === "string" && uid.trim() ? uid.trim() : null;
  } catch (_) {
    return null;
  }
};

/**
 * @param {string} uid The account to look for.
 * @return {Promise<boolean>} Whether an Auth account still exists for it.
 */
const accountExists = async (uid: string): Promise<boolean> => {
  try {
    await admin.auth().getUser(uid);
    return true;
  } catch (_) {
    return false;
  }
};

/**
 * Task 4.6 — establish that the caller really held the guest session.
 *
 * The uid is never taken from the request. It comes out of a verified ID token,
 * because a callable that accepted a named uid would let any account claim any
 * other account's drops. Anonymity is then confirmed against the Auth record, so
 * this can never absorb a real account even with a valid token.
 *
 * @param {string} destinationUid The signed-in account receiving the content.
 * @param {unknown} rawToken The guest session's ID token, from the client.
 * @return {Promise<string|null>} The guest uid, or null when the guest account is
 *   already gone — which means a previous merge succeeded.
 */
const resolveMergeableGuest = async (
  destinationUid: string,
  rawToken: unknown
): Promise<string | null> => {
  const token = typeof rawToken === "string" ? rawToken.trim() : "";
  if (!token) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The guest session token is required to keep your guest activity.",
      {reason: "GUEST_TOKEN_REQUIRED"}
    );
  }

  let decoded: admin.auth.DecodedIdToken;
  try {
    decoded = await admin.auth().verifyIdToken(token);
  } catch (_) {
    // Verification fails for a token whose user has been deleted, which is
    // exactly how a retry of an already-completed merge arrives: this callable
    // deletes the guest account on success. Distinguish that from a forged token
    // by reading the *unverified* claims and asking Auth whether that account
    // still exists.
    //
    // Safe because of what each branch does, not because the claims are trusted:
    // if the named account still exists, this refuses without touching anything,
    // so no content can move on an unverified token. If it does not exist, there
    // is nothing to merge and the answer is a no-op.
    const claimedUid = unverifiedUidFromToken(token);
    if (claimedUid && !(await accountExists(claimedUid))) return null;
    throw new functions.https.HttpsError(
      "permission-denied",
      "That guest session could not be verified. Sign in again to keep your guest activity.",
      {reason: "GUEST_TOKEN_INVALID"}
    );
  }

  const guestUid = decoded.uid;
  if (guestUid === destinationUid) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "That guest session is already this account.",
      {reason: "SAME_ACCOUNT"}
    );
  }

  if (decoded.firebase?.sign_in_provider !== "anonymous") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only a guest session can be merged into an account.",
      {reason: "GUEST_NOT_ANONYMOUS"}
    );
  }

  let guest: admin.auth.UserRecord;
  try {
    guest = await admin.auth().getUser(guestUid);
  } catch (_) {
    // The guest account is gone, so a previous merge already ran to completion.
    return null;
  }

  if (guest.providerData.length > 0) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only a guest session can be merged into an account.",
      {reason: "GUEST_NOT_ANONYMOUS"}
    );
  }

  const validAfter = guest.tokensValidAfterTime ?
    Math.floor(Date.parse(guest.tokensValidAfterTime) / 1000) : 0;
  if (Number.isFinite(validAfter) && decoded.auth_time < validAfter) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "That guest session has expired. Sign in again to keep your guest activity.",
      {reason: "GUEST_TOKEN_REVOKED"}
    );
  }

  return guestUid;
};

/**
 * Task 4.6 — move the identity fields, but only into empty space.
 *
 * A merge must never rename the account the user just signed into. The guest's
 * display name and username are taken only where the destination has none, which
 * is the case that matters: a guest who set a name before deciding to sign in.
 *
 * @param {admin.firestore.DocumentSnapshot} guestProfile The guest profile.
 * @param {admin.firestore.DocumentSnapshot} destinationProfile The destination profile.
 * @param {string} guestUid The guest uid.
 * @param {string} destinationUid The destination uid.
 * @return {Promise<Object>} The destination's username after the merge, for drop
 *   attribution, plus whether each identity field actually moved.
 */
const mergeProfileIdentity = async (
  guestProfile: admin.firestore.DocumentSnapshot,
  destinationProfile: admin.firestore.DocumentSnapshot,
  guestUid: string,
  destinationUid: string
): Promise<{
  username: string | null;
  movedDisplayName: boolean;
  movedUsername: boolean;
}> => {
  const firestore = admin.firestore();
  const trimmed = (value: unknown): string =>
    typeof value === "string" ? value.trim() : "";

  const guestDisplayName = trimmed(guestProfile.get("displayName"));
  const destinationDisplayName = trimmed(destinationProfile.get("displayName"));
  const guestUsername = trimmed(guestProfile.get("username"));
  const destinationUsername = trimmed(destinationProfile.get("username"));

  const updates: admin.firestore.DocumentData = {};
  if (guestDisplayName && !destinationDisplayName) {
    updates.displayName = guestDisplayName;
  }

  let resultingUsername = destinationUsername || null;
  let movedUsername = false;
  if (guestUsername && !destinationUsername) {
    // The username index is the source of truth for who owns a handle, so it
    // moves in a transaction that re-reads the pointer. A handle that stopped
    // pointing at the guest between read and write is left alone.
    movedUsername = await firestore.runTransaction(async (transaction) => {
      const usernameRef = firestore.collection("usernames").doc(guestUsername);
      const current = await transaction.get(usernameRef);
      if (current.exists && current.get("userId") !== guestUid) return false;
      transaction.set(usernameRef, {userId: destinationUid}, {merge: true});
      return true;
    });
    if (movedUsername) {
      updates.username = guestUsername;
      resultingUsername = guestUsername;
    }
  } else if (guestUsername && destinationUsername) {
    // The destination keeps its own handle, so the guest's is released rather
    // than left pointing at an account that is about to be deleted.
    await firestore.runTransaction(async (transaction) => {
      const usernameRef = firestore.collection("usernames").doc(guestUsername);
      const current = await transaction.get(usernameRef);
      if (current.exists && current.get("userId") === guestUid) {
        transaction.delete(usernameRef);
      }
    });
  }

  if (Object.keys(updates).length > 0) {
    await firestore.collection("users").doc(destinationUid).set(updates, {merge: true});
  }

  return {
    username: resultingUsername,
    movedDisplayName: Boolean(updates.displayName),
    movedUsername,
  };
};

/**
 * Task 4.6 — reassign authorship with the Admin SDK.
 *
 * `createdBy` is immutable to clients by design (no `allow update` in
 * firestore.rules permits changing it), which is exactly why this cannot be a
 * client fix: a client that could reassign authorship could steal or disown drops.
 *
 * @param {string} guestUid The guest uid.
 * @param {string} destinationUid The destination uid.
 * @param {string|null} username The destination's username after the identity merge.
 * @return {Promise<number>} How many drops changed hands.
 */
const reassignAuthoredDrops = async (
  guestUid: string,
  destinationUid: string,
  username: string | null
): Promise<number> => {
  const firestore = admin.firestore();
  const snapshot = await firestore.collection("drops")
    .where("createdBy", "==", guestUid)
    .get();
  if (snapshot.empty) return 0;

  const writer = firestore.bulkWriter();
  snapshot.docs.forEach((document) => {
    const updates: admin.firestore.DocumentData = {createdBy: destinationUid};
    // Only touch the denormalised handle where the drop already carried one,
    // and only when the destination has a handle to put there. Leaving the
    // guest's handle on a reassigned drop would misattribute it.
    if (typeof document.get("createdByUsername") === "string") {
      updates.createdByUsername = username ?? FieldValue.delete();
    }
    writer.update(document.ref, updates);
  });
  await writer.close();
  return snapshot.size;
};

/**
 * Task 4.6 — move the guest's per-user keys inside drop maps.
 *
 * The destination wins every collision. A claim is one-way as of task 4.2, so a
 * merge must never overwrite or remove a claim the destination already holds —
 * otherwise merging would become a way to un-collect.
 *
 * @param {string} guestUid The guest uid.
 * @param {string} destinationUid The destination uid.
 * @return {Promise<number>} How many drop documents were rewritten.
 */
const moveDropMapEntries = async (
  guestUid: string,
  destinationUid: string
): Promise<number> => {
  const firestore = admin.firestore();
  const touched = new Set<string>();

  for (const field of MERGED_DROP_MAPS) {
    const snapshot = await firestore.collection("drops")
      .where(new FieldPath(field, guestUid), "!=", null)
      .get();

    for (const document of snapshot.docs) {
      const map = document.get(field);
      const guestValue = map && typeof map === "object" ?
        (map as Record<string, unknown>)[guestUid] : undefined;
      const destinationValue = map && typeof map === "object" ?
        (map as Record<string, unknown>)[destinationUid] : undefined;

      const updates: Array<string | FieldValue | FieldPath | unknown> = [
        new FieldPath(field, guestUid),
        FieldValue.delete(),
      ];
      if (destinationValue === undefined || destinationValue === null) {
        updates.push(new FieldPath(field, destinationUid), guestValue ?? true);
      }

      await document.ref.update(
        updates[0] as FieldPath,
        updates[1],
        ...updates.slice(2)
      );
      touched.add(document.ref.path);
    }
  }

  return touched.size;
};

/**
 * Task 4.6 — copy the guest's subcollections, without overwriting.
 *
 * The destination's own document wins on an id collision: further trail progress
 * or an existing inventory copy is not replaced by the guest's older one.
 *
 * @param {admin.firestore.DocumentReference} guestRef The guest profile document.
 * @param {admin.firestore.DocumentReference} destinationRef The destination profile document.
 * @return {Promise<Record<string, number>>} Documents moved, per subcollection.
 */
const mergeUserSubcollections = async (
  guestRef: admin.firestore.DocumentReference,
  destinationRef: admin.firestore.DocumentReference
): Promise<Record<string, number>> => {
  const counts: Record<string, number> = {};

  for (const name of MERGED_USER_SUBCOLLECTIONS) {
    const snapshot = await guestRef.collection(name).get();
    if (snapshot.empty) continue;

    let moved = 0;
    for (const document of snapshot.docs) {
      const target = destinationRef.collection(name).doc(document.id);
      const existing = await target.get();
      if (existing.exists) continue;
      await target.set(document.data());
      moved += 1;
    }
    counts[name] = moved;
  }

  return counts;
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

/**
 * Task 4.6 — keep a guest's activity when they sign into an existing account.
 *
 * The common upgrade path never reaches here: the clients call
 * `linkWithCredential` first, which turns the anonymous account into a real one
 * *in place* and keeps the uid, so nothing needs moving. This callable exists for
 * the case linking cannot resolve — the credential already belongs to an account,
 * a returning attendee — where Firebase necessarily issues a different uid.
 *
 * Everything the pilot loop produces follows the user: authored drops, collect
 * claims, inventory copies, trail progress, and experience membership. Nothing
 * that describes the *account* follows: role, business metadata, moderation
 * state, and legal acceptances stay with the session that earned them.
 */
export const mergeGuestAccount = functions
  .region(REGION)
  .runWith({
    enforceAppCheck: true,
    timeoutSeconds: 540,
    memory: "1GB",
  })
  .https.onCall(async (data: MergeGuestAccountRequest, context) => {
    const destinationUid = requireRecentlyAuthenticatedUser(context);
    const guestUid = await resolveMergeableGuest(destinationUid, data?.guestIdToken);

    // A retry after the guest account was already deleted is a success, not an
    // error: the work it is asking for is done. Reporting it as a failure would
    // make the client show "we couldn't keep your drops" about drops it kept.
    if (!guestUid) {
      return {
        status: "already-merged",
        counts: {
          drops: 0,
          dropMapEntries: 0,
          subcollections: {},
        },
      };
    }

    const firestore = admin.firestore();
    const guestRef = firestore.collection("users").doc(guestUid);
    const destinationRef = firestore.collection("users").doc(destinationUid);
    const [guestProfile, destinationProfile] = await Promise.all([
      guestRef.get(),
      destinationRef.get(),
    ]);

    // A business account is never a guest session, so this should be
    // unreachable; refuse rather than carry business content across accounts.
    if (guestProfile.get("role") === "BUSINESS") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "A business account cannot be merged.",
        {reason: "GUEST_NOT_EXPLORER"}
      );
    }

    // Anything the guest holds that this callable does not know about. Reported
    // rather than guessed at: a new subcollection should show up in the receipt
    // and in the logs instead of being silently left behind on a deleted account.
    const guestCollections = await guestRef.listCollections();
    const unexpectedSubcollections = guestCollections
      .map((collection) => collection.id)
      .filter((id) => !MERGED_USER_SUBCOLLECTIONS.includes(id) &&
        !RETAINED_USER_SUBCOLLECTIONS.includes(id));
    if (unexpectedSubcollections.length > 0) {
      console.warn(
        "Guest merge left unrecognised subcollections behind: " +
        `${unexpectedSubcollections.join(", ")}. Classify them in ` +
        "MERGED_USER_SUBCOLLECTIONS or RETAINED_USER_SUBCOLLECTIONS."
      );
    }

    // Identity first: the drop reassignment below needs to know which username
    // the destination ends up with before it can attribute drops to it.
    const identity = await mergeProfileIdentity(
      guestProfile,
      destinationProfile,
      guestUid,
      destinationUid
    );

    const [drops, dropMapEntries, subcollections] = await Promise.all([
      reassignAuthoredDrops(guestUid, destinationUid, identity.username),
      moveDropMapEntries(guestUid, destinationUid),
      mergeUserSubcollections(guestRef, destinationRef),
    ]);

    await firestore.recursiveDelete(guestRef);

    const mergedAt = new Date();
    const expiresAt = new Date(
      mergedAt.getTime() +
      DELETION_RECEIPT_LIFETIME_DAYS * 24 * 60 * 60 * 1000
    );
    const receiptRef = firestore.collection("accountMergeReceipts").doc();
    const counts = {
      drops,
      dropMapEntries,
      subcollections,
      movedDisplayName: identity.movedDisplayName,
      movedUsername: identity.movedUsername,
    };
    const receipt = {
      receiptId: receiptRef.id,
      status: "completed",
      mergedAt: mergedAt.toISOString(),
      counts,
      unexpectedSubcollections,
    };
    // Digests, never the uids: this is an audit record of a merge, not a lookup
    // table linking a person's guest session to their account.
    await receiptRef.set({
      ...receipt,
      guestUidDigest: crypto.createHash("sha256").update(guestUid).digest("hex"),
      destinationUidDigest: crypto.createHash("sha256")
        .update(destinationUid).digest("hex"),
      expiresAt: Timestamp.fromDate(expiresAt),
    });

    console.log(
      `Guest merge ${receiptRef.id}: moved ${drops} drop(s), ` +
      `${dropMapEntries} drop map entr(ies), ` +
      `${JSON.stringify(subcollections)} into the destination account.`
    );

    // Authentication is deleted last, as in deleteAccount: a retry stays
    // possible while any data step can still fail. It also makes the merge
    // one-way — the guest token cannot be replayed against another account.
    await admin.auth().deleteUser(guestUid);
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
    // Both receipt collections share the retention window, so they share the
    // sweep. A merge receipt left behind would outlive its own policy.
    const collections = ["accountDeletionReceipts", "accountMergeReceipts"];
    for (const name of collections) {
      const expired = await firestore.collection(name)
        .where("expiresAt", "<=", Timestamp.now())
        .limit(500)
        .get();
      const writer = firestore.bulkWriter();
      expired.docs.forEach((document) => writer.delete(document.ref));
      await writer.close();
      console.log(`Purged ${expired.size} expired documents from ${name}.`);
    }
  });
