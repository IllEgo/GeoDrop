"use strict";

const admin = require("firebase-admin");

for (const name of ["FIREBASE_AUTH_EMULATOR_HOST", "FIRESTORE_EMULATOR_HOST"]) {
  if (!process.env[name]) throw new Error(`${name} is required; refusing production.`);
}

const projectId = process.env.GCLOUD_PROJECT || "geodrop-ci";
admin.initializeApp({projectId});
const db = admin.firestore();
const auth = admin.auth();
const password = "GeoDrop-P0-Moderation-42!";
const reporterUid = "p0-moderation-reporter";
const subjectUid = "p0-moderation-subject";
const moderatorOneUid = "p0-moderator-one";
const moderatorTwoUid = "p0-moderator-two";
const reportId = "p0-moderation-report";
const dropId = "p0-moderation-drop";
const redesignReportId = "r9-redesign-moderation-report";
const redesignDropId = "r9-redesign-moderation-drop";
const coverageFixtures = [
  {suffix: "text", contentType: "TEXT", mimeType: null},
  {suffix: "photo", contentType: "PHOTO", mimeType: "image/jpeg"},
  {suffix: "audio", contentType: "AUDIO", mimeType: "audio/mpeg"},
  {suffix: "video", contentType: "VIDEO", mimeType: "video/mp4"},
];

const callable = async (name, idToken, data) => {
  const configured = process.env.GEODROP_FUNCTIONS_BASE_URL;
  const base = configured || `http://127.0.0.1:5001/${projectId}/us-central1`;
  const response = await fetch(`${base.replace(/\/$/, "")}/${name}`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${idToken}`,
      "Content-Type": "application/json",
      "X-Firebase-AppCheck": "emulator-rehearsal",
    },
    body: JSON.stringify({data}),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.error) {
    throw new Error(payload.error?.message || `${name} returned ${response.status}`);
  }
  return payload.result;
};

const signIn = async (email) => {
  const host = process.env.FIREBASE_AUTH_EMULATOR_HOST;
  const response = await fetch(
    `http://${host}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake`,
    {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({email, password, returnSecureToken: true}),
    }
  );
  const payload = await response.json();
  if (!response.ok || !payload.idToken) {
    throw new Error(`Auth emulator sign-in failed: ${JSON.stringify(payload)}`);
  }
  return payload.idToken;
};

const waitForDocument = async (reference, timeoutMs = 30000) => {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const snapshot = await reference.get();
    if (snapshot.exists) return snapshot;
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error(`Timed out waiting for ${reference.path}`);
};

const seed = async () => {
  const users = [
    [reporterUid, "p0-moderation-reporter@geodrop.invalid"],
    [subjectUid, "p0-moderation-subject@geodrop.invalid"],
    [moderatorOneUid, "p0-moderator-one@geodrop.invalid"],
    [moderatorTwoUid, "p0-moderator-two@geodrop.invalid"],
  ];
  for (const [uid, email] of users) {
    await auth.createUser({uid, email, password});
  }
  await Promise.all([
    auth.setCustomUserClaims(moderatorOneUid, {moderator: true}),
    auth.setCustomUserClaims(moderatorTwoUid, {moderator: true}),
  ]);
  await db.collection("users").doc(subjectUid).set({moderationStatus: "ACTIVE"});
  await Promise.all([
    db.collection("experienceDrops").doc(redesignDropId).set({
      schemaVersion: 1,
      experienceCode: "R9GATE",
      ownerId: subjectUid,
      state: "PUBLISHED",
      moderationState: "SAFE",
      contentKind: "TEXT",
      dropKind: "STANDARD",
      payloadVersion: 1,
    }),
    db.doc(`dropPayloads/${redesignDropId}/versions/1`).set({
      schemaVersion: 1,
      title: "R9 safety intake",
      body: "Synthetic redesigned moderation evidence",
      contentKind: "TEXT",
    }),
  ]);
  await Promise.all(coverageFixtures.map((fixture) => {
    const fixtureDropId = fixture.suffix === "video" ?
      dropId : `p0-moderation-${fixture.suffix}-drop`;
    const data = {
      createdBy: subjectUid,
      contentType: fixture.contentType,
      visibility: "PUBLIC",
      isDeleted: false,
      isNsfw: false,
      text: `Synthetic ${fixture.suffix} moderation evidence`,
    };
    if (fixture.mimeType) data.mediaMimeType = fixture.mimeType;
    return db.collection("drops").doc(fixtureDropId).set(data);
  }));
};

const verify = async (appealId) => {
  const failures = [];
  const [drop, subject, moderationCase, reporterStatus, appeal, audits,
    redesignDrop, redesignCase, redesignReporterStatus,
    ...coverageCases] =
    await Promise.all([
      db.collection("drops").doc(dropId).get(),
      db.collection("users").doc(subjectUid).get(),
      db.collection("moderationCases").doc(reportId).get(),
      db.doc(`users/${reporterUid}/reportStatuses/${reportId}`).get(),
      db.collection("moderationAppeals").doc(appealId).get(),
      db.collection("moderationAuditEvents").get(),
      db.collection("experienceDrops").doc(redesignDropId).get(),
      db.collection("moderationCases").doc(redesignReportId).get(),
      db.doc(`users/${reporterUid}/reportStatuses/${redesignReportId}`).get(),
      ...coverageFixtures.map((fixture) => {
        const fixtureReportId = fixture.suffix === "video" ?
          reportId : `p0-moderation-${fixture.suffix}-report`;
        return db.collection("moderationCases").doc(fixtureReportId).get();
      }),
    ]);
  if (redesignDrop.get("moderationState") !== "REMOVED" ||
      redesignDrop.get("moderationRemovalCaseId") !== redesignReportId) {
    failures.push("Redesigned content was not removed from discovery");
  }
  if (redesignCase.get("sourceCollection") !== "safetyReports" ||
      redesignCase.get("contentCollection") !== "experienceDrops" ||
      redesignCase.get("evidence.text") !== "R9 safety intake") {
    failures.push("Redesigned report did not enter the human moderation queue");
  }
  if (redesignReporterStatus.get("status") !== "ACTION_TAKEN") {
    failures.push("Redesigned reporter did not receive the action status");
  }
  if (drop.get("isDeleted") !== false || drop.get("moderationRemovalCaseId") != null) {
    failures.push("Overturned content was not restored");
  }
  const subjectAuth = await auth.getUser(subjectUid);
  if (subjectAuth.customClaims?.suspended || subject.get("moderationStatus") !== "ACTIVE") {
    failures.push("Overturned suspension was not reversed");
  }
  if (moderationCase.get("appealStatus") !== "OVERTURNED") {
    failures.push("Case appeal status is not OVERTURNED");
  }
  if (reporterStatus.get("status") !== "ACTION_TAKEN") {
    failures.push("Reporter did not receive the case action status");
  }
  if (appeal.get("status") !== "OVERTURNED" ||
      appeal.get("decidedBy") !== moderatorTwoUid) {
    failures.push("Independent appeal decision was not recorded");
  }
  const actions = new Set(audits.docs.map((document) => document.get("action")));
  ["QUEUE_VIEWED", "CASE_TRIAGED", "CASE_DECIDED", "APPEAL_DECIDED"]
    .forEach((action) => {
      if (!actions.has(action)) failures.push(`Missing audit action ${action}`);
    });
  coverageCases.forEach((moderationCoverageCase, index) => {
    const fixture = coverageFixtures[index];
    if (!moderationCoverageCase.exists ||
        moderationCoverageCase.get("evidence.contentType") !== fixture.contentType) {
      failures.push(`Missing ${fixture.contentType} moderation intake coverage`);
    }
  });
  if (failures.length > 0) throw new Error(failures.join("; "));
};

const main = async () => {
  await seed();
  const [reporterToken, subjectToken, moderatorOneToken, moderatorTwoToken] =
    await Promise.all([
      signIn("p0-moderation-reporter@geodrop.invalid"),
      signIn("p0-moderation-subject@geodrop.invalid"),
      signIn("p0-moderator-one@geodrop.invalid"),
      signIn("p0-moderator-two@geodrop.invalid"),
    ]);
  await Promise.all(coverageFixtures.map((fixture) => {
    const fixtureReportId = fixture.suffix === "video" ?
      reportId : `p0-moderation-${fixture.suffix}-report`;
    const fixtureDropId = fixture.suffix === "video" ?
      dropId : `p0-moderation-${fixture.suffix}-drop`;
    return db.collection("reports").doc(fixtureReportId).set({
      dropId: fixtureDropId,
      reportedBy: reporterUid,
      reasonCodes: fixture.suffix === "video" ? ["violence"] : ["other"],
      status: "pending",
      context: {
        contentType: fixture.contentType,
        hasMedia: Boolean(fixture.mimeType),
        source: "detail",
      },
    });
  }));
  await db.collection("safetyReports").doc(redesignReportId).set({
    schemaVersion: 1,
    dropId: redesignDropId,
    experienceCode: "R9GATE",
    hostId: subjectUid,
    reporterId: reporterUid,
    reason: "HARASSMENT",
    narrative: "Synthetic redesigned report",
    status: "PENDING",
    submittedAt: admin.firestore.Timestamp.now(),
  });
  await Promise.all(coverageFixtures.map((fixture) => {
    const fixtureReportId = fixture.suffix === "video" ?
      reportId : `p0-moderation-${fixture.suffix}-report`;
    return waitForDocument(db.collection("moderationCases").doc(fixtureReportId));
  }));
  await waitForDocument(db.collection("moderationCases").doc(redesignReportId));
  const queue = await callable("listModerationQueue", moderatorOneToken, {limit: 20});
  if (!queue.cases?.some((item) => item.id === reportId)) {
    throw new Error("Severity-ranked queue omitted the report");
  }
  await callable("triageModerationCase", moderatorOneToken, {
    reportId,
    severity: "CRITICAL",
  });
  await callable("decideModerationCase", moderatorOneToken, {
    reportId,
    decision: "REMOVE_CONTENT",
    rationale: "Synthetic P0 rehearsal removal",
    suspendSubject: true,
  });
  await callable("decideModerationCase", moderatorOneToken, {
    reportId: redesignReportId,
    decision: "REMOVE_CONTENT",
    rationale: "Synthetic R9 redesigned-content removal",
    suspendSubject: false,
  });
  const appeal = await callable("submitModerationAppeal", subjectToken, {
    reportId,
    message: "Synthetic appeal for independent review",
  });
  await callable("decideModerationAppeal", moderatorTwoToken, {
    appealId: appeal.appealId,
    outcome: "OVERTURNED",
    rationale: "Synthetic P0 rehearsal overturn",
  });
  await verify(appeal.appealId);
  console.log(JSON.stringify({
    passed: true,
    reportId,
    appealId: appeal.appealId,
    reporterAuthenticated: Boolean(reporterToken),
  }, null, 2));
};

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
