# GeoDrop R2 — Additive Server Boundary and Gate Evidence

Status: **Approved — R2 gate complete; R3 client foundation authorized**  
Date: 2026-08-09  
Authority: approved R1 contract in `redesign-backend-contracts-r1.md`  
Production state: **not deployed; no production data was read or changed; no legacy
cutover was performed**

Approval record: on 2026-08-10 the owner approved every R2 gate item, including the
additive server boundary, organizer semantics, security model, lifecycle/retention
defaults, migration safeguards, intentional M4 deferrals, verification evidence, and R3
authorization. This approval does not authorize production access, deployment, rollout
enablement, data migration, or M4.

## 1. Outcome

R2 now exists as an additive backend boundary that can support the redesigned client in
later tasks without trusting the client for payload entitlement, location decisions,
Trail progress, reward inventory, organizer approval, likes, Results, or analytics facts.

The implementation deliberately leaves the current `drops`, `inventory`, `huntProgress`,
`redemptions`, and legacy group paths intact. This is the R1 M1 rollback guarantee, not a
hybrid target design. Their reads/writes can be denied only at the separately approved M4
point of no return after the target clients and migration evidence exist.

## 2. Implemented boundary

### Entry, account, and organizer access

- Added Experience preview, join, and leave callables over schema-v2 `groups` records.
- Added opaque 30-minute organizer application tokens, a single-use external form
  ingestion endpoint, and admin/operator decisions.
- Enforced `BUSINESS` if and only if organizer status is `APPROVED` on all new decisions.
- Changed `updateBusinessProfile` so verified email cannot self-promote an account. It now
  requires an already approved organizer and maintains `creatorProfiles/{uid}`.
- Added the safe creator projection and direct-denied application/application-token data.

### Experience authoring and private payloads

- Added approved-organizer Experience creation/update with server-generated invitation
  codes and OWNER membership.
- Added immediately published text/photo drop create/edit/delete and organizer read.
- Added allowlisted `experienceDrops` discovery records, immutable private payload
  versions, a 25 m new default radius, and 30 m migration default.
- Added private image staging (10 MiB, non-anonymous owner, JPEG/PNG/WebP), server promotion
  to direct-denied payload storage, token-scrubbing metadata normalization, 24-hour staging
  cleanup, and 10-minute Collection media URLs.

### Unlock, Trail, reward, and Results integrity

- Added App Check-enforced, non-anonymous `unlockDrop` with strict request fields,
  membership/active-window/expiry/safety checks, at-most-30-second location age,
  `accuracyM <= min(radiusM, 30)`, Haversine distance, bounded accuracy allowance, and only
  the approved `0_25`, `25_50`, or `50_PLUS` failure bucket.
- Added idempotent immutable unlock receipts and snapshot-based Collection payloads; no
  submitted or computed location is stored or returned.
- Added server-only Trail progress and step enforcement. Results count a completion only
  when the completed Trail is designated `isMain`.
- Added admin reward provisioning, transactional AVAILABLE-to-ISSUED assignment, owner
  use confirmation, correction history, private participant receipts, random receipt
  linkage, inventory bounds, and aggregate issued/used counters.
- Added server-owned likes, reporting, host blocking, feedback, owner-only Results, and
  direct-denied participant identity/linkage resources.

### Analytics and lifecycle

- Added the exact 21-event registry and an append-only direct-denied Firestore ledger.
- HMAC-protected actor/resource keys require the managed `ANALYTICS_HMAC_SECRET`; events
  reject arbitrary params and never store coordinates, codes, payload, uid, or narrative.
- Added event dedupe, 180-day raw-event/reward-audit cleanup, application-token cleanup,
  90-day declined-application cleanup, and guest actor aliases.
- Updated account lifecycle policy to
  `pilot-redesign-r2-2026-08-09-draft` and extended export, participant deletion,
  organizer-owned content deletion, participant entitlement removal, report
  anonymization, and guest-merge classification.
- Target guest merges move membership and blocks but refuse unexpected unlock, reward,
  Trail, or like value rather than promoting it.

## 3. Rules, indexes, and rollout controls

- New payload, reward, analytics, application, report, feedback, and linkage resources are
  direct-client-denied.
- Participants may query only published/safe discovery in a joined, currently active
  Experience; owners may inspect their own moderation state.
- Unlock/reward/Trail/like documents are caller-readable and server-write-only.
- Declared target discovery, reward pagination, analytics, and lifecycle
  collection-group indexes; the static validator covers every literal filtered
  collection-group query and the dynamic lifecycle queries are explicitly declared.
- Added `pilot_redesign_backend_enabled=false` and
  `pilot_redesign_min_contract_version=2147483647`. The latter intentionally marks every
  current client unsupported until a later approved client gate. Flags never replace
  Rules, App Check, Auth, ownership, or membership checks.

One privacy rule intentionally remains deferred: `users/{uid}` still has the current
signed-in global read because existing clients read profile documents directly. Safe new
UI uses `creatorProfiles`; M4 must change the user read to owner-only in the same release
that denies the legacy payload surface. Tightening it in R2 would violate the approved
additive/rollback requirement.

## 4. Migration safeguards

`functions/scripts/redesign-migration-audit.js` is read-only, has no apply mode, requires
an exact `--project`, inventories Firestore/Storage, classifies migration input, and emits
counts plus SHA-256 integrity hashes. It was not run against production in R2 because this
task did not authorize production access.

`functions/scripts/rehearse-redesign-migration.js` refuses to run without the Firestore
emulator and has no production override. Its verified two-pass rehearsal proves:

- missing legacy radius becomes 30 m;
- discovery receives no payload or user maps;
- payload becomes a private immutable version;
- `likedBy` becomes caller-private likes;
- legacy `inventory` never becomes an unlock receipt;
- legacy `huntProgress` never becomes target Trail progress;
- legacy redemption receipts become `ISSUED`, never inferred `USED`;
- BUSINESS accounts without approval evidence are flagged, never grandfather-approved;
- a second pass is idempotent.

## 5. Verification evidence

All checks passed on 2026-08-09 local time:

| Gate | Result |
| --- | --- |
| Functions TypeScript build and ESLint | Pass, zero warnings/errors |
| R2 contract unit check | Pass: 21 events, distance buckets, 25 m/50 m bound |
| Remote Config validator | Pass: 7 fail-closed keys |
| Collection-group index validator | Pass: 7 declared fields |
| R2 adversarial Firestore/Storage suite | Pass |
| Complete current Firestore/Storage suite | Pass: all 18 suites |
| Emulator migration rehearsal | Pass, including idempotent second pass |
| Functions emulator export/load check | Pass: all 50 definitions loaded |
| Android unit tests, lint, debug assembly | Pass |

The Functions emulator warned that the local CLI was unauthenticated and could not fetch
remote Admin configuration; this did not affect source enumeration. No callable was sent
to a real service. iOS was not built because R2 changes no client source and this Windows
workspace cannot run the Apple toolchain; the full Rules compatibility suite protects the
currently preserved iOS data paths.

## 6. Separately gated deployment plan

No step below is authorized by approval of this document alone.

1. Run the read-only audit against the exact production project and retain its JSON
   manifest. Review every `FLAG`; require `unclassified == 0` and no Storage inspection
   error.
2. Record the production project id, deployed function/rules/index/Remote Config versions,
   Firestore export, Storage inventory, and integrity hashes. Obtain the separate backup
   and additive-deployment approval.
3. Configure a generated `ANALYTICS_HMAC_SECRET` in Functions Secret Manager and the exact
   approved `ORGANIZER_APPLICATION_URL`. Confirm App Check enforcement/monitoring for the
   owned Android and web entry clients.
4. Deploy and wait for indexes, then deploy additive Firestore Rules, Storage Rules,
   Functions, and the fail-closed Remote Config template. Do not lower the minimum contract
   or enable the redesign flag.
5. Use operator-owned fixtures to smoke-test preview/join, approved authoring, text/photo
   staging, proximity pass/fail, idempotent unlock, Trail order, reward issue/use/correct,
   Results, export, and deletion. Reconcile every count and confirm no raw location in
   logs/events.
6. If smoke tests fail, keep both rollout values fail-closed, restore the previous
   functions/rules version, and remove only explicitly identified R2 fixture documents.
   Because R2 is additive, current clients continue on legacy paths.
7. Present the deployed-but-disabled evidence as a new gate. R3 or a later client task may
   integrate only after that gate; M4 remains prohibited.

## 7. R2 gate checklist

- [x] R1 approval recorded.
- [x] Additive schemas and server boundary implemented locally.
- [x] Organizer self-promotion removed from the target path.
- [x] Target Rules/Storage/index/Remote Config/lifecycle coverage added.
- [x] Adversarial and legacy compatibility suites pass.
- [x] Read-only audit and emulator-only rehearsal tooling added.
- [x] Deployment and rollback plan recorded.
- [x] No production deployment, flag enablement, data mutation, or M4 cutover performed.

The active task is now **R3 only**. R2 production audit/deployment and M4 remain separately
gated and are not implied by R3 client work.
