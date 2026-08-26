# Supported-device smoke runbook

This runbook defines the minimum release-candidate smoke matrix for Kithe. It is an evidence checklist, not a claim that a device or journey has passed. Record every run in the release evidence document and link defects rather than silently waiving failures.

## Entry criteria

- Use the exact Android APK/AAB and iOS archive proposed for release.
- Record the source commit, version/build number, artifact SHA-256, Firebase project, tester, device model, OS version, and run time.
- Use production-equivalent Firebase configuration with test accounts and seeded, non-sensitive content.
- Confirm the tester can restore the previous approved build before testing rollback.
- Start with a clean install, then repeat the upgrade checks over the previous approved build.

## Minimum device matrix

| Platform | Required target | Why it is required | Result/evidence |
|---|---|---|---|
| Android | API 26 phone emulator or physical device | Minimum supported SDK and constrained-device behavior | Not run |
| Android | API 29 phone | Background-location permission transition | Not run |
| Android | API 33 phone | Runtime notification permission | Not run |
| Android | API 36 phone | Current target-SDK behavior | Not run |
| Android | API 36 large-screen or tablet | Responsive layout and rotation | Not run |
| iOS | iOS 15.x iPhone simulator or physical device | Minimum deployment target | Not run |
| iOS | Latest stable iOS, compact iPhone | Current permission and notification behavior | Not run |
| iOS | Latest stable iOS, large iPhone | Layout and Dynamic Type coverage | Not run |
| iOS | Latest stable iOS, physical iPhone | Camera, microphone, location, push, and background behavior | Not run |

If a required OS/device is unavailable, record the gap, owner, risk acceptance, and approver. An unavailable target is not a pass.

## Critical smoke journeys

Run each applicable journey on every row in the matrix. Capture a screen recording or screenshots for permission, safety, account-lifecycle, and rollback checks.

1. Clean install and first launch
   - App launches without a crash or configuration error.
   - Terms and onboarding appear before runtime permission prompts.
   - Denying location and notifications leaves a useful degraded browse experience.
   - Settings recovery is clear and returns the app to a working state.
2. Guest discovery and account upgrade
   - Guest can load public, safe drops without exposing group-only or adult content.
   - Explorer sign-in completes and preserves the documented guest state.
3. Drop discovery and collection
   - Feed/map load, distance lock, preview, collect, inventory, and deleted/expired states behave correctly.
   - Offline and interrupted actions fail safely and recover after reconnecting.
4. Drop creation and media
   - Create safe text and supported media drops.
   - Unsafe/adult content is rejected and removed from temporary/storage paths.
   - Creation kill switch is exercised when available.
5. Groups and access control
   - Owner creates a group; member joins and reads authorized drops; non-member direct reads and queries fail.
   - Group media behavior matches the pilot policy.
6. Safety
   - Report and block produce confirmation and hide the expected content/creator.
   - Reporter data is not exposed to another client.
7. Nearby notifications
   - Notification permission is requested only after explicit enablement and rationale.
   - Radius change and opt-out take effect; deleted, group-unauthorized, and adult drops do not notify.
8. Business offer
   - Publish, view terms, redeem once, reject reuse/invalid code, and pause the offer when available.
9. Account lifecycle
   - Export and deletion require reauthentication and explicit confirmation.
   - Verify the documented data surfaces and completion receipt. Mark blocked until the product workflow exists.
10. Upgrade and rollback
    - Install the candidate over the previous approved build and verify retained state.
    - Restore the previous approved build or execute the approved store rollback path.
    - Confirm server schemas/rules remain compatible and document any irreversible migration.

## Per-run record

| Field | Value |
|---|---|
| Release candidate / commit | |
| Artifact and SHA-256 | |
| Platform / device / OS | |
| Clean install result | Not run |
| Upgrade result | Not run |
| Journeys passed / failed / blocked | |
| Crash or ANR evidence | |
| Defect links | |
| Tester and timestamp | |

## Exit rule

Every critical journey must pass on every available required target, with no unresolved critical/high security, privacy, safety, or crash defect. Missing account deletion/export, permission timing, operational moderation, kill-switch verification, signed-release evidence, or a required device result keeps the release at No-Go.
