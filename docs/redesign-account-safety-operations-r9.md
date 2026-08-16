# R9 - Account, safety, and operations evidence

Status: **local/device gate approved; pre-pilot dependencies remain open**
Date: 2026-08-11  
Scope: R9 only. No production deployment, Remote Config change, Play publication,
production-data mutation, M4 cutover, or R10 qualification is authorized by this work.

Approval record: on 2026-08-13 the owner approved every R9 local/device checklist item
while explicitly keeping the pre-pilot dependencies open. This approval accepts the
documented local and physical-device evidence; it does not convert the incomplete
full-functions rehearsal or missing operational assignments into passing evidence.

## Direction check

R9 does not require a fundamental change to `product-direction.md` or
`migration-plan.md`. It implements their non-negotiable account lifecycle, report/block,
moderation-queue, and rate-limit requirements without adding account types, social
features, merchant tooling, or a new public-creation path.

One necessary implementation correction was found: the target `submitReport` callable
wrote to `safetyReports`, while the existing moderation intake listened only to the
legacy `reports` collection. A report could therefore be accepted without entering the
operator queue. R9 adds target-report intake to the existing moderation case workflow.
This closes a safety requirement; it does not change product direction.

## Implemented locally

### Account

- Account identifies the signed-in participant in plain user-facing language.
- Joined Experiences show name, organizer, date range, code, availability, and the
  participant's current permissions.
- Organizer access and tools remain inside Account and continue to use the existing
  approved internal authorization model.
- Permission recovery, sign-out, export, and permanent deletion remain connected to the
  governed lifecycle behavior. Export and deletion still require recent authentication;
  private export links expire after 15 minutes.
- Retired Explorer/Business wording was removed from the R9 account and lifecycle copy.

### Safety

- Submitted target reports now enter the moderation case queue and appear in Account
  with a recent status.
- Blocking is server-authoritative. Account lists readable blocked-host labels and can
  call the server-authorized unblock operation.
- Moderator removal updates the target drop's moderation state; a successful appeal can
  restore only the same case-marked target.
- Fixed-window callable limits protect experience resolution, unlock, report, block, and
  unblock operations. Subjects are HMAC/hash protected, stored in a client-inaccessible
  collection, and expired records are included in scheduled cleanup.

### One-person pilot operations

- The R9 operations contract checks that target report intake, case provenance, target
  removal/appeal behavior, unblock, and private rate-limit boundaries stay present.
- The readiness command is fail-closed for named trust-and-safety ownership, staffed
  hours, critical escalation, legal escalation, independent appeal review, alert
  ownership, and the participant support channel.
- Local readiness intentionally reports those real-world assignments as missing until
  the owner supplies them. They are pre-pilot dependencies, not silently approved
  placeholders.

Commands:

```text
cd functions
npm run test:r9:operations
npm run r9:readiness:local
npm run r9:readiness
```

The last command requires the named `GEODROP_*` operational assignments and is intended
for pre-pilot qualification, not local UI development.

## Verification evidence

- Android: `testDebugUnitTest`, `lintDebug`, and `assembleDebug` passed; 118 tests, zero
  failures.
- Backend: TypeScript build, lint, redesign unit contract, R9 operations contract, and
  local readiness validation passed.
- Firestore/Storage: the emulator boundary suite passed, including denial of direct
  client access to callable rate-limit records.
- Physical Samsung SM-S938U: the R9 review APK installed and launched. Account displayed
  joined-Experience detail, availability, permissions, organizer access, and safety.
  A local report appeared as `Received`; block showed the readable host label; unblock
  cleared it. The export/deletion reauthentication surface was reviewed without invoking
  either destructive lifecycle operation.
- A device-found bottom-inset defect was then corrected so Report and Block host actions
  sit above system navigation. The corrected APK passed the full automated Android gate,
  installed successfully, and passed the focused action-row smoke test. Both actions ended
  at y=2161 while system navigation began at y=2214, leaving a 53-pixel clear gap; Report
  opened its reason dialog and was dismissed without submitting.
- No fatal exception or `Offset.getX-impl` crash appeared in the completed R9 device
  walkthrough log.

## Full-functions rehearsal resolved 2026-08-16

The previously incomplete Auth/Firestore/Storage/Functions rehearsal now passes end to
end. Investigating it found three defects, not an environment problem, and the same
failure had been red on CI's `P0 lifecycle emulator rehearsals` job since 2026-08-13:

1. **The deletion-policy version had drifted apart in three places.** R2 bumped the server
   `ACCOUNT_LIFECYCLE_POLICY_VERSION` to `pilot-redesign-r2-2026-08-09-draft`, while
   `AccountLifecycleRepo.POLICY_VERSION` (Android), `AccountLifecycleService.policyVersion`
   (iOS), and the lifecycle rehearsal all still sent the legal-bundle string
   `pilot-2026-07-21-draft`. Every export and deletion request was answered with
   `POLICY_VERSION_MISMATCH`, which is why the chain aborted at `account:rehearse` and why
   the two destructive surfaces could not have been demonstrated on the review device even
   if the session had attempted them. Nothing reached production, because the R2 server is
   not deployed. Both clients now send the server's value, the rehearsal derives it from
   source instead of repeating a literal, and `test:r9:operations` fails if the three ever
   disagree again.
2. **`ingestModerationReport` died on a report that had been deleted underneath it.**
   Account deletion removes the reports a user filed, so `batch.update(sourceRef, …)` could
   raise `5 NOT_FOUND`, kill the function, and drop the connection. It now skips ingestion
   when the source is gone rather than resurrecting deleted report data.
3. **The legacy `analyzeOnUpload` trigger called the real Vision API from the emulator.**
   With no valid project it failed unhandled after ~1s, and on a network that blackholes
   the request rather than refusing it, that call is a hang — the best available explanation
   for the original "stalled while loading a legacy function trigger" symptom. Server
   SafeSearch is now skipped under `FUNCTIONS_EMULATOR`, and remains a production signal.

A fourth item was noise rather than a defect: the Functions emulator reads
`ANALYTICS_HMAC_SECRET` from production Secret Manager while loading each declaring
trigger. The code already has a deliberate emulator fallback, so the 403 was harmless on a
networked machine but is a second hang candidate on a restricted one. The gitignored
`functions/.secret.local` step is now recorded in `moderation-operations-draft.md`.

Result: all six rehearsals — launch migration, account lifecycle, guest merge, moderation,
prototype wipe, and account roles — pass in one run with no killed function, no dropped
connection, and no outbound Google API call. Android re-verified at 130 unit tests, zero
failures. `test:redesign:unit` and `test:r9:operations` existed but ran nowhere; they are
now part of the `Functions lint and build` CI job. The iOS constant is a one-line literal
change and is compile-verified by CI only.

## Evidence not claimed
- Export generation and permanent deletion were not executed on the review device because
  they affect real account data. Their existing automated lifecycle coverage remains in
  place; release-build and safe-fixture rehearsals remain pre-pilot work. That device
  rehearsal is now unblocked by the policy-version correction above, but it has not been
  run.
- No production function, rule, index, configuration, or data was changed.

## R9 local/device approval checklist

- [x] Accept the Account identity, history, permissions, organizer access, sign-out, and
  lifecycle surfaces.
- [x] Accept report-status visibility plus server-authorized block/unblock.
- [x] Accept the target-report moderation bridge, target removal/appeal behavior, private
  rate limits, and expiration cleanup.
- [x] Accept the fail-closed one-person operations checklist and keep all missing real-world
  assignments open before R10/pilot.
- [x] Reinstall the final corrected APK and confirm the Report/Block host action row plus a
  clean launch log on the physical device.
- [x] Accept the automated and physical-device evidence, including the explicitly incomplete
  full-functions rehearsal.

The 2026-08-13 approval closes only the R9 local/device implementation gate. It does not
waive R5-P or the open R6-R9 pre-pilot evidence, and it does not authorize R10 or production
actions.
