import * as functions from "firebase-functions/v1";
import * as crypto from "crypto";
import * as admin from "firebase-admin";
import {FieldValue} from "firebase-admin/firestore";
import {ImageAnnotatorClient} from "@google-cloud/vision";

admin.initializeApp();
const vision = new ImageAnnotatorClient();
const messaging = admin.messaging();
const MODERATION_QUEUE_COLLECTION = "dropModerationQueue";
const MAX_SAFE_SEARCH_BYTES = 6 * 1024 * 1024;
const MAX_SAFE_SEARCH_BASE64_LENGTH = Math.ceil(MAX_SAFE_SEARCH_BYTES / 3) * 4;

const encodeStoragePath = (path: string): string =>
  Buffer.from(path, "utf8")
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

const moderationQueueRef = (path: string) =>
  admin.firestore()
    .collection(MODERATION_QUEUE_COLLECTION)
    .doc(encodeStoragePath(path));

const chunkArray = <T>(items: T[], chunkSize: number): T[][] => {
  if (chunkSize <= 0) return [items];
  const chunks: T[][] = [];
  for (let i = 0; i < items.length; i += chunkSize) {
    chunks.push(items.slice(i, i + chunkSize));
  }
  return chunks;
};

const isTruthyValue = (value: unknown): boolean => {
  if (value === null || value === undefined) return false;
  if (typeof value === "boolean") return value;
  if (typeof value === "number") return value !== 0;
  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    if (!normalized) return false;
    return !["false", "0", "off", "no", "none"].includes(normalized);
  }
  if (Array.isArray(value)) return value.length > 0;
  if (typeof value === "object") return Object.keys(value as Record<string, unknown>).length > 0;
  return false;
};

const extractCollectorIds = (raw: unknown): Set<string> => {
  if (!raw || typeof raw !== "object") return new Set();
  const entries = Object.entries(raw as Record<string, unknown>);
  const ids = entries
    .map(([uid]) => uid?.trim())
    .filter((uid) => typeof uid === "string" && uid.length > 0) as string[];

  const truthyIds = new Set<string>();
  entries.forEach(([uid, value]) => {
    const normalized = uid?.trim();
    if (normalized && isTruthyValue(value)) {
      truthyIds.add(normalized);
    }
  });

  if (truthyIds.size > 0) {
    return truthyIds;
  }

  return new Set(ids);
};

const fetchNotificationTokens = async (userId: string): Promise<string[]> => {
  const snapshot = await admin
    .firestore()
    .collection("users")
    .doc(userId)
    .collection("notificationTokens")
    .get();

  const tokens = snapshot.docs
    .map((doc) => {
      const token = doc.get("token");
      if (typeof token === "string" && token.trim().length > 0) {
        return token.trim();
      }
      const fallback = doc.id.trim();
      return fallback.length > 0 ? fallback : null;
    })
    .filter((token): token is string => Boolean(token));

  return Array.from(new Set(tokens));
};

const removeInvalidTokens = async (userId: string, tokens: string[]): Promise<void> => {
  if (tokens.length === 0) return;
  const batch = admin.firestore().batch();
  const baseRef = admin.firestore().collection("users").doc(userId).collection("notificationTokens");
  tokens.forEach((token) => {
    batch.delete(baseRef.doc(token));
  });
  await batch.commit();
};

const resolveCollectorLabel = async (userId: string): Promise<string> => {
  if (!userId) return "Someone";
  try {
    const snapshot = await admin.firestore().collection("users").doc(userId).get();
    if (!snapshot.exists) return "Someone";
    const displayName = snapshot.get("displayName");
    if (typeof displayName === "string" && displayName.trim().length > 0) {
      return displayName.trim();
    }
    const username = snapshot.get("username");
    if (typeof username === "string" && username.trim().length > 0) {
      const normalized = username.trim();
      return normalized.startsWith("@") ? normalized : `@${normalized}`;
    }
  } catch (error) {
    console.warn(`Failed to resolve collector label for ${userId}`, error);
  }
  return "Someone";
};

const truncate = (value: string, maxLength = 80): string => {
  if (value.length <= maxLength) return value;
  return `${value.slice(0, maxLength - 1)}…`;
};

const resolveDropLabel = (data: admin.firestore.DocumentData | undefined | null): string => {
  if (!data) return "your drop";
  const rawText = typeof data.text === "string" ? data.text.trim() : "";
  if (rawText.length > 0) {
    return `"${truncate(rawText)}"`;
  }
  const description = typeof data.description === "string" ? data.description.trim() : "";
  if (description.length > 0) {
    return `"${truncate(description)}"`;
  }
  const contentTypeRaw = typeof data.contentType === "string" ? data.contentType.trim().toUpperCase() : "TEXT";
  switch (contentTypeRaw) {
  case "PHOTO":
    return "your photo drop";
  case "AUDIO":
    return "your audio drop";
  case "VIDEO":
    return "your video drop";
  default:
    return "your drop";
  }
};

/** What actually happened to a send, as opposed to what was attempted. */
interface SendOutcome {
  delivered: number;
  failed: number;
  pruned: number;
}

/**
 * Sends to one user's devices and reports the result per device.
 *
 * The return value exists because the callers used to log "notified N" off the
 * fact that a send was attempted. During the 4.5 verification that read
 * "Notified 1" while the only token was stale and the push reached nobody — the
 * one number an operator would trust mid-pilot, wrong in the direction that
 * hides failure. FCM answers per token, so report that answer.
 *
 * @param {string} userId Owner of the tokens.
 * @param {string[]} tokens The device tokens to send to.
 * @param {object} message The multicast payload, minus its tokens.
 * @return {Promise<SendOutcome>} Delivered, failed, and pruned device counts.
 */
const sendToUserTokens = async (
  userId: string,
  tokens: string[],
  message: Omit<admin.messaging.MulticastMessage, "tokens">
): Promise<SendOutcome> => {
  const outcome: SendOutcome = {delivered: 0, failed: 0, pruned: 0};

  for (const batch of chunkArray(tokens, 500)) {
    if (batch.length === 0) continue;
    const response = await messaging.sendEachForMulticast({
      ...message,
      tokens: batch,
    });

    outcome.delivered += response.successCount;
    outcome.failed += response.failureCount;

    const invalid: string[] = [];
    response.responses.forEach((result, index) => {
      if (!result.success && result.error) {
        const code = result.error.code;
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token"
        ) {
          invalid.push(batch[index]);
        }
      }
    });

    if (invalid.length > 0) {
      await removeInvalidTokens(userId, invalid);
      outcome.pruned += invalid.length;
    }
  }

  return outcome;
};

const USERNAME_MIN_LENGTH = 3;
const USERNAME_MAX_LENGTH = 20;
const USERNAME_PATTERN = /^[a-z0-9._]+$/;
const GROUP_CODE_MIN_LENGTH = 4;
const GROUP_CODE_MAX_LENGTH = 32;
const GROUP_CODE_PATTERN = /^[A-Z0-9][A-Z0-9_-]*$/;

type ClaimExplorerUsernameRequest = {
  desiredUsername?: string;
};

type ManageGroupRequest = {
  action?: unknown;
  code?: unknown;
};

type GroupRole = "OWNER" | "SUBSCRIBER";

const sanitizeGroupCode = (raw: unknown): string => {
  const value = typeof raw === "string" ? raw : String(raw ?? "");
  const normalized = value.trim().toLocaleUpperCase("en-US");
  if (normalized.length < GROUP_CODE_MIN_LENGTH ||
      normalized.length > GROUP_CODE_MAX_LENGTH ||
      !GROUP_CODE_PATTERN.test(normalized)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Group codes must be 4 to 32 letters, numbers, underscores, or hyphens."
    );
  }
  return normalized;
};

const sanitizeExplorerUsername = (raw: unknown): string => {
  const value = typeof raw === "string" ? raw : String(raw ?? "");
  const trimmed = value.trim();
  if (trimmed.length < USERNAME_MIN_LENGTH) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Username must be at least 3 characters long.",
      {reason: "TOO_SHORT"}
    );
  }
  if (trimmed.length > USERNAME_MAX_LENGTH) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Username must be at most 20 characters long.",
      {reason: "TOO_LONG"}
    );
  }

  const normalized = trimmed.toLocaleLowerCase("en-US");
  if (!USERNAME_PATTERN.test(normalized)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Username contains invalid characters.",
      {reason: "INVALID_CHARACTERS"}
    );
  }

  return normalized;
};

// HTTPS callable — SafeSearch from base64
export const safeSearch = functions
  .region("us-central1")
  .runWith({enforceAppCheck: true, timeoutSeconds: 30, memory: "512MB"})
  .https.onCall(async (data: { base64?: unknown }, context) => {
    const uid = context.auth?.uid;
    if (!uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Sign in before scanning media."
      );
    }

    if (typeof data?.base64 !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Provide a base64-encoded image."
      );
    }

    const base64 = data.base64.trim();
    if (!base64 || base64.length > MAX_SAFE_SEARCH_BASE64_LENGTH ||
        !/^[A-Za-z0-9+/]+={0,2}$/.test(base64)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "The image payload is empty, malformed, or too large."
      );
    }

    const image = Buffer.from(base64, "base64");
    if (!image.length || image.length > MAX_SAFE_SEARCH_BYTES) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "The decoded image is empty or too large."
      );
    }

    try {
      const [result] = await vision.annotateImage({
        image: {content: image},
        features: [{type: "SAFE_SEARCH_DETECTION"}],
      });
      const s = result.safeSearchAnnotation;
      if (!s) {
        throw new Error("Vision returned no SafeSearch annotation");
      }
      return {
        adult: s.adult,
        spoof: s.spoof,
        medical: s.medical,
        violence: s.violence,
        racy: s.racy,
      };
    } catch (error) {
      console.error("SafeSearch Vision request failed", error);
      throw new functions.https.HttpsError(
        "unavailable",
        "Media safety scanning is temporarily unavailable."
      );
    }
  });

interface RedeemDropRequest {
  dropId?: unknown;
}

const REDEMPTION_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

/**
 * Per-user redemption code. Unambiguous alphabet (no I/O/0/1) because these get
 * read aloud and typed at a counter.
 *
 * @return {string} A formatted eight-character code, e.g. "A1B2-C3D4".
 */
const generateRedemptionCode = (): string => {
  const bytes = crypto.randomBytes(8);
  let code = "";
  for (const byte of bytes) {
    code += REDEMPTION_CODE_ALPHABET[byte % REDEMPTION_CODE_ALPHABET.length];
  }
  return `${code.slice(0, 4)}-${code.slice(4)}`;
};

/**
 * Task 4.3 (ADR P6) — redemption is server-only, and the code never lives on the
 * drop document.
 *
 * `redemptionCode` used to be a field on the drop. Firestore cannot restrict
 * individual fields, and drops are readable by any signed-in user and by guests
 * for public drops, so every reader received the code; `allow list` allowed bulk
 * harvesting. A code you can obtain without visiting the location is not a
 * proximity-gated reward.
 *
 * This callable performs the whole transaction with the Admin SDK and returns a
 * code to the caller alone. The issued code is also recorded at
 * `users/{uid}/redemptions/{dropId}`, which only that user can read.
 */
export const redeemDrop = functions
  .region("us-central1")
  .https.onCall(async (data: RedeemDropRequest, context) => {
    const uid = context.auth?.uid;
    if (!uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Sign in to redeem this offer."
      );
    }
    if (context.auth?.token?.firebase?.sign_in_provider === "anonymous") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Create an account to redeem offers."
      );
    }

    const dropId = typeof data?.dropId === "string" ? data.dropId.trim() : "";
    if (!dropId) {
      throw new functions.https.HttpsError("invalid-argument", "Missing drop id.");
    }

    const firestore = admin.firestore();
    const dropRef = firestore.collection("drops").doc(dropId);
    const receiptRef = firestore
      .collection("users")
      .doc(uid)
      .collection("redemptions")
      .doc(dropId);

    return firestore.runTransaction(async (transaction) => {
      const [dropSnapshot, receiptSnapshot] = await Promise.all([
        transaction.get(dropRef),
        transaction.get(receiptRef),
      ]);

      if (!dropSnapshot.exists) {
        throw new functions.https.HttpsError("not-found", "This offer no longer exists.");
      }
      const drop = dropSnapshot.data() ?? {};

      if (drop.isDeleted === true) {
        throw new functions.https.HttpsError("not-found", "This offer no longer exists.");
      }
      if (drop.dropType !== "RESTAURANT_COUPON") {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "This drop is not a redeemable offer."
        );
      }

      // Same expiry arithmetic as firestore.rules isNotExpired().
      const decayDays = typeof drop.decayDays === "number" ? drop.decayDays : 0;
      const createdAt = typeof drop.createdAt === "number" ? drop.createdAt : 0;
      if (decayDays > 0 && createdAt > 0 && Date.now() >= createdAt + decayDays * 86400000) {
        throw new functions.https.HttpsError("failed-precondition", "This offer has expired.");
      }

      // Idempotent: a caller who already redeemed gets their existing code back
      // rather than a second one, so a retry cannot consume two redemptions.
      if (receiptSnapshot.exists) {
        const existing = receiptSnapshot.get("code");
        if (typeof existing === "string" && existing.length > 0) {
          return {
            code: existing,
            alreadyRedeemed: true,
            redemptionCount: typeof drop.redemptionCount === "number" ? drop.redemptionCount : 0,
            redemptionLimit: typeof drop.redemptionLimit === "number" ? drop.redemptionLimit : null,
            redeemedAt: receiptSnapshot.get("redeemedAt") ?? null,
          };
        }
      }

      const redeemedBy = (drop.redeemedBy ?? {}) as Record<string, unknown>;
      if (Object.prototype.hasOwnProperty.call(redeemedBy, uid)) {
        throw new functions.https.HttpsError(
          "already-exists",
          "You have already redeemed this offer."
        );
      }

      const count = typeof drop.redemptionCount === "number" ? drop.redemptionCount : 0;
      const limit = typeof drop.redemptionLimit === "number" ? drop.redemptionLimit : null;
      if (limit !== null && count >= limit) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          "This offer has been fully redeemed."
        );
      }

      const code = generateRedemptionCode();
      const redeemedAt = Date.now();

      transaction.update(dropRef, {
        [`redeemedBy.${uid}`]: redeemedAt,
        redemptionCount: count + 1,
      });
      transaction.set(receiptRef, {
        dropId,
        code,
        redeemedAt,
        businessId: typeof drop.businessId === "string" ? drop.businessId : null,
      });

      // The clients render count/limit alongside the code, so return them here
      // rather than making every caller re-read the drop after redeeming.
      return {
        code,
        alreadyRedeemed: false,
        redemptionCount: count + 1,
        redemptionLimit: limit,
        redeemedAt,
      };
    });
  });

export const claimExplorerUsername = functions
  .region("us-central1")
  .https.onCall(async (data: ClaimExplorerUsernameRequest, context) => {
    const uid = context.auth?.uid;
    if (!uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Sign in again to update your username."
      );
    }

    const sanitized = sanitizeExplorerUsername(data?.desiredUsername);
    const firestore = admin.firestore();
    const usernames = firestore.collection("usernames");
    const users = firestore.collection("users");

    await firestore.runTransaction(async (transaction) => {
      const userRef = users.doc(uid);
      const usernameRef = usernames.doc(sanitized);

      const [userSnapshot, usernameSnapshot] = await Promise.all([
        transaction.get(userRef),
        transaction.get(usernameRef),
      ]);

      const currentUsername = userSnapshot.get("username") as string | undefined;
      const existingOwner = usernameSnapshot.get("userId") as string | undefined;

      if (existingOwner && existingOwner !== uid) {
        throw new functions.https.HttpsError(
          "already-exists",
          "That username is already taken. Try another one."
        );
      }

      transaction.set(usernameRef, {userId: uid}, {merge: true});

      if (currentUsername && currentUsername !== sanitized) {
        transaction.delete(usernames.doc(currentUsername));
      }

      transaction.set(userRef, {username: sanitized}, {merge: true});
    });

    return {username: sanitized};
  });

type UpdateBusinessProfileRequest = {
  businessName?: unknown;
  businessCategories?: unknown;
};

export const updateBusinessProfile = functions
  .region("us-central1")
  .runWith({enforceAppCheck: true})
  .https.onCall(async (data: UpdateBusinessProfileRequest, context) => {
    const uid = context.auth?.uid;
    if (!uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Sign in before updating a business profile."
      );
    }
    const businessName = typeof data?.businessName === "string" ?
      data.businessName.trim() : "";
    const rawCategories = Array.isArray(data?.businessCategories) ?
      data.businessCategories : [];
    const businessCategories = Array.from(new Set(rawCategories
      .filter((value): value is string => typeof value === "string")
      .map((value) => value.trim())
      .filter((value) => value.length > 0)));

    if (!businessName || businessName.length > 100 ||
        businessCategories.length < 1 || businessCategories.length > 10 ||
        businessCategories.some((value) => value.length > 50)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Provide a valid business name and 1 to 10 categories."
      );
    }

    const firestore = admin.firestore();
    const userRef = firestore.collection("users").doc(uid);
    const creatorProfileRef = firestore.collection("creatorProfiles").doc(uid);
    await firestore.runTransaction(async (transaction) => {
      const user = await transaction.get(userRef);
      if (!user.exists || user.get("role") !== "BUSINESS" ||
          user.get("organizerAccessStatus") !== "APPROVED") {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Organizer approval is required before editing a business profile.",
          {
            reason: "ORGANIZER_APPROVAL_REQUIRED",
            retryable: false,
            field: null,
            retryAfterSeconds: null,
            distanceBucket: null,
            contractVersion: 1,
          }
        );
      }
      const now = FieldValue.serverTimestamp();
      transaction.set(userRef, {
        businessName,
        businessCategories,
      }, {merge: true});
      transaction.set(creatorProfileRef, {
        schemaVersion: 1,
        hostLabel: businessName,
        username: user.get("username") ?? null,
        organizationName: businessName,
        updatedAt: now,
      });
    });

    return {businessName, businessCategories, role: "BUSINESS"};
  });

/**
 * Group codes are invitation capabilities. Clients may read their own
 * membership documents, but all group and membership writes are performed by
 * this callable so a signed-in user cannot enumerate groups or self-authorize
 * with a direct Firestore write.
 */
export const manageGroup = functions
  .region("us-central1")
  .runWith({enforceAppCheck: true})
  .https.onCall(async (data: ManageGroupRequest, context) => {
    const uid = context.auth?.uid;
    if (!uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Sign in before managing groups."
      );
    }
    if (context.auth?.token.suspended === true) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "This account is suspended."
      );
    }

    const action = typeof data?.action === "string" ?
      data.action.trim().toLocaleUpperCase("en-US") : "";
    if (!(["CREATE", "JOIN", "LEAVE"] as const).includes(
      action as "CREATE" | "JOIN" | "LEAVE"
    )) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Choose CREATE, JOIN, or LEAVE."
      );
    }

    const code = sanitizeGroupCode(data?.code);
    const firestore = admin.firestore();
    const groupRef = firestore.collection("groups").doc(code);
    const membershipRef = firestore
      .collection("users")
      .doc(uid)
      .collection("groups")
      .doc(code);

    if (action === "LEAVE") {
      await firestore.runTransaction(async (transaction) => {
        const [groupSnapshot, membershipSnapshot] = await Promise.all([
          transaction.get(groupRef),
          transaction.get(membershipRef),
        ]);
        if (!membershipSnapshot.exists) return;
        if (groupSnapshot.get("ownerId") === uid) {
          throw new functions.https.HttpsError(
            "failed-precondition",
            "Group owners cannot leave until ownership transfer is supported."
          );
        }
        transaction.delete(membershipRef);
      });
      return {code, left: true};
    }

    const membership = await firestore.runTransaction(async (transaction) => {
      const [groupSnapshot, membershipSnapshot] = await Promise.all([
        transaction.get(groupRef),
        transaction.get(membershipRef),
      ]);
      const existingOwner = groupSnapshot.get("ownerId");
      let ownerId: string;
      let role: GroupRole;

      if (action === "CREATE") {
        if (groupSnapshot.exists && existingOwner !== uid) {
          throw new functions.https.HttpsError(
            "already-exists",
            "That group code is already in use."
          );
        }
        ownerId = uid;
        role = "OWNER";
        if (!groupSnapshot.exists) {
          transaction.create(groupRef, {
            ownerId,
            createdAt: FieldValue.serverTimestamp(),
            updatedAt: FieldValue.serverTimestamp(),
          });
        }
      } else {
        if (!groupSnapshot.exists || typeof existingOwner !== "string" ||
            existingOwner.trim().length === 0) {
          throw new functions.https.HttpsError(
            "not-found",
            "That group does not exist."
          );
        }
        ownerId = existingOwner;
        role = ownerId === uid ? "OWNER" : "SUBSCRIBER";
      }

      const currentRole = membershipSnapshot.get("role");
      const currentOwnerId = membershipSnapshot.get("ownerId");
      if (!membershipSnapshot.exists || currentRole !== role ||
          currentOwnerId !== ownerId) {
        transaction.set(membershipRef, {
          code,
          role,
          ownerId,
          updatedAt: FieldValue.serverTimestamp(),
        });
      }

      return {code, ownerId, role};
    });

    return membership;
  });

const normalizeDropMediaPath = (raw: unknown): string | null => {
  if (typeof raw !== "string") return null;
  const normalized = raw.trim().replace(/^\/+/, "");
  if (!/^drops\/[^/]+\/[^/]+$/.test(normalized)) return null;
  return normalized;
};

const setDropMediaAccess = async (
  storagePath: string,
  dropId: string,
  ownerId: string,
  accessLevel: "PUBLIC" | "PRIVATE",
  safetyStatus: "SAFE" | "BLOCKED"
): Promise<void> => {
  const file = admin.storage().bucket().file(storagePath);
  const [exists] = await file.exists();
  if (!exists) {
    console.error(`Drop ${dropId} references missing media ${storagePath}.`);
    return;
  }

  const [fileMetadata] = await file.getMetadata();
  const existingOwnerId = fileMetadata.metadata?.ownerId;
  const existingDropId = fileMetadata.metadata?.dropId;
  if (existingOwnerId !== ownerId ||
      (existingDropId && existingDropId !== dropId)) {
    console.error(
      `Refused to bind media ${storagePath} to drop ${dropId}: ` +
      "object ownership or existing association did not match."
    );
    return;
  }
  const customMetadata = {
    ...(fileMetadata.metadata ?? {}),
    ownerId,
    dropId,
    accessLevel,
    safetyStatus,
  } as Record<string, string | null>;

  // Firebase download URLs contain a bearer token and bypass Storage Rules.
  // Remove every legacy token; clients store a token-free REST URL whose read
  // is evaluated by Storage Rules instead.
  customMetadata.firebaseStorageDownloadTokens = null;
  await file.setMetadata({
    metadata: customMetadata as unknown as Record<string, string>,
  });
};

export const synchronizeDropMediaAccess = functions
  .region("us-central1")
  .firestore.document("drops/{dropId}")
  .onWrite(async (change, context) => {
    const after = change.after.exists ? change.after.data() : undefined;
    const before = change.before.exists ? change.before.data() : undefined;
    const data = after ?? before;
    if (!data) return;

    const storagePath = normalizeDropMediaPath(data.mediaStoragePath);
    if (!storagePath) return;
    const ownerId = typeof data.createdBy === "string" ?
      data.createdBy.trim() : "";
    if (!ownerId) {
      console.error(`Drop ${context.params.dropId} has media without an owner.`);
      return;
    }

    const isPublicAndSafe = Boolean(after) &&
      after?.isDeleted === false &&
      after?.isNsfw === false &&
      after?.visibility === "PUBLIC";

    await setDropMediaAccess(
      storagePath,
      String(context.params.dropId),
      ownerId,
      isPublicAndSafe ? "PUBLIC" : "PRIVATE",
      isPublicAndSafe ? "SAFE" : "BLOCKED"
    );
  });

// Storage trigger — analyze photo uploads under drops/photos
export const analyzeOnUpload = functions
  .region("us-central1")
  .storage.object()
  .onFinalize(async (object: functions.storage.ObjectMetadata) => {
    const name = object.name || "";
    if (!name.startsWith("drops/photos/")) return;

    const contentType = object.contentType || "";
    if (!contentType.startsWith("image/")) return;

    const bucketName = object.bucket;
    if (!bucketName) return;

    const bucket = admin.storage().bucket(bucketName);
    const file = bucket.file(name);
    const [buffer] = await file.download();

    const [result] = await vision.annotateImage({
      image: {content: buffer},
      features: [{type: "SAFE_SEARCH_DETECTION"}],
    });
    const s = result.safeSearchAnnotation;


    const moderation = {
      adult: s?.adult,
      spoof: s?.spoof,
      medical: s?.medical,
      violence: s?.violence,
      racy: s?.racy,
      analyzedAt: FieldValue.serverTimestamp(),
      sourceObject: name,
    };

    const firestore = admin.firestore();
    const dropsSnapshot = await firestore
      .collection("drops")
      .where("mediaStoragePath", "==", name)
      .get();

    const queueRef = moderationQueueRef(name);

    if (dropsSnapshot.empty) {
      await queueRef.set({moderation});
      return;
    }

    const batch = firestore.batch();
    dropsSnapshot.docs.forEach((doc) => {
      batch.set(doc.ref, {moderation}, {merge: true});
    });
    batch.delete(queueRef);
    await batch.commit();
  });

export const applyPendingModeration = functions
  .region("us-central1")
  .firestore.document("drops/{dropId}")
  .onWrite(async (change) => {
    if (!change.after.exists) return;

    const data = change.after.data();
    if (!data) return;

    const storagePath = data.mediaStoragePath as string | undefined;
    if (!storagePath) return;

    const queueRef = moderationQueueRef(storagePath);
    const pending = await queueRef.get();
    if (!pending.exists) return;

    const moderation = pending.get("moderation");
    if (!moderation) {
      await queueRef.delete();
      return;
    }

    await change.after.ref.set({moderation}, {merge: true});
    await queueRef.delete();
  });

export const notifyDropCreatorOnCollection = functions
  .region("us-central1")
  .firestore.document("drops/{dropId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    if (!after) return;

    if (after.isDeleted === true) return;
    if (after.isNsfw === true || after.visibility !== "PUBLIC") return;

    const creatorRaw = after.createdBy;
    const creatorId = typeof creatorRaw === "string" ? creatorRaw.trim() : "";
    if (!creatorId) return;

    const beforeCollectors = extractCollectorIds(before?.collectedBy);
    const afterCollectors = extractCollectorIds(after.collectedBy);

    if (afterCollectors.size === 0) return;

    const newCollectors = Array.from(afterCollectors).filter(
      (uid) => !beforeCollectors.has(uid) && uid !== creatorId
    );

    if (newCollectors.length === 0) return;

    const tokens = await fetchNotificationTokens(creatorId);
    if (tokens.length === 0) return;

    const collectorLabel = await resolveCollectorLabel(newCollectors[0]);
    const dropLabel = resolveDropLabel(after);
    const othersCount = Math.max(newCollectors.length - 1, 0);

    let body: string;
    if (othersCount <= 0) {
      body = `${collectorLabel} picked up ${dropLabel}.`;
    } else if (othersCount === 1) {
      body = `${collectorLabel} and ${othersCount} other picked up ${dropLabel}.`;
    } else {
      body = `${collectorLabel} and ${othersCount} others picked up ${dropLabel}.`;
    }

    const dropId = String(context.params.dropId ?? "");
    if (!dropId) return;

    const contentType = typeof after.contentType === "string" ?
      after.contentType :
      "TEXT";

    const data: Record<string, string> = {
      event: "DROP_COLLECTED",
      dropId,
      collectorName: collectorLabel,
      collectorCount: String(newCollectors.length),
      dropContentType: contentType,
      title: "Your drop was collected!",
      body,
      dropLabel,
    };

    const text = typeof after.text === "string" ? after.text.trim() : "";
    if (text) {
      data.dropTitle = truncate(text, 200);
    }
    const description = typeof after.description === "string" ? after.description.trim() : "";
    if (description) {
      data.dropDescription = truncate(description, 200);
    }

    const message: Omit<admin.messaging.MulticastMessage, "tokens"> = {
      android: {
        priority: "high",
      },
      data,
    };

    const outcome = await sendToUserTokens(creatorId, tokens, message);
    console.log(
      `Collection alert for drop ${dropId} (${newCollectors.length} new collector(s)): ` +
      `delivered to ${outcome.delivered}/${tokens.length} device(s) for ${creatorId}` +
      `${outcome.failed > 0 ? `, ${outcome.failed} failed` : ""}` +
      `${outcome.pruned > 0 ? `, ${outcome.pruned} stale token(s) removed` : ""}.`
    );
  });

/**
 * Task 4.5 — the recipient's own opt-out, read where the send happens.
 *
 * Membership decides who may be notified; this decides who still wants to be. A
 * missing document means opted in: joining an experience is the opt-in, so a
 * member who never opened the setting hears about the experiences they joined.
 * Any read failure also means opted in — a transient Firestore error must not
 * silently unsubscribe an attendee mid-event.
 *
 * @param {string} userId The member being considered for a send.
 * @return {Promise<boolean>} Whether this member still wants experience alerts.
 */
const wantsExperienceAlerts = async (userId: string): Promise<boolean> => {
  try {
    const snapshot = await admin
      .firestore()
      .collection("users")
      .doc(userId)
      .collection("notificationSettings")
      .doc("preferences")
      .get();
    if (!snapshot.exists) return true;
    return snapshot.get("experienceAlertsEnabled") !== false;
  } catch (error) {
    console.error(`Failed to read alert preference for ${userId}; defaulting to enabled.`, error);
    return true;
  }
};

/**
 * Task 3.4 — alerts are membership-scoped, not proximity-scoped. When a drop is added
 * to an experience, everyone who explicitly joined that experience is notified. This
 * replaces the on-device geofences that required ACCESS_BACKGROUND_LOCATION, and it is
 * what the launch scope actually asks for: "push notifications only for experiences the
 * user explicitly joined".
 */
export const notifyGroupMembersOnDropCreated = functions
  .region("us-central1")
  .firestore.document("drops/{dropId}")
  .onCreate(async (snapshot, context) => {
    const drop = snapshot.data();
    if (!drop) return;

    if (drop.isDeleted === true) return;
    if (drop.isNsfw === true) return;
    if (drop.visibility !== "GROUP") return;

    const groupCode = typeof drop.groupCode === "string" ? drop.groupCode.trim() : "";
    if (!groupCode) return;

    const creatorRaw = drop.createdBy;
    const creatorId = typeof creatorRaw === "string" ? creatorRaw.trim() : "";

    const memberships = await admin
      .firestore()
      .collectionGroup("groups")
      .where("code", "==", groupCode)
      .get();

    const memberIds = Array.from(
      new Set(
        memberships.docs
          .map((doc) => doc.ref.parent.parent?.id ?? "")
          .filter((uid) => uid.length > 0 && uid !== creatorId)
      )
    );

    if (memberIds.length === 0) return;

    const dropId = String(context.params.dropId ?? "");
    if (!dropId) return;

    const dropLabel = resolveDropLabel(drop);
    const contentType = typeof drop.contentType === "string" ? drop.contentType : "TEXT";

    const data: Record<string, string> = {
      event: "DROP_ADDED_TO_EXPERIENCE",
      dropId,
      groupCode,
      dropContentType: contentType,
      title: "New drop in your experience",
      body: `${dropLabel} is waiting to be found.`,
      dropLabel,
    };

    const text = typeof drop.text === "string" ? drop.text.trim() : "";
    if (text) {
      data.dropTitle = truncate(text, 200);
    }

    const message: Omit<admin.messaging.MulticastMessage, "tokens"> = {
      android: {
        priority: "high",
      },
      data,
    };

    // Deliberately no location in the payload: the notification says a drop exists in
    // an experience you joined, never where the recipient is.
    let reached = 0;
    let devices = 0;
    let failed = 0;
    let pruned = 0;
    let optedOut = 0;
    let tokenless = 0;

    for (const memberId of memberIds) {
      if (!(await wantsExperienceAlerts(memberId))) {
        optedOut += 1;
        continue;
      }
      const tokens = await fetchNotificationTokens(memberId);
      if (tokens.length === 0) {
        // Alerts on, nowhere to send: the silent failure worth counting.
        tokenless += 1;
        continue;
      }
      const outcome = await sendToUserTokens(memberId, tokens, message);
      devices += outcome.delivered;
      failed += outcome.failed;
      pruned += outcome.pruned;
      if (outcome.delivered > 0) reached += 1;
    }

    // One line, and every number in it is an outcome rather than an attempt.
    console.log(
      `Drop ${dropId} in group ${groupCode}: reached ${reached}/${memberIds.length} member(s) ` +
      `on ${devices} device(s); ${optedOut} opted out, ${tokenless} had no registered device, ` +
      `${failed} send(s) failed, ${pruned} stale token(s) removed.`
    );
  });

const analyticsRef = (groupCode: string) =>
  admin.firestore().collection("groups").doc(groupCode).collection("analytics").doc("summary");

const countKeys = (value: unknown): number =>
  value && typeof value === "object" ? Object.keys(value as Record<string, unknown>).length : 0;

/**
 * Task 4.4 (ADR P7) — organiser rollup, maintained incrementally.
 *
 * Per-drop counts already live on the drop; what an organiser wants is the total
 * across an experience. Aggregating that client-side would mean reading every drop
 * on every dashboard open and would answer differently per device, so it is
 * computed here and stored in a document only the owner can read.
 *
 * Aggregates only: the rollup deliberately records no attendee identity.
 */
export const rollUpExperienceActivity = functions
  .region("us-central1")
  .firestore.document("drops/{dropId}")
  .onWrite(async (change) => {
    const after = change.after.exists ? change.after.data() ?? {} : {};
    const before = change.before.exists ? change.before.data() ?? {} : {};

    const groupCode = typeof after.groupCode === "string" ?
      after.groupCode.trim() :
      (typeof before.groupCode === "string" ? before.groupCode.trim() : "");
    if (!groupCode) return;

    const collectDelta = countKeys(after.collectedBy) - countKeys(before.collectedBy);
    const redemptionDelta = countKeys(after.redeemedBy) - countKeys(before.redeemedBy);
    const dropDelta =
      (change.after.exists && after.isDeleted !== true ? 1 : 0) -
      (change.before.exists && before.isDeleted !== true ? 1 : 0);

    if (collectDelta === 0 && redemptionDelta === 0 && dropDelta === 0) return;

    await analyticsRef(groupCode).set({
      groupCode,
      collects: FieldValue.increment(collectDelta),
      redemptions: FieldValue.increment(redemptionDelta),
      drops: FieldValue.increment(dropDelta),
      updatedAt: Date.now(),
    }, {merge: true});
  });

/**
 * Incremental counters drift when a trigger fails or retries, and the failure is
 * silent — the dashboard keeps showing a number that is quietly wrong, which for
 * analytics an organiser pays for is worse than showing nothing. This recomputes
 * every rollup from source and corrects it.
 */
export const reconcileExperienceActivity = functions
  .region("us-central1")
  .pubsub.schedule("every 24 hours")
  .onRun(async () => {
    const firestore = admin.firestore();
    const drops = await firestore.collection("drops").get();

    const totals = new Map<string, {collects: number; redemptions: number; drops: number}>();
    drops.forEach((doc) => {
      const data = doc.data();
      const groupCode = typeof data.groupCode === "string" ? data.groupCode.trim() : "";
      if (!groupCode) return;
      const entry = totals.get(groupCode) ?? {collects: 0, redemptions: 0, drops: 0};
      entry.collects += countKeys(data.collectedBy);
      entry.redemptions += countKeys(data.redeemedBy);
      if (data.isDeleted !== true) entry.drops += 1;
      totals.set(groupCode, entry);
    });

    let corrected = 0;
    for (const [groupCode, entry] of totals) {
      const ref = analyticsRef(groupCode);
      const current = await ref.get();
      const drifted =
        current.get("collects") !== entry.collects ||
        current.get("redemptions") !== entry.redemptions ||
        current.get("drops") !== entry.drops;
      if (!drifted) continue;
      await ref.set({groupCode, ...entry, updatedAt: Date.now(), reconciledAt: Date.now()},
        {merge: true});
      corrected += 1;
    }

    console.log(`Reconciled ${totals.size} experience rollup(s); corrected ${corrected}.`);
  });

export const cleanupCollectedNotesOnDropDelete = functions
  .region("us-central1")
  .firestore.document("drops/{dropId}")
  .onUpdate(async (change, context) => {
    const beforeDeleted = change.before.get("isDeleted") === true;
    const afterDeleted = change.after.get("isDeleted") === true;

    if (!afterDeleted || beforeDeleted) return;

    const dropData = change.after.data();
    if (!dropData) return;

    const rawCollectedBy = dropData.collectedBy;
    if (!rawCollectedBy || typeof rawCollectedBy !== "object") return;

    const createdBy = typeof dropData.createdBy === "string" ? dropData.createdBy : undefined;
    const dropId = context.params.dropId as string;

    const collectorIds = Array.from(
      new Set(
        Object.entries(rawCollectedBy)
          .filter(([uid, value]) => typeof uid === "string" && uid.trim().length > 0 && Boolean(value))
          .map(([uid]) => uid.trim())
          .filter((uid) => uid.length > 0 && uid !== createdBy)
      )
    );

    if (collectorIds.length === 0) return;

    const firestore = admin.firestore();

    for (const batchCollectors of chunkArray(collectorIds, 500)) {
      const batch = firestore.batch();
      batchCollectors.forEach((uid) => {
        const inventoryRef = firestore
          .collection("users")
          .doc(uid)
          .collection("inventory")
          .doc(dropId);
        batch.delete(inventoryRef);
      });
      await batch.commit();
    }

    console.log(
      `Removed drop ${dropId} from ${collectorIds.length} collected inventories after deletion.`
    );
  });

export {
  deleteAccount,
  mergeGuestAccount,
  purgeExpiredAccountExports,
  purgeExpiredDeletionReceipts,
  requestAccountExport,
} from "./accountLifecycle";

export {
  getLegalPolicyManifest,
  recordLegalAcceptance,
} from "./legalConsent";

export {
  alertOverdueModerationCases,
  decideModerationAppeal,
  decideModerationCase,
  ingestUserReport,
  ingestRedesignReport,
  listModerationQueue,
  submitModerationAppeal,
  triageModerationCase,
} from "./moderationOperations";

export {
  blockHost,
  correctRewardCodeUse,
  createExperience,
  createOrganizerApplicationLink,
  deleteDrop,
  experienceEntryPage,
  getCollectionMedia,
  getOrganizerDrop,
  ingestOrganizerApplication,
  joinExperience,
  leaveExperience,
  listRewardCodes,
  markRewardCodeUsed,
  provisionRewardCodes,
  purgeExpiredRedesignData,
  reconcileRedesignResults,
  recordAuthCompletion,
  recordClientEvent,
  resolveExperience,
  saveDrop,
  sanitizeDropStagingUpload,
  setDropLike,
  setOrganizerAccessDecision,
  submitFeedback,
  submitReport,
  unlockDrop,
  unblockHost,
  updateExperience,
} from "./redesign";
