# GeoDrop Redesign Alignment Proposal

Status: **R0-R5-L approved; R5-P deferred; R6-R8 local/device approved; R9 local/device implementation complete, approval pending; R10 not authorized**  
Date: 2026-08-11  
Scope: alignment of the 13 design/UX specifications added to `docs/` with
`product-direction.md`, `migration-plan.md`, the signed migration decisions, and the
current pre-pilot implementation.

Approval record: on 2026-08-09 the owner directed the work to continue after presentation
of F1-F7 and approved all recommended options, the section 4 resolutions, and the R0-R10
sequence. R0 amended the governing documents and marked conflicting draft language as
superseded. No application, rules, function, schema, deployment, or production-state change
was made in R0. A target described here is not live until its later implementation gate
closes.

R3 implementation and approval evidence is recorded in
`redesign-android-foundation-r3.md`. The owner approved the complete R3 gate on 2026-08-10
after installing the verified build on a physical device and authorized R4. R4 was
implemented with passing evidence in `redesign-navigation-shell-r4.md`; the owner approved
the complete gate on 2026-08-10 after the verified R4 build was reinstalled and launched
  on the physical device, explicitly authorizing R5. R5 is implemented locally and on-device.
  On 2026-08-10 the owner split its gate: R5-L local/device evidence is approved, while R5-P
  is deferred until the owned host, Play path, deployment approval, and clean-device matrix
  recorded in `redesign-entry-guest-permissions-r5.md` are ready. R5-P remains mandatory
  before pilot/public release. No backend rollout, production-data, Remote Config, Play
  publication, or M4 work is implied by these approvals.

On 2026-08-10 the owner explicitly authorized **R6 local implementation**, beginning with
the participant-loop crash-stability checkpoint. This authorization does not include R5-P,
production deployment, backend enablement, Remote Config changes, Play publication,
production-data mutation, M4, R7, or any later task.

On 2026-08-11 the owner approved the implemented R6 local/device participant experience
after reviewing the working physical-device map and relocated debug demo. Remaining
server, private-media, accessibility, and outdoor qualification evidence stays open in
`redesign-participant-loop-r6.md`; that approval did not authorize production work.

The owner then directed the work to continue, authorizing R7 local implementation. The
approved-only Experiences and core text/photo authoring surface is installed and device-
smoke tested. On 2026-08-11 the owner approved the R7 local/device gate after the physical
organizer-to-Explorer discovery path passed. Timed venue, unapproved-server-denial, and
cross-device/production evidence remains explicitly open in
`redesign-organizer-authoring-r7.md`. This approval does not authorize R8 or production
actions.

The owner then directed the work to continue, authorizing R8 local/device implementation.
Reward/Results work is implemented, verified, installed, and passed its physical
interaction walkthrough. The owner approved the R8 local/device gate on 2026-08-11.
Evidence and remaining production dependencies are recorded in
`redesign-rewards-results-r8.md`. This authorization does not include backend deployment,
production data, Remote Config, Play publication, M4, R9, or later work.

The owner then directed the work to continue, authorizing R9 local/device implementation.
Account history and lifecycle surfaces, report-status and block/unblock safety controls,
the target-report moderation bridge, private callable rate limits, and fail-closed pilot
operations readiness are implemented and verified locally. The R9 walkthrough and final
bottom-inset correction passed on the physical review device. Physical-device evidence and
the deliberately open pre-pilot dependencies are recorded in
`redesign-account-safety-operations-r9.md`. R9 owner approval is pending. No production
action or R10 qualification is authorized.

## 1. Decision authority

When the documents disagree, use this order:

1. `product-direction.md` defines the product and the closed v1 boundary.
2. Signed decisions and completed/deployed work in `migration-plan.md`,
   `migration-decisions.md`, and `account-model.md` define current safety and production
   constraints.
3. The 13 new design documents define the desired experience, terminology, visual system,
   accessibility standard, and candidate workflows. Their status is mostly draft; they do
   not silently reopen v1 scope.
4. Within the new documents, `voice-and-glossary-v1.md` is the terminology authority and
   the most specific flow owns its screen behavior, except where this proposal explicitly
   resolves a conflict.

The review covered:

- `accessibility-standard-v1.md`
- `account-tab-flow.md`
- `design-entry-and-unlock-v1.md`
- `design-flows-and-ia-v1.md`
- `design-system-v1.md`
- `drop-authoring-flow.md`
- `experience-creation-flow.md`
- `geodrop-product-spec-v1.md`
- `geodrop-wireframe-spec-v1.md`
- `organizer-access-request-flow.md`
- `redemption-tracking-flow.md`
- `top-level-navigation.md`
- `voice-and-glossary-v1.md`

## 2. Proposed launch contract

The redesign should make GeoDrop feel like a quiet, trustworthy local guide for a bounded
event experience. It should not restore the prototype's social-map identity.

### 2.1 Adopt for Pilot 1

- Android-first redesign, while preserving the iOS project and shared-backend compatibility.
- One shared participant shell with **Nearby**, **Collection**, and **Account** tabs.
- One active Experience at a time, with an Experience switcher and code/QR join path.
- Invite-only Experiences backed initially by the existing `groups` storage model. Use
  "Experience" everywhere in user-facing copy; avoid a risky collection rename during the
  pilot migration.
- A **Trail** as the ordered set of drops inside an Experience. Reuse the existing
  hunt-chain/progress concepts behind an adapter, but remove "hunt" from participant copy.
- Guest preview and browsing, followed by an account gate at the first unlock attempt.
  Resume the exact pending unlock after authentication. Guests cannot create, unlock,
  collect, or redeem.
- Approximate, one-shot location for browse; precise, one-shot location only after the user
  presses the unlock action; no background or continuous location.
- Equal, accessible map and list access. The list remains useful when location is denied.
- Text and single-photo drops, expiration, persistent Collection receipts, report/block,
  creator traceability, scoped notifications, reward codes, and organizer analytics.
- Approved-organizer-only event creation for Pilot 1. Internally, approved Organizer maps
  to the existing `BUSINESS` role; do not add another account type.
- The visual tokens, state language, externalized copy, dark mode, touch targets, dynamic
  type behavior, screen-reader semantics, reduced motion, and contrast requirements in the
  new design/accessibility standards.
- Audio is cut from Pilot 1. Video remains prohibited.

### 2.2 Defer unless separately approved

These are useful ideas, but they are not in the closed v1 list or they introduce avoidable
backend complexity before the core loop is proven:

- Scheduled future publishing of drops.
- Offline queued organizer publishing and offline reward-status mutation. Cached reading
  is allowed; authoritative writes should fail clearly and be retried online for Pilot 1.
- Thirty-minute propagation of edits into already-earned Collection items. Pilot 1 should
  store an immutable payload snapshot at unlock. Later edits affect future unlocks and show
  an "Edited" marker/version, but never rewrite an earned item.
- Self-service organizer approval, identity verification, a trial organizer tier, merchant
  accounts, merchant scanners, or a business-facing redemption app.
- Shareable analytics images, formal exports, reusable templates, bulk authoring, and
  advanced Trail authoring.
- In-app guest lists or targeted/per-guest messaging.
- Full iOS visual parity before the Android pilot is accepted.

Basic cloud-saved organizer drafts are a conditional exception: add them only if the
timed venue walkthrough proves that interrupted authoring is a real Pilot 1 blocker. Do
not build the more complex offline queue in advance of that evidence.

## 3. Fundamental decisions requiring approval

### F1 - Move proximity unlock from client-enforced to server-authoritative

**Conflict:** `migration-plan.md` records that proximity is currently client-enforced and
that no callable or rule verifies where the collector was. The new product and wireframe
specs require server validation and require locked payloads not to be retrievable before a
successful unlock.

**Recommended option:** introduce a callable `unlockDrop` and split discovery metadata
from locked payload data.

- A readable discovery record contains only what Nearby needs: drop id, Experience id,
  location, state/type indicators, active window, and non-secret map/list metadata.
- Locked title/body/photo/reward payload lives in a server-only document.
- `unlockDrop` requires a non-anonymous account, receives one precise one-shot fix plus its
  age/accuracy, validates Experience membership, active window, configured radius, prior
  receipt, and abuse limits, and returns the payload only after success.
- Persist an unlock receipt and immutable payload snapshot/version, never the submitted
  coordinates or a location trail.
- Do not log request coordinates. Add App Check/Play Integrity and rate limiting. State
  honestly that server validation still relies on client-reported GPS and is not
  cryptographic proof of physical presence.

**Alternative A:** retain client-only proximity for Pilot 1. This is faster, but it
contradicts the new locked-payload requirement and leaves paid rewards easy to spoof.

**Alternative B:** add device-attestation-heavy anti-spoofing now. This raises cost and
false-rejection risk beyond the pilot's needs and is not recommended.

**Required governing change if approved:** amend the 4.2 statement in
`migration-plan.md`, add a new signed migration decision, and sequence the server/rules
change before the redesigned unlock UI.

### F2 - Separate reward issuance from actual business redemption

**Conflict:** the deployed `redeemDrop` callable currently generates a code and immediately
increments `redemptionCount`. It therefore measures codes issued, not codes actually used
at a business. The new product and redemption specs require distinct issued and redeemed
states, manual business validation, correction history, and both counts.

**Recommended option:** use a two-state reward receipt.

- Pre-generate a unique code pool per reward so a pilot business can receive a founder-
  supplied validation list before the event.
- On reward unlock, assign/reserve one code server-side and record `issuedAt`. Issuance
  consumes limited inventory so GeoDrop never promises more rewards than exist.
- An approved organizer later calls an owner-authorized `markRewardCodeUsed` operation,
  recording `usedAt`, actor, and a small correction history. A deliberate correction can
  return the state to issued without creating a second code.
- Rollups expose `issuedCount` and `usedCount`; do not label issued codes as redemptions.
- Treat existing pre-pilot receipts as **issued**, not proven used, during migration.

**Alternative:** keep issuance as a redemption proxy for Pilot 1 and label the number
"codes issued." This is smaller, but it cannot answer whether the business delivered the
reward and conflicts with the tracking flow.

**Required governing change if approved:** supersede migration decision P6's terminology,
update P7's rollup fields, and migrate without exposing or invalidating already-issued
codes.

### F3 - Restrict Pilot 1 creation to approved organizers

**Conflict:** the current account model permits authenticated `EXPLORER` accounts to create
community drops. The new IA contains no participant creation surface and requires a manual
organizer request/approval gate. `product-direction.md` requires organizer control inside
events but does not explicitly prohibit authenticated public creation.

**Recommended option:** during Pilot 1, only an existing `BUSINESS` account that owns the
Experience may create or mutate its drops. "Organizer" is the product term; `BUSINESS`
remains the internal role to avoid a third account type.

- Start with a low-key Account link to an external application form.
- Founder review promotes an approved account through a server/admin operation.
- If in-app pending/denied states are needed, add a server-owned
  `organizerAccessStatus`; it is workflow state, not another role.
- No organizer capability exists before approval. No ID verification in Pilot 1.

**Alternative A:** keep community creation in the backend but hide it with a feature flag.
This preserves future flexibility but is weaker than a rules-layer pilot boundary.

**Alternative B:** founder-only content seeding with no application flow. This is safest
and smallest, but it does not test whether organizer tooling can become self-service.

**Required governing change if approved:** update `account-model.md` and Firestore/callable
authorization before adding organizer UI.

### F4 - Keep the existing organizer dashboard or replace it with founder reports

**Conflict:** `geodrop-product-spec-v1.md` and `experience-creation-flow.md` say Pilot 1
analytics are a founder-produced post-event report and explicitly omit an in-app Results
section. `design-flows-and-ia-v1.md` designs a Results surface. Migration task 4.4 already
deployed private server rollups and shipped Android/iOS organizer dashboards.

**Recommended option:** retain the secure, aggregate in-app Results view because the work
already exists and organizer analytics are launch scope in `product-direction.md`. Keep it
small: drops, unlocks, reward codes issued, and reward codes used. A founder-produced
post-event report can supplement it with interpretation and qualitative findings. Never
show attendee identity.

**Alternative A:** hide the dashboard for Pilot 1 and use only founder reports. This
matches the design product spec but discards an already-working load-bearing feature.

**Alternative B:** expand Results with charts and shareable images now. This is unnecessary
redesign scope and is not recommended.

### F5 - Make Pilot 1 Android-first without deleting iOS

**Conflict:** several new specs explicitly choose an Android-only pilot, while the
migration plan has been maintaining both clients and has already shipped shared feature
work to each. `product-direction.md` does not choose a platform.

**Recommended option:** redesign and device-qualify Android first. Keep iOS source,
security behavior, and shared-backend compatibility intact; require compile coverage but
defer the iOS visual redesign until the Android outdoor pilot gate passes. Do not delete
iOS or let backend changes silently break installed iOS builds.

**Alternative:** require full Android/iOS redesign parity at every step. This reduces later
catch-up but roughly doubles the UI validation surface before the first event.

### F6 - Do not add scheduled drops to the closed v1 scope

**Conflict:** `drop-authoring-flow.md` explicitly reopens scheduled publishing as v1. It
also says the product spec previously listed it as a non-goal. Scheduled publishing is not
in the closed launch list in `product-direction.md`.

**Recommended option:** defer it. Keep immediate publish plus expiration. Record the
authoring and server-transition design as post-pilot backlog, with a revisit trigger only
if the pilot format demonstrably needs timed reveals.

**Alternative:** explicitly amend `product-direction.md` to add scheduled drops, then build
server time-window enforcement, scheduled notification behavior, timezone handling, and
failure recovery. Do not implement only the date picker.

### F7 - Use a real Play-distribution path for the QR cold-install funnel

**Conflict:** the entry spec correctly treats QR -> install -> restored Experience as one
funnel. A closed-testing enrollment screen or manually installed APK changes that funnel
and cannot validate the advertised guest experience. The migration plan currently proves
internal/device behavior but does not settle public Play distribution.

**Recommended option:** publish a fail-closed production Play build before the real pilot.
The app may be publicly installable while Experiences remain invite-only and controlled by
codes. Use an owned-domain App Link and landing page, Play Install Referrer, and event-code
fallback. Run the full path with a device that has never installed GeoDrop.

**Alternative:** use Play open testing and disclose the opt-in step as part of the measured
funnel. This is operationally safer but does not represent the final cold-install flow and
will add avoidable abandonment.

Do not use a sideloaded APK or a closed-test-only account flow as evidence that QR
onboarding is ready for attendees.

## 4. Resolved specification inconsistencies

Unless the founder chooses otherwise at this gate, implementation should use these
resolutions:

| Inconsistency | Proposed resolution |
| --- | --- |
| `Map` vs `Nearby`; `You` vs `Account` | **Nearby**, **Collection**, **Account**. |
| Hunt vs Trail | Trail is user-facing and metric-facing; hunt may remain only behind an adapter during migration. |
| Explorer/Organizer role copy | "Guest" and "host" where natural; Organizer in professional/account tooling; Explorer remains internal only. |
| Account at Join vs first unlock | Preview and enter as guest; require an account at first unlock; preserve and resume the exact target. |
| Approximate permission immediately vs contextually | Load the Experience/list without location, then show a contextual "Show nearby distances" primer when the user invokes location. Precise permission appears only after Unlock is pressed. |
| Push prompt at join/map/first success | Ask after the first successful unlock. Notifications remain scoped to joined Experiences and are never proximity alerts. |
| Live distance in unlock sheet | A coarse distance band may be shown before the attempt. Do not stream a live precise distance; acquire one precise fix only after Unlock is pressed. |
| Multiple Experiences vs one active Experience | Membership may include many; the participant shell has exactly one active Experience at a time. Collection combines all and labels each item. |
| Experience vs existing group schema | User-facing Experience backed by the existing invite-only group schema for Pilot 1. |
| Edited earned content | Immutable unlock-time snapshot for Pilot 1; future unlocks can receive a later version. No 30-minute retroactive rewrite. |
| Organizer analytics UI vs report | Keep the existing aggregate Results view; add a founder report operationally. |
| App entry technology | Android App Links on an owned domain, `/e/<code>` web landing, Play Install Referrer, and a human event code fallback; do not use Firebase Dynamic Links. |
| Default unlock radius | New drops default to 25 m and remain organizer-configurable. Existing drops without a radius retain the current 30 m interpretation until outdoor calibration approves a migration; accuracy handling stays explicit. |
| Pilot size and interpretation | Invite within the governing 50-150-attendee range, require at least 40 eligible Android participants if feasible, report absolute funnel counts, and treat five interviews/qualitative findings as primary. Existing percentage thresholds remain directional, not statistically conclusive. |
| Pilot content shape | Use 10-20 drops, one reward, and one designated main Trail. Text and photos satisfy the multimedia need; audio does not block Pilot 1. |

## 5. Step-by-step redesign process

Each numbered task is one reviewable implementation unit and ends at its gate. Do not roll
into the next task without sign-off.

### R0 - Approve and normalize the specification

1. Decide F1-F7.
2. Amend only the governing passages affected by approved reversals.
3. Mark conflicting draft passages as superseded and link them to the signed decision.
4. Freeze a Pilot 1 scope matrix: launch, conditional, deferred.

**Gate:** founder signs the decision record and there is one unambiguous product contract.

### R1 - Define backend and analytics contracts

1. Document the Experience facade over `groups`, Trail facade over existing progress data,
   public discovery record, private payload, unlock receipt, payload version, organizer
   access state, and two-state reward receipt.
2. Specify callable request/response/error contracts for unlock, organizer approval,
   reward issuance/use/correction, Experience join, and account lifecycle.
3. Define the Pilot 1 event taxonomy and owners (client vs server), including invitation,
   preview, activation, unlock attempt/result, account gate, Trail completion, code issued,
   code used, permission outcomes, and report/block.
4. Write migration/backfill/rollback rules before schema changes.

**Gate:** schema/security review proves no locked payload, exact location, attendee identity,
or organizer-only metric becomes broadly readable.

### R2 - Build the server boundary first

1. Add the private payload and immutable unlock receipt model.
2. Add and test `unlockDrop` with expiry, radius, accuracy, staleness, membership, idempotency,
   App Check, and rate limits.
3. Enforce organizer ownership on Experience/drop writes.
4. Split code issuance from confirmed use and update aggregate rollups.
5. Add emulator tests, migration rehearsal, dry run, and explicit production deployment plan.
6. Preserve compatibility or fail closed for older Android/iOS builds.

**Gate:** an unauthenticated/anonymous/non-near/non-owner client cannot retrieve payload,
forge an unlock receipt, create an event drop, consume inventory twice, or mutate Results.

### R3 - Establish the Android design foundation

1. Implement design tokens for color, type, spacing, shape, elevation, state, and motion.
2. Externalize strings and verify Hawaiian diacritics and 40% text expansion.
3. Build accessible shared components: DropCard, DropPin, UnlockButton, ResultSheet,
   PermissionPrimer, TrailStrip, StatCard, CodeDisplay, and EmptyState.
4. Add light/dark/system themes, non-color state cues, 48 dp targets, reduced motion, and
   screenshot/accessibility checks.

**Gate:** component catalog passes light/dark, 200% font, screen reader, contrast, reduced
motion, and compact-device review before screens are migrated.

### R4 - Replace the navigation shell

1. Add Nearby, Collection, and Account as the only participant tabs.
2. Add the active-Experience header/switcher and no-Experience join empty state.
3. Put Organizer tools under Account for approved organizers; do not add a fourth tab or a
   separate permanent mode.
4. Preserve state across tab and Experience switches.

**Gate:** a participant and an approved organizer can each reach every allowed surface,
and neither sees unauthorized actions.

### R5 - Implement entry, guest, account, and permissions

1. Build owned-domain App Links, web fallback, Install Referrer recovery, event codes, and
   QR/share artifacts.
2. Show Experience preview before auth or permission prompts.
3. Enter view-only guest browsing; gate the first unlock; link/merge auth safely; resume the
   same pending unlock.
4. Use contextual approximate-location primer, precise primer at unlock, push primer after
   first success, and useful denial/settings recovery states.
5. Instrument every branch and preserve the Experience target through install/sign-in.

**R5-L gate — local/device implementation:** compilation, automated coverage, installed
physical-device preview, entry parsing, guest/account transitions, permission recovery, and
exact-resume behavior pass without requiring a live host or production mutation.

**R5-P gate — production funnel:** the app-owned HTTPS host, release `assetlinks.json`, Play
installation path, reviewed deployment/rollback bundle, safe Experience/account fixtures,
and the complete installed/not-installed external matrix all pass. R5-P may be deferred so
R6-R9 local work can proceed under their own gates, but it must close before R10 can
authorize a pilot or public release.

### R6 - Rebuild the participant loop

Implementation record: `redesign-participant-loop-r6.md` (authorized and in progress).

1. Implement equal map/list browsing using non-secret discovery records.
2. Add locked/near/found/expired visual and semantic states.
3. Implement explicit unlock, all specified failure states, success payload, Collection
   snapshot, Trail advancement, and next-drop guidance.
4. Keep Collection combined across Experiences with Experience/date labels and durable
   cross-device receipts.
5. Add report/block at content and creator surfaces.

**Gate:** outdoor Android test proves one-shot precise access, acceptable GPS behavior,
locked payload secrecy, no location retention, screen-reader completion, and recovery from
every unlock failure.

### R7 - Rebuild organizer access and core authoring

1. Add the request/pending/approved/denied Account states using the approved Pilot 1
   application process.
2. Add Experiences list, create/edit form, date range/timezone, join QR/code, and detail.
3. Add fast text/photo authoring with pin/current-location placement, configurable radius,
   expiry, image compression, alt text, edit/delete, and honest save states.
4. Time a real 8-20-drop venue walkthrough. Add basic drafts only if the conditional gate
   is triggered; do not add scheduled publishing.

**Gate:** an approved organizer creates an Experience and a valid drop in under three
minutes; unapproved users cannot write either; the full 8-20-drop authoring cost is recorded.

### R8 - Finish rewards and Results

1. Add reward configuration and pre-generated code-pool operations.
2. Show the assigned code in the participant success/Collection surfaces and make it
   available offline after issuance.
3. Add organizer issued/used lists, search, timestamps, correction history, and owner-only
   status actions. Defer merchant access.
4. Update Results to show consistent aggregate definitions and add the founder report
   process outside the app.

**Gate:** rehearse issue, duplicate retry, sold-out, manual validation, mark used,
correction, non-owner denial, offline code display, and rollup reconciliation.

### R9 - Complete account, safety, and operational surfaces

1. Finish Account identity, joined-Experience history, permission recovery, sign-out,
   export, and deletion using the already-governed lifecycle behavior.
2. Verify report/block/moderation/rate-limit operations rather than redesigning only the
   visible buttons.
3. Replace remaining retired terms and any social/gamified copy.

**Gate:** moderation, deletion, export, organizer review, reward reconciliation, and event
support can be operated by one person at pilot volume.

### R10 - Instrument, qualify, and run Pilot 1

1. Validate the funnel against the revised activation definition and Experience-based
   north-star event.
2. Run automated unit/rules/function/UI/accessibility tests and real-device outdoor tests.
3. Run a small rehearsal before the paid event, including sunlight, poor network, denied
   permissions, GPS drift, compact screen, 200% font, TalkBack, and reward operations.
4. Use qualitative-primary reporting with absolute counts for the small Android pilot;
   record authoring/support/moderation load and GPS frustration.
5. Hold the iOS redesign gate after Android evidence, then decide whether to port, revise,
   or reposition.

**Gate:** founder accepts the pilot-readiness checklist and explicitly enables only the
required fail-closed flags.

## 6. Approval checklist

Owner decisions recorded 2026-08-09:

- [x] F1: server-authoritative unlock and private payload split
- [x] F2: issued vs used reward states
- [x] F3: approved-organizer-only Pilot 1 creation
- [x] F4: retain the aggregate in-app Results view
- [x] F5: Android-first redesign with iOS preserved
- [x] F6: scheduled drops deferred
- [x] F7: production Play distribution for the real QR funnel
- [x] Accept the resolved inconsistencies in section 4
- [x] Accept the ordered task/gate sequence in section 5

**R0 gate result:** complete. `product-direction.md`, `migration-plan.md`,
`migration-decisions.md`, and `account-model.md` now record the approved target and clearly
separate it from deployed behavior. Conflicting draft specs carry explicit R0 overrides.

**R1 gate result:** complete. On 2026-08-09 the owner approved the complete backend,
security, data-migration, callable, analytics, retention, and cutover contract in
`redesign-backend-contracts-r1.md` by directing the work to continue.

**R2 gate result:** approved in full on 2026-08-10; evidence is in
`redesign-server-boundary-r2.md`. The additive
server boundary, adversarial verification, migration rehearsal, lifecycle coverage, and
deployment/rollback plan are implemented and passing locally. Production deployment,
Remote Config enablement, destructive migration, and the M4 point-of-no-return remain
separate approval gates.

**R3 gate result:** complete. Evidence is in `redesign-android-foundation-r3.md`.

**R4 gate result:** complete. The three-tab participant shell, Experience context,
Account-contained organizer entry, authorization behavior, and state restoration passed
the gate in `redesign-navigation-shell-r4.md`. The owner approved all R4 items on
2026-08-10 and authorized R5. R5-L is approved and complete. The owner approved deferring
R5-P on 2026-08-10 without waiving it; it remains a pre-pilot/public-release gate. The owner
then explicitly authorized R6 local implementation on 2026-08-10 and approved the
implemented local/device experience on 2026-08-11. The R6 production/outdoor qualification
items remain open. The owner approved R7 local/device implementation on 2026-08-11 and
then authorized R8 local/device implementation. R8 has passing automated and physical
walkthrough evidence and was approved by the owner on 2026-08-11. R9 local/device work is
implemented with automated and physical walkthrough evidence; owner approval is pending
under `redesign-account-safety-operations-r9.md`. R10 and all production actions remain
unauthorized.
