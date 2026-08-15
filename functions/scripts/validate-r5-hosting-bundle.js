"use strict";

const fs = require("fs");
const path = require("path");

const repositoryRoot = path.resolve(__dirname, "..", "..");
const firebasePath = path.join(repositoryRoot, "firebase.json");
const hostingRoot = path.join(repositoryRoot, "hosting");
const assetLinksPath = path.join(hostingRoot, ".well-known", "assetlinks.json");
const requireReleaseAssets = process.argv.includes("--require-release-assets");
const errors = [];
const blockers = [];

const walkFiles = (directory, prefix = "") => {
  if (!fs.existsSync(directory)) return [];
  return fs.readdirSync(directory, {withFileTypes: true}).flatMap((entry) => {
    const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
    const absolute = path.join(directory, entry.name);
    return entry.isDirectory() ? walkFiles(absolute, relative) : [relative];
  });
};

const exists = (relativePath) => fs.existsSync(path.join(repositoryRoot, relativePath));
const firebase = JSON.parse(fs.readFileSync(firebasePath, "utf8"));
const hosting = Array.isArray(firebase.hosting) ? firebase.hosting[0] : firebase.hosting;

if (!hosting || hosting.public !== "hosting") {
  errors.push("Firebase Hosting must use the dedicated hosting/ directory.");
}
if ((hosting?.ignore ?? []).includes("**/.*")) {
  errors.push("The Hosting ignore list would exclude .well-known/assetlinks.json.");
}

const entryRewrite = (hosting?.rewrites ?? []).find((rewrite) => rewrite.source === "/e/**");
const entryFunction = typeof entryRewrite?.function === "string" ?
  entryRewrite.function : entryRewrite?.function?.functionId;
if (entryFunction !== "experienceEntryPage") {
  errors.push("/e/** must rewrite to experienceEntryPage.");
}

const assetHeaders = (hosting?.headers ?? []).find((header) =>
  header.source === "/.well-known/assetlinks.json"
);
const contentType = (assetHeaders?.headers ?? []).find((header) =>
  header.key.toLowerCase() === "content-type"
)?.value;
if (!contentType?.toLowerCase().startsWith("application/json")) {
  errors.push("assetlinks.json must be configured with application/json content type.");
}

for (const required of [
  "hosting/index.html",
  "hosting/404.html",
  "functions/src/entryPage.ts",
]) {
  if (!exists(required)) errors.push(`Missing required bundle file: ${required}`);
}

for (const prohibited of [
  "privacy.html",
  "account-deletion.html",
  "terms.html",
  "community-guidelines.html",
  "data-retention.html",
  "minors.html",
  "promotion-terms.html",
  "subprocessors.html",
]) {
  if (walkFiles(hostingRoot).some((relative) => path.basename(relative) === prohibited)) {
    errors.push(`Unapproved policy draft is inside the entry-host bundle: ${prohibited}`);
  }
}

const allowedHostingFiles = new Set([
  "index.html",
  "404.html",
  ".well-known/assetlinks.json",
]);
for (const relative of walkFiles(hostingRoot)) {
  if (!allowedHostingFiles.has(relative.replace(/\\/g, "/"))) {
    errors.push(`Unexpected file in the minimal entry-host bundle: ${relative}`);
  }
}

let fingerprintCount = 0;
if (!fs.existsSync(assetLinksPath)) {
  blockers.push("Play App Signing SHA-256 is required before creating hosting/.well-known/assetlinks.json.");
} else {
  try {
    const statements = JSON.parse(fs.readFileSync(assetLinksPath, "utf8"));
    if (!Array.isArray(statements) || statements.length !== 1) {
      errors.push("assetlinks.json must contain exactly one production Kithe statement.");
    } else {
      const statement = statements[0];
      const target = statement?.target ?? {};
      const fingerprints = target.sha256_cert_fingerprints;
      if (!statement?.relation?.includes("delegate_permission/common.handle_all_urls")) {
        errors.push("assetlinks.json is missing the handle_all_urls relation.");
      }
      if (target.namespace !== "android_app" || target.package_name !== "com.kitheapp") {
        errors.push("assetlinks.json must target only Android package com.kitheapp.");
      }
      if (!Array.isArray(fingerprints) || fingerprints.length !== 1) {
        errors.push("assetlinks.json must contain exactly one Play App Signing fingerprint.");
      } else {
        fingerprintCount = fingerprints.length;
        const fingerprintPattern = /^(?:[0-9A-F]{2}:){31}[0-9A-F]{2}$/;
        if (!fingerprintPattern.test(fingerprints[0])) {
          errors.push("The App Links fingerprint must be uppercase, colon-delimited SHA-256.");
        }
      }
    }
  } catch (error) {
    errors.push(`assetlinks.json is not valid JSON: ${error.message}`);
  }
}

const result = {
  structuralChecksPassed: errors.length === 0,
  productionReady: errors.length === 0 && blockers.length === 0,
  hostingDirectory: hosting?.public ?? null,
  entryFunction,
  fingerprintCount,
  excludedPolicyDraftDirectory: "public",
  errors,
  blockers,
};
console.log(JSON.stringify(result, null, 2));

if (errors.length > 0 || (requireReleaseAssets && blockers.length > 0)) {
  process.exitCode = 1;
}
