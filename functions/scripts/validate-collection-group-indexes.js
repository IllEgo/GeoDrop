"use strict";

/**
 * Static check: every filtered collection-group query has a declared index.
 *
 * This bug class has reached production twice. Firestore's default single-field
 * indexing covers COLLECTION scope only, so a `collectionGroup(...).where(...)`
 * query needs an explicit COLLECTION_GROUP entry in `firestore.indexes.json` —
 * and **the emulator creates indexes on demand**, so no emulator test can catch
 * the omission. The query the emulator answers is not the query production
 * refuses.
 *
 * What it cost each time:
 *
 *  - task 3.4/4.5: `notifyGroupMembersOnDropCreated` filtered on `groups.code`
 *    and failed FAILED_PRECONDITION on every invocation for weeks. Nobody
 *    noticed because notifications were flag-off.
 *  - task 5.4: `deleteAccount` filtered on `inventory.id`, so deleting an
 *    account that had ever created a drop failed — after report anonymisation
 *    had already run. A partial deletion, on the deletion path.
 *
 * Deliberately conservative: it reports only when a literal `.where("field"`
 * follows a `collectionGroup("name")` call, so an unfiltered
 * `collectionGroup(x).get()` — which needs no index — is not flagged. A dynamic
 * field path it cannot read is reported as unverifiable rather than passed.
 */

const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..");
const indexFile = path.join(repoRoot, "firestore.indexes.json");
const sourceDirs = [
  path.join(repoRoot, "functions", "src"),
  path.join(repoRoot, "functions", "scripts"),
];

const listFiles = (dir) => {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, {withFileTypes: true}).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) return listFiles(full);
    return /\.(ts|js)$/.test(entry.name) ? [full] : [];
  });
};

const declaredCollectionGroupFields = () => {
  const config = JSON.parse(fs.readFileSync(indexFile, "utf8"));
  const declared = new Set();

  (config.fieldOverrides || []).forEach((override) => {
    const scoped = (override.indexes || [])
      .some((index) => index.queryScope === "COLLECTION_GROUP");
    if (scoped) declared.add(`${override.collectionGroup}.${override.fieldPath}`);
  });

  // Composite indexes carry their own query scope and can also satisfy a
  // filtered collection-group query.
  (config.indexes || []).forEach((index) => {
    if (index.queryScope !== "COLLECTION_GROUP") return;
    (index.fields || []).forEach((field) => {
      if (field.fieldPath) declared.add(`${index.collectionGroup}.${field.fieldPath}`);
    });
  });

  return declared;
};

const findings = [];
const declared = declaredCollectionGroupFields();

sourceDirs.flatMap(listFiles).forEach((file) => {
  const source = fs.readFileSync(file, "utf8");
  const pattern = /collectionGroup\(\s*["'`]([A-Za-z0-9_]+)["'`]\s*\)/g;
  let match;

  while ((match = pattern.exec(source)) !== null) {
    const collection = match[1];
    // Look only as far as the terminating .get()/.stream(): anything past it
    // belongs to a different statement.
    const tail = source.slice(match.index, match.index + 400);
    const terminator = tail.search(/\.(get|stream|onSnapshot|count)\s*\(/);
    const statement = terminator === -1 ? tail : tail.slice(0, terminator);

    const whereLiteral = /\.where\(\s*["'`]([A-Za-z0-9_.]+)["'`]/.exec(statement);
    const whereDynamic = /\.where\(\s*(?!["'`])/.test(statement);
    const line = source.slice(0, match.index).split("\n").length;
    const where = path.relative(repoRoot, file).replace(/\\/g, "/") + ":" + line;

    if (whereLiteral) {
      const key = `${collection}.${whereLiteral[1]}`;
      if (!declared.has(key)) {
        findings.push(
          `${where} filters collectionGroup("${collection}") on "${whereLiteral[1]}" ` +
          `but firestore.indexes.json declares no COLLECTION_GROUP index for ${key}`
        );
      }
    } else if (whereDynamic) {
      findings.push(
        `${where} filters collectionGroup("${collection}") on a field this check ` +
        "cannot read statically; declare the index and add the field here"
      );
    }
  }
});

if (findings.length > 0) {
  findings.forEach((finding) => console.error(`FAIL ${finding}`));
  console.error(
    "\nA fieldOverrides entry REPLACES the field's default indexes rather than " +
    "adding to them, so list the three COLLECTION-scope entries alongside the " +
    "COLLECTION_GROUP one — see groups.code in firestore.indexes.json."
  );
  process.exit(1);
}

console.log(
  `Collection-group index check passed: every filtered collection-group query ` +
  `has a declared index (${declared.size} declared).`
);
