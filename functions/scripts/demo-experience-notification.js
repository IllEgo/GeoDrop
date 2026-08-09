"use strict";

/**
 * Task 4.5 evidence: who would be notified when a drop lands in an experience,
 * and why the rest would not.
 *
 * `--audit` answers that without writing anything, which is the part worth
 * reviewing: membership, the server-visible opt-out, and whether the member has
 * a registered token at all. A member with alerts on and no token is the quiet
 * failure this exists to surface — nothing errors, the push simply reaches nobody.
 *
 * `--apply` then creates a real drop in the experience so the deployed
 * notifyGroupMembersOnDropCreated trigger fires for an end-to-end demo.
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *     node scripts/demo-experience-notification.js --code=EATZ --audit
 *
 *   ... --code=EATZ --owner=<uid> --apply      # sends for real
 *   ... --code=EATZ --retire --apply           # soft-delete the demo drops
 */

const admin = require("firebase-admin");

const args = process.argv.slice(2);
const flags = new Set(args);
const valueFor = (flag) => {
  const prefix = `${flag}=`;
  const match = args.find((value) => value.startsWith(prefix));
  return match ? match.slice(prefix.length).trim() : "";
};

const groupCode = valueFor("--code").toUpperCase();
const owner = valueFor("--owner");
const shouldApply = flags.has("--apply");
const shouldAudit = flags.has("--audit");
const shouldRetire = flags.has("--retire");

if (!groupCode) {
  console.error(
    "Usage: node scripts/demo-experience-notification.js --code=<CODE> --audit\n" +
    "       node scripts/demo-experience-notification.js --code=<CODE> --owner=<uid> --apply\n" +
    "       node scripts/demo-experience-notification.js --code=<CODE> --retire --apply"
  );
  process.exit(2);
}

admin.initializeApp();
const db = admin.firestore();

const membersOf = async (code) => {
  const memberships = await db.collectionGroup("groups").where("code", "==", code).get();
  return Array.from(new Set(
    memberships.docs.map((doc) => doc.ref.parent.parent?.id).filter(Boolean)
  ));
};

const audit = async () => {
  const memberIds = await membersOf(groupCode);
  const rows = [];
  for (const memberId of memberIds) {
    const [preference, tokens, profile] = await Promise.all([
      db.collection("users").doc(memberId).collection("notificationSettings").doc("preferences").get(),
      db.collection("users").doc(memberId).collection("notificationTokens").get(),
      db.collection("users").doc(memberId).get(),
    ]);
    // Absent means opted in; joining the experience is the opt-in.
    const optedIn = !preference.exists || preference.get("experienceAlertsEnabled") !== false;
    rows.push({
      userId: memberId,
      role: profile.get("role") || null,
      preference: preference.exists ?
        (preference.get("experienceAlertsEnabled") === false ? "opted out" : "opted in") :
        "unset (opted in)",
      tokens: tokens.size,
      wouldBeNotified: optedIn && tokens.size > 0,
      reasonIfNot: !optedIn ? "opted out" : (tokens.size === 0 ? "no registered token" : null),
    });
  }

  console.log(JSON.stringify({
    groupCode,
    members: rows.length,
    wouldBeNotified: rows.filter((r) => r.wouldBeNotified).length,
    detail: rows,
  }, null, 2));

  const silent = rows.filter((r) => r.reasonIfNot === "no registered token");
  if (silent.length > 0) {
    console.log(
      `\n${silent.length} member(s) have alerts on but no token, so a send reaches nobody.\n` +
      "Expected while pilot_notifications_enabled is false: the clients gate token\n" +
      "registration on that flag, so tokens appear only once it is on."
    );
  }
};

const retire = async () => {
  const snapshot = await db.collection("drops").where("groupCode", "==", groupCode).get();
  const demo = snapshot.docs.filter((doc) => doc.id.startsWith(`demo-notify-${groupCode}-`));
  console.log(JSON.stringify({
    mode: shouldApply ? "apply" : "dry-run",
    action: "retire",
    drops: demo.map((doc) => doc.id),
  }, null, 2));
  if (!shouldApply) return;
  for (const doc of demo) {
    await doc.ref.update({isDeleted: true, deletedAt: Date.now()});
  }
  console.log(`\nSoft-deleted ${demo.length} demo drop(s).`);
};

const send = async () => {
  if (!owner) {
    console.error("--apply needs --owner=<uid> to author the drop.");
    process.exit(2);
  }
  const group = await db.collection("groups").doc(groupCode).get();
  if (!group.exists) {
    console.error(`No group ${groupCode}.`);
    process.exit(1);
  }

  const now = Date.now();
  const dropId = `demo-notify-${groupCode}-${now.toString(36)}`;
  const document = {
    text: "Demo drop for the 4.5 notification check",
    description: "Created to fire notifyGroupMembersOnDropCreated.",
    lat: 19.703995,
    lng: -155.076800,
    createdBy: owner,
    createdAt: now,
    isDeleted: false,
    isNsfw: false,
    nsfwLabels: [],
    visibility: "GROUP",
    groupCode,
    dropType: "COMMUNITY",
    contentType: "TEXT",
    likeCount: 0,
    likedBy: {},
    reportCount: 0,
    reportedBy: {},
    collectedBy: {},
  };

  console.log(JSON.stringify({mode: "apply", dropId, groupCode, createdBy: owner}, null, 2));
  await db.collection("drops").doc(dropId).set(document);
  console.log(
    `\nCreated ${dropId}. The trigger runs asynchronously — check delivery on the\n` +
    "device, and the decision in the logs:\n" +
    "  firebase functions:log --only notifyGroupMembersOnDropCreated\n" +
    `Clean up with: node scripts/demo-experience-notification.js --code=${groupCode} --retire --apply`
  );
};

const main = async () => {
  if (shouldRetire) return retire();
  if (shouldAudit || !shouldApply) return audit();
  await send();
};

main().catch((error) => {
  console.error("Demo notification script failed", error);
  process.exitCode = 1;
});
