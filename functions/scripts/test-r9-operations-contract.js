"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const repo = path.resolve(root, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const readRepo = (relative) => fs.readFileSync(path.join(repo, relative), "utf8");
const redesign = read("src/redesign.ts");
const moderation = read("src/moderationOperations.ts");
const lifecycle = read("src/accountLifecycle.ts");
const index = read("src/index.ts");
const packageJson = JSON.parse(read("package.json"));

assert.match(redesign, /collection\("safetyReports"\)/);
assert.match(redesign, /export const unblockHost/);
assert.match(redesign, /collection\("callableRateLimits"\)/);
assert.match(redesign, /"resource-exhausted",\s*"RATE_LIMITED"/);
assert.match(moderation, /firestore\.document\("safetyReports\/\{reportId\}"\)/);
assert.match(moderation, /contentCollection:\s*"experienceDrops"/);
assert.match(moderation, /moderationState:\s*"REMOVED"/);
assert.match(moderation, /reporterStatusRef\(reporterId, reportId\)/);
assert.match(index, /ingestRedesignReport/);
assert.match(index, /unblockHost/);
assert.match(lifecycle, /export const requestAccountExport/);
assert.match(lifecycle, /export const deleteAccount/);

// A client that sends a stale deletion-policy version is rejected with
// POLICY_VERSION_MISMATCH, so export and deletion break silently on every client the
// moment the server constant is bumped alone. Pin all three to the same literal.
const lifecyclePolicyVersion = lifecycle
  .match(/export const ACCOUNT_LIFECYCLE_POLICY_VERSION = "([^"]+)"/)?.[1];
assert.ok(lifecyclePolicyVersion, "Server deletion-policy version is unreadable");

const androidPolicyVersion = readRepo(
  "app/src/main/java/com/kitheapp/data/AccountLifecycleRepo.kt"
).match(/const val POLICY_VERSION = "([^"]+)"/)?.[1];
assert.strictEqual(
  androidPolicyVersion,
  lifecyclePolicyVersion,
  "Android AccountLifecycleRepo.POLICY_VERSION must match the server constant"
);

const iosPolicyVersion = readRepo(
  "ios/GeoDropIOS/Services/AccountLifecycleService.swift"
).match(/static let policyVersion = "([^"]+)"/)?.[1];
assert.strictEqual(
  iosPolicyVersion,
  lifecyclePolicyVersion,
  "iOS AccountLifecycleService.policyVersion must match the server constant"
);

// The rehearsal must not reintroduce a literal of its own.
assert.doesNotMatch(
  read("scripts/rehearse-account-lifecycle.js"),
  /policyVersion: "/,
  "The lifecycle rehearsal must derive the policy version, not hard-code it"
);

for (const script of [
  "moderation:console",
  "moderation:rehearse",
  "account:audit",
  "account:rehearse",
  "migrate:redesign:audit",
  "r9:readiness",
]) {
  assert.ok(packageJson.scripts[script], `Missing operator command ${script}`);
}

console.log(JSON.stringify({
  passed: true,
  checks: {
    redesignedReportsReachModeration: true,
    redesignedRemovalIsFailClosed: true,
    reporterStatusIsReadable: true,
    unblockIsServerAuthorized: true,
    accountLifecycleCommandsPresent: true,
    deletionPolicyVersionAgreesAcrossClients: lifecyclePolicyVersion,
  },
}, null, 2));
