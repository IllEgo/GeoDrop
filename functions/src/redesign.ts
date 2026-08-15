import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as functions from "firebase-functions/v1";
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {
  renderExperienceEntryNotFound,
  renderExperienceEntryPage,
} from "./entryPage";

const REGION = "us-central1";
const CONTRACT_VERSION = 1;
const ANALYTICS_RETENTION_DAYS = 180;
const APPLICATION_TOKEN_LIFETIME_MS = 30 * 60 * 1000;
const MEDIA_URL_LIFETIME_MS = 10 * 60 * 1000;
const MAX_LOCATION_AGE_MS = 30 * 1000;
const MAX_STAGING_IMAGE_BYTES = 10 * 1024 * 1024;
const EXPERIENCE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const REWARD_CODE_PATTERN = /^[A-Z0-9][A-Z0-9-]{3,31}$/;

type CallableContext = functions.https.CallableContext;
type JsonMap = Record<string, unknown>;

const protectedCallable = functions
  .region(REGION)
  .runWith({
    enforceAppCheck: true,
    secrets: ["ANALYTICS_HMAC_SECRET"],
  });

const errorDetails = (
  reason: string,
  overrides: Record<string, unknown> = {}
): Record<string, unknown> => ({
  reason,
  retryable: false,
  field: null,
  retryAfterSeconds: null,
  distanceBucket: null,
  contractVersion: CONTRACT_VERSION,
  ...overrides,
});

const fail = (
  code: functions.https.FunctionsErrorCode,
  reason: string,
  message: string,
  overrides: Record<string, unknown> = {}
): never => {
  throw new functions.https.HttpsError(
    code,
    message,
    errorDetails(reason, overrides)
  );
};

const asObject = (raw: unknown, field = "request"): JsonMap => {
  if (!raw || typeof raw !== "object" || Array.isArray(raw)) {
    return fail("invalid-argument", "INVALID_REQUEST", "Provide a valid request.", {
      field,
    });
  }
  return raw as JsonMap;
};

const assertOnlyKeys = (
  value: JsonMap,
  allowed: string[],
  field = "request"
): void => {
  if (Object.keys(value).some((key) => !allowed.includes(key))) {
    fail("invalid-argument", "INVALID_REQUEST", "The request contains unsupported fields.", {
      field,
    });
  }
};

const requireApiVersion = (value: JsonMap): void => {
  if (value.apiVersion !== CONTRACT_VERSION) {
    fail(
      "failed-precondition",
      "CONTRACT_VERSION_UNSUPPORTED",
      "Update Kithe before continuing."
    );
  }
};

const textValue = (
  raw: unknown,
  field: string,
  minLength: number,
  maxLength: number,
  optional = false
): string | null => {
  if (raw === null || raw === undefined) {
    if (optional) return null;
    return fail("invalid-argument", "INVALID_REQUEST", `Provide ${field}.`, {field});
  }
  if (typeof raw !== "string") {
    return fail("invalid-argument", "INVALID_REQUEST", `Provide valid ${field}.`, {field});
  }
  const normalized = raw.trim();
  if (normalized.length < minLength || normalized.length > maxLength) {
    return fail("invalid-argument", "INVALID_REQUEST", `Provide valid ${field}.`, {field});
  }
  return normalized;
};

const optionalText = (
  raw: unknown,
  field: string,
  maxLength: number
): string | null => {
  if (raw === null || raw === undefined || raw === "") return null;
  return textValue(raw, field, 1, maxLength, true);
};

const timestampValue = (raw: unknown, field: string): Timestamp => {
  if (typeof raw !== "string") {
    return fail("invalid-argument", "INVALID_REQUEST", `Provide valid ${field}.`, {field});
  }
  const milliseconds = Date.parse(raw);
  if (!Number.isFinite(milliseconds)) {
    return fail("invalid-argument", "INVALID_REQUEST", `Provide valid ${field}.`, {field});
  }
  return Timestamp.fromMillis(milliseconds);
};

const requireActiveUser = (
  context: CallableContext,
  nonAnonymous = false
): string => {
  const uid = context.auth?.uid;
  if (!uid) {
    return fail("unauthenticated", "ACCOUNT_REQUIRED", "Sign in before continuing.");
  }
  if (context.auth?.token.suspended === true) {
    return fail("permission-denied", "ACCOUNT_SUSPENDED", "This account is suspended.");
  }
  if (nonAnonymous &&
      context.auth?.token.firebase?.sign_in_provider === "anonymous") {
    return fail(
      "failed-precondition",
      "ACCOUNT_REQUIRED",
      "Finish creating an account before continuing."
    );
  }
  return uid;
};

const requireAdmin = (context: CallableContext): string => {
  const uid = requireActiveUser(context, true);
  if (context.auth?.token.admin !== true && context.auth?.token.operator !== true) {
    return fail("permission-denied", "ADMIN_REQUIRED", "Operator access is required.");
  }
  return uid;
};

const normalizeExperienceCode = (raw: unknown): string => {
  const value = typeof raw === "string" ? raw.trim().toUpperCase() : "";
  if (!/^[A-Z0-9]{4,32}$/.test(value)) {
    return fail("invalid-argument", "INVALID_CODE", "Enter a valid Experience code.", {
      field: "code",
    });
  }
  return value;
};

const generateExperienceCode = (): string => Array.from({length: 8})
  .map(() => EXPERIENCE_CODE_ALPHABET[crypto.randomInt(EXPERIENCE_CODE_ALPHABET.length)])
  .join("");

const isoTimestamp = (raw: unknown): string | null => {
  if (raw instanceof Timestamp) return raw.toDate().toISOString();
  if (raw instanceof Date) return raw.toISOString();
  return null;
};

const availabilityFor = (data: admin.firestore.DocumentData): string => {
  if (data.state === "CANCELLED") return "CANCELLED";
  const now = Date.now();
  const startsAt = data.startsAt instanceof Timestamp ? data.startsAt.toMillis() : 0;
  const endsAt = data.endsAt instanceof Timestamp ? data.endsAt.toMillis() : 0;
  if (now < startsAt) return "UPCOMING";
  if (endsAt > 0 && now >= endsAt) return "ENDED";
  return "ACTIVE";
};

const analyticsSecret = (): string => {
  const configured = process.env.ANALYTICS_HMAC_SECRET?.trim();
  if (configured) return configured;
  if (process.env.FIRESTORE_EMULATOR_HOST || process.env.FUNCTIONS_EMULATOR === "true") {
    return "geodrop-emulator-analytics-secret-do-not-use-in-production";
  }
  return fail(
    "failed-precondition",
    "SERVER_CONFIGURATION_REQUIRED",
    "Analytics protection is not configured."
  );
};

const protectedKey = (namespace: string, value: string): string => crypto
  .createHmac("sha256", analyticsSecret())
  .update(`${namespace}:${value}`)
  .digest("hex");

const enforceRateLimit = async (
  scope: string,
  subject: string,
  limit: number,
  windowSeconds: number
): Promise<void> => {
  const windowMs = windowSeconds * 1000;
  const nowMs = Date.now();
  const window = Math.floor(nowMs / windowMs);
  const retryAfterSeconds = Math.max(
    1,
    Math.ceil(((window + 1) * windowMs - nowMs) / 1000)
  );
  const key = crypto.createHash("sha256")
    .update(`${scope}:${subject}:${window}`)
    .digest("hex");
  const reference = admin.firestore().collection("callableRateLimits").doc(key);
  await admin.firestore().runTransaction(async (transaction) => {
    const current = await transaction.get(reference);
    const count = Number(current.get("count") ?? 0);
    if (count >= limit) {
      fail(
        "resource-exhausted",
        "RATE_LIMITED",
        "Wait before trying again.",
        {retryAfterSeconds}
      );
    }
    transaction.set(reference, {
      scope,
      count: count + 1,
      window,
      expiresAt: Timestamp.fromMillis((window + 2) * windowMs),
      updatedAt: Timestamp.now(),
    });
  });
};

const canonicalEvents = new Set([
  "invite_link_opened",
  "app_first_open",
  "auth_completed",
  "experience_joined",
  "location_permission_result",
  "map_loaded_with_drops",
  "drop_viewed_locked",
  "unlock_attempted",
  "unlock_failed_distance",
  "unlock_succeeded",
  "drop_collected",
  "trail_completed",
  "redemption_code_issued",
  "redemption_code_marked_used",
  "push_sent",
  "push_opened",
  "report_submitted",
  "block_created",
  "feedback_submitted",
  "drop_created",
  "experience_published",
]);

const clientEvents = new Set([
  "app_first_open",
  "location_permission_result",
  "map_loaded_with_drops",
  "drop_viewed_locked",
  "unlock_attempted",
  "push_opened",
]);

const clientParamAllowlist: Record<string, string[]> = {
  app_first_open: [],
  location_permission_result: ["precision", "result", "context"],
  map_loaded_with_drops: ["dropCountBucket"],
  drop_viewed_locked: ["source"],
  unlock_attempted: ["accountState", "accountGateShown"],
  push_opened: ["notificationKey"],
};

const sanitizeEventParams = (
  eventName: string,
  raw: unknown,
  explicitAllowlist?: string[]
): JsonMap => {
  const params = raw === undefined || raw === null ? {} : asObject(raw, "params");
  const allowed = explicitAllowlist ?? clientParamAllowlist[eventName] ?? [];
  assertOnlyKeys(params, allowed, "params");
  const cleaned: JsonMap = {};
  for (const [key, value] of Object.entries(params)) {
    if (typeof value === "boolean" ||
        (typeof value === "number" && Number.isFinite(value)) ||
        (typeof value === "string" && value.length <= 80)) {
      cleaned[key] = value;
    } else {
      fail("invalid-argument", "INVALID_EVENT", "Event parameters are invalid.", {
        field: `params.${key}`,
      });
    }
  }
  return cleaned;
};

type LedgerEvent = {
  eventName: string;
  origin: "ENTRY" | "CLIENT" | "SERVER";
  dedupeKey: string;
  actorUid?: string | null;
  entrySessionId?: string | null;
  experienceCode?: string | null;
  dropId?: string | null;
  trailId?: string | null;
  occurredAt?: Timestamp;
  platform?: string | null;
  appVersion?: string | null;
  params?: JsonMap;
};

const writeLedgerEvent = async (event: LedgerEvent): Promise<boolean> => {
  if (!canonicalEvents.has(event.eventName)) {
    fail("internal", "INVALID_EVENT", "The event contract is invalid.");
  }
  const firestore = admin.firestore();
  const dedupeDigest = crypto.createHash("sha256")
    .update(event.dedupeKey)
    .digest("hex");
  const dedupeRef = firestore.collection("analyticsEventDedupe").doc(dedupeDigest);
  const eventRef = firestore.collection("analyticsEvents").doc();
  const now = Timestamp.now();
  return firestore.runTransaction(async (transaction) => {
    const existing = await transaction.get(dedupeRef);
    if (existing.exists) return false;
    transaction.create(dedupeRef, {
      eventId: eventRef.id,
      expiresAt: Timestamp.fromMillis(
        now.toMillis() + ANALYTICS_RETENTION_DAYS * 24 * 60 * 60 * 1000
      ),
    });
    transaction.create(eventRef, {
      schemaVersion: 1,
      eventName: event.eventName,
      eventVersion: 1,
      origin: event.origin,
      occurredAt: event.occurredAt ?? now,
      receivedAt: now,
      actorKey: event.actorUid ? protectedKey("actor", event.actorUid) : null,
      entrySessionId: event.entrySessionId ?? null,
      experienceKey: event.experienceCode ?
        protectedKey("experience", event.experienceCode) : null,
      dropKey: event.dropId ? protectedKey("drop", event.dropId) : null,
      trailKey: event.trailId ? protectedKey("trail", event.trailId) : null,
      platform: event.platform ?? null,
      appVersion: event.appVersion ?? null,
      params: event.params ?? {},
      dedupeKey: dedupeDigest,
      expiresAt: Timestamp.fromMillis(
        now.toMillis() + ANALYTICS_RETENTION_DAYS * 24 * 60 * 60 * 1000
      ),
    });
    return true;
  });
};

const experiencePreview = async (
  code: string,
  uid: string | null
): Promise<JsonMap> => {
  const firestore = admin.firestore();
  const reference = firestore.collection("groups").doc(code);
  const snapshot = await reference.get();
  if (!snapshot.exists || snapshot.get("schemaVersion") !== 2) {
    return fail("not-found", "EXPERIENCE_NOT_FOUND", "Experience not found.");
  }
  const data = snapshot.data() ?? {};
  if (data.state === "CANCELLED") {
    return fail("failed-precondition", "EXPERIENCE_CANCELLED", "This Experience was cancelled.");
  }
  const drops = await firestore.collection("experienceDrops")
    .where("experienceCode", "==", code)
    .where("state", "==", "PUBLISHED")
    .where("moderationState", "==", "SAFE")
    .get();
  let membership = "NONE";
  if (uid === data.ownerId) {
    membership = "OWNER";
  } else if (uid) {
    const member = await firestore.collection("users").doc(uid)
      .collection("groups").doc(code).get();
    if (member.exists) membership = "MEMBER";
  }
  return {
    code,
    name: data.name,
    description: data.description ?? null,
    hostLabel: data.hostLabel,
    startsAt: isoTimestamp(data.startsAt),
    endsAt: isoTimestamp(data.endsAt),
    timeZone: data.timeZone,
    state: data.state,
    availability: availabilityFor(data),
    availableDropCount: drops.size,
    membership,
  };
};

const parseEntrySessionId = (raw: unknown): string | null => {
  if (raw === undefined || raw === null || raw === "") return null;
  return textValue(raw, "entrySessionId", 16, 128, true);
};

export const resolveExperience = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "code", "entrySessionId", "channel"]);
  requireApiVersion(data);
  const code = normalizeExperienceCode(data.code);
  const entrySessionId = parseEntrySessionId(data.entrySessionId);
  const rateLimitSubject = context.auth?.uid ?? entrySessionId;
  if (rateLimitSubject) {
    await enforceRateLimit("resolveExperience", rateLimitSubject, 120, 60 * 60);
  }
  const channel = data.channel === undefined ? "LINK" :
    textValue(data.channel, "channel", 2, 16);
  if (!(["QR", "LINK", "MANUAL"] as unknown[]).includes(channel)) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a valid entry channel.", {
      field: "channel",
    });
  }
  const experience = await experiencePreview(code, context.auth?.uid ?? null);
  if (entrySessionId) {
    await writeLedgerEvent({
      eventName: "invite_link_opened",
      origin: "ENTRY",
      dedupeKey: `invite:${entrySessionId}:${code}`,
      actorUid: context.auth?.uid ?? null,
      entrySessionId,
      experienceCode: code,
      params: {channel},
    });
  }
  return {schemaVersion: 1, experience, membership: experience.membership};
});

export const joinExperience = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "code", "entrySessionId"]);
  requireApiVersion(data);
  const code = normalizeExperienceCode(data.code);
  const entrySessionId = parseEntrySessionId(data.entrySessionId);
  const firestore = admin.firestore();
  const groupRef = firestore.collection("groups").doc(code);
  const memberRef = firestore.collection("users").doc(uid).collection("groups").doc(code);
  let created = false;
  await firestore.runTransaction(async (transaction) => {
    const [group, member] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(memberRef),
    ]);
    if (!group.exists || group.get("schemaVersion") !== 2) {
      fail("not-found", "EXPERIENCE_NOT_FOUND", "Experience not found.");
    }
    const groupData = group.data() ?? {};
    const availability = availabilityFor(groupData);
    if (availability === "CANCELLED") {
      fail("failed-precondition", "EXPERIENCE_CANCELLED", "This Experience was cancelled.");
    }
    if (availability === "ENDED") {
      fail("failed-precondition", "EXPERIENCE_ENDED", "This Experience has ended.");
    }
    const role = groupData.ownerId === uid ? "OWNER" : "SUBSCRIBER";
    const now = Timestamp.now();
    created = !member.exists;
    transaction.set(memberRef, {
      schemaVersion: 2,
      code,
      ownerId: groupData.ownerId,
      role,
      joinedAt: member.get("joinedAt") ?? now,
      updatedAt: now,
    });
    if (created) {
      transaction.set(groupRef.collection("analytics").doc("summary"), {
        schemaVersion: 2,
        joinedParticipants: FieldValue.increment(1),
        updatedAt: now,
      }, {merge: true});
    }
  });
  if (created) {
    await writeLedgerEvent({
      eventName: "experience_joined",
      origin: "SERVER",
      dedupeKey: `joined:${uid}:${code}`,
      actorUid: uid,
      entrySessionId,
      experienceCode: code,
    });
  }
  const experience = await experiencePreview(code, uid);
  return {schemaVersion: 1, experience, membership: experience.membership};
});

/**
 * Hosting fallback for a QR/App Link opened before Kithe is installed.
 * It exposes preview metadata only; payload content and attendee identity never
 * enter the page or its URL.
 */
export const experienceEntryPage = functions
  .region(REGION)
  .https.onRequest(async (request, response) => {
    response.set({
      "Cache-Control": "no-store",
      "Content-Security-Policy": "default-src 'none'; style-src 'unsafe-inline'; " +
        "img-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
      "Referrer-Policy": "no-referrer",
      "Permissions-Policy": "camera=(), geolocation=(), microphone=()",
      "X-Content-Type-Options": "nosniff",
    });
    if (request.method !== "GET" && request.method !== "HEAD") {
      response.status(405).send("Method not allowed");
      return;
    }

    const match = request.path.match(/^\/e\/([^/?#]+)$/);
    const rawCode = match ? decodeURIComponent(match[1]) : "";
    try {
      const code = normalizeExperienceCode(rawCode);
      const preview = await experiencePreview(code, null);
      const rawEntrySessionId = Array.isArray(request.query.entry_session_id) ?
        request.query.entry_session_id[0] : request.query.entry_session_id;
      const entrySessionId = typeof rawEntrySessionId === "string" &&
        /^[A-Za-z0-9_-]{16,128}$/.test(rawEntrySessionId) ?
        rawEntrySessionId : crypto.randomBytes(16).toString("hex");
      const channel = request.query.channel === "QR" ? "QR" : "LINK";
      const referrer = new URLSearchParams({
        code,
        entry_session_id: entrySessionId,
        channel,
      }).toString();
      const playUrl = "https://play.google.com/store/apps/details" +
        `?id=com.kitheapp&referrer=${encodeURIComponent(referrer)}`;
      const html = renderExperienceEntryPage({
        code,
        name: typeof preview.name === "string" ? preview.name : code,
        description: typeof preview.description === "string" ? preview.description : null,
        hostLabel: typeof preview.hostLabel === "string" ? preview.hostLabel : "Host",
        availability: typeof preview.availability === "string" ? preview.availability : "ACTIVE",
        availableDropCount: Number(preview.availableDropCount ?? 0),
      }, playUrl);
      response.status(200).type("html").send(request.method === "HEAD" ? "" : html);
    } catch (error) {
      response.status(404).type("html").send(request.method === "HEAD" ? "" :
        renderExperienceEntryNotFound());
    }
  });

export const leaveExperience = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "code"]);
  requireApiVersion(data);
  const code = normalizeExperienceCode(data.code);
  const firestore = admin.firestore();
  const groupRef = firestore.collection("groups").doc(code);
  const memberRef = firestore.collection("users").doc(uid).collection("groups").doc(code);
  await firestore.runTransaction(async (transaction) => {
    const [group, member] = await Promise.all([
      transaction.get(groupRef),
      transaction.get(memberRef),
    ]);
    if (!member.exists) return;
    if (group.get("ownerId") === uid || member.get("role") === "OWNER") {
      fail("failed-precondition", "OWNER_CANNOT_LEAVE", "Experience owners cannot leave.");
    }
    transaction.delete(memberRef);
  });
  return {schemaVersion: 1, left: true};
});

export const createOrganizerApplicationLink = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion"]);
  requireApiVersion(data);
  const token = crypto.randomBytes(32).toString("base64url");
  const digest = crypto.createHash("sha256").update(token).digest("hex");
  const expiresAt = Timestamp.fromMillis(Date.now() + APPLICATION_TOKEN_LIFETIME_MS);
  await admin.firestore().collection("organizerApplicationTokens").doc(digest).create({
    schemaVersion: 1,
    uid,
    createdAt: Timestamp.now(),
    expiresAt,
    usedAt: null,
  });
  const configuredUrl = process.env.ORGANIZER_APPLICATION_URL?.trim();
  const base = configuredUrl ||
    ((process.env.FIRESTORE_EMULATOR_HOST || process.env.FUNCTIONS_EMULATOR === "true") ?
      "https://example.invalid/geodrop-organizer-application" :
      fail(
        "failed-precondition",
        "SERVER_CONFIGURATION_REQUIRED",
        "Organizer applications are not configured."
      ));
  const url = new URL(base);
  url.searchParams.set("token", token);
  return {
    schemaVersion: 1,
    url: url.toString(),
    expiresAt: expiresAt.toDate().toISOString(),
  };
});

export const setOrganizerAccessDecision = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const operatorUid = requireAdmin(context);
  const data = asObject(raw);
  assertOnlyKeys(data, [
    "apiVersion",
    "targetUid",
    "decision",
    "applicationSubmittedAt",
    "termsVersion",
  ]);
  requireApiVersion(data);
  const targetUid = textValue(data.targetUid, "targetUid", 1, 128) as string;
  const decision = textValue(data.decision, "decision", 4, 16) as string;
  if (!(["PENDING", "APPROVE", "DENY"] as string[]).includes(decision)) {
    fail("invalid-argument", "INVALID_DECISION", "Choose a valid organizer decision.", {
      field: "decision",
    });
  }
  const submittedAt = data.applicationSubmittedAt ?
    timestampValue(data.applicationSubmittedAt, "applicationSubmittedAt") : null;
  const termsVersion = optionalText(data.termsVersion, "termsVersion", 80);
  const status = decision === "APPROVE" ? "APPROVED" :
    decision === "DENY" ? "DENIED" : "PENDING";
  const role = status === "APPROVED" ? "BUSINESS" : "EXPLORER";
  const now = Timestamp.now();
  const firestore = admin.firestore();
  await firestore.runTransaction(async (transaction) => {
    const userRef = firestore.collection("users").doc(targetUid);
    const applicationRef = firestore.collection("organizerApplications").doc(targetUid);
    const user = await transaction.get(userRef);
    if (!user.exists) {
      fail("not-found", "ACCOUNT_NOT_FOUND", "Account not found.");
    }
    transaction.set(userRef, {
      role,
      organizerAccessStatus: status,
      organizerAccessSubmittedAt: submittedAt ??
        user.get("organizerAccessSubmittedAt") ?? null,
      organizerAccessReviewedAt: decision === "PENDING" ? null : now,
    }, {merge: true});
    transaction.set(applicationRef, {
      schemaVersion: 1,
      status,
      submittedAt: submittedAt ?? null,
      termsVersion,
      reviewedAt: decision === "PENDING" ? null : now,
      updatedAt: now,
    }, {merge: true});
    if (status === "APPROVED") {
      const hostLabel = String(
        user.get("businessName") ?? user.get("displayName") ?? "Host"
      ).trim().slice(0, 100) || "Host";
      transaction.set(firestore.collection("creatorProfiles").doc(targetUid), {
        schemaVersion: 1,
        hostLabel,
        username: user.get("username") ?? null,
        organizationName: user.get("businessName") ?? null,
        updatedAt: now,
      });
    }
    transaction.create(applicationRef.collection("audit").doc(), {
      schemaVersion: 1,
      decision,
      operatorKey: protectedKey("operator", operatorUid),
      occurredAt: now,
    });
  });
  return {schemaVersion: 1, status, role};
});

export const ingestOrganizerApplication = functions
  .region(REGION)
  .https.onRequest(async (request, response) => {
    response.set("Cache-Control", "no-store");
    if (request.method !== "POST") {
      response.status(405).json({accepted: false, reason: "METHOD_NOT_ALLOWED"});
      return;
    }
    try {
      const data = asObject(request.body);
      assertOnlyKeys(data, [
        "token",
        "organizationName",
        "contactName",
        "contactEmail",
        "description",
        "termsVersion",
      ]);
      const token = textValue(data.token, "token", 32, 128) as string;
      if (!/^[A-Za-z0-9_-]{32,128}$/.test(token)) {
        fail("invalid-argument", "APPLICATION_TOKEN_INVALID", "Application link is invalid.");
      }
      const organizationName = textValue(
        data.organizationName,
        "organizationName",
        1,
        100
      ) as string;
      const contactName = textValue(data.contactName, "contactName", 1, 100) as string;
      const contactEmail = textValue(data.contactEmail, "contactEmail", 3, 254) as string;
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contactEmail)) {
        fail("invalid-argument", "APPLICATION_INVALID", "Application details are invalid.");
      }
      const description = textValue(data.description, "description", 1, 2000) as string;
      const termsVersion = textValue(data.termsVersion, "termsVersion", 1, 80) as string;
      const digest = crypto.createHash("sha256").update(token).digest("hex");
      const firestore = admin.firestore();
      const tokenRef = firestore.collection("organizerApplicationTokens").doc(digest);
      await firestore.runTransaction(async (transaction) => {
        const tokenDocument = await transaction.get(tokenRef);
        const expiresAt = tokenDocument.get("expiresAt");
        if (!tokenDocument.exists || tokenDocument.get("usedAt") !== null ||
            !(expiresAt instanceof Timestamp) || expiresAt.toMillis() <= Date.now()) {
          fail("failed-precondition", "APPLICATION_TOKEN_INVALID", "Application link is invalid.");
        }
        const uid = String(tokenDocument.get("uid") ?? "");
        const userRef = firestore.collection("users").doc(uid);
        const user = await transaction.get(userRef);
        if (!user.exists || user.get("organizerAccessStatus") === "APPROVED") {
          fail("failed-precondition", "APPLICATION_NOT_ACCEPTED", "Application cannot be accepted.");
        }
        const now = Timestamp.now();
        transaction.set(firestore.collection("organizerApplications").doc(uid), {
          schemaVersion: 1,
          organizationName,
          contactName,
          contactEmail,
          description,
          termsVersion,
          status: "PENDING",
          submittedAt: now,
          reviewedAt: null,
          updatedAt: now,
        });
        transaction.set(userRef, {
          role: "EXPLORER",
          organizerAccessStatus: "PENDING",
          organizerAccessSubmittedAt: now,
          organizerAccessReviewedAt: null,
        }, {merge: true});
        transaction.update(tokenRef, {usedAt: now});
      });
      response.status(202).json({accepted: true, status: "PENDING"});
    } catch (error) {
      const reason = error instanceof functions.https.HttpsError &&
        error.details && typeof error.details === "object" &&
        typeof (error.details as Record<string, unknown>).reason === "string" ?
        (error.details as Record<string, unknown>).reason : "APPLICATION_INVALID";
      response.status(400).json({accepted: false, reason});
    }
  });

const organizerProfile = async (uid: string): Promise<admin.firestore.DocumentData> => {
  const profile = await admin.firestore().collection("users").doc(uid).get();
  if (!profile.exists || profile.get("role") !== "BUSINESS" ||
      profile.get("organizerAccessStatus") !== "APPROVED") {
    return fail(
      "permission-denied",
      "ORGANIZER_APPROVAL_REQUIRED",
      "Organizer approval is required."
    );
  }
  return profile.data() ?? {};
};

const experienceInput = (data: JsonMap): {
  name: string;
  description: string | null;
  startsAt: Timestamp;
  endsAt: Timestamp;
  timeZone: string;
  defaultRadiusM: number;
} => {
  const name = textValue(data.name, "name", 1, 100) as string;
  const description = optionalText(data.description, "description", 240);
  const startsAt = timestampValue(data.startsAt, "startsAt");
  const endsAt = timestampValue(data.endsAt, "endsAt");
  const timeZone = textValue(data.timeZone, "timeZone", 1, 64) as string;
  const defaultRadiusM = data.defaultRadiusM === undefined ? 25 : data.defaultRadiusM;
  if (!Number.isInteger(defaultRadiusM) ||
      (defaultRadiusM as number) < 15 || (defaultRadiusM as number) > 100) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a radius from 15 to 100 metres.", {
      field: "defaultRadiusM",
    });
  }
  if (endsAt.toMillis() <= startsAt.toMillis()) {
    fail("invalid-argument", "INVALID_REQUEST", "Experience end must follow its start.", {
      field: "endsAt",
    });
  }
  try {
    new Intl.DateTimeFormat("en-US", {timeZone});
  } catch (_) {
    fail("invalid-argument", "INVALID_REQUEST", "Provide a valid time zone.", {
      field: "timeZone",
    });
  }
  return {
    name,
    description,
    startsAt,
    endsAt,
    timeZone,
    defaultRadiusM: defaultRadiusM as number,
  };
};

export const createExperience = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const profile = await organizerProfile(uid);
  const data = asObject(raw);
  assertOnlyKeys(data, [
    "apiVersion",
    "name",
    "description",
    "startsAt",
    "endsAt",
    "timeZone",
    "defaultRadiusM",
  ]);
  requireApiVersion(data);
  const input = experienceInput(data);
  const hostLabel = textValue(
    profile.businessName ?? profile.displayName,
    "hostLabel",
    1,
    100
  ) as string;
  const firestore = admin.firestore();
  let code = "";
  for (let attempt = 0; attempt < 8; attempt += 1) {
    const candidate = generateExperienceCode();
    const groupRef = firestore.collection("groups").doc(candidate);
    const memberRef = firestore.collection("users").doc(uid)
      .collection("groups").doc(candidate);
    try {
      await firestore.runTransaction(async (transaction) => {
        const existing = await transaction.get(groupRef);
        if (existing.exists) throw new Error("CODE_COLLISION");
        const now = Timestamp.now();
        transaction.create(groupRef, {
          schemaVersion: 2,
          code: candidate,
          ownerId: uid,
          hostLabel,
          ...input,
          state: "PUBLISHED",
          createdAt: now,
          publishedAt: now,
          updatedAt: now,
        });
        transaction.create(memberRef, {
          schemaVersion: 2,
          code: candidate,
          ownerId: uid,
          role: "OWNER",
          joinedAt: now,
          updatedAt: now,
        });
        transaction.set(groupRef.collection("analytics").doc("summary"), {
          schemaVersion: 2,
          joinedParticipants: 0,
          publishedDrops: 0,
          uniqueUnlockers: 0,
          unlocks: 0,
          mainTrailCompletions: 0,
          codesIssued: 0,
          codesUsed: 0,
          createdAt: now,
          updatedAt: now,
        });
      });
      code = candidate;
      break;
    } catch (error) {
      if (!(error instanceof Error) || error.message !== "CODE_COLLISION") throw error;
    }
  }
  if (!code) fail("resource-exhausted", "CODE_GENERATION_FAILED", "Try again shortly.");
  await writeLedgerEvent({
    eventName: "experience_published",
    origin: "SERVER",
    dedupeKey: `experience-published:${code}`,
    actorUid: uid,
    experienceCode: code,
  });
  return {schemaVersion: 1, experience: await experiencePreview(code, uid)};
});

export const updateExperience = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, [
    "apiVersion",
    "code",
    "name",
    "description",
    "startsAt",
    "endsAt",
    "timeZone",
    "defaultRadiusM",
    "state",
  ]);
  requireApiVersion(data);
  const code = normalizeExperienceCode(data.code);
  const input = experienceInput(data);
  const state = data.state === undefined ? "PUBLISHED" :
    textValue(data.state, "state", 8, 10);
  if (!(state === "PUBLISHED" || state === "CANCELLED")) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a valid Experience state.", {
      field: "state",
    });
  }
  const ref = admin.firestore().collection("groups").doc(code);
  await admin.firestore().runTransaction(async (transaction) => {
    const current = await transaction.get(ref);
    if (!current.exists || current.get("ownerId") !== uid ||
        current.get("schemaVersion") !== 2) {
      fail("not-found", "EXPERIENCE_NOT_FOUND", "Experience not found.");
    }
    transaction.update(ref, {...input, state, updatedAt: Timestamp.now()});
  });
  return {schemaVersion: 1, experience: await experiencePreview(code, uid)};
});

type ParsedDrop = {
  experienceCode: string;
  dropId: string | null;
  lat: number;
  lng: number;
  radiusM: number | null;
  expiryMode: "NONE" | "CUSTOM" | "EXPERIENCE_END";
  expiresAt: Timestamp | null;
  contentKind: "TEXT" | "PHOTO";
  dropKind: "STANDARD" | "REWARD";
  title: string;
  body: string | null;
  mediaAltText: string | null;
  stagingUploadId: string | null;
  rewardPresentation: JsonMap | null;
  inventoryLimit: number | null;
  trailId: string | null;
  trailStepIndex: number | null;
  trailTotalSteps: number | null;
};

const parseDrop = (data: JsonMap): ParsedDrop => {
  const location = asObject(data.location, "location");
  assertOnlyKeys(location, ["lat", "lng"], "location");
  const lat = location.lat;
  const lng = location.lng;
  if (typeof lat !== "number" || !Number.isFinite(lat) || lat < -90 || lat > 90 ||
      typeof lng !== "number" || !Number.isFinite(lng) || lng < -180 || lng > 180) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a valid drop location.", {
      field: "location",
    });
  }
  const content = asObject(data.content, "content");
  assertOnlyKeys(content, [
    "contentKind",
    "title",
    "body",
    "mediaAltText",
    "rewardPresentation",
  ], "content");
  const contentKind = textValue(content.contentKind, "content.contentKind", 4, 5);
  if (!(contentKind === "TEXT" || contentKind === "PHOTO")) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose text or photo content.", {
      field: "content.contentKind",
    });
  }
  const title = textValue(content.title, "content.title", 1, 80) as string;
  const body = optionalText(content.body, "content.body", 2000);
  const mediaAltText = optionalText(content.mediaAltText, "content.mediaAltText", 240);
  if (contentKind === "TEXT" && !body) {
    fail("invalid-argument", "INVALID_REQUEST", "Text drops require a message.", {
      field: "content.body",
    });
  }
  if (contentKind === "PHOTO" && !mediaAltText) {
    fail("invalid-argument", "INVALID_REQUEST", "Photo drops require alt text.", {
      field: "content.mediaAltText",
    });
  }
  const dropKind = textValue(data.dropKind, "dropKind", 6, 8);
  if (!(dropKind === "STANDARD" || dropKind === "REWARD")) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a valid drop kind.", {
      field: "dropKind",
    });
  }
  let rewardPresentation: JsonMap | null = null;
  if (content.rewardPresentation !== undefined && content.rewardPresentation !== null) {
    rewardPresentation = asObject(content.rewardPresentation, "content.rewardPresentation");
    assertOnlyKeys(rewardPresentation, [
      "rewardLabel",
      "businessLabel",
      "instructions",
      "terms",
    ], "content.rewardPresentation");
    const sanitizedPresentation = rewardPresentation;
    Object.entries(sanitizedPresentation).forEach(([key, value]) => {
      sanitizedPresentation[key] = optionalText(
        value,
        `content.rewardPresentation.${key}`,
        key === "terms" ? 500 : 240
      );
    });
  }
  if (dropKind === "STANDARD" && rewardPresentation) {
    fail("invalid-argument", "INVALID_REQUEST", "Standard drops cannot include a reward.", {
      field: "content.rewardPresentation",
    });
  }
  const expiryMode = textValue(data.expiryMode, "expiryMode", 4, 14);
  if (!(expiryMode === "NONE" || expiryMode === "CUSTOM" ||
      expiryMode === "EXPERIENCE_END")) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a valid expiry mode.", {
      field: "expiryMode",
    });
  }
  const expiresAt = expiryMode === "CUSTOM" ?
    timestampValue(data.expiresAt, "expiresAt") : null;
  if (expiryMode !== "CUSTOM" && data.expiresAt !== undefined && data.expiresAt !== null) {
    fail("invalid-argument", "INVALID_REQUEST", "This expiry mode has no custom time.", {
      field: "expiresAt",
    });
  }
  const radiusM = data.radiusM === undefined ? null : data.radiusM;
  if (radiusM !== null && (!Number.isInteger(radiusM) ||
      (radiusM as number) < 15 || (radiusM as number) > 100)) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a radius from 15 to 100 metres.", {
      field: "radiusM",
    });
  }
  const inventoryLimit = data.inventoryLimit === undefined ? null : data.inventoryLimit;
  if (dropKind === "REWARD" &&
      (!Number.isInteger(inventoryLimit) || (inventoryLimit as number) < 1 ||
        (inventoryLimit as number) > 10000)) {
    fail("invalid-argument", "INVALID_REQUEST", "Provide a valid reward inventory limit.", {
      field: "inventoryLimit",
    });
  }
  if (dropKind === "STANDARD" && inventoryLimit !== null) {
    fail("invalid-argument", "INVALID_REQUEST", "Standard drops have no reward inventory.", {
      field: "inventoryLimit",
    });
  }
  const trailId = optionalText(data.trailId, "trailId", 128);
  const trailStepIndex = data.trailStepIndex === undefined ? null : data.trailStepIndex;
  const trailTotalSteps = data.trailTotalSteps === undefined ? null : data.trailTotalSteps;
  if ((trailId === null) !== (trailStepIndex === null || trailTotalSteps === null) ||
      (trailId !== null && (!Number.isInteger(trailStepIndex) ||
        !Number.isInteger(trailTotalSteps) || (trailStepIndex as number) < 0 ||
        (trailTotalSteps as number) < 1 ||
        (trailStepIndex as number) >= (trailTotalSteps as number)))) {
    fail("invalid-argument", "INVALID_REQUEST", "Provide a valid Trail placement.", {
      field: "trailId",
    });
  }
  return {
    experienceCode: normalizeExperienceCode(data.experienceCode),
    dropId: optionalText(data.dropId, "dropId", 128),
    lat: lat as number,
    lng: lng as number,
    radiusM: radiusM as number | null,
    expiryMode: expiryMode as "NONE" | "CUSTOM" | "EXPERIENCE_END",
    expiresAt,
    contentKind: contentKind as "TEXT" | "PHOTO",
    dropKind: dropKind as "STANDARD" | "REWARD",
    title,
    body,
    mediaAltText,
    stagingUploadId: optionalText(data.stagingUploadId, "stagingUploadId", 128),
    rewardPresentation,
    inventoryLimit: inventoryLimit as number | null,
    trailId,
    trailStepIndex: trailStepIndex as number | null,
    trailTotalSteps: trailTotalSteps as number | null,
  };
};

const promoteStagingImage = async (
  uid: string,
  uploadId: string,
  dropId: string,
  version: number
): Promise<{assetId: string; mimeType: string; objectPath: string}> => {
  const bucket = admin.storage().bucket();
  const source = bucket.file(`drop-upload-staging/${uid}/${uploadId}`);
  const [exists] = await source.exists();
  if (!exists) {
    return fail("not-found", "UPLOAD_NOT_FOUND", "The staged image is no longer available.");
  }
  const [metadata] = await source.getMetadata();
  const mimeType = metadata.contentType ?? "";
  const size = Number(metadata.size ?? 0);
  const ownerId = metadata.metadata?.ownerId;
  const downloadTokens = metadata.metadata?.firebaseStorageDownloadTokens;
  if (ownerId !== uid || !["image/jpeg", "image/png", "image/webp"].includes(mimeType) ||
      !Number.isFinite(size) || size <= 0 || size > MAX_STAGING_IMAGE_BYTES || downloadTokens) {
    return fail("failed-precondition", "UPLOAD_INVALID", "The staged image is invalid.");
  }
  const assetId = crypto.randomUUID();
  const objectPath = `drop-payloads/${dropId}/${version}/${assetId}`;
  const destination = bucket.file(objectPath);
  await source.copy(destination);
  await destination.setMetadata({
    cacheControl: "private, no-store, max-age=0",
    contentType: mimeType,
    metadata: {ownerId: uid, dropId, payloadVersion: String(version)},
  });
  await source.delete({ignoreNotFound: true});
  return {assetId, mimeType, objectPath};
};

export const saveDrop = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  await organizerProfile(uid);
  const data = asObject(raw);
  assertOnlyKeys(data, [
    "apiVersion",
    "experienceCode",
    "dropId",
    "location",
    "radiusM",
    "expiryMode",
    "expiresAt",
    "content",
    "stagingUploadId",
    "dropKind",
    "inventoryLimit",
    "trailId",
    "trailStepIndex",
    "trailTotalSteps",
  ]);
  requireApiVersion(data);
  const input = parseDrop(data);
  const firestore = admin.firestore();
  const dropId = input.dropId ?? firestore.collection("experienceDrops").doc().id;
  const discoveryRef = firestore.collection("experienceDrops").doc(dropId);
  const payloadRef = firestore.collection("dropPayloads").doc(dropId);
  const groupRef = firestore.collection("groups").doc(input.experienceCode);
  const [group, current] = await Promise.all([groupRef.get(), discoveryRef.get()]);
  if (!group.exists || group.get("ownerId") !== uid || group.get("schemaVersion") !== 2) {
    fail("not-found", "EXPERIENCE_NOT_FOUND", "Experience not found.");
  }
  if (input.dropId && !current.exists) {
    fail("not-found", "DROP_NOT_AVAILABLE", "Drop not found.");
  }
  if (current.exists && (current.get("ownerId") !== uid ||
      current.get("experienceCode") !== input.experienceCode)) {
    fail("not-found", "DROP_NOT_AVAILABLE", "Drop not found.");
  }
  if (current.exists && current.get("dropKind") !== input.dropKind) {
    fail(
      "failed-precondition",
      "DROP_KIND_IMMUTABLE",
      "Drop type cannot change after publishing."
    );
  }
  const currentVersion = current.exists ? Number(current.get("payloadVersion")) : 0;
  const version = currentVersion + 1;
  if (input.contentKind === "PHOTO" && !input.stagingUploadId) {
    fail("invalid-argument", "UPLOAD_REQUIRED", "Photo drops require a staged image.", {
      field: "stagingUploadId",
    });
  }
  let promoted: {assetId: string; mimeType: string; objectPath: string} | null = null;
  if (input.contentKind === "PHOTO" && input.stagingUploadId) {
    promoted = await promoteStagingImage(uid, input.stagingUploadId, dropId, version);
  }
  const now = Timestamp.now();
  const radiusM = input.radiusM ?? Number(group.get("defaultRadiusM") ?? 25);
  const hostLabel = String(group.get("hostLabel") ?? "Host");
  try {
    await firestore.runTransaction(async (transaction) => {
      const [freshGroup, freshDrop, freshPayload] = await Promise.all([
        transaction.get(groupRef),
        transaction.get(discoveryRef),
        transaction.get(payloadRef),
      ]);
      if (!freshGroup.exists || freshGroup.get("ownerId") !== uid ||
          freshGroup.get("state") !== "PUBLISHED") {
        fail("failed-precondition", "EXPERIENCE_NOT_AVAILABLE", "Experience is unavailable.");
      }
      const observedVersion = freshDrop.exists ? Number(freshDrop.get("payloadVersion")) : 0;
      if (observedVersion !== currentVersion ||
          (freshPayload.exists && Number(freshPayload.get("currentVersion")) !== currentVersion)) {
        fail("aborted", "DROP_EDIT_CONFLICT", "The drop changed. Reload and try again.");
      }
      if (input.trailId) {
        const trail = await transaction.get(groupRef.collection("trails").doc(input.trailId));
        if (!trail.exists || trail.get("state") !== "ACTIVE" ||
            trail.get("dropIds")?.[input.trailStepIndex as number] !== dropId ||
            trail.get("dropIds")?.length !== input.trailTotalSteps) {
          fail("failed-precondition", "TRAIL_PLACEMENT_INVALID", "Trail placement is invalid.");
        }
      }
      const rewardRef = input.dropKind === "REWARD" ?
        firestore.collection("rewards").doc(dropId) : null;
      const reward = rewardRef ? await transaction.get(rewardRef) : null;
      if (reward && reward.exists &&
          input.inventoryLimit !== Number(reward.get("inventoryLimit"))) {
        fail(
          "failed-precondition",
          "REWARD_INVENTORY_IMMUTABLE",
          "Reward inventory cannot change after creation."
        );
      }
      const payloadVersionRef = payloadRef.collection("versions").doc(String(version));
      transaction.create(payloadVersionRef, {
        schemaVersion: 1,
        title: input.title,
        body: input.body,
        contentKind: input.contentKind,
        mediaAssetId: promoted?.assetId ?? null,
        mediaMimeType: promoted?.mimeType ?? null,
        mediaAltText: input.mediaAltText,
        rewardPresentation: input.rewardPresentation,
        createdAt: now,
      });
      transaction.set(payloadRef, {
        schemaVersion: 1,
        dropId,
        experienceCode: input.experienceCode,
        ownerId: uid,
        currentVersion: version,
        createdAt: freshPayload.get("createdAt") ?? now,
        updatedAt: now,
      });
      transaction.set(discoveryRef, {
        schemaVersion: 1,
        experienceCode: input.experienceCode,
        ownerId: uid,
        hostLabel,
        state: "PUBLISHED",
        moderationState: input.contentKind === "PHOTO" ? "PENDING" : "SAFE",
        lat: input.lat,
        lng: input.lng,
        radiusM,
        contentKind: input.contentKind,
        dropKind: input.dropKind,
        payloadVersion: version,
        trailId: input.trailId,
        trailStepIndex: input.trailStepIndex,
        trailTotalSteps: input.trailTotalSteps,
        likeCount: freshDrop.get("likeCount") ?? 0,
        createdAt: freshDrop.get("createdAt") ?? now,
        publishedAt: freshDrop.get("publishedAt") ?? now,
        updatedAt: now,
        editedAt: freshDrop.exists ? now : null,
        expiryMode: input.expiryMode,
        expiresAt: input.expiresAt,
      });
      if (input.dropKind === "REWARD") {
        const writableRewardRef = firestore.collection("rewards").doc(dropId);
        transaction.set(writableRewardRef, {
          schemaVersion: 1,
          dropId,
          experienceCode: input.experienceCode,
          ownerId: uid,
          state: reward?.get("state") ?? "ACTIVE",
          inventoryLimit: input.inventoryLimit,
          issuedCount: reward?.get("issuedCount") ?? 0,
          usedCount: reward?.get("usedCount") ?? 0,
          createdAt: reward?.get("createdAt") ?? now,
          updatedAt: now,
        });
      }
      if (!freshDrop.exists) {
        transaction.set(groupRef.collection("analytics").doc("summary"), {
          schemaVersion: 2,
          publishedDrops: FieldValue.increment(1),
          updatedAt: now,
        }, {merge: true});
      }
    });
  } catch (error) {
    if (promoted) {
      await admin.storage().bucket().file(promoted.objectPath)
        .delete({ignoreNotFound: true});
    }
    throw error;
  }
  if (!current.exists) {
    await writeLedgerEvent({
      eventName: "drop_created",
      origin: "SERVER",
      dedupeKey: `drop-created:${dropId}`,
      actorUid: uid,
      experienceCode: input.experienceCode,
      dropId,
      params: {contentKind: input.contentKind, dropKind: input.dropKind},
    });
  }
  return {schemaVersion: 1, dropId, payloadVersion: version};
});

export const deleteDrop = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  const firestore = admin.firestore();
  const dropRef = firestore.collection("experienceDrops").doc(dropId);
  await firestore.runTransaction(async (transaction) => {
    const drop = await transaction.get(dropRef);
    if (!drop.exists || drop.get("ownerId") !== uid) {
      fail("not-found", "DROP_NOT_AVAILABLE", "Drop not found.");
    }
    transaction.update(dropRef, {
      state: "DELETED",
      updatedAt: Timestamp.now(),
    });
    if (drop.get("dropKind") === "REWARD") {
      transaction.set(firestore.collection("rewards").doc(dropId), {
        state: "CLOSED",
        updatedAt: Timestamp.now(),
      }, {merge: true});
    }
  });
  return {schemaVersion: 1, deleted: true};
});

export const getOrganizerDrop = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  const firestore = admin.firestore();
  const [drop, payload, reward] = await Promise.all([
    firestore.collection("experienceDrops").doc(dropId).get(),
    firestore.collection("dropPayloads").doc(dropId).get(),
    firestore.collection("rewards").doc(dropId).get(),
  ]);
  if (!drop.exists || drop.get("ownerId") !== uid || payload.get("ownerId") !== uid) {
    fail("not-found", "DROP_NOT_AVAILABLE", "Drop not found.");
  }
  const version = Number(payload.get("currentVersion"));
  const versionDoc = await payload.ref.collection("versions").doc(String(version)).get();
  if (!versionDoc.exists) fail("internal", "PAYLOAD_MISSING", "Drop payload is unavailable.");
  return {
    schemaVersion: 1,
    discovery: drop.data(),
    payloadVersion: version,
    payload: versionDoc.data(),
    reward: reward.exists && reward.get("ownerId") === uid ? {
      state: reward.get("state"),
      inventoryLimit: reward.get("inventoryLimit"),
      issuedCount: reward.get("issuedCount"),
      usedCount: reward.get("usedCount"),
    } : null,
  };
});

type UnlockLocation = {
  lat: number;
  lng: number;
  accuracyM: number;
  capturedAt: Timestamp;
};

const parseUnlockLocation = (raw: unknown): UnlockLocation => {
  const location = asObject(raw, "location");
  assertOnlyKeys(location, ["lat", "lng", "accuracyM", "capturedAt"], "location");
  const lat = location.lat;
  const lng = location.lng;
  const accuracyM = location.accuracyM;
  if (typeof lat !== "number" || !Number.isFinite(lat) || lat < -90 || lat > 90 ||
      typeof lng !== "number" || !Number.isFinite(lng) || lng < -180 || lng > 180 ||
      typeof accuracyM !== "number" || !Number.isFinite(accuracyM) || accuracyM <= 0) {
    return fail("invalid-argument", "LOCATION_INVALID", "A valid location fix is required.", {
      field: "location",
    });
  }
  const capturedAt = timestampValue(location.capturedAt, "location.capturedAt");
  const age = Date.now() - capturedAt.toMillis();
  if (age < -5000 || age > MAX_LOCATION_AGE_MS) {
    return fail("failed-precondition", "LOCATION_STALE", "Refresh your location and try again.");
  }
  return {lat, lng, accuracyM, capturedAt};
};

const haversineMetres = (
  lat1: number,
  lng1: number,
  lat2: number,
  lng2: number
): number => {
  const radians = (degrees: number) => degrees * Math.PI / 180;
  const deltaLat = radians(lat2 - lat1);
  const deltaLng = radians(lng2 - lng1);
  const a = Math.sin(deltaLat / 2) ** 2 +
    Math.cos(radians(lat1)) * Math.cos(radians(lat2)) *
    Math.sin(deltaLng / 2) ** 2;
  return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
};

const distanceBucket = (beyondBoundaryM: number): "0_25" | "25_50" | "50_PLUS" => {
  if (beyondBoundaryM <= 25) return "0_25";
  if (beyondBoundaryM <= 50) return "25_50";
  return "50_PLUS";
};

type UnlockResult = {
  created: boolean;
  trailCompleted: boolean;
  mainTrailCompleted: boolean;
  rewardIssued: boolean;
  rewardUnavailable: boolean;
  receipt: admin.firestore.DocumentData;
  rewardReceipt: admin.firestore.DocumentData | null;
  experienceCode: string;
  trailId: string | null;
};

export const unlockDrop = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId", "entrySessionId", "location"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  await enforceRateLimit("unlockDrop", `${uid}:${dropId}`, 20, 5 * 60);
  const entrySessionId = parseEntrySessionId(data.entrySessionId);
  const location = parseUnlockLocation(data.location);
  const firestore = admin.firestore();
  const userRef = firestore.collection("users").doc(uid);
  const receiptRef = userRef.collection("unlocks").doc(dropId);
  const rewardReceiptRef = userRef.collection("rewardReceipts").doc(dropId);
  const dropRef = firestore.collection("experienceDrops").doc(dropId);
  try {
    const result = await firestore.runTransaction(async (transaction): Promise<UnlockResult> => {
      const [existingReceipt, existingReward, drop] = await Promise.all([
        transaction.get(receiptRef),
        transaction.get(rewardReceiptRef),
        transaction.get(dropRef),
      ]);
      if (existingReceipt.exists) {
        return {
          created: false,
          trailCompleted: false,
          mainTrailCompleted: false,
          rewardIssued: false,
          rewardUnavailable: drop.get("dropKind") === "REWARD" && !existingReward.exists,
          receipt: existingReceipt.data() ?? {},
          rewardReceipt: existingReward.data() ?? null,
          experienceCode: String(existingReceipt.get("experienceCode")),
          trailId: existingReceipt.get("trail")?.trailId ?? null,
        };
      }
      if (!drop.exists) {
        fail("not-found", "DROP_NOT_AVAILABLE", "This drop is unavailable.");
      }
      const dropData = drop.data() ?? {};
      const experienceCode = String(dropData.experienceCode ?? "");
      const groupRef = firestore.collection("groups").doc(experienceCode);
      const membershipRef = userRef.collection("groups").doc(experienceCode);
      const payloadRef = firestore.collection("dropPayloads").doc(dropId);
      const [group, membership, payloadParent] = await Promise.all([
        transaction.get(groupRef),
        transaction.get(membershipRef),
        transaction.get(payloadRef),
      ]);
      if (!membership.exists) {
        fail("permission-denied", "EXPERIENCE_NOT_JOINED", "Join this Experience first.");
      }
      if (!group.exists || availabilityFor(group.data() ?? {}) !== "ACTIVE" ||
          dropData.state !== "PUBLISHED" || dropData.moderationState !== "SAFE") {
        fail("failed-precondition", "DROP_NOT_AVAILABLE", "This drop is unavailable.");
      }
      const now = Timestamp.now();
      const expiryMode = dropData.expiryMode;
      const expiresAt = dropData.expiresAt instanceof Timestamp ?
        dropData.expiresAt.toMillis() : null;
      const groupEnd = group.get("endsAt") instanceof Timestamp ?
        (group.get("endsAt") as Timestamp).toMillis() : null;
      if ((expiryMode === "CUSTOM" && expiresAt !== null && now.toMillis() >= expiresAt) ||
          (expiryMode === "EXPERIENCE_END" && groupEnd !== null && now.toMillis() >= groupEnd)) {
        fail("failed-precondition", "DROP_EXPIRED", "This drop has expired.");
      }
      const radiusM = Number(dropData.radiusM);
      if (location.accuracyM > Math.min(radiusM, 30)) {
        fail(
          "failed-precondition",
          "ACCURACY_INSUFFICIENT",
          "Wait for a more accurate location fix."
        );
      }
      const distanceM = haversineMetres(
        location.lat,
        location.lng,
        Number(dropData.lat),
        Number(dropData.lng)
      );
      if (distanceM > radiusM + location.accuracyM) {
        const bucket = distanceBucket(distanceM - radiusM - location.accuracyM);
        fail("failed-precondition", "TOO_FAR", "Move closer and try again.", {
          distanceBucket: bucket,
        });
      }
      const payloadVersion = Number(dropData.payloadVersion);
      const payloadVersionRef = payloadParent.ref.collection("versions")
        .doc(String(payloadVersion));
      const payload = await transaction.get(payloadVersionRef);
      if (!payloadParent.exists || !payload.exists ||
          Number(payloadParent.get("currentVersion")) !== payloadVersion) {
        fail("failed-precondition", "DROP_NOT_AVAILABLE", "This drop is unavailable.");
      }
      const trailId = typeof dropData.trailId === "string" ? dropData.trailId : null;
      let trail: JsonMap | null = null;
      let trailCompleted = false;
      let mainTrailCompleted = false;
      let progressRef: admin.firestore.DocumentReference | null = null;
      let progressData: JsonMap | null = null;
      if (trailId) {
        const trailRef = groupRef.collection("trails").doc(trailId);
        progressRef = userRef.collection("trailProgress").doc(trailId);
        const [trailDoc, progress] = await Promise.all([
          transaction.get(trailRef),
          transaction.get(progressRef),
        ]);
        if (!trailDoc.exists || trailDoc.get("state") !== "ACTIVE") {
          fail("failed-precondition", "DROP_NOT_AVAILABLE", "This drop is unavailable.");
        }
        const step = Number(dropData.trailStepIndex);
        const total = Number(dropData.trailTotalSteps);
        const currentStep = progress.exists ? Number(progress.get("currentStepIndex")) : 0;
        if (step !== currentStep) {
          fail("failed-precondition", "TRAIL_STEP_LOCKED", "Find the previous Trail drop first.");
        }
        const completedDropIds = progress.exists && Array.isArray(progress.get("completedDropIds")) ?
          [...progress.get("completedDropIds")] : [];
        completedDropIds.push(dropId);
        trailCompleted = step + 1 === total;
        mainTrailCompleted = trailCompleted && trailDoc.get("isMain") === true;
        progressData = {
          schemaVersion: 1,
          experienceCode,
          trailId,
          trailVersion: trailDoc.get("version"),
          currentStepIndex: step + 1,
          completedDropIds,
          startedAt: progress.get("startedAt") ?? now,
          updatedAt: now,
          completedAt: trailCompleted ? now : null,
        };
        trail = {
          trailId,
          stepIndex: step,
          totalSteps: total,
          completedAtUnlock: trailCompleted,
        };
      }
      const actorKey = protectedKey("actor", uid);
      const actorMarker = groupRef.collection("analyticsActors").doc(actorKey);
      const existingActor = await transaction.get(actorMarker);
      const receiptId = firestore.collection("receiptIds").doc().id;
      const snapshot = {
        title: payload.get("title"),
        body: payload.get("body") ?? null,
        contentKind: payload.get("contentKind"),
        hostLabel: dropData.hostLabel,
        mediaAssetId: payload.get("mediaAssetId") ?? null,
        mediaMimeType: payload.get("mediaMimeType") ?? null,
        mediaAltText: payload.get("mediaAltText") ?? null,
        rewardPresentation: payload.get("rewardPresentation") ?? null,
        editedAt: dropData.editedAt ?? null,
      };
      let rewardReceipt: admin.firestore.DocumentData | null = null;
      let rewardIssued = false;
      let rewardUnavailable = false;
      if (dropData.dropKind === "REWARD") {
        const rewardRef = firestore.collection("rewards").doc(dropId);
        const reward = await transaction.get(rewardRef);
        if (reward.exists && reward.get("state") === "ACTIVE" &&
            Number(reward.get("issuedCount")) < Number(reward.get("inventoryLimit"))) {
          const availableCodes = await transaction.get(
            rewardRef.collection("codes").where("state", "==", "AVAILABLE").limit(1)
          );
          const codeDoc = availableCodes.docs[0];
          if (codeDoc) {
            rewardReceipt = {
              schemaVersion: 1,
              receiptId,
              dropId,
              experienceCode,
              code: codeDoc.get("code"),
              state: "ISSUED",
              issuedAt: now,
              usedAt: null,
              updatedAt: now,
            };
            transaction.update(codeDoc.ref, {
              state: "ISSUED",
              receiptId,
              issuedAt: now,
              usedAt: null,
              lastChangedBy: "SERVER_UNLOCK",
              version: FieldValue.increment(1),
            });
            transaction.create(codeDoc.ref.collection("events").doc(), {
              schemaVersion: 1,
              transition: "AVAILABLE_TO_ISSUED",
              occurredAt: now,
              reason: "UNLOCK_SUCCEEDED",
              expiresAt: Timestamp.fromMillis(
                now.toMillis() + ANALYTICS_RETENTION_DAYS * 24 * 60 * 60 * 1000
              ),
            });
            transaction.update(rewardRef, {
              issuedCount: FieldValue.increment(1),
              updatedAt: now,
            });
            transaction.create(firestore.collection("rewardReceiptOwners").doc(receiptId), {
              uid,
              dropId,
              createdAt: now,
            });
            transaction.create(rewardReceiptRef, rewardReceipt);
            rewardIssued = true;
          } else {
            rewardUnavailable = true;
          }
        } else {
          rewardUnavailable = true;
        }
      }
      const receipt = {
        schemaVersion: 1,
        receiptId,
        dropId,
        experienceCode,
        unlockedAt: now,
        payloadVersion,
        source: "SERVER_PROXIMITY_V1",
        snapshot,
        trail,
        hasRewardReceipt: rewardIssued,
      };
      transaction.create(receiptRef, receipt);
      if (progressRef && progressData) transaction.set(progressRef, progressData);
      if (!existingActor.exists) {
        transaction.create(actorMarker, {firstUnlockedAt: now, expiresAt: null});
      }
      const summaryUpdates: JsonMap = {
        schemaVersion: 2,
        unlocks: FieldValue.increment(1),
        updatedAt: now,
      };
      if (!existingActor.exists) summaryUpdates.uniqueUnlockers = FieldValue.increment(1);
      if (mainTrailCompleted) {
        summaryUpdates.mainTrailCompletions = FieldValue.increment(1);
      }
      if (rewardIssued) summaryUpdates.codesIssued = FieldValue.increment(1);
      transaction.set(groupRef.collection("analytics").doc("summary"), summaryUpdates, {
        merge: true,
      });
      transaction.set(groupRef.collection("analytics").doc(`drop_${dropId}`), {
        schemaVersion: 2,
        dropId,
        unlocks: FieldValue.increment(1),
        codesIssued: FieldValue.increment(rewardIssued ? 1 : 0),
        updatedAt: now,
      }, {merge: true});
      return {
        created: true,
        trailCompleted,
        mainTrailCompleted,
        rewardIssued,
        rewardUnavailable,
        receipt,
        rewardReceipt,
        experienceCode,
        trailId,
      };
    });
    if (result.created) {
      await Promise.all([
        writeLedgerEvent({
          eventName: "unlock_succeeded",
          origin: "SERVER",
          dedupeKey: `unlock-succeeded:${result.receipt.receiptId}`,
          actorUid: uid,
          entrySessionId,
          experienceCode: result.experienceCode,
          dropId,
          trailId: result.trailId,
        }),
        writeLedgerEvent({
          eventName: "drop_collected",
          origin: "SERVER",
          dedupeKey: `drop-collected:${result.receipt.receiptId}`,
          actorUid: uid,
          entrySessionId,
          experienceCode: result.experienceCode,
          dropId,
          trailId: result.trailId,
        }),
        result.trailCompleted ? writeLedgerEvent({
          eventName: "trail_completed",
          origin: "SERVER",
          dedupeKey: `trail-completed:${uid}:${result.trailId}`,
          actorUid: uid,
          experienceCode: result.experienceCode,
          trailId: result.trailId,
        }) : Promise.resolve(false),
        result.rewardIssued ? writeLedgerEvent({
          eventName: "redemption_code_issued",
          origin: "SERVER",
          dedupeKey: `reward-issued:${result.receipt.receiptId}`,
          actorUid: uid,
          experienceCode: result.experienceCode,
          dropId,
        }) : Promise.resolve(false),
      ]);
    }
    return {
      schemaVersion: 1,
      status: result.created ? "UNLOCKED" : "ALREADY_UNLOCKED",
      receipt: result.receipt,
      reward: result.rewardReceipt ?? (result.rewardUnavailable ? {
        state: "UNAVAILABLE",
        code: null,
      } : {
        state: "NONE",
        code: null,
      }),
    };
  } catch (error) {
    const details = error instanceof functions.https.HttpsError &&
      error.details && typeof error.details === "object" ?
      error.details as Record<string, unknown> : {};
    if (details.reason === "TOO_FAR" && typeof details.distanceBucket === "string") {
      const failedDrop = await dropRef.get();
      const failedExperienceCode = failedDrop.exists &&
        typeof failedDrop.get("experienceCode") === "string" ?
        failedDrop.get("experienceCode") as string : null;
      if (failedExperienceCode) {
        await writeLedgerEvent({
          eventName: "unlock_failed_distance",
          origin: "SERVER",
          dedupeKey: `distance-failure:${uid}:${dropId}:${entrySessionId ?? crypto.randomUUID()}`,
          actorUid: uid,
          entrySessionId,
          experienceCode: failedExperienceCode,
          dropId,
          params: {distanceBucket: details.distanceBucket},
        });
      }
    }
    throw error;
  }
});

export const getCollectionMedia = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId", "payloadVersion"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  const payloadVersion = data.payloadVersion;
  if (!Number.isInteger(payloadVersion) || (payloadVersion as number) < 1) {
    fail("invalid-argument", "INVALID_REQUEST", "Provide a valid payload version.", {
      field: "payloadVersion",
    });
  }
  const firestore = admin.firestore();
  const receipt = await firestore.collection("users").doc(uid)
    .collection("unlocks").doc(dropId).get();
  if (!receipt.exists || receipt.get("payloadVersion") !== payloadVersion) {
    fail("not-found", "MEDIA_NOT_AVAILABLE", "Collection media is unavailable.");
  }
  const version = await firestore.collection("dropPayloads").doc(dropId)
    .collection("versions").doc(String(payloadVersion)).get();
  const assetId = version.get("mediaAssetId");
  if (!version.exists || typeof assetId !== "string") {
    fail("not-found", "MEDIA_NOT_AVAILABLE", "Collection media is unavailable.");
  }
  const objectPath = `drop-payloads/${dropId}/${payloadVersion}/${assetId}`;
  const file = admin.storage().bucket().file(objectPath);
  const [exists] = await file.exists();
  if (!exists) fail("not-found", "MEDIA_NOT_AVAILABLE", "Collection media is unavailable.");
  const expiresAt = new Date(Date.now() + MEDIA_URL_LIFETIME_MS);
  let url: string;
  const emulator = process.env.FIREBASE_STORAGE_EMULATOR_HOST;
  if (emulator) {
    url = `http://${emulator}/v0/b/${encodeURIComponent(file.bucket.name)}/o/` +
      `${encodeURIComponent(objectPath)}?alt=media`;
  } else {
    [url] = await file.getSignedUrl({action: "read", expires: expiresAt});
  }
  return {
    schemaVersion: 1,
    mimeType: version.get("mediaMimeType"),
    altText: version.get("mediaAltText"),
    url,
    expiresAt: expiresAt.toISOString(),
  };
});

const rewardOwner = async (
  uid: string,
  dropId: string
): Promise<admin.firestore.DocumentSnapshot> => {
  const reward = await admin.firestore().collection("rewards").doc(dropId).get();
  if (!reward.exists || reward.get("ownerId") !== uid) {
    return fail("not-found", "REWARD_NOT_FOUND", "Reward not found.");
  }
  return reward;
};

const normalizeRewardCode = (raw: unknown): string => {
  const code = typeof raw === "string" ? raw.trim().toUpperCase() : "";
  if (!REWARD_CODE_PATTERN.test(code)) {
    return fail("invalid-argument", "INVALID_REWARD_CODE", "Enter a valid reward code.", {
      field: "code",
    });
  }
  return code;
};

const rewardCodeHash = (code: string): string => crypto
  .createHash("sha256")
  .update(code)
  .digest("hex");

export const provisionRewardCodes = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  requireAdmin(context);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId", "codes"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  if (!Array.isArray(data.codes) || data.codes.length < 1 || data.codes.length > 500) {
    fail("invalid-argument", "INVALID_REWARD_POOL", "Provide 1 to 500 reward codes.", {
      field: "codes",
    });
  }
  const rawCodes = data.codes as unknown[];
  const codes = Array.from(new Set(rawCodes.map(normalizeRewardCode)));
  if (codes.length !== rawCodes.length) {
    fail("invalid-argument", "DUPLICATE_REWARD_CODE", "Reward codes must be unique.", {
      field: "codes",
    });
  }
  const firestore = admin.firestore();
  const rewardRef = firestore.collection("rewards").doc(dropId);
  const reward = await rewardRef.get();
  if (!reward.exists || reward.get("state") !== "ACTIVE") {
    fail("not-found", "REWARD_NOT_FOUND", "Reward not found.");
  }
  const existing = await rewardRef.collection("codes").get();
  if (existing.size + codes.length > Number(reward.get("inventoryLimit"))) {
    fail("invalid-argument", "REWARD_POOL_TOO_LARGE", "The pool exceeds its inventory limit.");
  }
  const existingHashes = new Set(existing.docs.map((document) => document.get("codeHash")));
  if (codes.some((code) => existingHashes.has(rewardCodeHash(code)))) {
    fail("already-exists", "DUPLICATE_REWARD_CODE", "A reward code already exists.");
  }
  const writer = firestore.bulkWriter();
  const now = Timestamp.now();
  codes.forEach((code) => writer.create(rewardRef.collection("codes").doc(), {
    schemaVersion: 1,
    code,
    codeHash: rewardCodeHash(code),
    state: "AVAILABLE",
    receiptId: null,
    issuedAt: null,
    usedAt: null,
    lastChangedBy: "OPERATOR_PROVISION",
    version: 1,
    createdAt: now,
    updatedAt: now,
  }));
  await writer.close();
  return {schemaVersion: 1, provisioned: codes.length};
});

export const listRewardCodes = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId", "state", "searchCode", "limit"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  const reward = await rewardOwner(uid, dropId);
  const state = optionalText(data.state, "state", 16);
  if (state && !["AVAILABLE", "ISSUED", "USED"].includes(state)) {
    fail("invalid-argument", "INVALID_REWARD_STATE", "Choose a valid reward state.");
  }
  const searchCode = data.searchCode ? normalizeRewardCode(data.searchCode) : null;
  const limit = data.limit === undefined ? 50 : data.limit;
  if (!Number.isInteger(limit) || (limit as number) < 1 || (limit as number) > 100) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a limit from 1 to 100.", {
      field: "limit",
    });
  }
  let query: admin.firestore.Query = reward.ref.collection("codes");
  if (searchCode) query = query.where("codeHash", "==", rewardCodeHash(searchCode));
  else if (state) query = query.where("state", "==", state);
  const snapshot = await query.limit(limit as number).get();
  const codes = await Promise.all(snapshot.docs.map(async (document) => {
    const events = await document.ref.collection("events")
      .orderBy("occurredAt", "desc").limit(10).get();
    return {
      code: document.get("code"),
      state: document.get("state"),
      issuedAt: isoTimestamp(document.get("issuedAt")),
      usedAt: isoTimestamp(document.get("usedAt")),
      version: document.get("version"),
      history: events.docs.map((event) => ({
        transition: event.get("transition"),
        occurredAt: isoTimestamp(event.get("occurredAt")),
        reason: event.get("reason") ?? null,
      })),
    };
  }));
  return {
    schemaVersion: 1,
    codes,
  };
});

const transitionRewardCode = async (
  uid: string,
  dropId: string,
  code: string,
  correctionReason: string | null
): Promise<{changed: boolean; experienceCode: string; receiptId: string}> => {
  const firestore = admin.firestore();
  const reward = await rewardOwner(uid, dropId);
  const query = await reward.ref.collection("codes")
    .where("codeHash", "==", rewardCodeHash(code)).limit(1).get();
  const codeDocument = query.docs[0];
  if (!codeDocument) {
    return fail("not-found", "REWARD_CODE_NOT_FOUND", "Reward code not found.");
  }
  const targetUsed = correctionReason === null;
  return firestore.runTransaction(async (transaction) => {
    const [freshReward, freshCode] = await Promise.all([
      transaction.get(reward.ref),
      transaction.get(codeDocument.ref),
    ]);
    const state = freshCode.get("state");
    if (targetUsed && state === "USED") {
      return {
        changed: false,
        experienceCode: String(freshReward.get("experienceCode")),
        receiptId: String(freshCode.get("receiptId")),
      };
    }
    if (targetUsed && state === "AVAILABLE") {
      return fail("failed-precondition", "REWARD_NOT_ISSUED", "This code was not issued.");
    }
    if (!targetUsed && state === "ISSUED") {
      return {
        changed: false,
        experienceCode: String(freshReward.get("experienceCode")),
        receiptId: String(freshCode.get("receiptId")),
      };
    }
    if (!targetUsed && state !== "USED") {
      return fail("failed-precondition", "REWARD_NOT_USED", "This code is not marked used.");
    }
    const receiptId = String(freshCode.get("receiptId") ?? "");
    const ownerMapRef = firestore.collection("rewardReceiptOwners").doc(receiptId);
    const ownerMap = await transaction.get(ownerMapRef);
    if (!ownerMap.exists || ownerMap.get("dropId") !== dropId) {
      return fail("internal", "REWARD_LINKAGE_MISSING", "Reward linkage is unavailable.");
    }
    const recipientUid = String(ownerMap.get("uid"));
    const userReceiptRef = firestore.collection("users").doc(recipientUid)
      .collection("rewardReceipts").doc(dropId);
    const now = Timestamp.now();
    transaction.update(codeDocument.ref, {
      state: targetUsed ? "USED" : "ISSUED",
      usedAt: targetUsed ? now : null,
      lastChangedBy: targetUsed ? "OWNER_CONFIRMED" : "OWNER_CORRECTED",
      version: FieldValue.increment(1),
      updatedAt: now,
    });
    transaction.create(codeDocument.ref.collection("events").doc(), {
      schemaVersion: 1,
      transition: targetUsed ? "ISSUED_TO_USED" : "USED_TO_ISSUED",
      occurredAt: now,
      reason: correctionReason,
      actorKey: protectedKey("actor", uid),
      expiresAt: Timestamp.fromMillis(
        now.toMillis() + ANALYTICS_RETENTION_DAYS * 24 * 60 * 60 * 1000
      ),
    });
    transaction.update(userReceiptRef, {
      state: targetUsed ? "USED" : "ISSUED",
      usedAt: targetUsed ? now : null,
      updatedAt: now,
    });
    transaction.update(reward.ref, {
      usedCount: FieldValue.increment(targetUsed ? 1 : -1),
      updatedAt: now,
    });
    transaction.set(
      firestore.collection("groups").doc(String(freshReward.get("experienceCode")))
        .collection("analytics").doc("summary"),
      {
        schemaVersion: 2,
        codesUsed: FieldValue.increment(targetUsed ? 1 : -1),
        updatedAt: now,
      },
      {merge: true}
    );
    transaction.set(
      firestore.collection("groups").doc(String(freshReward.get("experienceCode")))
        .collection("analytics").doc(`drop_${dropId}`),
      {
        schemaVersion: 2,
        dropId,
        codesUsed: FieldValue.increment(targetUsed ? 1 : -1),
        updatedAt: now,
      },
      {merge: true}
    );
    return {
      changed: true,
      experienceCode: String(freshReward.get("experienceCode")),
      receiptId,
    };
  });
};

export const markRewardCodeUsed = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId", "code"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  const code = normalizeRewardCode(data.code);
  const result = await transitionRewardCode(uid, dropId, code, null);
  if (result.changed) {
    await writeLedgerEvent({
      eventName: "redemption_code_marked_used",
      origin: "SERVER",
      dedupeKey: `reward-used:${result.receiptId}`,
      actorUid: uid,
      experienceCode: result.experienceCode,
      dropId,
    });
  }
  return {schemaVersion: 1, state: "USED", changed: result.changed};
});

export const correctRewardCodeUse = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId", "code", "reason"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  const code = normalizeRewardCode(data.code);
  const reason = textValue(data.reason, "reason", 10, 32) as string;
  if (!["MARKED_BY_MISTAKE", "BUSINESS_CORRECTION"].includes(reason)) {
    fail("invalid-argument", "INVALID_CORRECTION_REASON", "Choose a correction reason.");
  }
  const result = await transitionRewardCode(uid, dropId, code, reason);
  return {schemaVersion: 1, state: "ISSUED", changed: result.changed};
});

const verifyEventContext = async (
  uid: string,
  experienceCode: string | null,
  dropId: string | null,
  trailId: string | null
): Promise<{experienceCode: string | null; dropId: string | null; trailId: string | null}> => {
  const firestore = admin.firestore();
  let resolvedExperience = experienceCode;
  if (dropId) {
    const drop = await firestore.collection("experienceDrops").doc(dropId).get();
    if (!drop.exists || drop.get("state") !== "PUBLISHED") {
      return fail("not-found", "EVENT_CONTEXT_INVALID", "Event context is invalid.");
    }
    resolvedExperience = String(drop.get("experienceCode"));
    if (experienceCode && experienceCode !== resolvedExperience) {
      return fail("not-found", "EVENT_CONTEXT_INVALID", "Event context is invalid.");
    }
  }
  if (resolvedExperience) {
    const membership = await firestore.collection("users").doc(uid)
      .collection("groups").doc(resolvedExperience).get();
    if (!membership.exists) {
      return fail("not-found", "EVENT_CONTEXT_INVALID", "Event context is invalid.");
    }
  }
  if (trailId && !resolvedExperience) {
    return fail("not-found", "EVENT_CONTEXT_INVALID", "Event context is invalid.");
  }
  return {experienceCode: resolvedExperience, dropId, trailId};
};

export const recordClientEvent = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context);
  const data = asObject(raw);
  assertOnlyKeys(data, [
    "apiVersion",
    "eventId",
    "eventName",
    "occurredAt",
    "entrySessionId",
    "platform",
    "appVersion",
    "experienceCode",
    "dropId",
    "trailId",
    "installKey",
    "params",
  ]);
  requireApiVersion(data);
  const eventId = textValue(data.eventId, "eventId", 16, 128) as string;
  const eventName = textValue(data.eventName, "eventName", 3, 64) as string;
  if (!clientEvents.has(eventName)) {
    fail("permission-denied", "EVENT_OWNER_INVALID", "This event is server-owned.");
  }
  const occurredAt = timestampValue(data.occurredAt, "occurredAt");
  if (Math.abs(Date.now() - occurredAt.toMillis()) > 24 * 60 * 60 * 1000) {
    fail("invalid-argument", "INVALID_EVENT", "Event time is outside the accepted window.");
  }
  const entrySessionId = parseEntrySessionId(data.entrySessionId);
  const platform = textValue(data.platform, "platform", 3, 16) as string;
  const appVersion = textValue(data.appVersion, "appVersion", 1, 32) as string;
  const experienceCode = data.experienceCode ? normalizeExperienceCode(data.experienceCode) : null;
  const dropId = optionalText(data.dropId, "dropId", 128);
  const trailId = optionalText(data.trailId, "trailId", 128);
  const contextValues = await verifyEventContext(uid, experienceCode, dropId, trailId);
  let dedupeKey = `client-event:${uid}:${eventId}`;
  if (eventName === "app_first_open") {
    const installKey = textValue(data.installKey, "installKey", 16, 128) as string;
    dedupeKey = `app-first-open:${protectedKey("install", installKey)}`;
  } else if (data.installKey !== undefined) {
    fail("invalid-argument", "INVALID_EVENT", "Install key is not accepted for this event.", {
      field: "installKey",
    });
  }
  const created = await writeLedgerEvent({
    eventName,
    origin: "CLIENT",
    dedupeKey,
    actorUid: uid,
    entrySessionId,
    experienceCode: contextValues.experienceCode,
    dropId: contextValues.dropId,
    trailId: contextValues.trailId,
    occurredAt,
    platform,
    appVersion,
    params: sanitizeEventParams(eventName, data.params),
  });
  return {schemaVersion: 1, recorded: created};
});

export const recordAuthCompletion = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context);
  const data = asObject(raw);
  assertOnlyKeys(data, [
    "apiVersion",
    "entrySessionId",
    "upgradePath",
    "pendingUnlockResumed",
    "platform",
    "appVersion",
  ]);
  requireApiVersion(data);
  const entrySessionId = parseEntrySessionId(data.entrySessionId);
  if (!entrySessionId) {
    fail("invalid-argument", "INVALID_REQUEST", "Entry session is required.", {
      field: "entrySessionId",
    });
  }
  const upgradePath = optionalText(data.upgradePath, "upgradePath", 16);
  if (upgradePath && !["LINK", "MERGE"].includes(upgradePath)) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a valid upgrade path.");
  }
  if (typeof data.pendingUnlockResumed !== "boolean") {
    fail("invalid-argument", "INVALID_REQUEST", "Pending-unlock state is required.", {
      field: "pendingUnlockResumed",
    });
  }
  const stage = context.auth?.token.firebase?.sign_in_provider === "anonymous" ?
    "GUEST_SESSION" : "ACCOUNT";
  const created = await writeLedgerEvent({
    eventName: "auth_completed",
    origin: "SERVER",
    dedupeKey: `auth-completed:${entrySessionId}:${stage}`,
    actorUid: uid,
    entrySessionId,
    platform: textValue(data.platform, "platform", 3, 16),
    appVersion: textValue(data.appVersion, "appVersion", 1, 32),
    params: {stage, upgradePath, pendingUnlockResumed: data.pendingUnlockResumed},
  });
  return {schemaVersion: 1, recorded: created, stage};
});

const visibleDrop = async (
  uid: string,
  dropId: string
): Promise<admin.firestore.DocumentSnapshot> => {
  const firestore = admin.firestore();
  const drop = await firestore.collection("experienceDrops").doc(dropId).get();
  if (!drop.exists || drop.get("state") !== "PUBLISHED" ||
      drop.get("moderationState") !== "SAFE") {
    return fail("not-found", "DROP_NOT_AVAILABLE", "This drop is unavailable.");
  }
  const member = await firestore.collection("users").doc(uid)
    .collection("groups").doc(String(drop.get("experienceCode"))).get();
  if (!member.exists) {
    return fail("not-found", "DROP_NOT_AVAILABLE", "This drop is unavailable.");
  }
  return drop;
};

export const setDropLike = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context, true);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId", "liked"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  if (typeof data.liked !== "boolean") {
    fail("invalid-argument", "INVALID_REQUEST", "Like state is required.", {field: "liked"});
  }
  const firestore = admin.firestore();
  const dropRef = firestore.collection("experienceDrops").doc(dropId);
  const receiptRef = firestore.collection("users").doc(uid).collection("unlocks").doc(dropId);
  const likeRef = firestore.collection("users").doc(uid).collection("likes").doc(dropId);
  let changed = false;
  await firestore.runTransaction(async (transaction) => {
    const [drop, receipt, like] = await Promise.all([
      transaction.get(dropRef),
      transaction.get(receiptRef),
      transaction.get(likeRef),
    ]);
    if (!drop.exists || !receipt.exists) {
      fail("failed-precondition", "DROP_NOT_UNLOCKED", "Find this drop before liking it.");
    }
    const currentlyLiked = like.exists;
    if (currentlyLiked === data.liked) return;
    changed = true;
    if (data.liked) {
      transaction.create(likeRef, {
        schemaVersion: 1,
        dropId,
        experienceCode: drop.get("experienceCode"),
        likedAt: Timestamp.now(),
      });
    } else {
      transaction.delete(likeRef);
    }
    transaction.update(dropRef, {
      likeCount: FieldValue.increment(data.liked ? 1 : -1),
      updatedAt: Timestamp.now(),
    });
  });
  return {schemaVersion: 1, liked: data.liked, changed};
});

export const submitReport = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId", "reason", "narrative"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  await enforceRateLimit("submitReport", uid, 20, 24 * 60 * 60);
  const reason = textValue(data.reason, "reason", 3, 32) as string;
  if (!["SPAM", "HARASSMENT", "NSFW", "VIOLENCE", "OTHER"].includes(reason)) {
    fail("invalid-argument", "INVALID_REPORT_REASON", "Choose a report reason.");
  }
  const narrative = optionalText(data.narrative, "narrative", 500);
  const drop = await visibleDrop(uid, dropId);
  const window = Math.floor(Date.now() / (24 * 60 * 60 * 1000));
  const reportId = crypto.createHash("sha256")
    .update(`${uid}:${dropId}:${reason}:${window}`)
    .digest("hex");
  const reportRef = admin.firestore().collection("safetyReports").doc(reportId);
  let created = false;
  try {
    await reportRef.create({
      schemaVersion: 1,
      dropId,
      experienceCode: drop.get("experienceCode"),
      hostId: drop.get("ownerId"),
      reporterId: uid,
      reason,
      narrative,
      status: "PENDING",
      submittedAt: Timestamp.now(),
    });
    created = true;
  } catch (error) {
    if ((error as {code?: number}).code !== 6) throw error;
  }
  if (created) {
    await writeLedgerEvent({
      eventName: "report_submitted",
      origin: "SERVER",
      dedupeKey: `report-submitted:${reportId}`,
      actorUid: uid,
      experienceCode: String(drop.get("experienceCode")),
      dropId,
      params: {reason},
    });
  }
  return {schemaVersion: 1, accepted: true};
});

export const blockHost = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "dropId"]);
  requireApiVersion(data);
  const dropId = textValue(data.dropId, "dropId", 1, 128) as string;
  await enforceRateLimit("blockHost", uid, 60, 60 * 60);
  const drop = await visibleDrop(uid, dropId);
  const hostId = String(drop.get("ownerId"));
  if (hostId === uid) {
    fail("failed-precondition", "CANNOT_BLOCK_SELF", "You cannot block yourself.");
  }
  const ref = admin.firestore().collection("users").doc(uid)
    .collection("blockedHosts").doc(hostId);
  let created = false;
  try {
    await ref.create({
      schemaVersion: 1,
      hostId,
      createdAt: Timestamp.now(),
    });
    created = true;
  } catch (error) {
    if ((error as {code?: number}).code !== 6) throw error;
  }
  if (created) {
    await writeLedgerEvent({
      eventName: "block_created",
      origin: "SERVER",
      dedupeKey: `block-created:${uid}:${hostId}`,
      actorUid: uid,
      experienceCode: String(drop.get("experienceCode")),
      dropId,
    });
  }
  return {schemaVersion: 1, blocked: true, changed: created};
});

export const unblockHost = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context);
  const data = asObject(raw);
  assertOnlyKeys(data, ["apiVersion", "hostId"]);
  requireApiVersion(data);
  const hostId = textValue(data.hostId, "hostId", 1, 128) as string;
  await enforceRateLimit("unblockHost", uid, 60, 60 * 60);
  const ref = admin.firestore().collection("users").doc(uid)
    .collection("blockedHosts").doc(hostId);
  const existing = await ref.get();
  if (existing.exists) {
    await ref.delete();
  }
  return {schemaVersion: 1, blocked: false, changed: existing.exists};
});

export const submitFeedback = protectedCallable.https.onCall(async (
  raw: unknown,
  context
) => {
  const uid = requireActiveUser(context);
  const data = asObject(raw);
  assertOnlyKeys(data, [
    "apiVersion",
    "experienceCode",
    "rating",
    "category",
    "narrative",
  ]);
  requireApiVersion(data);
  const experienceCode = normalizeExperienceCode(data.experienceCode);
  const rating = data.rating;
  if (!Number.isInteger(rating) || (rating as number) < 1 || (rating as number) > 5) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a rating from 1 to 5.", {
      field: "rating",
    });
  }
  const category = textValue(data.category, "category", 3, 32) as string;
  if (!["OVERALL", "MAP", "UNLOCK", "REWARD", "ACCESSIBILITY"].includes(category)) {
    fail("invalid-argument", "INVALID_REQUEST", "Choose a feedback category.");
  }
  const narrative = optionalText(data.narrative, "narrative", 1000);
  const membership = await admin.firestore().collection("users").doc(uid)
    .collection("groups").doc(experienceCode).get();
  if (!membership.exists) {
    fail("permission-denied", "EXPERIENCE_NOT_JOINED", "Join this Experience first.");
  }
  const feedbackRef = admin.firestore().collection("feedbackResponses").doc();
  await feedbackRef.set({
    schemaVersion: 1,
    experienceCode,
    actorKey: protectedKey("actor", uid),
    rating,
    category,
    narrative,
    submittedAt: Timestamp.now(),
  });
  await writeLedgerEvent({
    eventName: "feedback_submitted",
    origin: "SERVER",
    dedupeKey: `feedback-submitted:${feedbackRef.id}`,
    actorUid: uid,
    experienceCode,
    params: {rating, category},
  });
  return {schemaVersion: 1, accepted: true};
});

/**
 * Recomputes private R2 Results from canonical membership, unlock, Trail, drop, and
 * reward-code documents. This is deliberately separate from the legacy rollup, whose
 * participant maps are not an approved R2 source after cutover.
 */
export const reconcileRedesignResults = functions
  .region(REGION)
  .runWith({timeoutSeconds: 540, memory: "1GB"})
  .pubsub.schedule("every 24 hours")
  .onRun(async () => {
    const firestore = admin.firestore();
    const [groups, memberships, drops, unlocks, trails, progress, rewardCodes] =
      await Promise.all([
        firestore.collection("groups").where("schemaVersion", "==", 2).get(),
        firestore.collectionGroup("groups").where("schemaVersion", "==", 2).get(),
        firestore.collection("experienceDrops").where("schemaVersion", "==", 1).get(),
        firestore.collectionGroup("unlocks").where("schemaVersion", "==", 1).get(),
        firestore.collectionGroup("trails").where("schemaVersion", "==", 1).get(),
        firestore.collectionGroup("trailProgress").where("schemaVersion", "==", 1).get(),
        firestore.collectionGroup("codes").where("schemaVersion", "==", 1).get(),
      ]);

    type Counts = {
      joinedParticipants: number;
      publishedDrops: number;
      uniqueUnlockers: Set<string>;
      unlocks: number;
      mainTrailCompletions: number;
      codesIssued: number;
      codesUsed: number;
    };
    type DropCounts = {unlocks: number; codesIssued: number; codesUsed: number};
    const totals = new Map<string, Counts>();
    const perDrop = new Map<string, DropCounts>();
    const dropExperience = new Map<string, string>();
    const mainTrails = new Set<string>();
    groups.docs.forEach((group) => totals.set(group.id, {
      joinedParticipants: 0,
      publishedDrops: 0,
      uniqueUnlockers: new Set<string>(),
      unlocks: 0,
      mainTrailCompletions: 0,
      codesIssued: 0,
      codesUsed: 0,
    }));
    memberships.docs.forEach((membership) => {
      const code = String(membership.get("code") ?? "");
      if (membership.get("role") === "SUBSCRIBER") {
        const total = totals.get(code);
        if (total) total.joinedParticipants += 1;
      }
    });
    drops.docs.forEach((drop) => {
      const code = String(drop.get("experienceCode") ?? "");
      if (!totals.has(code)) return;
      dropExperience.set(drop.id, code);
      perDrop.set(drop.id, {unlocks: 0, codesIssued: 0, codesUsed: 0});
      if (drop.get("state") === "PUBLISHED") {
        (totals.get(code) as Counts).publishedDrops += 1;
      }
    });
    trails.docs.forEach((trail) => {
      const code = trail.ref.parent.parent?.id;
      if (code && trail.get("isMain") === true) mainTrails.add(`${code}:${trail.id}`);
    });
    unlocks.docs.forEach((unlock) => {
      const code = String(unlock.get("experienceCode") ?? "");
      const dropId = String(unlock.get("dropId") ?? unlock.id);
      const total = totals.get(code);
      if (!total || dropExperience.get(dropId) !== code) return;
      total.unlocks += 1;
      const userId = unlock.ref.parent.parent?.id;
      if (userId) total.uniqueUnlockers.add(userId);
      const dropTotal = perDrop.get(dropId);
      if (dropTotal) dropTotal.unlocks += 1;
    });
    progress.docs.forEach((item) => {
      const code = String(item.get("experienceCode") ?? "");
      const trailId = String(item.get("trailId") ?? item.id);
      if (item.get("completedAt") && mainTrails.has(`${code}:${trailId}`)) {
        const total = totals.get(code);
        if (total) total.mainTrailCompletions += 1;
      }
    });
    rewardCodes.docs.forEach((rewardCode) => {
      const rewardRef = rewardCode.ref.parent.parent;
      if (!rewardRef || rewardRef.parent.id !== "rewards") return;
      const dropId = rewardRef.id;
      const code = dropExperience.get(dropId);
      const total = code ? totals.get(code) : null;
      const dropTotal = perDrop.get(dropId);
      if (!total || !dropTotal) return;
      const state = rewardCode.get("state");
      if (state === "ISSUED" || state === "USED") {
        total.codesIssued += 1;
        dropTotal.codesIssued += 1;
      }
      if (state === "USED") {
        total.codesUsed += 1;
        dropTotal.codesUsed += 1;
      }
    });

    const now = Timestamp.now();
    const writer = firestore.bulkWriter();
    totals.forEach((total, code) => writer.set(
      firestore.collection("groups").doc(code).collection("analytics").doc("summary"),
      {
        schemaVersion: 2,
        experienceCode: code,
        joinedParticipants: total.joinedParticipants,
        publishedDrops: total.publishedDrops,
        uniqueUnlockers: total.uniqueUnlockers.size,
        unlocks: total.unlocks,
        mainTrailCompletions: total.mainTrailCompletions,
        codesIssued: total.codesIssued,
        codesUsed: total.codesUsed,
        updatedAt: now,
        reconciledAt: now,
      },
      {merge: true}
    ));
    perDrop.forEach((total, dropId) => {
      const code = dropExperience.get(dropId);
      if (!code) return;
      writer.set(
        firestore.collection("groups").doc(code).collection("analytics")
          .doc(`drop_${dropId}`),
        {
          schemaVersion: 2,
          dropId,
          unlocks: total.unlocks,
          codesIssued: total.codesIssued,
          codesUsed: total.codesUsed,
          updatedAt: now,
          reconciledAt: now,
        },
        {merge: true}
      );
    });
    await writer.close();
    console.log(`Reconciled ${totals.size} R2 Results rollup(s).`);
  });

const deleteExpiredDocuments = async (
  query: admin.firestore.Query
): Promise<number> => {
  let deleted = 0;
  let hasMore = true;
  while (hasMore) {
    const snapshot = await query.limit(400).get();
    if (snapshot.empty) {
      hasMore = false;
      continue;
    }
    const writer = admin.firestore().bulkWriter();
    snapshot.docs.forEach((document) => writer.delete(document.ref));
    await writer.close();
    deleted += snapshot.size;
  }
  return deleted;
};

export const purgeExpiredRedesignData = functions
  .region(REGION)
  .runWith({timeoutSeconds: 540, memory: "1GB"})
  .pubsub.schedule("every 24 hours")
  .onRun(async () => {
    const firestore = admin.firestore();
    const now = Timestamp.now();
    const [events, dedupe, tokens, aliases, rewardAudits, rateLimits] = await Promise.all([
      deleteExpiredDocuments(
        firestore.collection("analyticsEvents").where("expiresAt", "<=", now)
      ),
      deleteExpiredDocuments(
        firestore.collection("analyticsEventDedupe").where("expiresAt", "<=", now)
      ),
      deleteExpiredDocuments(
        firestore.collection("organizerApplicationTokens").where("expiresAt", "<=", now)
      ),
      deleteExpiredDocuments(
        firestore.collection("analyticsActorAliases").where("expiresAt", "<=", now)
      ),
      deleteExpiredDocuments(
        firestore.collectionGroup("events").where("expiresAt", "<=", now)
      ),
      deleteExpiredDocuments(
        firestore.collection("callableRateLimits").where("expiresAt", "<=", now)
      ),
    ]);
    const declinedThreshold = Date.now() - 90 * 24 * 60 * 60 * 1000;
    const declined = await firestore.collection("organizerApplications")
      .where("status", "==", "DENIED").get();
    let applications = 0;
    for (const application of declined.docs) {
      const updatedAt = application.get("updatedAt");
      if (updatedAt instanceof Timestamp && updatedAt.toMillis() <= declinedThreshold) {
        await firestore.recursiveDelete(application.ref);
        applications += 1;
      }
    }
    const bucket = admin.storage().bucket();
    const [stagingFiles] = await bucket.getFiles({prefix: "drop-upload-staging/"});
    const stagingThreshold = Date.now() - 24 * 60 * 60 * 1000;
    let staging = 0;
    for (const file of stagingFiles) {
      const [metadata] = await file.getMetadata();
      const createdAt = Date.parse(metadata.timeCreated ?? "");
      if (Number.isFinite(createdAt) && createdAt <= stagingThreshold) {
        await file.delete({ignoreNotFound: true});
        staging += 1;
      }
    }
    console.log("Purged expired R2 data", {
      events,
      dedupe,
      tokens,
      aliases,
      rewardAudits,
      rateLimits,
      applications,
      staging,
    });
  });

export const sanitizeDropStagingUpload = functions
  .region(REGION)
  .storage.object()
  .onFinalize(async (object) => {
    const objectPath = object.name ?? "";
    const match = /^drop-upload-staging\/([^/]+)\/([^/]+)$/.exec(objectPath);
    if (!match) return;
    const ownerId = object.metadata?.ownerId;
    const size = Number(object.size ?? 0);
    const valid = ownerId === match[1] &&
      ["image/jpeg", "image/png", "image/webp"].includes(object.contentType ?? "") &&
      Number.isFinite(size) && size > 0 && size <= MAX_STAGING_IMAGE_BYTES;
    const file = admin.storage().bucket(object.bucket).file(objectPath);
    if (!valid) {
      await file.delete({ignoreNotFound: true});
      return;
    }
    // Replacing custom metadata removes any download-token value added outside
    // the approved client rule path while retaining only the ownership proof.
    await file.setMetadata({
      cacheControl: "private, no-store, max-age=0",
      contentType: object.contentType,
      metadata: {ownerId, purpose: "DROP_STAGING"},
    });
  });

export const REDESIGN_TEST_ONLY = Object.freeze({
  canonicalEventCount: canonicalEvents.size,
  distanceBucket,
  haversineMetres,
  rateLimits: Object.freeze({
    resolveExperience: {limit: 120, windowSeconds: 60 * 60},
    unlockDrop: {limit: 20, windowSeconds: 5 * 60},
    submitReport: {limit: 20, windowSeconds: 24 * 60 * 60},
    blockHost: {limit: 60, windowSeconds: 60 * 60},
    unblockHost: {limit: 60, windowSeconds: 60 * 60},
  }),
});
