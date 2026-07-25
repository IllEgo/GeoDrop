"use strict";

const args = process.argv.slice(2);
const command = args[0];

const valueFor = (name) => {
  const index = args.indexOf(name);
  return index >= 0 ? args[index + 1] : undefined;
};

const requireValue = (name) => {
  const value = valueFor(name);
  if (!value || value.startsWith("--")) {
    throw new Error(`Missing ${name}`);
  }
  return value;
};

const projectId = process.env.GEODROP_PROJECT_ID?.trim();
const idToken = process.env.GEODROP_MODERATOR_ID_TOKEN?.trim();
const appCheckToken = process.env.GEODROP_APPCHECK_TOKEN?.trim();
const region = process.env.GEODROP_FUNCTIONS_REGION?.trim() || "us-central1";
const configuredBase = process.env.GEODROP_FUNCTIONS_BASE_URL?.trim();

if (!projectId || !idToken || !appCheckToken) {
  throw new Error(
    "Set GEODROP_PROJECT_ID, GEODROP_MODERATOR_ID_TOKEN, and " +
    "GEODROP_APPCHECK_TOKEN. Tokens are read only from the environment."
  );
}

const baseUrl = configuredBase ||
  `https://${region}-${projectId}.cloudfunctions.net`;

const callable = async (name, data) => {
  const response = await fetch(`${baseUrl.replace(/\/$/, "")}/${name}`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${idToken}`,
      "Content-Type": "application/json",
      "X-Firebase-AppCheck": appCheckToken,
    },
    body: JSON.stringify({data}),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.error) {
    const message = payload.error?.message ||
      `Callable ${name} failed with HTTP ${response.status}`;
    throw new Error(message);
  }
  return payload.result;
};

const main = async () => {
  let result;
  switch (command) {
  case "list":
    result = await callable("listModerationQueue", {
      limit: Number(valueFor("--limit") || 50),
    });
    break;
  case "triage":
    result = await callable("triageModerationCase", {
      reportId: requireValue("--report"),
      severity: requireValue("--severity"),
    });
    break;
  case "decide":
    result = await callable("decideModerationCase", {
      reportId: requireValue("--report"),
      decision: requireValue("--decision"),
      rationale: requireValue("--rationale"),
      suspendSubject: args.includes("--suspend-subject"),
    });
    break;
  case "appeal":
    result = await callable("decideModerationAppeal", {
      appealId: requireValue("--appeal"),
      outcome: requireValue("--outcome"),
      rationale: requireValue("--rationale"),
    });
    break;
  default:
    throw new Error(
      "Usage: moderation-console.js list [--limit N] | " +
      "triage --report ID --severity LEVEL | " +
      "decide --report ID --decision ACTION --rationale TEXT " +
      "[--suspend-subject] | appeal --appeal ID --outcome OUTCOME " +
      "--rationale TEXT"
    );
  }
  console.log(JSON.stringify(result, null, 2));
};

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
