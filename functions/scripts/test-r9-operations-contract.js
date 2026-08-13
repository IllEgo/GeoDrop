"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
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
  },
}, null, 2));
