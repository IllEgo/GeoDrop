# A5a local release-candidate remediation evidence

Status: **complete locally. A5b was subsequently approved; all external release actions
remain separately gated**.

The owner explicitly approved A5a as bounded in `A5-APPROVAL.md`. The work ran locally on
2026-08-25 UTC and made no Firebase, Play, Cloudflare, policy, Remote Config, tester, or other
external mutation.

## Remediation completed

- Android `compileSdk` and `targetSdk` are API 36.
- Android Gradle Plugin is 8.12.2 with Gradle 8.13. The obsolete manifest `package`
  declaration was removed; the unchanged `com.kitheapp` namespace and application ID remain
  in Gradle.
- The test-only Robolectric dependency is 4.16.1. The one previously unpinned legacy JSON
  test now runs on SDK 34, matching the rest of the established Robolectric suite.
- `R3TestApplication` registers `androidx.activity.ComponentActivity` only in Robolectric's
  shadow package manager. This repairs `createComposeRule` for the release unit-test variant
  without adding a test activity to the production manifest or AAB.

## Passing local gates

- `testReleaseUnitTest`: **130 tests, 130 passed, 0 failed, 0 errors, 0 skipped**.
- `lintRelease`: **0 errors, 156 warnings, 14 hints**.
- `bundleRelease` with `KITHE_APP_LINK_HOST=join.kitheapp.com`: **passed**.
- The release merged manifest and resolved runtime dependency graph were inspected after the
  build. They contain no Advertising ID permission, AdServices declaration, microphone
  permission, registered recorder component, Firebase Analytics SDK, Google Play Services
  measurement SDK, or ads/ads-identifier dependency. The lightweight
  `firebase-measurement-connector` interface remains transitively required by Crashlytics,
  Remote Config, and Messaging; it is not the Analytics collection runtime.
- The release manifest targets API 36 and contains `join.kitheapp.com`. It contains neither
  `androidx.activity.ComponentActivity` nor `AudioRecorderActivity`.
- Generated release configuration confirms `APP_LINK_CONFIGURED=true`,
  `USE_PLAY_INTEGRITY_APPCHECK=true`, `MAPS_CONFIGURED=false`, and every build-time feature
  upper bound false. The Maps resource remains the not-configured sentinel.
- Google Services generation remains bound to Firebase project `kithe-production`, and the
  resolved release graph retains `firebase-appcheck-playintegrity`.

## Unsigned diagnostic artifact

- File: `app/build/outputs/bundle/release/app-release.aab`
- Size: `20,172,875` bytes
- SHA-256: `C3B7EA328607936E609D26C7A3EEE5B46B67AAED1AD300427328E5FB35286963`
- Signing audit: zero JAR signature entries; `jarsigner` reports **jar is unsigned**.

This AAB is diagnostic evidence only. It is not an upload candidate and must not be sent to
Play. No upload key, signing configuration, Maps key, policy URL, test credential, secret,
reviewer fixture, tester list, or release was created or changed.

## Gate

A5a stopped here. The owner subsequently approved A5b's local upload-key and exact-candidate
preparation; that later work is recorded separately in `A5B-EVIDENCE.md`.
