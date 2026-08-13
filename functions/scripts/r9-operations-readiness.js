"use strict";

const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const packageJson = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8"));
const requiredCommands = [
  "moderation:console",
  "moderation:rehearse",
  "moderator:check",
  "moderator:apply",
  "account:audit",
  "account:rehearse",
  "migrate:redesign:audit",
];
const requiredAssignments = [
  ["GEODROP_TRUST_SAFETY_LEAD", "Trust & Safety lead"],
  ["GEODROP_PILOT_STAFFED_HOURS", "pilot staffed hours and timezone"],
  ["GEODROP_CRITICAL_CONTACT", "Critical after-hours contact"],
  ["GEODROP_LEGAL_ESCALATION_CHANNEL", "Legal escalation channel"],
  ["GEODROP_APPEAL_REVIEWER", "appeal reviewer"],
  ["GEODROP_ALERT_OWNER", "moderation SLA alert owner"],
  ["GEODROP_SUPPORT_CHANNEL", "event support channel"],
];

const missingCommands = requiredCommands.filter((command) => !packageJson.scripts[command]);
const assignments = requiredAssignments.map(([key, label]) => ({
  key,
  label,
  assigned: Boolean(process.env[key]?.trim()),
}));
const missingAssignments = assignments.filter((item) => !item.assigned);
const localOnly = process.argv.includes("--local-only");
const passed = missingCommands.length === 0 && (localOnly || missingAssignments.length === 0);

console.log(JSON.stringify({
  passed,
  mode: localOnly ? "LOCAL_ONLY" : "PRE_PILOT",
  commands: {
    required: requiredCommands,
    missing: missingCommands,
  },
  assignments,
  next: missingAssignments.map((item) => `Assign ${item.label} with ${item.key}.`),
}, null, 2));

if (!passed) process.exitCode = 1;
