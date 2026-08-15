"use strict";

/**
 * Task 2.7 — Account-type migration for existing accounts.
 *
 * The launch scope has exactly two account types: EXPLORER and BUSINESS. This
 * script brings the surviving profile documents onto that model and reports the
 * profile/Auth mismatches that task 0.2 flagged for the account-model work.
 *
 * It performs four things, in this order:
 *
 *   1. NORMALIZE  — a `role` that is a recognized value written in the wrong
 *                   case (`business`) becomes the canonical uppercase form.
 *                   firestore.rules compares the string exactly, so a
 *                   mis-cased value is a silent explorer on the server while
 *                   older clients read it as a business.
 *   2. BACKFILL   — a profile with NO `role` gets one. It becomes BUSINESS if it
 *                   carries business metadata (businessName or a non-empty
 *                   businessCategories), otherwise EXPLORER. This is the
 *                   one-time server-side replacement for the client-side
 *                   inference that task 2.7 deleted from FirestoreRepo /
 *                   FirestoreService: the clients now trust the stored role, so
 *                   the inference has to happen once, here, where it is
 *                   auditable.
 *   3. FLAG       — a `role` that is neither EXPLORER nor BUSINESS in any
 *                   casing (a removed or invented type) is reported and NOT
 *                   rewritten. A stray value blocks profile updates under the
 *                   new rules, so it needs a human decision, not a guess.
 *   4. RECONCILE  — profiles with no Auth user and Auth users with no profile
 *                   are counted and listed (0.2 saw 25 profiles vs 22 Auth
 *                   users). Reported only; this script never touches Auth and
 *                   never deletes a document.
 *
 * Usage:
 *
 *   # dry run (read-only, prints the exact writes it would make)
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *     GCLOUD_PROJECT=kithe-production node scripts/normalize-account-roles.js
 *
 *   # live run
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *     GCLOUD_PROJECT=kithe-production node scripts/normalize-account-roles.js \
 *       --apply --confirm-project=kithe-production
 *
 * Safety properties:
 *   - Dry run by default; --apply requires --confirm-project=<id> matching the
 *     resolved project, so a wrong-project run fails closed.
 *   - Writes only the `role` field, only on documents listed in the dry run,
 *     and only with one of the two launch values. Never writes business
 *     metadata, never deletes, never touches Auth.
 *   - Idempotent: a second run finds nothing to change.
 */

const admin = require("firebase-admin");

// --- arguments -----------------------------------------------------------

const argv = process.argv.slice(2);
const flags = new Set(argv.filter((a) => a.startsWith("--") && !a.includes("=")));
const valueOf = (name, fallback = null) => {
  const prefix = `--${name}=`;
  const hit = argv.find((a) => a.startsWith(prefix));
  return hit ? hit.slice(prefix.length) : fallback;
};

const APPLY = flags.has("--apply");
const JSON_ONLY = flags.has("--json");
const CONFIRM_PROJECT = valueOf("confirm-project");

// --- account model -------------------------------------------------------

const EXPLORER = "EXPLORER";
const BUSINESS = "BUSINESS";
const LAUNCH_ROLES = [EXPLORER, BUSINESS];

admin.initializeApp();
const db = admin.firestore();
const auth = admin.auth();
const projectId =
  process.env.GCLOUD_PROJECT ||
  process.env.GOOGLE_CLOUD_PROJECT ||
  admin.app().options.projectId ||
  "(unknown)";

const hasBusinessMetadata = (data) => {
  const name = data.businessName;
  const categories = data.businessCategories;
  return (
    (typeof name === "string" && name.trim().length > 0) ||
    (Array.isArray(categories) && categories.length > 0)
  );
};

/**
 * Classifies one profile document.
 *
 * @param {string} id document id (the uid)
 * @param {object} data document data
 * @return {{id: string, action: string, from: *, to: (string|null), reason: string}}
 */
const classify = (id, data) => {
  const raw = data.role;
  const business = hasBusinessMetadata(data);

  if (raw === undefined || raw === null || (typeof raw === "string" && raw.trim() === "")) {
    const to = business ? BUSINESS : EXPLORER;
    return {
      id,
      action: "BACKFILL",
      from: raw === undefined ? "(absent)" : raw,
      to,
      reason: business ?
        "no role stored; business metadata present" :
        "no role stored; no business metadata",
    };
  }

  if (typeof raw !== "string") {
    return {
      id,
      action: "FLAG",
      from: raw,
      to: null,
      reason: `role is ${typeof raw}, not a string`,
    };
  }

  const trimmed = raw.trim();
  const canonical = LAUNCH_ROLES.find((role) => role === trimmed.toUpperCase());

  if (!canonical) {
    return {
      id,
      action: "FLAG",
      from: raw,
      to: null,
      reason: "role is not one of the two launch types",
    };
  }
  if (trimmed !== raw || canonical !== trimmed) {
    return {
      id,
      action: "NORMALIZE",
      from: raw,
      to: canonical,
      reason: "recognized role stored in a non-canonical form",
    };
  }
  return {id, action: "OK", from: raw, to: raw, reason: "already canonical"};
};

// --- main ----------------------------------------------------------------

const main = async () => {
  if (APPLY && CONFIRM_PROJECT !== projectId) {
    throw new Error(
      `--apply requires --confirm-project=${projectId} ` +
        `(got ${CONFIRM_PROJECT === null ? "nothing" : CONFIRM_PROJECT})`
    );
  }

  const snapshot = await db.collection("users").get();
  const classified = snapshot.docs.map((doc) => classify(doc.id, doc.data() || {}));

  const byAction = {OK: [], NORMALIZE: [], BACKFILL: [], FLAG: []};
  classified.forEach((entry) => byAction[entry.action].push(entry));

  const roleCounts = {};
  classified.forEach((entry) => {
    const key = entry.action === "FLAG" ? `FLAGGED:${entry.from}` : entry.to;
    roleCounts[key] = (roleCounts[key] || 0) + 1;
  });

  // Reconcile profiles against Auth (0.2 follow-up). Read-only.
  const profileIds = new Set(classified.map((entry) => entry.id));
  const authUids = new Set();
  let pageToken;
  do {
    const page = await auth.listUsers(1000, pageToken);
    page.users.forEach((user) => authUids.add(user.uid));
    pageToken = page.pageToken;
  } while (pageToken);

  const orphanProfiles = [...profileIds].filter((id) => !authUids.has(id)).sort();
  const profilelessAuthUsers = [...authUids].filter((uid) => !profileIds.has(uid)).sort();

  const willWrite = [...byAction.NORMALIZE, ...byAction.BACKFILL];

  const report = {
    mode: APPLY ? "APPLY" : "DRY RUN",
    project: projectId,
    generatedAt: new Date().toISOString(),
    profiles: classified.length,
    authUsers: authUids.size,
    roleDistributionAfter: roleCounts,
    counts: {
      alreadyCanonical: byAction.OK.length,
      normalize: byAction.NORMALIZE.length,
      backfill: byAction.BACKFILL.length,
      flagged: byAction.FLAG.length,
    },
    willWrite: willWrite.map(({id, action, from, to, reason}) => ({id, action, from, to, reason})),
    flagged: byAction.FLAG.map(({id, from, reason}) => ({id, role: from, reason})),
    reconciliation: {
      profilesWithoutAuthUser: orphanProfiles,
      authUsersWithoutProfile: profilelessAuthUsers,
      note:
        "Reported only. Orphan profiles are harmless (nobody can sign in as them) " +
        "and a missing profile is created on next sign-in; neither is deleted here.",
    },
  };

  if (APPLY && willWrite.length > 0) {
    const writer = db.bulkWriter();
    willWrite.forEach((entry) => {
      writer.set(db.collection("users").doc(entry.id), {role: entry.to}, {merge: true});
    });
    await writer.close();
    report.wrote = willWrite.length;
  } else if (APPLY) {
    report.wrote = 0;
  }

  if (JSON_ONLY) {
    console.log(JSON.stringify(report, null, 2));
    return;
  }
  printHuman(report);
};

const printHuman = (report) => {
  const line = (s = "") => console.log(s);
  line(`GeoDrop account-role normalization — ${report.mode} — ${report.project}`);
  line(`Generated ${report.generatedAt}`);
  line();
  line(`Profiles: ${report.profiles}   Auth users: ${report.authUsers}`);
  line(
    `Already canonical: ${report.counts.alreadyCanonical}   ` +
      `Normalize: ${report.counts.normalize}   ` +
      `Backfill: ${report.counts.backfill}   ` +
      `Flagged: ${report.counts.flagged}`
  );
  line();
  line("Role distribution after this run:");
  Object.entries(report.roleDistributionAfter)
    .sort(([a], [b]) => a.localeCompare(b))
    .forEach(([role, count]) => line(`  ${role}: ${count}`));
  line();
  if (report.willWrite.length === 0) {
    line("No role writes needed.");
  } else {
    line(`${report.mode === "APPLY" ? "Wrote" : "Would write"} role on ${report.willWrite.length} profile(s):`);
    report.willWrite.forEach((entry) => {
      line(`  ${entry.id}: ${entry.from} -> ${entry.to}   (${entry.reason})`);
    });
  }
  if (report.flagged.length > 0) {
    line();
    line(`NEEDS A DECISION — ${report.flagged.length} profile(s) carry an off-model role.`);
    line("These are NOT rewritten. Under the 2.7 rules an off-model role blocks the");
    line("account's own profile updates, so each one needs a deliberate choice of type.");
    report.flagged.forEach((entry) => line(`  ${entry.id}: role=${JSON.stringify(entry.role)} (${entry.reason})`));
  }
  line();
  line("Reconciliation (read-only):");
  line(`  profiles with no Auth user: ${report.reconciliation.profilesWithoutAuthUser.length}`);
  report.reconciliation.profilesWithoutAuthUser.forEach((id) => line(`    ${id}`));
  line(`  Auth users with no profile: ${report.reconciliation.authUsersWithoutProfile.length}`);
  report.reconciliation.authUsersWithoutProfile.forEach((id) => line(`    ${id}`));
  line();
  if (report.mode === "DRY RUN") {
    line("Dry run — nothing was written. Re-run with:");
    line(`  --apply --confirm-project=${report.project}`);
  }
};

main().catch((err) => {
  console.error(err.message || err);
  process.exitCode = 1;
});
