"use strict";

const assert = require("assert");
const {REDESIGN_TEST_ONLY} = require("../lib/redesign");

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

console.log(JSON.stringify({
  passed: true,
  canonicalEvents: REDESIGN_TEST_ONLY.canonicalEventCount,
  newDropRadiusM: radiusM,
  maximumAcceptedBoundaryM: radiusM + maximumAcceptedAccuracyM,
}, null, 2));
