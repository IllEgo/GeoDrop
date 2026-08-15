# Kithe R8 — Rewards and Results

Status: **R8 local/device gate approved; pre-pilot dependencies remain open**  
Date: 2026-08-11  
Scope: Android local/device implementation and undeployed target-server source only. No
backend deployment, production data, Remote Config, Play release, operator code import, or
M4 cutover is authorized by this work.

## Authorization and boundaries

After approving R7, the owner directed the redesign to continue. This authorized R8
local/device work under the already-signed F2 and F4 decisions:

- assigning a unique pre-generated code is `ISSUED`, not proof that the reward was used;
- only the Experience owner may mark an issued code `USED` or correct it back to `ISSUED`;
- the participant keeps the assigned code in Collection and can display it after it has
  loaded, including while offline;
- Pilot 1 uses manual business validation and introduces no merchant/employee account;
- Results are private aggregates with no participant identity; and
- the founder report stays outside the app and uses the same definitions.

The app does not let an organizer invent or upload raw codes. The organizer configures the
reward presentation and inventory limit; the existing `provisionRewardCodes` operation is
admin/operator-only. The debug APK simulates that operator step with a clearly labelled
local pool so the device workflow can be rehearsed without touching production.

## Implemented locally

### Reward authoring and participant receipt

- Standard and Reward are explicit drop types, and type is immutable after publishing so
  issued history cannot become detached from a converted drop.
- Reward authoring collects reward name, business/redemption location, instructions,
  optional terms, and an inventory limit from 1 to 10,000.
- A successful proximity unlock atomically assigns one available code. Duplicate retries
  return the same receipt and do not consume another code.
- Sold-out rewards now follow the approved contract: content still unlocks and saves to
  Collection while the participant is told the reward has run out.
- Success and Collection show only the participant's assigned code. Collection explains
  that a loaded code remains available offline.

### Owner reward operations

- Reward drop cards open owner-only Issued and Used lists.
- Exact-code search, issued/used timestamps, bounded transition history, and idempotent
  actions are supported.
- Mark Used requires an explicit manual-validation confirmation.
- Correction requires one of the fixed reasons `MARKED_BY_MISTAKE` or
  `BUSINESS_CORRECTION`; it clears the current used timestamp but never erases history.
- Status changes are online-only. No offline mutation queue or merchant role was added.

### Private Results

- Experience detail now opens a private aggregate Results surface with Joined, Published
  drops, Unique finders, Total finds, Main Trail completions, Codes issued, and Codes used.
- Per-drop Results use the same unlock-receipt and reward-state definitions.
- Definitions and updated/reconciled timestamps are visible in-app; participant names,
  email addresses, exact locations, and receipt owners are absent.
- The founder report is identified as an outside-the-app Pilot process, not a replacement
  for the organizer's Results.

## Target-server corrections found during R8

Reviewing the undeployed R2 source against the approved R1 contract found four gaps. These
were alignment defects, not new product-direction changes:

1. Sold-out reward inventory raised an error that cancelled content unlock. It now returns
   `UNAVAILABLE` while preserving the unlock receipt.
2. `listRewardCodes` omitted the promised history summaries. It now returns the ten most
   recent timestamped transitions without participant identity.
3. Per-drop `codesIssued`/`codesUsed` counters were incomplete. Issuance, use, and
   correction now update both summary and per-drop Results.
4. The target had no R2 scheduled reconciliation implementation. The new daily reconciler
   recomputes Results only from target memberships, discovery, unlock receipts, Trail
   progress, and reward-code state; it never reads legacy participant maps.

The target `getOrganizerDrop` response also returns owner-authorized reward configuration
needed for safe editing, and server validation now rejects post-publication drop-type
conversion. None of this source has been deployed.

## Verification evidence

- Focused R8 Android suite: 5 tests, 0 failures.
- Full Android gate: 30 suites, 112 tests, 0 failures, 0 errors.
- `compileDebugKotlin`, `lintDebug`, and `assembleDebug`: passed.
- Functions TypeScript build, ESLint, and redesign contract unit check: passed.
- Final debug APK installed successfully on Samsung `SM-S938U`, serial `R5CY114LNCE`.
- Physical walkthrough passed unique issuance (`DEMO-7K4P`), success, Collection, offline
  display with Wi-Fi and mobile data disabled then restored, owner Mark Used, fixed-reason
  correction, full three-event history, summary Results, and per-drop Results.
- Results after correction reconciled to 2 finds, 1 unique finder, 1 code issued, and 0
  codes used; the reward drop showed 1 find, 1 issued, and 0 used.
- Post-walkthrough and final-build launch logs contained no Kithe fatal exception and no
  `Offset.getX-impl` signature.

Automated R8 coverage proves reward validation, issuance, duplicate retry, sold-out
content receipt, offline-retained Collection model, idempotent mark Used, correction
history, participant receipt status refresh, and summary/per-drop reconciliation.

## R8 local/device approval list

- [x] Reward configuration has an explicit, bounded inventory and honest operator-pool
  boundary.
- [x] Unique issuance and duplicate retry are deterministic.
- [x] Sold-out reward content still unlocks.
- [x] Participant success and Collection preserve the assigned code.
- [x] Issued/Used lists, exact search, timestamps, manual validation, and correction
  history are implemented.
- [x] Results use consistent aggregate-only definitions and include per-drop rollups.
- [x] Source authorization keeps provisioning admin-only and status mutations owner-only;
  direct reward documents stay client-denied.
- [x] Daily target Results reconciliation is implemented and locally verified.
- [x] Physical-device interaction: unlock a reward, display the code in Collection, mark
  it Used, correct it, and inspect Results with no fatal crash.
- [x] Owner approved the R8 local/device gate on 2026-08-11 after the complete physical
  reward, offline Collection, status correction, and Results walkthrough.

## Still required before Pilot 1

- Deploy and emulator-test the target Functions/rules only under a separate production
  authorization, including a real non-owner denial and owner receipt synchronization.
- Provision a founder-approved real code list through the admin/operator runbook and
  reconcile its exact inventory before accepting participants.
- Run the offline test across process restart and a poor-network venue condition, not only
  the in-memory debug fixture.
- Reconcile production aggregates from source and compare the founder report before and
  after the event.
- Complete the still-open R5-P, R6 outdoor/private-media, R7 venue/cross-device, R9, and
  R10 gates. R8 approval cannot waive them.
