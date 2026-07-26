# Release and rollback evidence

Copy this template for each release candidate. Leave unknown items as `Not verified`; do not infer production readiness from a local build.

## Release identity

| Field | Evidence |
|---|---|
| Release name | |
| Source commit / tag | |
| Android versionCode / versionName | |
| iOS build / marketing version | |
| Android artifact path and SHA-256 | Not verified |
| iOS archive path and SHA-256 | Not verified |
| Firebase project / functions region | |
| Release owner | |
| Candidate created at | |

## Automated checks

| Check | Required evidence | Status |
|---|---|---|
| Android unit tests | CI run URL and test report artifact | Not verified |
| Android lint | CI run URL and lint report artifact | Not verified |
| Android debug build | CI run URL and APK artifact | Not verified |
| Functions lint/build | CI run URL | Not verified |
| Firestore/Storage adversarial tests | Emulator CI run URL and logs | Not verified |
| Signed Android release | Authorized operator, signing identity/fingerprint, artifact hash | Not verified |
| Signed iOS release/archive | Authorized operator, team/profile, artifact hash | Not verified |

## Security and production configuration

| Gate | Required evidence | Status |
|---|---|---|
| Production Firebase configuration confirmed | Project identifiers and reviewer; never attach credentials | Not verified |
| Client/API credentials restricted | Provider-console evidence, restrictions, owner, review date | Not verified |
| Exposed or legacy secrets rotated | Rotation ticket/log and post-rotation verification | Not verified |
| Android Play Integrity App Check | Console evidence and release-build validation | Not verified |
| iOS App Attest/DeviceCheck | Console evidence and release-build validation | Not verified |
| Backup, retention, and deletion match policy | Approved policy and end-to-end test | Not verified |

## Observability and controls

| Gate | Android evidence/status | iOS evidence/status |
|---|---|---|
| Crash reporting receives a release-build test event | Not verified | Not verified |
| Release/version symbols or mappings uploaded | Not verified | Not verified |
| Core-action success dashboard | Not verified | Not verified |
| Alert owner and notification channel tested | Not verified | Not verified |
| Creation kill switch exercised | Not verified | Not verified |
| Notification kill switch exercised | Not verified | Not verified |
| Coupon/redemption kill switch exercised | Not verified | Not verified |
| Media kill switch exercised | Not verified | Not verified |

Record the dashboard URLs, alert test time, observed test event, flag source, before/after values, propagation time, and tester. A hardcoded value or an unexercised flag is not evidence of a kill switch.

The repository defines fail-closed Android build fields and iOS Info.plist/`.xcconfig` values as upper bounds for Firebase Remote Config parameters. Client action, repository, media, notification, and background entry points require both controls to be true. Both clients fetch and activate Remote Config, listen for realtime updates, and default every parameter to false. The checked-in `remoteconfig.template.json` also defaults all parameters to false. Production publication, ownership, propagation timing, and an observed on/off exercise are still required evidence.

| Remote Config parameter | Controlled surface |
|---|---|
| `pilot_creation_enabled` | All drop creation |
| `pilot_notifications_enabled` | Push token handling, nearby registration, and notification delivery |
| `pilot_coupons_enabled` | Coupon creation, exposure, and redemption |
| `pilot_media_enabled` | Media upload, exposure, and playback |
| `pilot_nsfw_enabled` | Mature-content creation and exposure; must remain false for the pilot |
| `pilot_hunts_enabled` | Hunt creation, exposure, notification, and progression; must remain false for the pilot |

## Manual qualification

- Supported-device smoke record: `Not verified` (attach a completed copy of `supported-device-smoke-runbook.md`).
- Crash-free sessions: `Not verified` (required gate: at least 99.7%).
- Core-action success: `Not verified` (required gate: at least 99%).
- Open critical/high defects: `Not verified`.
- Account deletion/export end to end: `Not verified`.
- Moderation operations and SLA coverage: `Not verified`.

## Rollback plan and drill

| Field | Evidence |
|---|---|
| Previous approved Android artifact/version/hash | Not verified |
| Previous approved iOS artifact/version/hash | Not verified |
| Previous compatible Firebase rules commit | Not verified |
| Previous compatible Functions commit | Not verified |
| Database/storage migrations and reversibility | Not verified |
| Rollback commander and backup | |
| Store rollout pause access confirmed | Not verified |
| Feature-control access confirmed | Not verified |
| Last rollback drill and elapsed time | Not verified |

Rollback procedure:

1. Declare the incident, assign a commander, preserve logs, and pause staged store rollout/invitations.
2. Use verified kill switches to contain creation, notifications, coupons, or media when that reduces impact.
3. Select only the previous approved, hash-verified artifacts and backend/rules revisions listed above.
4. Check schema and client compatibility before any backend or rules rollback; do not reverse an irreversible migration.
5. Deploy through the normal authorized release path, then repeat the critical smoke subset for the affected journey.
6. Confirm recovery in crash/core-action dashboards and record timestamps, approvers, commands or console actions, and evidence links.
7. Keep rollout paused until Engineering, Independent QA, Operations, Product, and Legal record the required approvals.

Immediate pause triggers include confirmed unauthorized access, uncontained harmful-content exposure, inability to delete accounts, critical merchant fraud, or crash-free sessions below 99%.

## Decision

| Approver | Name | Go / No-Go | Evidence / exception | Timestamp |
|---|---|---|---|---|
| Product | | | | |
| Engineering | | | | |
| Independent QA | | | | |
| Operations | | | | |
| Legal | | | | |

Final decision: **No-Go until every required row is verified and all P0 epics are Done.**
