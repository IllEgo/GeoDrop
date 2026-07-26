"use strict";

const admin = require("firebase-admin");

const args = new Set(process.argv.slice(2));
const valueFor = (flag) => {
  const prefix = `${flag}=`;
  const match = [...args].find((value) => value.startsWith(prefix));
  return match ? match.slice(prefix.length).trim() : "";
};

const uid = valueFor("--uid");
const shouldApply = args.has("--apply");
const enable = args.has("--enable");
const disable = args.has("--disable");

if (!uid || uid.length > 128 || enable === disable) {
  console.error(
    "Usage: node scripts/set-moderator-claim.js --uid=<firebase-uid> " +
    "(--enable|--disable) [--apply]"
  );
  process.exit(2);
}

admin.initializeApp();

const main = async () => {
  const auth = admin.auth();
  const user = await auth.getUser(uid);
  const before = user.customClaims || {};
  const after = {...before};
  if (enable) after.moderator = true;
  else delete after.moderator;

  console.log(JSON.stringify({
    mode: shouldApply ? "apply" : "dry-run",
    uid,
    email: user.email || null,
    moderatorBefore: before.moderator === true,
    moderatorAfter: after.moderator === true,
  }));

  if (!shouldApply) return;
  await auth.setCustomUserClaims(uid, after);
  await auth.revokeRefreshTokens(uid);
  console.log("Moderator claim updated; the account must refresh its ID token.");
};

main().catch((error) => {
  console.error("Failed to update moderator claim", error);
  process.exitCode = 1;
});
