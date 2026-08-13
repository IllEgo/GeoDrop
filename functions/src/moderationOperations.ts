import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import {FieldValue, Timestamp} from "firebase-admin/firestore";

const REGION = "us-central1";
const MODERATION_CASES = "moderationCases";
const APPEALS = "moderationAppeals";
const AUDIT_EVENTS = "moderationAuditEvents";
const MAX_QUEUE_RESULTS = 100;
const MAX_RATIONALE_LENGTH = 2000;
const MAX_APPEAL_LENGTH = 4000;

type Severity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
type Decision = "REMOVE_CONTENT" | "DISMISS" | "ESCALATE";

type ReportContext = {
  source?: unknown;
  contentType?: unknown;
  hasMedia?: unknown;
  dropType?: unknown;
};

const requireAuthenticatedUid = (
  context: functions.https.CallableContext
): string => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Sign in before continuing."
    );
  }
  return uid;
};

const requireModeratorUid = (
  context: functions.https.CallableContext
): string => {
  const uid = requireAuthenticatedUid(context);
  if (context.auth?.token.moderator !== true &&
      context.auth?.token.admin !== true) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "A moderator account is required."
    );
  }
  return uid;
};

const stringArray = (raw: unknown): string[] => {
  if (!Array.isArray(raw)) return [];
  return Array.from(new Set(raw
    .filter((value): value is string => typeof value === "string")
    .map((value) => value.trim().toLowerCase())
    .filter(Boolean)));
};

const severityFor = (reasons: string[]): Severity => {
  if (reasons.includes("violence")) return "HIGH";
  if (reasons.includes("nsfw") || reasons.includes("harassment")) {
    return "MEDIUM";
  }
  return "LOW";
};

const severityRank = (severity: Severity): number => {
  switch (severity) {
  case "CRITICAL": return 0;
  case "HIGH": return 1;
  case "MEDIUM": return 2;
  case "LOW": return 3;
  }
};

const normalizeContext = (raw: unknown): ReportContext => {
  if (!raw || typeof raw !== "object" || Array.isArray(raw)) return {};
  const source = raw as ReportContext;
  const normalized: ReportContext = {};
  if (typeof source.source === "string") {
    normalized.source = source.source.trim().slice(0, 100);
  }
  if (typeof source.contentType === "string") {
    normalized.contentType = source.contentType.trim().slice(0, 50);
  }
  if (typeof source.hasMedia === "boolean") {
    normalized.hasMedia = source.hasMedia;
  }
  if (typeof source.dropType === "string") {
    normalized.dropType = source.dropType.trim().slice(0, 50);
  }
  return normalized;
};

const reporterStatusRef = (reporterId: string, reportId: string) =>
  admin.firestore()
    .collection("users")
    .doc(reporterId)
    .collection("reportStatuses")
    .doc(reportId);

const recordModeratorAudit = async (
  moderatorId: string,
  action: string,
  targetId: string | null,
  details: Record<string, unknown> = {}
): Promise<void> => {
  await admin.firestore().collection(AUDIT_EVENTS).add({
    moderatorId,
    action,
    targetId,
    details,
    occurredAt: FieldValue.serverTimestamp(),
  });
};

const notifyReporter = async (
  reporterId: string,
  reportId: string,
  status: string
): Promise<void> => {
  const tokenSnapshot = await admin.firestore()
    .collection("users")
    .doc(reporterId)
    .collection("notificationTokens")
    .get();
  const tokens = Array.from(new Set(tokenSnapshot.docs.map((document) => {
    const token = document.get("token");
    return typeof token === "string" ? token.trim() : "";
  }).filter(Boolean))).slice(0, 500);
  if (tokens.length === 0) return;

  const response = await admin.messaging().sendEachForMulticast({
    tokens,
    notification: {
      title: "Report status updated",
      body: status === "ACTION_TAKEN" ?
        "GeoDrop took action after reviewing your report." :
        status === "ESCALATED" ?
          "Your report was escalated for additional review." :
          "GeoDrop completed its review of your report.",
    },
    data: {
      event: "REPORT_STATUS_UPDATED",
      reportId,
      status,
    },
  });
  const invalidRefs: admin.firestore.DocumentReference[] = [];
  response.responses.forEach((result, index) => {
    if (result.success || !result.error) return;
    if ([
      "messaging/registration-token-not-registered",
      "messaging/invalid-registration-token",
    ].includes(result.error.code)) {
      tokenSnapshot.docs.forEach((document) => {
        if (document.get("token") === tokens[index]) invalidRefs.push(document.ref);
      });
    }
  });
  if (invalidRefs.length > 0) {
    const writer = admin.firestore().bulkWriter();
    invalidRefs.forEach((reference) => writer.delete(reference));
    await writer.close();
  }
};

const setSuspended = async (
  userId: string,
  moderatorId: string,
  reason: string,
  caseId: string
): Promise<void> => {
  const auth = admin.auth();
  const user = await auth.getUser(userId);
  await auth.setCustomUserClaims(userId, {
    ...(user.customClaims ?? {}),
    suspended: true,
  });
  await auth.revokeRefreshTokens(userId);
  await admin.firestore().collection("users").doc(userId).set({
    moderationStatus: "SUSPENDED",
    suspendedAt: FieldValue.serverTimestamp(),
    suspendedBy: moderatorId,
    suspensionReason: reason,
    suspensionCaseId: caseId,
  }, {merge: true});
};

const reverseEnforcementForOverturnedAppeal = async (
  reportId: string,
  subjectUserId: string,
  dropId: string,
  contentCollection: string
): Promise<void> => {
  const firestore = admin.firestore();
  if (dropId) {
    const targetCollection = contentCollection === "experienceDrops" ?
      "experienceDrops" : "drops";
    const dropRef = firestore.collection(targetCollection).doc(dropId);
    const drop = await dropRef.get();
    if (drop.exists && drop.get("moderationRemovalCaseId") === reportId) {
      await dropRef.set(targetCollection === "experienceDrops" ? {
        moderationState: "SAFE",
        moderationRemovalCaseId: FieldValue.delete(),
        moderatedAt: FieldValue.delete(),
        updatedAt: FieldValue.serverTimestamp(),
      } : {
        isDeleted: false,
        deletedAt: FieldValue.delete(),
        moderationRemovalCaseId: FieldValue.delete(),
      }, {merge: true});
    }
  }
  if (!subjectUserId) return;
  const profileRef = firestore.collection("users").doc(subjectUserId);
  const profile = await profileRef.get();
  if (!profile.exists || profile.get("suspensionCaseId") !== reportId) return;

  const auth = admin.auth();
  const user = await auth.getUser(subjectUserId);
  const claims = {...(user.customClaims ?? {})};
  delete claims.suspended;
  await auth.setCustomUserClaims(subjectUserId, claims);
  await auth.revokeRefreshTokens(subjectUserId);
  await profileRef.set({
    moderationStatus: "ACTIVE",
    suspendedAt: FieldValue.delete(),
    suspendedBy: FieldValue.delete(),
    suspensionReason: FieldValue.delete(),
    suspensionCaseId: FieldValue.delete(),
    reinstatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});
};

type ModerationReportInput = {
  reportId: string;
  dropId: string;
  reporterId: string;
  reasons: string[];
  reportedAt: unknown;
  context: ReportContext;
  sourceCollection: "reports" | "safetyReports";
  contentCollection: "drops" | "experienceDrops";
  sourceRef: admin.firestore.DocumentReference;
};

const ingestModerationReport = async (input: ModerationReportInput): Promise<void> => {
  const {
    reportId,
    dropId,
    reporterId,
    reasons,
    reportedAt,
    context,
    sourceCollection,
    contentCollection,
    sourceRef,
  } = input;
  if (!reportId || !dropId || !reporterId) {
    console.error(`Report ${reportId || "<missing>"} has invalid identifiers.`);
    return;
  }

  const severity = severityFor(reasons);
  const firestore = admin.firestore();
  const dropSnapshot = await firestore.collection(contentCollection).doc(dropId).get();
  const drop = dropSnapshot.data() ?? {};
  const subjectUserId = typeof (drop.createdBy ?? drop.ownerId) === "string" ?
    String(drop.createdBy ?? drop.ownerId).trim() : null;
  let evidence: Record<string, unknown>;

  if (contentCollection === "experienceDrops") {
    const version = typeof drop.payloadVersion === "number" ? drop.payloadVersion : null;
    const payloadSnapshot = version === null ? null : await firestore
      .collection("dropPayloads").doc(dropId)
      .collection("versions").doc(String(version)).get();
    const payload = payloadSnapshot?.data() ?? {};
    const assetId = typeof payload.mediaAssetId === "string" ? payload.mediaAssetId : null;
    const asset = assetId ?
      (await firestore.collection("dropMediaAssets").doc(assetId).get()).data() ?? {} : {};
    evidence = {
      contentType: drop.contentKind ?? payload.contentKind ?? "TEXT",
      dropType: drop.dropKind ?? null,
      visibility: "EXPERIENCE",
      groupCode: drop.experienceCode ?? null,
      text: payload.title ?? null,
      description: payload.body ?? null,
      mediaStoragePath: asset.storagePath ?? null,
      mediaMimeType: payload.mediaMimeType ?? asset.mimeType ?? null,
      mediaAltText: payload.mediaAltText ?? null,
      isDeleted: drop.state === "DELETED",
      isNsfw: false,
      payloadVersion: version,
      sourceContext: normalizeContext(context),
    };
  } else {
    const contentType = typeof drop.contentType === "string" ?
      drop.contentType : normalizeContext(context).contentType ?? "TEXT";
    evidence = {
      contentType,
      dropType: drop.dropType ?? null,
      visibility: drop.visibility ?? null,
      groupCode: drop.groupCode ?? null,
      text: drop.text ?? null,
      description: drop.description ?? null,
      mediaStoragePath: drop.mediaStoragePath ?? null,
      mediaMimeType: drop.mediaMimeType ?? null,
      isDeleted: drop.isDeleted === true,
      isNsfw: drop.isNsfw === true,
      sourceContext: normalizeContext(context),
    };
  }

  const caseRef = firestore.collection(MODERATION_CASES).doc(reportId);
  const batch = firestore.batch();
  batch.set(caseRef, {
    reportId,
    dropId,
    reporterId,
    subjectUserId,
    reasons,
    severity,
    severityRank: severityRank(severity),
    status: "OPEN",
    sourceCollection,
    contentCollection,
    receivedAt: FieldValue.serverTimestamp(),
    reportedAt: reportedAt ?? null,
    firstTriagedAt: null,
    decidedAt: null,
    assignedTo: null,
    escalationState: severity === "HIGH" ? "REVIEW_PROMPTLY" : "STANDARD",
    evidence,
  }, {merge: false});
  batch.set(reporterStatusRef(reporterId, reportId), {
    reportId,
    dropId,
    status: "RECEIVED",
    receivedAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  });
  batch.update(sourceRef, {
    status: "queued",
    severity,
    receivedAt: FieldValue.serverTimestamp(),
  });
  await batch.commit();
};

export const ingestUserReport = functions
  .region(REGION)
  .firestore.document("reports/{reportId}")
  .onCreate(async (snapshot, context) => {
    const reportId = String(context.params.reportId ?? "");
    const report = snapshot.data();
    const dropId = typeof report.dropId === "string" ? report.dropId.trim() : "";
    const reporterId = typeof report.reportedBy === "string" ?
      report.reportedBy.trim() : "";
    await ingestModerationReport({
      reportId,
      dropId,
      reporterId,
      reasons: stringArray(report.reasonCodes),
      reportedAt: report.reportedAt ?? null,
      context: normalizeContext(report.context),
      sourceCollection: "reports",
      contentCollection: "drops",
      sourceRef: snapshot.ref,
    });
  });

export const ingestRedesignReport = functions
  .region(REGION)
  .firestore.document("safetyReports/{reportId}")
  .onCreate(async (snapshot, context) => {
    const reportId = String(context.params.reportId ?? "");
    const report = snapshot.data();
    const reason = typeof report.reason === "string" ? report.reason : "other";
    await ingestModerationReport({
      reportId,
      dropId: typeof report.dropId === "string" ? report.dropId.trim() : "",
      reporterId: typeof report.reporterId === "string" ? report.reporterId.trim() : "",
      reasons: stringArray([reason]),
      reportedAt: report.submittedAt ?? null,
      context: {
        source: "REDESIGN_PARTICIPANT",
        contentType: report.contentKind,
        hasMedia: report.contentKind === "PHOTO",
        dropType: report.dropKind,
      },
      sourceCollection: "safetyReports",
      contentCollection: "experienceDrops",
      sourceRef: snapshot.ref,
    });
  });

export const listModerationQueue = functions
  .region(REGION)
  .runWith({enforceAppCheck: true})
  .https.onCall(async (data: {limit?: unknown}, context) => {
    const moderatorId = requireModeratorUid(context);
    const rawLimit = typeof data?.limit === "number" ? data.limit : 50;
    const limit = Math.max(1, Math.min(MAX_QUEUE_RESULTS, Math.floor(rawLimit)));
    const snapshot = await admin.firestore().collection(MODERATION_CASES)
      .where("status", "in", ["OPEN", "TRIAGED", "ESCALATED"])
      .limit(MAX_QUEUE_RESULTS)
      .get();
    const cases = snapshot.docs
      .map((document) => ({
        id: document.id,
        ...document.data(),
      }) as admin.firestore.DocumentData & {id: string})
      .sort((left, right) => {
        const leftRank = typeof left.severityRank === "number" ? left.severityRank : 99;
        const rightRank = typeof right.severityRank === "number" ? right.severityRank : 99;
        if (leftRank !== rightRank) return leftRank - rightRank;
        const leftTime = left.receivedAt instanceof Timestamp ?
          left.receivedAt.toMillis() : 0;
        const rightTime = right.receivedAt instanceof Timestamp ?
          right.receivedAt.toMillis() : 0;
        return leftTime - rightTime;
      })
      .slice(0, limit);
    await recordModeratorAudit(moderatorId, "QUEUE_VIEWED", null, {
      returnedCases: cases.length,
    });
    return {cases};
  });

export const triageModerationCase = functions
  .region(REGION)
  .runWith({enforceAppCheck: true})
  .https.onCall(async (
    data: {reportId?: unknown; severity?: unknown},
    context
  ) => {
    const moderatorId = requireModeratorUid(context);
    const reportId = typeof data?.reportId === "string" ?
      data.reportId.trim() : "";
    const severity = typeof data?.severity === "string" ?
      data.severity.trim().toUpperCase() as Severity : null;
    if (!reportId || !severity ||
        !["CRITICAL", "HIGH", "MEDIUM", "LOW"].includes(severity)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Provide a report ID and valid severity."
      );
    }
    const caseRef = admin.firestore().collection(MODERATION_CASES).doc(reportId);
    const current = await caseRef.get();
    if (!current.exists) {
      throw new functions.https.HttpsError("not-found", "Moderation case not found.");
    }
    await caseRef.set({
      status: severity === "CRITICAL" ? "ESCALATED" : "TRIAGED",
      severity,
      severityRank: severityRank(severity),
      assignedTo: moderatorId,
      firstTriagedAt: current.get("firstTriagedAt") ??
        FieldValue.serverTimestamp(),
      escalationState: severity === "CRITICAL" ?
        "IMMEDIATE_ESCALATION_REQUIRED" : "STANDARD",
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
    await recordModeratorAudit(moderatorId, "CASE_TRIAGED", reportId, {
      severity,
    });
    return {reportId, severity};
  });

export const decideModerationCase = functions
  .region(REGION)
  .runWith({enforceAppCheck: true, timeoutSeconds: 60})
  .https.onCall(async (
    data: {
      reportId?: unknown;
      decision?: unknown;
      rationale?: unknown;
      suspendSubject?: unknown;
    },
    context
  ) => {
    const moderatorId = requireModeratorUid(context);
    const reportId = typeof data?.reportId === "string" ?
      data.reportId.trim() : "";
    const decision = typeof data?.decision === "string" ?
      data.decision.trim().toUpperCase() as Decision : null;
    const rationale = typeof data?.rationale === "string" ?
      data.rationale.trim() : "";
    const suspendSubject = data?.suspendSubject === true;
    if (!reportId || !decision ||
        !["REMOVE_CONTENT", "DISMISS", "ESCALATE"].includes(decision) ||
        !rationale || rationale.length > MAX_RATIONALE_LENGTH) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Provide a valid decision and rationale up to ${MAX_RATIONALE_LENGTH} characters.`
      );
    }

    const firestore = admin.firestore();
    const caseRef = firestore.collection(MODERATION_CASES).doc(reportId);
    const caseSnapshot = await caseRef.get();
    if (!caseSnapshot.exists) {
      throw new functions.https.HttpsError("not-found", "Moderation case not found.");
    }
    const moderationCase = caseSnapshot.data() ?? {};
    const dropId = typeof moderationCase.dropId === "string" ?
      moderationCase.dropId : "";
    const reporterId = typeof moderationCase.reporterId === "string" ?
      moderationCase.reporterId : "";
    const subjectUserId = typeof moderationCase.subjectUserId === "string" ?
      moderationCase.subjectUserId : "";
    const sourceCollection = moderationCase.sourceCollection === "safetyReports" ?
      "safetyReports" : "reports";
    const contentCollection = moderationCase.contentCollection === "experienceDrops" ?
      "experienceDrops" : "drops";
    const publicStatus = decision === "REMOVE_CONTENT" ?
      "ACTION_TAKEN" : decision === "DISMISS" ? "CLOSED" : "ESCALATED";

    const batch = firestore.batch();
    batch.set(caseRef, {
      status: decision === "ESCALATE" ? "ESCALATED" : "DECIDED",
      decision,
      rationale,
      decidedAt: decision === "ESCALATE" ? null :
        FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
      assignedTo: moderatorId,
      escalationState: decision === "ESCALATE" ?
        "LEGAL_OR_SAFETY_REVIEW" : "COMPLETE",
    }, {merge: true});
    batch.set(firestore.collection(sourceCollection).doc(reportId), {
      status: publicStatus.toLowerCase(),
      resolvedAt: decision === "ESCALATE" ? null :
        FieldValue.serverTimestamp(),
    }, {merge: true});
    if (reporterId) {
      batch.set(reporterStatusRef(reporterId, reportId), {
        status: publicStatus,
        updatedAt: FieldValue.serverTimestamp(),
        resolvedAt: decision === "ESCALATE" ? null :
          FieldValue.serverTimestamp(),
      }, {merge: true});
    }
    if (decision === "REMOVE_CONTENT" && dropId) {
      batch.set(
        firestore.collection(contentCollection).doc(dropId),
        contentCollection === "experienceDrops" ? {
          moderationState: "REMOVED",
          moderationRemovalCaseId: reportId,
          moderatedAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        } : {
          isDeleted: true,
          deletedAt: FieldValue.serverTimestamp(),
          moderationRemovalCaseId: reportId,
        },
        {merge: true}
      );
    }
    await batch.commit();

    if (suspendSubject) {
      if (!subjectUserId) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "The reported content has no account to suspend."
        );
      }
      await setSuspended(subjectUserId, moderatorId, rationale, reportId);
    }
    if (reporterId) {
      await notifyReporter(reporterId, reportId, publicStatus);
    }
    await recordModeratorAudit(moderatorId, "CASE_DECIDED", reportId, {
      decision,
      subjectSuspended: suspendSubject,
      publicStatus,
    });
    return {reportId, status: publicStatus, subjectSuspended: suspendSubject};
  });

export const submitModerationAppeal = functions
  .region(REGION)
  .runWith({enforceAppCheck: true})
  .https.onCall(async (
    data: {reportId?: unknown; message?: unknown},
    context
  ) => {
    const uid = requireAuthenticatedUid(context);
    const reportId = typeof data?.reportId === "string" ?
      data.reportId.trim() : "";
    const message = typeof data?.message === "string" ? data.message.trim() : "";
    if (!reportId || !message || message.length > MAX_APPEAL_LENGTH) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Provide a case ID and appeal up to ${MAX_APPEAL_LENGTH} characters.`
      );
    }
    const firestore = admin.firestore();
    const caseSnapshot = await firestore.collection(MODERATION_CASES).doc(reportId).get();
    if (!caseSnapshot.exists || caseSnapshot.get("subjectUserId") !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Only the affected account may appeal this decision."
      );
    }
    if (caseSnapshot.get("status") !== "DECIDED") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "This case is not eligible for appeal."
      );
    }

    const appealRef = firestore.collection(APPEALS).doc();
    await appealRef.set({
      appealId: appealRef.id,
      reportId,
      appellantId: uid,
      message,
      status: "OPEN",
      submittedAt: FieldValue.serverTimestamp(),
      decidedAt: null,
    });
    await firestore.collection(MODERATION_CASES).doc(reportId).set({
      appealStatus: "OPEN",
      appealId: appealRef.id,
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
    return {appealId: appealRef.id, status: "OPEN"};
  });

export const decideModerationAppeal = functions
  .region(REGION)
  .runWith({enforceAppCheck: true})
  .https.onCall(async (
    data: {appealId?: unknown; outcome?: unknown; rationale?: unknown},
    context
  ) => {
    const moderatorId = requireModeratorUid(context);
    const appealId = typeof data?.appealId === "string" ?
      data.appealId.trim() : "";
    const outcome = typeof data?.outcome === "string" ?
      data.outcome.trim().toUpperCase() : "";
    const rationale = typeof data?.rationale === "string" ?
      data.rationale.trim() : "";
    if (!appealId || !["UPHELD", "OVERTURNED"].includes(outcome) ||
        !rationale || rationale.length > MAX_RATIONALE_LENGTH) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Provide a valid appeal decision and rationale."
      );
    }
    const firestore = admin.firestore();
    const appealRef = firestore.collection(APPEALS).doc(appealId);
    const appeal = await appealRef.get();
    if (!appeal.exists || appeal.get("status") !== "OPEN") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Open appeal not found."
      );
    }
    const reportId = appeal.get("reportId") as string;
    const appellantId = appeal.get("appellantId") as string;
    const moderationCase = await firestore
      .collection(MODERATION_CASES)
      .doc(reportId)
      .get();
    const dropId = moderationCase.get("dropId") as string | undefined;
    const subjectUserId = moderationCase.get("subjectUserId") as string | undefined;
    const contentCollection = moderationCase.get("contentCollection") === "experienceDrops" ?
      "experienceDrops" : "drops";
    const batch = firestore.batch();
    batch.set(appealRef, {
      status: outcome,
      rationale,
      decidedBy: moderatorId,
      decidedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
    batch.set(firestore.collection(MODERATION_CASES).doc(reportId), {
      appealStatus: outcome,
      appealDecidedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
    batch.set(reporterStatusRef(appellantId, `appeal-${appealId}`), {
      appealId,
      reportId,
      status: outcome,
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
    await batch.commit();
    if (outcome === "OVERTURNED") {
      await reverseEnforcementForOverturnedAppeal(
        reportId,
        subjectUserId ?? appellantId,
        dropId ?? "",
        contentCollection
      );
    }
    await notifyReporter(appellantId, `appeal-${appealId}`, outcome);
    await recordModeratorAudit(moderatorId, "APPEAL_DECIDED", appealId, {
      reportId,
      outcome,
    });
    return {appealId, status: outcome};
  });

export const alertOverdueModerationCases = functions
  .region(REGION)
  .pubsub.schedule("every 15 minutes")
  .onRun(async () => {
    const snapshot = await admin.firestore().collection(MODERATION_CASES)
      .where("status", "in", ["OPEN", "TRIAGED", "ESCALATED"])
      .limit(500)
      .get();
    const now = Date.now();
    const thresholds: Record<Severity, number> = {
      CRITICAL: 60 * 60 * 1000,
      HIGH: 4 * 60 * 60 * 1000,
      MEDIUM: 24 * 60 * 60 * 1000,
      LOW: 24 * 60 * 60 * 1000,
    };
    const breaches: Array<Record<string, unknown>> = [];
    snapshot.docs.forEach((document) => {
      const severity = String(document.get("severity") ?? "LOW") as Severity;
      const receivedAt = document.get("receivedAt");
      const triagedAt = document.get("firstTriagedAt");
      if (!(receivedAt instanceof Timestamp) || triagedAt) return;
      const ageMs = now - receivedAt.toMillis();
      const thresholdMs = thresholds[severity] ?? thresholds.LOW;
      if (ageMs >= thresholdMs) {
        breaches.push({
          reportId: document.id,
          severity,
          ageMinutes: Math.floor(ageMs / 60000),
          thresholdMinutes: Math.floor(thresholdMs / 60000),
        });
      }
    });

    if (breaches.length > 0) {
      functions.logger.error("MODERATION_SLA_BREACH", {
        breachCount: breaches.length,
        breaches,
        alertPolicyRequired: true,
      });
    } else {
      functions.logger.info("MODERATION_SLA_HEALTHY", {
        openCaseCount: snapshot.size,
      });
    }
    return null;
  });
