# GeoDrop P0 Execution Status

Assessment date: 2026-07-21  
External-beta decision: **NO-GO**  
Backlog disposition: **0 Done, 7 Blocked, 0 In progress, 0 Not started**

Every P0 now has a complete disposition. The repository work that can be
performed locally is implemented and covered by repeatable checks. None of the
seven rows can honestly be marked `Done` until its production, Legal,
Operations, signing, or physical-device evidence below is recorded.

## Consolidated status

| # | P0 epic | Repository disposition | Evidence still required | Status |
|---:|---|---|---|---|
| 1 | Secure media and group access | Rules, callable group mutations, canonical media paths, migration tooling, and adversarial tests are implemented. | Engineering/QA must deploy atomically, approve and apply the production-data migration, and repeat abuse tests against the release project. | Blocked |
| 2 | Account deletion and export | Android/iOS entry points, recent reauthentication, export/deletion callables, receipts, audit tooling, and the full emulator rehearsal are implemented. | Legal must approve retention; QA must prove password and Google flows, URL expiry/cleanup/retry behavior, and receipts through signed builds in a production-like project. | Blocked |
| 3 | Moderation operations | Severity queue, enforcement, reporter status, appeals, independent overturn, operator CLI, audit events, SLA breach detector, and text/photo/audio/video rehearsal are implemented. | Operations/Legal must assign the roster and channels, approve evidence/escalation policy, and connect and test the production SLA alert. | Blocked |
| 4 | Contextual permissions | Startup prompting is removed; Android/iOS Nearby, notification, background-location rationale, denial, and Settings recovery paths are implemented. | QA must pass the supported physical-device matrix, macOS/iOS build, upgrade cases, approximate/precise cases, and zero-premature-prompt analytics. | Blocked |
| 5 | Legal and privacy consistency | Versioned server manifest, required-route validation, fail-closed Android/iOS clients, server acceptance, and guest-to-account reconciliation are implemented. | Legal/Product must decide the operator, region, minimum age, retention, processors, minors/support policy, approve all copy, and publish all required HTTPS routes. | Blocked |
| 6 | Adult-content launch guard | Server rules/triggers, migration quarantine, notification suppression, NSFW preference shutdown, and fail-closed build-plus-Remote-Config enforcement are implemented. | Engineering/Product/QA must publish and own the production template, apply the migration, and prove API/cache/direct-read/notification/offline paths in signed releases. | Blocked |
| 7 | Release and observability baseline | One Android build system, Node 22 Functions runtime, CI jobs, P0 emulator rehearsals, remote-backed client kill controls with false defaults and realtime activation, and release/rollback templates are implemented. | Engineering/Security/QA must rotate and restrict production credentials, publish and exercise the Remote Config template, produce signed Android/iOS builds, verify crash/core-action telemetry and alerts, run the device matrix, and rehearse rollback. | Blocked |

## Verification evidence

- Functions on Node 22: ESLint and TypeScript build passed locally.
- Firebase rules: group membership, likes, inventory, adversarial Firestore, and
  adversarial Storage suites all passed in the emulators.
- Launch migration rehearsal: passed canonical backfill, unsafe/group media
  quarantine, bearer-token hardening, and mature-preference shutdown using only
  Firestore and Storage emulators.
- Account lifecycle rehearsal: passed private export/payload checks and deletion
  of Auth, nested profile data, username ownership, owned drop/media, inventory
  copies, raw reporter identity, identifier maps, and receipt creation.
- Moderation rehearsal: passed text/photo/audio/video intake plus authenticated
  queue access, Critical triage, removal, suspension, affected-user appeal,
  independent overturn, restoration, reporter status, and audit events.
- Android `testDebugUnitTest`, `lintDebug`, and `assembleDebug` passed after the
  final client feature-gate patch. Lint reports 0 errors and 123 non-blocking
  warnings.
- iOS compilation and physical permission/background-notification behavior cannot
  be verified on this Windows workstation; the repository contains a macOS CI
  build job but no completed remote run is available here.
- No production deployment, migration, signed binary, credential rotation,
  telemetry observation, or device-matrix result was performed from this task.

## Blocking register

| Blocker | Accountable owner(s) | Required closure evidence | Blocks |
|---|---|---|---|
| B1: Production security rollout | Engineering, Independent QA | Approved dry-run output; migration approval; function/rules deployment record; release-project abuse results | 1, 6 |
| B2: Policy and retention approval | Legal, Product, Privacy | Named approver; approved versions; eight live HTTPS routes; retention/backup decisions; matching client/server version record | 2, 3, 5 |
| B3: Moderation operations | Operations, Trust & Safety, Legal | Named lead and appeal reviewer; staffed hours; critical/legal channels; production console access record; fired and acknowledged SLA alert | 3 |
| B4: Release artifacts and device QA | Engineering, Independent QA | Signed Android/iOS artifact hashes; successful CI/macOS run; completed supported-device matrix; Google/password lifecycle proof | 2, 4, 6, 7 |
| B5: Production controls and observability | Engineering, Security, QA | Restricted/rotated credentials; App Check proof; published Remote Config template and observed on/off exercise with propagation times; crash/core-action dashboards; rollback rehearsal | 6, 7 |

## Next handoff, in dependency order

1. Legal/Product close B2 and publish the approved policy origin.
2. Operations/Trust & Safety close B3 and record the staffed launch roster.
3. Engineering/QA close B1 with the approved migration and atomic Firebase
   rollout in the production-like project.
4. Engineering/Security/QA close B5 and attach alert, flag, telemetry, and
   rollback evidence.
5. Independent QA close B4 using signed builds, macOS CI, and the physical-device
   matrix.
6. Change each backlog row from `Blocked` to `Done` only when its linked evidence
   exists. Product may schedule an external beta only after all seven are Done
   and Legal, Operations, Engineering, and Independent QA record Go approval.
