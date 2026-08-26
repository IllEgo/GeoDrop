"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..", "..");
const deployment = path.join(root, "deployment", "r5-p");
const productionConfig = JSON.parse(fs.readFileSync(path.join(root, "firebase.json"), "utf8"));
const a3Config = JSON.parse(fs.readFileSync(path.join(root, "firebase.r5-p.json"), "utf8"));
const indexes = JSON.parse(
  fs.readFileSync(path.join(deployment, "firestore.indexes.json"), "utf8")
);
const rules = fs.readFileSync(path.join(deployment, "firestore.rules"), "utf8");
const rollbackRules = fs.readFileSync(
  path.join(deployment, "rollback-firestore.rules"),
  "utf8"
);
const sourceIndex = fs.readFileSync(path.join(root, "functions", "src", "index.ts"), "utf8");
const allowlist = fs.readFileSync(path.join(deployment, "functions.allowlist.txt"), "utf8")
  .split(/\r?\n/)
  .map((value) => value.trim())
  .filter(Boolean);

const expectedFunctions = [
  "experienceEntryPage",
  "resolveExperience",
  "joinExperience",
  "recordClientEvent",
  "recordAuthCompletion",
  "unlockDrop",
  "mergeGuestAccount",
];

assert.deepStrictEqual(allowlist, expectedFunctions, "A3 Functions allowlist changed");
for (const functionName of allowlist) {
  assert.match(sourceIndex, new RegExp(`\\b${functionName}\\b`), `${functionName} is not exported`);
}
assert.deepStrictEqual(a3Config.functions, productionConfig.functions, "Functions config drifted");
assert.deepStrictEqual(a3Config.hosting, productionConfig.hosting, "Hosting config drifted");
assert.deepStrictEqual(a3Config.firestore, {
  rules: "deployment/r5-p/firestore.rules",
  indexes: "deployment/r5-p/firestore.indexes.json",
});
assert.strictEqual(indexes.indexes.length, 1, "A3 must deploy exactly one composite index");
assert.deepStrictEqual(indexes.fieldOverrides, [], "A3 must not deploy field overrides");
assert.deepStrictEqual(indexes.indexes[0], {
  collectionGroup: "experienceDrops",
  queryScope: "COLLECTION",
  fields: [
    {fieldPath: "experienceCode", order: "ASCENDING"},
    {fieldPath: "state", order: "ASCENDING"},
    {fieldPath: "moderationState", order: "ASCENDING"},
    {fieldPath: "publishedAt", order: "DESCENDING"},
  ],
});
assert.match(rules, /match \/experienceDrops\/\{dropId\}/);
assert.match(rules, /match \/\{document=\*\*\}[\s\S]*allow read, write: if false;/);
assert.doesNotMatch(rules, /match \/drops\/\{dropId\}/, "Legacy drops must stay closed in A3");
assert.doesNotMatch(rules, /allow create: if isSignedIn\(\).*drop/s);
assert.strictEqual(rollbackRules, `rules_version = '2';\n\n` +
  "service cloud.firestore {\n" +
  "  match /databases/{database}/documents {\n" +
  "    match /{document=**} {\n" +
  "      allow read, write: if false;\n" +
  "    }\n" +
  "  }\n" +
  "}\n");

console.log(JSON.stringify({
  passed: true,
  config: "firebase.r5-p.json",
  functionCount: allowlist.length,
  functions: allowlist,
  compositeIndexCount: indexes.indexes.length,
  fieldOverrideCount: indexes.fieldOverrides.length,
  legacyDropsExposed: false,
}, null, 2));
