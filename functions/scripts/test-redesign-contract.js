"use strict";

const assert = require("assert");
const {REDESIGN_TEST_ONLY} = require("../lib/redesign");
const {
  renderExperienceEntryNotFound,
  renderExperienceEntryPage,
} = require("../lib/entryPage");

assert.strictEqual(REDESIGN_TEST_ONLY.canonicalEventCount, 21);
assert.strictEqual(REDESIGN_TEST_ONLY.distanceBucket(0), "0_25");
assert.strictEqual(REDESIGN_TEST_ONLY.distanceBucket(25), "0_25");
assert.strictEqual(REDESIGN_TEST_ONLY.distanceBucket(25.01), "25_50");
assert.strictEqual(REDESIGN_TEST_ONLY.distanceBucket(50), "25_50");
assert.strictEqual(REDESIGN_TEST_ONLY.distanceBucket(50.01), "50_PLUS");

const samePoint = REDESIGN_TEST_ONLY.haversineMetres(19.7, -155.1, 19.7, -155.1);
assert.ok(samePoint < 0.001);
const oneDegreeLatitude = REDESIGN_TEST_ONLY.haversineMetres(0, 0, 1, 0);
assert.ok(oneDegreeLatitude > 111000 && oneDegreeLatitude < 111300);

const radiusM = 25;
const maximumAcceptedAccuracyM = Math.min(radiusM, 30);
assert.strictEqual(maximumAcceptedAccuracyM, 25);
assert.strictEqual(radiusM + maximumAcceptedAccuracyM, 50);
assert.deepStrictEqual(REDESIGN_TEST_ONLY.rateLimits.submitReport, {
  limit: 20,
  windowSeconds: 24 * 60 * 60,
});
assert.deepStrictEqual(REDESIGN_TEST_ONLY.rateLimits.unlockDrop, {
  limit: 20,
  windowSeconds: 5 * 60,
});

const activeEntryHtml = renderExperienceEntryPage({
  code: "DEMO2026",
  name: "<Hilo Garden Walk>",
  description: "Find what the host left for you.",
  hostLabel: "Kithe & Friends",
  availability: "ACTIVE",
  availableDropCount: 3,
}, "https://play.google.com/store/apps/details?id=com.kitheapp&referrer=safe");
assert.ok(activeEntryHtml.includes("DEMO-2026"));
assert.ok(activeEntryHtml.includes("3 drops"));
assert.ok(activeEntryHtml.includes("&lt;Hilo Garden Walk&gt;"));
assert.ok(activeEntryHtml.includes("Kithe &amp; Friends"));
assert.ok(!activeEntryHtml.includes("<Hilo Garden Walk>"));
assert.ok(!activeEntryHtml.includes("latitude"));
assert.ok(!activeEntryHtml.includes("longitude"));

const upcomingEntryHtml = renderExperienceEntryPage({
  code: "NEXT2026",
  name: "Tomorrow's Walk",
  description: null,
  hostLabel: "Host",
  availability: "UPCOMING",
  availableDropCount: 1,
}, "https://play.google.com/store/apps/details?id=com.kitheapp");
assert.ok(upcomingEntryHtml.includes("STARTS SOON"));
assert.ok(upcomingEntryHtml.includes("1 drop"));

const endedEntryHtml = renderExperienceEntryPage({
  code: "PAST2026",
  name: "Past Walk",
  description: null,
  hostLabel: "Host",
  availability: "ENDED",
  availableDropCount: 4,
}, "https://play.google.com/store/apps/details?id=com.kitheapp");
assert.ok(endedEntryHtml.includes("THIS ONE&#39;S CLOSED"));
assert.ok(!endedEntryHtml.includes("primary-action\" href="));
assert.ok(renderExperienceEntryNotFound().includes("We couldn't find that Experience"));

console.log(JSON.stringify({
  passed: true,
  canonicalEvents: REDESIGN_TEST_ONLY.canonicalEventCount,
  newDropRadiusM: radiusM,
  maximumAcceptedBoundaryM: radiusM + maximumAcceptedAccuracyM,
}, null, 2));
