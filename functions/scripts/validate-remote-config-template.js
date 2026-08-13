"use strict";

const fs = require("fs");
const path = require("path");

const expectedKeys = [
  "pilot_creation_enabled",
  "pilot_notifications_enabled",
  "pilot_coupons_enabled",
  "pilot_media_enabled",
  "pilot_hunts_enabled",
  "pilot_redesign_backend_enabled",
];
const templatePath = path.resolve(__dirname, "..", "..", "remoteconfig.template.json");
const template = JSON.parse(fs.readFileSync(templatePath, "utf8"));
const parameters = template.parameters ?? {};
const actualKeys = Object.keys(parameters).sort();
const minimumContractKey = "pilot_redesign_min_contract_version";
const allExpectedKeys = [...expectedKeys, minimumContractKey];

if (JSON.stringify(actualKeys) !== JSON.stringify(allExpectedKeys.sort())) {
  throw new Error(`Remote Config keys differ: ${actualKeys.join(", ")}`);
}
if (parameters[minimumContractKey]?.defaultValue?.value !== "2147483647") {
  throw new Error(`${minimumContractKey} must default to an unsupported version`);
}
if (parameters[minimumContractKey]?.conditionalValues &&
    Object.keys(parameters[minimumContractKey].conditionalValues).length > 0) {
  throw new Error(`${minimumContractKey} must not ship with conditional enablement`);
}
for (const key of expectedKeys) {
  const parameter = parameters[key];
  if (parameter.defaultValue?.value !== "false") {
    throw new Error(`${key} must default to false`);
  }
  if (parameter.conditionalValues &&
      Object.keys(parameter.conditionalValues).length > 0) {
    throw new Error(`${key} must not ship with conditional enablement`);
  }
}
if (!Array.isArray(template.conditions) || template.conditions.length > 0) {
  throw new Error("The launch template must have an empty conditions array");
}

console.log(`Remote Config template passed (${allExpectedKeys.length} fail-closed keys).`);
