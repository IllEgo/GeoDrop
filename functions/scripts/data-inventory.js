"use strict";

/**
 * Task 0.2 — Data inventory (READ-ONLY).
 *
 * Counts existing records that the launch scope disallows, broken out by
 * collection, with date ranges and a per-creator breakdown (enriched with Auth
 * emails) so real-user vs. own-testing data can be told apart.
 *
 * This script performs NO writes. It has no --apply flag and never mutates
 * Firestore, Storage, or Auth. Run it against the production project:
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *     node scripts/data-inventory.js
 *
 * Add --json for machine-readable output only.
 */

const admin = require("firebase-admin");

const JSON_ONLY = new Set(process.argv.slice(2)).has("--json");

admin.initializeApp();
const db = admin.firestore();
const auth = admin.auth();

// --- helpers -------------------------------------------------------------

const toMillis = (v) => {
  if (v == null) return null;
  if (typeof v === "number") return v > 1e12 ? v : v * 1000; // secs vs millis
  if (typeof v === "string") {
    const t = Date.parse(v);
    return Number.isNaN(t) ? null : t;
  }
  if (typeof v.toMillis === "function") return v.toMillis();
  if (typeof v._seconds === "number") return v._seconds * 1000;
  if (typeof v.seconds === "number") return v.seconds * 1000;
  return null;
};

const iso = (millis) => (millis == null ? null : new Date(millis).toISOString());

const nonEmptyMap = (m) => m && typeof m === "object" && Object.keys(m).length > 0;
const nonEmptyArr = (a) => Array.isArray(a) && a.length > 0;

class Range {
  constructor() {
    this.min = null;
    this.max = null;
    this.missing = 0;
    this.n = 0;
  }
  add(millis) {
    this.n += 1;
    if (millis == null) {
      this.missing += 1;
      return;
    }
    if (this.min == null || millis < this.min) this.min = millis;
    if (this.max == null || millis > this.max) this.max = millis;
  }
  toJSON() {
    return {count: this.n, earliest: iso(this.min), latest: iso(this.max), missingTimestamp: this.missing};
  }
}

// --- main ----------------------------------------------------------------

const main = async () => {
  // 1. Enumerate the actual root collections (catches anything a code sweep
  //    might miss: orphaned prototype collections, DM/location trails, etc.)
  const rootCollections = (await db.listCollections()).map((c) => c.id).sort();

  // 2. Scan drops.
  const dropsSnap = await db.collection("drops").get();
  const dropsRange = new Range();

  const disallowed = {
    anonymousDrops: new Range(), // isAnonymous == true (the display toggle being removed)
    videoDrops: new Range(), // contentType == VIDEO
    nsfwFlagged: new Range(), // isNsfw / nsfw / nsfwLabels
    groupDrops: new Range(), // visibility == GROUP or groupCode set
    dropsWithDislikes: new Range(), // dislikeCount>0 or dislikedBy non-empty
  };
  const contentTypeCounts = {};
  const byCreator = new Map(); // uid -> {drops, anonymous, video, nsfw, group, range}

  dropsSnap.forEach((doc) => {
    const d = doc.data();
    const created = toMillis(d.createdAt);
    dropsRange.add(created);

    const ct = typeof d.contentType === "string" ? d.contentType : "TEXT";
    contentTypeCounts[ct] = (contentTypeCounts[ct] || 0) + 1;

    const isAnon = d.isAnonymous === true;
    const isVideo = ct === "VIDEO";
    const isNsfw = d.isNsfw === true || d.nsfw === true || nonEmptyArr(d.nsfwLabels);
    const isGroup = d.visibility === "GROUP" ||
      (typeof d.groupCode === "string" && d.groupCode.trim().length > 0);
    const hasDislikes = (typeof d.dislikeCount === "number" && d.dislikeCount > 0) ||
      nonEmptyMap(d.dislikedBy);

    if (isAnon) disallowed.anonymousDrops.add(created);
    if (isVideo) disallowed.videoDrops.add(created);
    if (isNsfw) disallowed.nsfwFlagged.add(created);
    if (isGroup) disallowed.groupDrops.add(created);
    if (hasDislikes) disallowed.dropsWithDislikes.add(created);

    const uid = typeof d.createdBy === "string" && d.createdBy.trim() ? d.createdBy.trim() : "(missing)";
    if (!byCreator.has(uid)) {
      byCreator.set(uid, {drops: 0, anonymous: 0, video: 0, nsfw: 0, group: 0, range: new Range()});
    }
    const c = byCreator.get(uid);
    c.drops += 1;
    if (isAnon) c.anonymous += 1;
    if (isVideo) c.video += 1;
    if (isNsfw) c.nsfw += 1;
    if (isGroup) c.group += 1;
    c.range.add(created);
  });

  // 3. Group records.
  const groupsSnap = await db.collection("groups").get();
  const groupsRange = new Range();
  groupsSnap.forEach((g) => groupsRange.add(toMillis(g.data().createdAt)));
  const groupMembershipsSnap = await db.collectionGroup("groups").get();

  // 3b. Other retained collections (context / completeness).
  const reportsSnap = await db.collection("reports").get();
  const reportsRange = new Range();
  reportsSnap.forEach((r) => reportsRange.add(toMillis(r.data().createdAt) ?? toMillis(r.data().reportedAt)));
  const usernamesSnap = await db.collection("usernames").get();

  // 4. Users + NSFW preference + hunt data (context, not necessarily disallowed).
  const usersSnap = await db.collection("users").get();
  const usersRange = new Range();
  let nsfwEnabledUsers = 0;
  usersSnap.forEach((u) => {
    const data = u.data();
    usersRange.add(toMillis(data.createdAt) ?? toMillis(data.memberSince));
    if (data.nsfwEnabled !== false || data.nsfwEnabledAt != null) nsfwEnabledUsers += 1;
  });

  // 5. Auth enrichment for real-vs-test attribution.
  const authUsers = new Map(); // uid -> {email, providers, created}
  let nextPageToken;
  do {
    const page = await auth.listUsers(1000, nextPageToken);
    page.users.forEach((u) => {
      authUsers.set(u.uid, {
        email: u.email || null,
        anonymous: (u.providerData || []).length === 0,
        providers: (u.providerData || []).map((p) => p.providerId),
        created: u.metadata.creationTime || null,
        lastSignIn: u.metadata.lastSignInTime || null,
      });
    });
    nextPageToken = page.pageToken;
  } while (nextPageToken);

  const creatorRows = [...byCreator.entries()]
    .map(([uid, c]) => {
      const a = authUsers.get(uid) || {};
      return {
        uid,
        email: a.email || null,
        providers: a.providers || (uid === "(missing)" ? [] : ["<not in Auth>"]),
        authAnonymous: a.anonymous === true,
        drops: c.drops,
        anonymousDrops: c.anonymous,
        videoDrops: c.video,
        nsfwDrops: c.nsfw,
        groupDrops: c.group,
        firstDrop: iso(c.range.min),
        lastDrop: iso(c.range.max),
      };
    })
    .sort((x, y) => y.drops - x.drops);

  // 6. Assemble report.
  const report = {
    generatedAt: new Date().toISOString(),
    project: admin.app().options.projectId || process.env.GCLOUD_PROJECT || "(unknown)",
    rootCollections,
    directMessages: {
      note: "No DM/thread/conversation collection present.",
      candidateCollectionsFound: rootCollections.filter((c) =>
        /message|thread|conversation|chat|dm/i.test(c)),
    },
    locationHistory: {
      note: "No per-user location-history/trail collection present. Drop lat/lng is " +
        "content placement, not user tracking.",
      candidateCollectionsFound: rootCollections.filter((c) =>
        /location|trail|history|track|geo/i.test(c)),
    },
    totals: {
      drops: dropsRange.toJSON(),
      users: usersRange.toJSON(),
      groups: groupsRange.toJSON(),
      groupMembershipDocs: groupMembershipsSnap.size,
      reports: reportsRange.toJSON(),
      usernames: usernamesSnap.size,
      nsfwEnabledUserPrefs: nsfwEnabledUsers,
      dropContentTypeBreakdown: contentTypeCounts,
    },
    disallowedByScope: {
      anonymousPostingToggle: disallowed.anonymousDrops.toJSON(),
      videoDrops: disallowed.videoDrops.toJSON(),
      nsfwFlagged: disallowed.nsfwFlagged.toJSON(),
      groupScopedDrops: disallowed.groupDrops.toJSON(),
      dropsCarryingDislikes: disallowed.dropsWithDislikes.toJSON(),
      groupRecords: groupsRange.toJSON(),
      dmThreads: 0,
      storedLocationHistories: 0,
    },
    authUserCount: authUsers.size,
    perCreator: creatorRows,
  };

  if (JSON_ONLY) {
    console.log(JSON.stringify(report, null, 2));
    return;
  }

  const line = (s = "") => console.log(s);
  line(`GeoDrop data inventory — ${report.project}`);
  line(`Generated ${report.generatedAt}`);
  line();
  line(`Root collections (${rootCollections.length}): ${rootCollections.join(", ")}`);
  line();
  line("TOTALS");
  line(`  drops: ${report.totals.drops.count}  (${report.totals.drops.earliest} → ${report.totals.drops.latest})`);
  line(`  users (profiles): ${report.totals.users.count}   auth users: ${report.authUserCount}`);
  line(`  groups: ${report.totals.groups.count}   group-membership docs: ${report.totals.groupMembershipDocs}`);
  line(`  reports: ${report.totals.reports.count}   usernames: ${report.totals.usernames}`);
  line(`  content types: ${JSON.stringify(report.totals.dropContentTypeBreakdown)}`);
  line();
  line("DISALLOWED BY LAUNCH SCOPE");
  const d = report.disallowedByScope;
  const show = (label, r) =>
    line(`  ${label}: ${r.count}${r.count ? `  (${r.earliest} → ${r.latest})` : ""}`);
  show("anonymous-posting toggle drops", d.anonymousPostingToggle);
  show("video drops", d.videoDrops);
  show("NSFW-flagged drops", d.nsfwFlagged);
  show("group-scoped drops", d.groupScopedDrops);
  show("drops carrying dislikes", d.dropsCarryingDislikes);
  show("group records", d.groupRecords);
  line(`  DM threads: 0 (feature absent)`);
  line(`  stored location histories: 0 (no trail collection)`);
  line(`  NSFW-enabled user prefs: ${report.totals.nsfwEnabledUserPrefs}`);
  line();
  line("PER-CREATOR (real-vs-test attribution — identify your own test accounts)");
  report.perCreator.forEach((c) => {
    line(`  ${c.email || c.uid}  [${(c.providers || []).join(",") || "anon"}]  ` +
      `drops=${c.drops} anon=${c.anonymousDrops} video=${c.videoDrops} ` +
      `nsfw=${c.nsfwDrops} group=${c.groupDrops}  (${c.firstDrop} → ${c.lastDrop})`);
  });
  line();
  line("Re-run with --json for the full machine-readable report.");
};

main().catch((error) => {
  console.error("Data inventory failed:", error);
  process.exitCode = 1;
});
