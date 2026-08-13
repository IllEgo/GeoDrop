# R9 - Account, safety, and operations evidence

Status: **local/device implementation complete; owner approval pending**  
Date: 2026-08-11  
Scope: R9 only. No production deployment, Remote Config change, Play publication,
production-data mutation, M4 cutover, or R10 qualification is authorized by this work.

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

## Evidence not claimed

- The full Auth/Firestore/Storage/Functions moderation rehearsal did not complete in the
  current local emulator run: the emulator stalled while loading an existing legacy
  function trigger before the R9 assertion. No target assertion failed, but this is not a
  pass. A clean full-functions rehearsal remains mandatory before pilot qualification.
- Export generation and permanent deletion were not executed on the review device because
  they affect real account data. Their existing automated lifecycle coverage remains in
  place; release-build and safe-fixture rehearsals remain pre-pilot work.
- No production function, rule, index, configuration, or data was changed.

## R9 local/device approval checklist

- [ ] Accept the Account identity, history, permissions, organizer access, sign-out, and
  lifecycle surfaces.
- [ ] Accept report-status visibility plus server-authorized block/unblock.
- [ ] Accept the target-report moderation bridge, target removal/appeal behavior, private
  rate limits, and expiration cleanup.
- [ ] Accept the fail-closed one-person operations checklist and keep all missing real-world
  assignments open before R10/pilot.
- [x] Reinstall the final corrected APK and confirm the Report/Block host action row plus a
  clean launch log on the physical device.
- [ ] Accept the automated and physical-device evidence, including the explicitly incomplete
  full-functions rehearsal.

Approval closes only the R9 local/device implementation gate. It does not waive R5-P or
the open R6-R9 pre-pilot evidence, and it does not authorize R10 or production actions.
