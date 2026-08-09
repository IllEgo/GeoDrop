"use strict";

/**
 * Publishes `remoteconfig.template.json` to a project's live Remote Config.
 *
 * **This exists because nothing else here can do it.** The template is managed
 * as code and validated in CI, but the Firebase CLI has no Remote Config publish
 * command — it offers only `remoteconfig:get`, `remoteconfig:rollback`, and
 * `remoteconfig:versions:list`. Worse, `firebase deploy --only remoteconfig`
 * *accepts* the target (because firebase.json carries a `remoteconfig` key) and
 * then publishes nothing while printing "Deploy complete!". Four consecutive
 * attempts to flip a flag that way reported success and changed nothing, which
 * is how this script came to exist.
 *
 * Dry run (default) — prints what would change against the live template:
 *   GOOGLE_APPLICATION_CREDENTIALS=../.secrets/<key>.json \
 *     node scripts/publish-remote-config.js --project=geodrop-dfcba
 *
 * Publish:
 *   ... --project=geodrop-dfcba --apply --confirm-project=geodrop-dfcba
 *
 * Temporarily enable flags for a supervised demo **without editing the
 * template**, which is what keeps the committed file fail-closed and makes the
 * revert unmissable:
 *   ... --apply --confirm-project=geodrop-dfcba \
 *       --enable=pilot_creation_enabled,pilot_hunts_enabled --allow-enabled
 *
 * Revert by running with no --enable at all: the committed template is all-false
 * by construction, so publishing it *is* the revert. Nothing to remember, and no
 * dirty working file that a later commit could pick up.
 *
 * Any enabled flag — from --enable or from an edited template — needs
 * --allow-enabled. Every pilot flag is meant to ship false (see
 * validate-remote-config-template.js), so enabling one is deliberate,
 * temporary, and supervised, and must not be possible by accident.
 */

const fs = require("fs");
const path = require("path");
const {GoogleAuth} = require("google-auth-library");

const args = process.argv.slice(2);
const flags = new Set(args);
const valueFor = (flag) => {
  const prefix = `${flag}=`;
  const match = args.find((value) => value.startsWith(prefix));
  return match ? match.slice(prefix.length).trim() : "";
};

const projectId = valueFor("--project") ||
  process.env.GCLOUD_PROJECT ||
  process.env.GOOGLE_CLOUD_PROJECT;
const shouldApply = flags.has("--apply");
const confirmedProject = valueFor("--confirm-project");
const allowEnabled = flags.has("--allow-enabled");

if (!projectId) {
  console.error("Usage: node scripts/publish-remote-config.js --project=<id> [--enable=<key,key>] [--apply --confirm-project=<id> --allow-enabled]");
  process.exit(1);
}

const templatePath = path.resolve(__dirname, "..", "..", "remoteconfig.template.json");
const template = JSON.parse(fs.readFileSync(templatePath, "utf8"));

// Overrides live in this process only. The template on disk stays fail-closed,
// so the revert is "run me again with no --enable" rather than "remember to
// undo a file edit".
const requestedEnables = valueFor("--enable")
  .split(",")
  .map((key) => key.trim())
  .filter((key) => key.length > 0);

requestedEnables.forEach((key) => {
  if (!template.parameters || !template.parameters[key]) {
    console.error(`Unknown parameter in --enable: ${key}`);
    process.exit(1);
  }
  template.parameters[key].defaultValue = {value: "true"};
});

const enabled = Object.entries(template.parameters || {})
  .filter(([, parameter]) => (parameter.defaultValue || {}).value === "true")
  .map(([key]) => key);

const endpoint = `https://firebaseremoteconfig.googleapis.com/v1/projects/${projectId}/remoteConfig`;

const describe = (parameters) => Object.keys(parameters || {}).sort()
  .map((key) => `${key}=${(parameters[key].defaultValue || {}).value}`);

(async () => {
  const auth = new GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/firebase.remoteconfig"],
  });
  const client = await auth.getClient();

  const current = await client.request({url: endpoint});
  const etag = current.headers.etag;
  const liveVersion = (current.data.version || {}).versionNumber;

  const before = describe(current.data.parameters);
  const after = describe(template.parameters);
  const changes = after.filter((entry) => !before.includes(entry));

  console.log(`project:      ${projectId}`);
  console.log(`live version: ${liveVersion}`);
  console.log(`live:         ${before.join(", ")}`);
  console.log(`template:     ${after.join(", ")}`);
  console.log(`changes:      ${changes.length ? changes.join(", ") : "none — the live template already matches"}`);

  if (enabled.length > 0) {
    console.log(`\nENABLED FLAGS: ${enabled.join(", ")}`);
    console.log("These ship false by policy. Publishing them is temporary and supervised; revert by publishing the committed template.");
  }

  if (!shouldApply) {
    console.log("\nDry run only. Re-run with --apply --confirm-project=<id> to publish.");
    return;
  }

  if (confirmedProject !== projectId) {
    console.error(`\nRefusing to publish: --confirm-project must equal --project (${projectId}).`);
    process.exit(1);
  }

  if (enabled.length > 0 && !allowEnabled) {
    console.error(`\nRefusing to publish an enabled flag without --allow-enabled: ${enabled.join(", ")}.`);
    process.exit(1);
  }

  if (changes.length === 0) {
    console.log("\nNothing to publish.");
    return;
  }

  // If-Match against the live ETag, so a template edited from somewhere else
  // between the read above and this write is rejected rather than clobbered.
  const published = await client.request({
    url: endpoint,
    method: "PUT",
    headers: {
      "If-Match": etag,
      "Content-Type": "application/json; UTF-8",
    },
    data: template,
  });

  const version = (published.data.version || {}).versionNumber;
  console.log(`\nPublished. Live version is now ${version}.`);
  console.log("Verify with: npx firebase remoteconfig:get --project " + projectId);
})().catch((error) => {
  const detail = error.response && error.response.data ?
    JSON.stringify(error.response.data).slice(0, 400) : "";
  console.error("FAILED:", error.message, detail);
  process.exit(1);
});
