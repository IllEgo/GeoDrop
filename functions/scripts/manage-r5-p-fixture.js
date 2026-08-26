"use strict";

/**
 * Creates, verifies, or retires the one production-safe R5-P rehearsal fixture.
 * Dry run is the default. Coordinates are required only at apply time and are
 * deliberately never printed.
 */

const assert = require("assert");
const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const PROJECT_ID = "kithe-production";
const DEFAULT_CODE = "R5PTEST2";
const DROP_ID = "r5p-release-text-1";
const args = process.argv.slice(2);
const flags = new Set(args);
const valueFor = (name) => {
  const prefix = `${name}=`;
  const match = args.find((value) => value.startsWith(prefix));
  return match ? match.slice(prefix.length).trim() : "";
};

const apply = flags.has("--apply");
const verify = flags.has("--verify");
const retire = flags.has("--retire");
const useCliAuth = flags.has("--use-cli-auth");
const useExistingTestOwner = flags.has("--use-existing-test-owner");
const useEstablishedTestPoint = flags.has("--use-established-test-point");
const confirmProject = valueFor("--confirm-project");
let ownerUid = valueFor("--owner");
const code = (valueFor("--code") || DEFAULT_CODE).toUpperCase();
let latitudeRaw = valueFor("--lat");
let longitudeRaw = valueFor("--lng");

const establishedTestPoint = () => {
  const source = fs.readFileSync(
    path.join(__dirname, "seed-experience-activity.js"),
    "utf8"
  );
  const latitudeMatch = source.match(/const BASE_LAT = (-?\d+(?:\.\d+)?);/);
  const longitudeMatch = source.match(/const BASE_LNG = (-?\d+(?:\.\d+)?);/);
  if (!latitudeMatch || !longitudeMatch) {
    throw new Error("The established repository test point could not be loaded.");
  }
  return {latitude: latitudeMatch[1], longitude: longitudeMatch[1]};
};

if (useEstablishedTestPoint) {
  const point = establishedTestPoint();
  latitudeRaw = point.latitude;
  longitudeRaw = point.longitude;
}
const latitude = Number(latitudeRaw);
const longitude = Number(longitudeRaw);

const failUsage = (message) => {
  if (message) console.error(message);
  console.error(
    "Dry run: node scripts/manage-r5-p-fixture.js --owner=<uid> --lat=<value> --lng=<value> " +
      "--confirm-project=kithe-production\n" +
    "Apply: add --apply\n" +
    "Verify: node scripts/manage-r5-p-fixture.js --verify --confirm-project=kithe-production\n" +
    "CLI auth: add --use-cli-auth --use-existing-test-owner " +
      "--use-established-test-point\n" +
    "Retire: node scripts/manage-r5-p-fixture.js --retire --apply " +
      "--confirm-project=kithe-production"
  );
  process.exit(2);
};

if (confirmProject !== PROJECT_ID) {
  failUsage(`Refusing project '${confirmProject || "(missing)"}'.`);
}
if (!/^[A-Z2-9]{8}$/.test(code) || /[01IO]/.test(code)) {
  failUsage("Fixture code must be eight ambiguity-free uppercase characters.");
}
if (!verify && !retire && !ownerUid && !useExistingTestOwner) {
  failUsage("--owner or --use-existing-test-owner is required.");
}
if (!verify && !retire && (!latitudeRaw || !longitudeRaw ||
    !Number.isFinite(latitude) || !Number.isFinite(longitude) ||
    latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
  failUsage("Valid --lat and --lng values are required.");
}
if (retire && !apply) failUsage("Retirement requires --retire --apply.");
if (verify && (apply || retire)) failUsage("--verify cannot be combined with a mutation.");
if (useExistingTestOwner && !useCliAuth) {
  failUsage("--use-existing-test-owner requires --use-cli-auth.");
}

let db;
let auth;
let Timestamp;
if (!useCliAuth) {
  admin.initializeApp({projectId: PROJECT_ID});
  db = admin.firestore();
  auth = admin.auth();
  Timestamp = admin.firestore.Timestamp;
}

const refs = () => {
  const group = db.collection("groups").doc(code);
  const drop = db.collection("experienceDrops").doc(DROP_ID);
  const payload = db.collection("dropPayloads").doc(DROP_ID);
  return {
    group,
    drop,
    payload,
    payloadVersion: payload.collection("versions").doc("1"),
    ownerMembership: ownerUid ? db.collection("users").doc(ownerUid)
      .collection("groups").doc(code) : null,
    summary: group.collection("analytics").doc("summary"),
  };
};

const redactedPlan = (mode) => ({
  projectId: PROJECT_ID,
  mode,
  code,
  dropId: DROP_ID,
  ownerConfigured: Boolean(ownerUid || useExistingTestOwner),
  locationConfigured: Number.isFinite(latitude) && Number.isFinite(longitude),
  locationLogged: false,
  contentKind: "TEXT",
  dropKind: "STANDARD",
  radiusM: 25,
  documentCount: 6,
});

const verifyFixture = async () => {
  const target = refs();
  const [group, drop, payload, payloadVersion, summary] = await Promise.all([
    target.group.get(),
    target.drop.get(),
    target.payload.get(),
    target.payloadVersion.get(),
    target.summary.get(),
  ]);
  assert(group.exists, "Fixture Experience is missing");
  assert(drop.exists, "Fixture discovery is missing");
  assert(payload.exists, "Fixture payload parent is missing");
  assert(payloadVersion.exists, "Fixture payload version is missing");
  assert(summary.exists, "Fixture analytics summary is missing");
  assert.strictEqual(group.get("schemaVersion"), 2);
  assert.strictEqual(group.get("code"), code);
  assert.strictEqual(group.get("state"), "PUBLISHED");
  assert.strictEqual(drop.get("moderationState"), "SAFE");
  assert.strictEqual(drop.get("state"), "PUBLISHED");
  assert.strictEqual(drop.get("contentKind"), "TEXT");
  assert.strictEqual(drop.get("dropKind"), "STANDARD");
  assert.strictEqual(drop.get("payloadVersion"), 1);
  assert.strictEqual(payload.get("currentVersion"), 1);
  assert.strictEqual(payloadVersion.get("contentKind"), "TEXT");
  assert(Number.isFinite(drop.get("lat")) && Number.isFinite(drop.get("lng")));
  console.log(JSON.stringify({
    ...redactedPlan("verify"),
    verified: true,
    ownerUidLogged: false,
  }, null, 2));
};

const createFixture = async () => {
  const owner = await auth.getUser(ownerUid);
  if (owner.disabled || owner.providerData.length === 0) {
    throw new Error("The fixture owner must be an enabled non-anonymous test account.");
  }
  const target = refs();
  const now = Timestamp.now();
  const startsAt = Timestamp.fromMillis(now.toMillis() - 60 * 60 * 1000);
  const endsAt = Timestamp.fromMillis(now.toMillis() + 72 * 60 * 60 * 1000);
  await db.runTransaction(async (transaction) => {
    const existing = await Promise.all([
      transaction.get(target.group),
      transaction.get(target.drop),
      transaction.get(target.payload),
      transaction.get(target.payloadVersion),
      transaction.get(target.ownerMembership),
      transaction.get(target.summary),
    ]);
    if (existing.some((document) => document.exists)) {
      throw new Error("Fixture targets already exist; refusing to overwrite production data.");
    }
    transaction.create(target.group, {
      schemaVersion: 2,
      code,
      ownerId: ownerUid,
      name: "Kithe release rehearsal",
      description: "Private release verification. No real event or participant data.",
      hostLabel: "Kithe Test Host",
      startsAt,
      endsAt,
      timeZone: "Pacific/Honolulu",
      defaultRadiusM: 25,
      state: "PUBLISHED",
      createdAt: now,
      publishedAt: now,
      updatedAt: now,
    });
    transaction.create(target.ownerMembership, {
      schemaVersion: 2,
      code,
      ownerId: ownerUid,
      role: "OWNER",
      joinedAt: now,
      updatedAt: now,
    });
    transaction.create(target.summary, {
      schemaVersion: 2,
      joinedParticipants: 0,
      publishedDrops: 1,
      uniqueUnlockers: 0,
      unlocks: 0,
      mainTrailCompletions: 0,
      codesIssued: 0,
      codesUsed: 0,
      createdAt: now,
      updatedAt: now,
    });
    transaction.create(target.payloadVersion, {
      schemaVersion: 1,
      title: "Release verification",
      body: "Test payload only; no offer or real-world instruction.",
      contentKind: "TEXT",
      mediaAssetId: null,
      mediaMimeType: null,
      mediaAltText: null,
      rewardPresentation: null,
      createdAt: now,
    });
    transaction.create(target.payload, {
      schemaVersion: 1,
      dropId: DROP_ID,
      experienceCode: code,
      ownerId: ownerUid,
      currentVersion: 1,
      createdAt: now,
      updatedAt: now,
    });
    transaction.create(target.drop, {
      schemaVersion: 1,
      experienceCode: code,
      ownerId: ownerUid,
      hostLabel: "Kithe Test Host",
      state: "PUBLISHED",
      moderationState: "SAFE",
      lat: latitude,
      lng: longitude,
      radiusM: 25,
      contentKind: "TEXT",
      dropKind: "STANDARD",
      payloadVersion: 1,
      trailId: null,
      trailStepIndex: null,
      trailTotalSteps: null,
      likeCount: 0,
      createdAt: now,
      publishedAt: now,
      updatedAt: now,
      editedAt: null,
      expiryMode: "NONE",
      expiresAt: null,
    });
  });
  console.log(JSON.stringify({...redactedPlan("apply"), applied: true}, null, 2));
};

const retireFixture = async () => {
  const target = refs();
  const now = Timestamp.now();
  await db.runTransaction(async (transaction) => {
    const [group, drop] = await Promise.all([
      transaction.get(target.group),
      transaction.get(target.drop),
    ]);
    if (!group.exists || !drop.exists) throw new Error("Fixture is missing.");
    transaction.update(target.group, {state: "CANCELLED", updatedAt: now, retiredAt: now});
    transaction.update(target.drop, {state: "REMOVED", updatedAt: now, retiredAt: now});
  });
  console.log(JSON.stringify({...redactedPlan("retire"), retired: true}, null, 2));
};

const firestoreDocumentName = (documentPath) =>
  `projects/${PROJECT_ID}/databases/(default)/documents/${documentPath}`;
const firestoreDocumentUrl = (documentPath) =>
  "https://firestore.googleapis.com/v1/" +
  firestoreDocumentName(documentPath).split("/").map(encodeURIComponent).join("/");
const stringValue = (value) => ({stringValue: value});
const integerValue = (value) => ({integerValue: String(value)});
const doubleValue = (value) => ({doubleValue: value});
const timestampValue = (value) => ({timestampValue: value.toISOString()});
const nullValue = () => ({nullValue: null});

const cliAccessToken = async () => {
  const appData = process.env.APPDATA;
  if (!appData) throw new Error("APPDATA is required for Firebase CLI auth.");
  const authModule = require(path.join(
    appData,
    "npm",
    "node_modules",
    "firebase-tools",
    "lib",
    "auth"
  ));
  const account = authModule.getGlobalDefaultAccount();
  if (!account || !account.tokens || !account.tokens.refresh_token) {
    throw new Error("No signed-in Firebase CLI account is available.");
  }
  const tokens = await authModule.getAccessToken(
    account.tokens.refresh_token,
    []
  );
  if (!tokens || !tokens.access_token) {
    throw new Error("Firebase CLI access token acquisition failed.");
  }
  return tokens.access_token;
};

const authorizedJson = async (url, token, options = {}) => {
  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
  });
  if (!response.ok) {
    throw new Error(
      `Authorized request failed (${response.status} ${response.statusText}).`
    );
  }
  return response.json();
};

const selectExistingTestOwner = async (token) => {
  const result = await authorizedJson(
    "https://identitytoolkit.googleapis.com/v1/projects/" +
      `${PROJECT_ID}/accounts:batchGet?maxResults=1000`,
    token
  );
  const candidates = (result.users || []).filter((user) =>
    !user.disabled &&
    Array.isArray(user.providerUserInfo) &&
    user.providerUserInfo.length > 0
  );
  if (ownerUid) {
    const owner = candidates.find((candidate) => candidate.localId === ownerUid);
    if (!owner) {
      throw new Error("The requested fixture owner is not an enabled test user.");
    }
    return owner.localId;
  }
  if (candidates.length !== 1) {
    throw new Error(
      "Expected exactly one enabled non-anonymous test user; found " +
      `${candidates.length}.`
    );
  }
  return candidates[0].localId;
};

const cliDocument = (documentPath, fields) => ({
  name: firestoreDocumentName(documentPath),
  fields,
});

const cliCreateFixture = async (token) => {
  ownerUid = await selectExistingTestOwner(token);
  const now = new Date();
  const startsAt = new Date(now.getTime() - 60 * 60 * 1000);
  const endsAt = new Date(now.getTime() + 72 * 60 * 60 * 1000);
  const documents = [
    cliDocument(`groups/${code}`, {
      schemaVersion: integerValue(2),
      code: stringValue(code),
      ownerId: stringValue(ownerUid),
      name: stringValue("Kithe release rehearsal"),
      description: stringValue(
        "Private release verification. No real event or participant data."
      ),
      hostLabel: stringValue("Kithe Test Host"),
      startsAt: timestampValue(startsAt),
      endsAt: timestampValue(endsAt),
      timeZone: stringValue("Pacific/Honolulu"),
      defaultRadiusM: integerValue(25),
      state: stringValue("PUBLISHED"),
      createdAt: timestampValue(now),
      publishedAt: timestampValue(now),
      updatedAt: timestampValue(now),
    }),
    cliDocument(`users/${ownerUid}/groups/${code}`, {
      schemaVersion: integerValue(2),
      code: stringValue(code),
      ownerId: stringValue(ownerUid),
      role: stringValue("OWNER"),
      joinedAt: timestampValue(now),
      updatedAt: timestampValue(now),
    }),
    cliDocument(`groups/${code}/analytics/summary`, {
      schemaVersion: integerValue(2),
      joinedParticipants: integerValue(0),
      publishedDrops: integerValue(1),
      uniqueUnlockers: integerValue(0),
      unlocks: integerValue(0),
      mainTrailCompletions: integerValue(0),
      codesIssued: integerValue(0),
      codesUsed: integerValue(0),
      createdAt: timestampValue(now),
      updatedAt: timestampValue(now),
    }),
    cliDocument(`dropPayloads/${DROP_ID}`, {
      schemaVersion: integerValue(1),
      dropId: stringValue(DROP_ID),
      experienceCode: stringValue(code),
      ownerId: stringValue(ownerUid),
      currentVersion: integerValue(1),
      createdAt: timestampValue(now),
      updatedAt: timestampValue(now),
    }),
    cliDocument(`dropPayloads/${DROP_ID}/versions/1`, {
      schemaVersion: integerValue(1),
      title: stringValue("Release verification"),
      body: stringValue(
        "Test payload only; no offer or real-world instruction."
      ),
      contentKind: stringValue("TEXT"),
      mediaAssetId: nullValue(),
      mediaMimeType: nullValue(),
      mediaAltText: nullValue(),
      rewardPresentation: nullValue(),
      createdAt: timestampValue(now),
    }),
    cliDocument(`experienceDrops/${DROP_ID}`, {
      schemaVersion: integerValue(1),
      experienceCode: stringValue(code),
      ownerId: stringValue(ownerUid),
      hostLabel: stringValue("Kithe Test Host"),
      state: stringValue("PUBLISHED"),
      moderationState: stringValue("SAFE"),
      lat: doubleValue(latitude),
      lng: doubleValue(longitude),
      radiusM: integerValue(25),
      contentKind: stringValue("TEXT"),
      dropKind: stringValue("STANDARD"),
      payloadVersion: integerValue(1),
      trailId: nullValue(),
      trailStepIndex: nullValue(),
      trailTotalSteps: nullValue(),
      likeCount: integerValue(0),
      createdAt: timestampValue(now),
      publishedAt: timestampValue(now),
      updatedAt: timestampValue(now),
      editedAt: nullValue(),
      expiryMode: stringValue("NONE"),
      expiresAt: nullValue(),
    }),
  ];
  await authorizedJson(
    "https://firestore.googleapis.com/v1/projects/" +
      `${PROJECT_ID}/databases/(default)/documents:commit`,
    token,
    {
      method: "POST",
      body: JSON.stringify({
        writes: documents.map((document) => ({
          update: document,
          currentDocument: {exists: false},
        })),
      }),
    }
  );
  console.log(JSON.stringify({...redactedPlan("apply"), applied: true}, null, 2));
};

const cliVerifyFixture = async (token) => {
  const group = await authorizedJson(
    firestoreDocumentUrl(`groups/${code}`),
    token
  );
  ownerUid = group.fields.ownerId && group.fields.ownerId.stringValue;
  assert(ownerUid, "Fixture owner is missing");
  const paths = [
    `users/${ownerUid}/groups/${code}`,
    `groups/${code}/analytics/summary`,
    `dropPayloads/${DROP_ID}`,
    `dropPayloads/${DROP_ID}/versions/1`,
    `experienceDrops/${DROP_ID}`,
  ];
  const [membership, summary, payload, payloadVersion, drop] =
    await Promise.all(paths.map((documentPath) =>
      authorizedJson(firestoreDocumentUrl(documentPath), token)
    ));
  assert.strictEqual(group.fields.schemaVersion.integerValue, "2");
  assert.strictEqual(group.fields.state.stringValue, "PUBLISHED");
  assert.strictEqual(membership.fields.role.stringValue, "OWNER");
  assert.strictEqual(summary.fields.publishedDrops.integerValue, "1");
  assert.strictEqual(payload.fields.currentVersion.integerValue, "1");
  assert.strictEqual(payloadVersion.fields.contentKind.stringValue, "TEXT");
  assert.strictEqual(drop.fields.moderationState.stringValue, "SAFE");
  assert.strictEqual(drop.fields.state.stringValue, "PUBLISHED");
  assert.strictEqual(drop.fields.contentKind.stringValue, "TEXT");
  assert.strictEqual(drop.fields.dropKind.stringValue, "STANDARD");
  assert.strictEqual(drop.fields.payloadVersion.integerValue, "1");
  assert(Number.isFinite(drop.fields.lat.doubleValue));
  assert(Number.isFinite(drop.fields.lng.doubleValue));
  console.log(JSON.stringify({
    ...redactedPlan("verify"),
    verified: true,
    ownerUidLogged: false,
  }, null, 2));
};

const runWithCliAuth = async () => {
  const token = await cliAccessToken();
  if (verify) return cliVerifyFixture(token);
  if (retire) {
    throw new Error("CLI-auth retirement is not supported; use Admin ADC.");
  }
  console.log(JSON.stringify(redactedPlan(apply ? "apply" : "dry-run"), null, 2));
  if (apply) await cliCreateFixture(token);
};

(async () => {
  if (useCliAuth) return runWithCliAuth();
  if (verify) return verifyFixture();
  if (retire) return retireFixture();
  console.log(JSON.stringify(redactedPlan(apply ? "apply" : "dry-run"), null, 2));
  if (apply) await createFixture();
})().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
}).finally(async () => {
  await Promise.allSettled(admin.apps.map((app) => app.delete()));
});
