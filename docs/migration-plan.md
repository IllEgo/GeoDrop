# Kithe — Prototype → Launch Scope Migration Plan

Companion to `docs/product-direction.md`. That file defines the target state; this file
is the ordered work to get there from the current prototype.

**Do not import this file into `CLAUDE.md`.** It is a working backlog, not persistent
context. Point Claude at it per task instead.

---

## How to run this

### Approved redesign continuation (2026-08-09)

The owner approved all F1–F7 and the resolution table in
`docs/redesign-alignment-proposal.md`. That document now supplies the ordered **R0–R10**
redesign sequence and gates. Current/deployed behavior recorded below remains historical
fact until a later R task changes and verifies it; it must not be mistaken for the new
target where the documents now differ.

- **R0** is documentation normalization only. No app, rules, function, schema, or
  deployment work belongs in it.
- **R1** is approved and complete in `docs/redesign-backend-contracts-r1.md`.
- **R2** is approved and complete with passing evidence in
  `docs/redesign-server-boundary-r2.md`. It has not been deployed and its production audit
  has not been run.
- **R3** is approved and complete in `docs/redesign-android-foundation-r3.md` after the
  verified build was installed and launched on a physical Android device.
- **R4** is approved and complete with passing evidence in
  `docs/redesign-navigation-shell-r4.md` after the verified build was reinstalled and
  launched on a physical Android device. On 2026-08-10 the owner split R5 into **R5-L**
  (local/device implementation) and **R5-P** (production funnel). **R5-L is approved and
  complete; R5-P remains blocking before any pilot or public release. On 2026-08-14 the
  owner authorized R5-P read-only audit and local bundle preparation; external mutations
  remain separately gated. The owner explicitly authorized R6 local implementation on 2026-08-10 and
  approved the implemented local/device participant experience on 2026-08-11 after the
  working map and relocated demo drops were reviewed. Its crash fix and target participant
  loop are locally implemented and device-smoke tested, while the real server/outdoor
  qualification gate remains open in
  `redesign-participant-loop-r6.md`. The owner then directed work to continue into **R7
  local implementation** and approved its installed local/device Experience and core
  text/photo authoring surface on 2026-08-11 after organizer-to-Explorer discovery passed
  on the physical device. Timed venue, unapproved-server-denial, and cross-device evidence
  remains open in `redesign-organizer-authoring-r7.md`; it is a pre-pilot dependency, not
  an unrecorded pass. The owner then authorized **R8 local/device implementation**. Its
  reward and aggregate Results surfaces are implemented, verified, installed, passed the
  physical interaction walkthrough, and were approved by the owner on 2026-08-11. The
  remaining pre-pilot dependencies are recorded in `redesign-rewards-results-r8.md`. The
  owner then authorized **R9 local/device implementation**. Account, safety, moderation
  intake, private rate limits, and fail-closed operations readiness are implemented and
  verified locally; the R9 review APK was installed and the Account/report/block/unblock
  flows were physically reviewed. A subsequent bottom-inset correction passed the full
  automated Android gate, was installed on the physical device, and passed its focused
  action-row and clean-log smoke test. On 2026-08-13 the owner approved every R9 local/device
  item while keeping its intentionally open pre-pilot dependencies recorded in
  `redesign-account-safety-operations-r9.md`.
  On 2026-08-13 the owner split R10 and authorized **R10-L local Android qualification
  only**; on 2026-08-14 the owner approved and closed R10-L after its evidence passed.
  On 2026-08-25 the owner approved and completed the fail-closed **R5-P A3** generated-host
  deployment and then separately approved and completed **R5-P A4**. The exact
  `join.kitheapp.com` CNAME is live, Firebase reports the custom domain connected, and HTTPS
  plus Digital Asset Links verification passes. The owner then approved and completed
  **R5-P A5a** locally: the project targets API 36, all 130 release tests pass, release lint
  and bundle generation pass, and the unsigned diagnostic AAB/privacy audit is recorded in
  `../deployment/r5-p/A5A-EVIDENCE.md`. Signing, Play setup/closed-test, policy, Maps, Remote
  Config, screenshot, and reviewer-fixture blockers remain. The next separately gated action
  is **A5b upload-key and internal-test candidate**; no A5b-A5e stage is authorized.
  Play, release-client flags, legal-policy backend, device-matrix, QR-distribution, and later
  production actions remain unauthorized.
  **R10-P and production actions beyond the recorded R5-P A4 scope are not authorized.** See
  `redesign-entry-guest-permissions-r5.md`. Do not
  deploy or enable the target backend, mutate production data, migrate screens ahead of
  their approved R task, or perform the M4 legacy cutover.
- The unfinished 4.6 QR deliverable is absorbed by **R5**.
- Phase 5 requirements remain mandatory and are absorbed by **R9**; public exposure still
  cannot precede their operational gate.
- Phase 6 contracts begin in **R1** and final production qualification occurs in **R10-P**.
- Phase 7 store readiness is part of **R10-P**, including the approved real Play install
  path. A sideload or closed-test enrollment is not QR-funnel acceptance evidence.
- The R5 split is a sequencing exception for local redesign work only. R5-P must close
  before R10-P can authorize the pilot, and neither the R5 nor R10 split weakens any
  hosting, App Link, Play, clean-install, deployment, rollback, or production-data
  requirement. R10-L is limited to local Android qualification.

If a legacy task below conflicts with the approved R sequence, follow the R sequence and
the signed redesign decision in `migration-decisions.md`; do not silently combine tasks.

Every task below is sized for roughly one session and one PR, and ends at an approval
gate. The working loop:

1. Enter **plan mode** (`Shift+Tab` twice, or `/plan`, or start with
   `claude --permission-mode plan`).
2. Give Claude the task ID and let it read the relevant code.
3. It proposes a plan. You approve, refine, or reject — nothing is edited until you do.
4. Approve with **manually approve edits**, not auto mode, for anything in Phase 1–3.
5. Task ends at its **Gate**. Claude stops there and does not roll into the next task.

Add this to `CLAUDE.md` so the gates hold across sessions:

```markdown
## Migration work
Tasks come from @docs/migration-plan.md. Complete exactly one task per session.
Stop at the task's Gate and wait for sign-off. Never start the next task unprompted.
```

Two safety rails before any of this starts:

- **Tag the current prototype** (`git tag pre-migration-baseline`) and work on a branch.
  Much of this plan deletes things; you want a cheap way back.
- **Never delete user data as part of a code removal task.** Code removal and data
  disposition are separate decisions with separate gates (0.3, 1.4).

---

## Production deployment log

**Merging is not deploying.** Several tasks change `firestore.rules` or `storage.rules`,
and those files only take effect in `geodrop-dfcba` when someone runs a deploy. Nothing
in CI does it. Tasks 2.6 and 2.7 sat merged and undeployed for eleven days before this
was noticed — during which the live rules still let a client write `businessName`, still
permitted dislike writes, and still carried the 2.7 profile lockout (`acceptLegalPolicies`
stamps `legalAcceptanceVersion`, the old allow-list did not include it, and
`hasOnlyAllowedUserFields()` sees the *merged* document — so every later profile update
from an account that had accepted the policies was refused).

**Record every production action here, in the same session it happens.**

| Date (UTC) | Target | What | Covers | Evidence |
| --- | --- | --- | --- | --- |
| 2026-07-26 | Firestore rules | Deploy | 1.2, 1.3 | ruleset `337797dc-37bf-486f-a0f2-7363f53c2595` |
| 2026-07-26 | Storage rules | Deploy | 1.2, 1.3 | `storage.rules` unchanged since; still current |
| 2026-07-26 | Firestore + Storage data | **Live wipe** | 1.4 | 172 docs, 98 objects / 592.8 MiB. Backup at `C:\Users\rober\GeoDrop-backups\wipe-2026-07-26\` — outside the repo, only copy of the prototype content |
| 2026-08-07 | Firestore rules | Deploy | 2.6, 2.7 | ruleset `b83ea329-fbd5-4a25-b9fb-ca3c86f922f9`; live source verified byte-identical to repo |
| 2026-08-07 | Firestore data | `roles:check` dry run (read-only) | 2.7 gate (c) | 25 profiles, all canonical, 0 normalize / 0 backfill / 0 flagged; `roles:apply` is a no-op. 3 profiles have no Auth user |
| 2026-08-07 | Firestore data | Location-trail scan (read-only) | 3.5 gate 1 | 25 user docs, 0 with position-shaped fields, 0 trail docs/collections. Gate 2 not applicable |
| 2026-08-07 | Firestore rules | Deploy | 3.5, 4.1 | ruleset `865c4276-e280-4e0f-b758-40fb9281d028`; live source verified byte-identical to repo. Ships expiry enforcement **and** the drops `allow update` split |
| 2026-08-07 | Remote Config | Publish | 2.8 follow-up | `pilot_nsfw_enabled` removed; template down to 5 fail-closed keys, validator updated |
| 2026-08-07 | Firestore rules | Deploy | 4.2 | ruleset `ff9c9ba6-25df-42cd-ac7e-aba4ac968030`; live source verified byte-identical to repo. Claims are now one-way in production |
| 2026-08-07 | Firestore rules | Deploy | 4.3 prerequisite | ruleset `f319574d-7935-4bf0-bb37-c815f5b33394`; verified byte-identical. **Redemption is now possible in production for the first time** — the allowed-keys list omitted the top-level `redeemedBy`, so `hasOnly()` was false for every real redemption |
| 2026-08-07 | Firestore rules + **functions** | Deploy | 4.3 complete | ruleset `91f5ac5d-7196-4925-b657-7e216131aa68`, verified byte-identical, released **with** the `redeemDrop` callable. Redemption is now server-owned: no client-writable path to `redeemedBy`/`redemptionCount`, and no `redemptionCode` on a readable document. Rules and functions must ship together here — rules alone would leave no redemption path at all |
| 2026-08-07 | Firestore rules + functions | Deploy | 4.4 server side | ruleset `24b8bd69-6e39-446d-a2b0-eeaa6a3ff688`, verified byte-identical. Ships the organiser rollup trigger, the daily reconcile, and the owner-read-only analytics rules block |
| 2026-08-09 | Firestore rules + **indexes** | Deploy | 4.5 | ruleset `ff3f8688-a382-440a-bc80-a0bc354fad2e` (createTime 04:12:49Z), verified byte-identical to repo. First index deploy in the project's history: `groups.code` now carries all four configs **READY**, including the `COLLECTION_GROUP` scope `notifyGroupMembersOnDropCreated` needs |
| 2026-08-09 | Functions | Deploy | 4.5 | All 26 functions `ACTIVE`, `updateTime` 04:56:11–04:56:32Z; `notifyGroupMembersOnDropCreated` at 04:56:22Z. **Which commit this release carries is unproven** — see the caveat under 4.5. Recorded retroactively on 2026-08-09 rather than in the session that deployed it |
| 2026-08-09 | Firestore rules + functions | Deploy | 4.6 prerequisite | ruleset `f4aa366b-d23d-49d6-af09-816e33fd1a3c` (21:08:28Z), verified byte-identical to repo and confirmed to contain the `accountMergeReceipts` block. Ships `mergeGuestAccount`, created 21:09:45Z. **27 functions, all `ACTIVE`, all carrying this release** — the deploy ran to completion even though the invoking shell timed out at two minutes mid-output, which is why the function list was checked rather than trusted |
| 2026-08-09 | Firestore indexes | Read-only check | 4.6 prerequisite | `drops` has **no field overrides**, so `collectedBy`/`likedBy`/`createdBy` inherit `__default__/fields/*` and the merge's map-key queries are answerable in production. Same check found **`inventory.id` has no `COLLECTION_GROUP` index**, which breaks `deleteAccount` — see the follow-up under 4.6 |
| 2026-08-09 | Remote Config | Publish — **reverted, see the row below** | Pilot device demo | Version **2** (22:11:42Z, `REST_API`, `firebase-adminsdk-fbsvc@`). `pilot_creation_enabled`, `pilot_hunts_enabled`, `pilot_notifications_enabled` → `true` for the device-demo session; coupons and media stay false. Enabled **in memory via `--enable`, not in the committed template**, which remains all-false. **Revert:** `cd functions && node scripts/publish-remote-config.js --project=geodrop-dfcba --apply --confirm-project=geodrop-dfcba` — publishing the committed template *is* the revert. Verified against `listVersions`, not a command's exit status: four earlier attempts reported success and published nothing |
| 2026-08-09 | Remote Config | Publish — **revert of the row above** | Pilot device demo | Version **3** (22:30:53Z). All five keys fail-closed again; the demo was deferred to 2026-08-10 rather than held open overnight. Achieved by publishing the committed template with no `--enable`, which is the whole point of that design: the revert needs no memory of what was flipped. Re-enable with `--enable=…` when the demo runs |
| 2026-08-09 | Firestore indexes | Deploy | 5.4 defect fix | `inventory.id` declared with `COLLECTION_GROUP ASCENDING` plus the three collection-scope indexes (an override replaces the defaults). All four went `CREATING` → `READY`; `usesAncestorConfig` is now false for that field, confirming the override took effect without dropping the defaults. **This is what makes `deleteAccount` work for an account that ever created a drop** |
| 2026-08-25 | `kithe-production` billing | Activate Blaze + budget alerts | R5-P A2 | Dedicated Kithe billing linked; $25 USD monthly alert budget at 50/90/100/150 percent. Alerts are not a hard cap. Evidence in `r5-p-external-approval-list.md` |
| 2026-08-25 | `kithe-production` Secret Manager | Enable API + create secret | R5-P A3 | `ANALYTICS_HMAC_SECRET` version 1 enabled without displaying/logging the random value; runtime accessor limited to the secret. Evidence in `../deployment/r5-p/A3-EVIDENCE.md` |
| 2026-08-25 | `kithe-production` Firestore rules + index | Deploy | R5-P A3 | Dedicated fail-closed A3 rules live; exactly one `experienceDrops` composite and zero field overrides verified |
| 2026-08-25 | `kithe-production` Functions | Deploy | R5-P A3 | Exactly seven reviewed Node.js 22 first-generation Functions `ACTIVE` in `us-central1`; no extras, no legacy policy-base environment variable, one-day artifact cleanup policy |
| 2026-08-25 | `kithe-production` Firestore data | Atomic safe fixture create | R5-P A3 | Guarded six-document `R5PTEST2` commit passed authenticated verification without logging owner or test-point values |
| 2026-08-25 | `kithe-production` Firebase Hosting | Deploy generated host | R5-P A3 | `kithe-production.web.app` passed root/404/headers, direct Digital Asset Links parity, and redacted `R5PTEST2` entry checks; no custom domain or DNS mutation |
| 2026-08-25 | `kithe-production` Firebase Hosting domain | Create pending direct association | R5-P A4a | `join.kitheapp.com` now **Needs setup** with no redirect. Firebase returned one CNAME target, `kithe-production.web.app`; independent DNS still had zero answers. Cloudflare remained unchanged; A4b not authorized |
| 2026-08-25 | `kitheapp.com` DNS + `kithe-production` Hosting domain | Add exact CNAME and verify | R5-P A4b | Owner approved exactly `CNAME join.kitheapp.com -> kithe-production.web.app`, DNS only, TTL Auto. Cloudflare and Google resolvers returned the target; Firebase reported **Connected**; certificate-validated HTTPS, root/404/headers, safe entry page, and one-statement/three-fingerprint Digital Asset Links parity passed. Unrelated DNS, email, DNSSEC, redirect, Worker, SSL, Play, Remote Config, and QR settings were unchanged |

### Verifying what is actually live

Do not infer this from git history. Fetch the released ruleset and diff it against the
repo — the Rules API returns the deployed source, and a zero-line diff is the only proof
that a merge reached production:

```
GET https://firebaserules.googleapis.com/v1/projects/geodrop-dfcba/releases/cloud.firestore
GET https://firebaserules.googleapis.com/v1/{rulesetName}
```

Authenticate with the service-account key in `.secrets/` (gitignored). The response
carries `createTime`, which dates the deployment.

Rules are the only artefact that can be verified this exactly. For the two others that
now ship to production:

```
GET https://firestore.googleapis.com/v1/projects/geodrop-dfcba/databases/(default)/collectionGroups/{collection}/fields/{field}
GET https://cloudfunctions.googleapis.com/v2/projects/geodrop-dfcba/locations/-/functions
```

The field response lists every live index and its `state`, which is real proof. The
functions response gives only `updateTime` — **it does not prove which commit is
deployed**, and the key in `.secrets/` can read neither Cloud Logging nor the functions
upload bucket, so deployed function *source* cannot be diffed the way rules can. Where a
function's behaviour is observable (a changed log line, a changed return shape), use that
as the check instead of inferring from timestamps.

### Remote Config: publishing it, and proving it published

**Nothing in the Firebase CLI publishes Remote Config**, and `firebase deploy --only
remoteconfig` silently publishes nothing while reporting success (see the correction under
2.8). Publish with the script, which asserts the resulting live version rather than
reporting that a target list was accepted:

```
cd functions
node scripts/publish-remote-config.js --project=geodrop-dfcba            # dry run
node scripts/publish-remote-config.js --project=geodrop-dfcba --apply \
  --confirm-project=geodrop-dfcba
```

Temporarily enabling a flag for a supervised demo takes `--enable=<key,key>` plus
`--allow-enabled`, and **never an edit to the template** — the committed file stays
fail-closed, so *publishing the committed template is the revert* and there is no dirty
working file for a later commit to pick up. `remoteconfig:validate` fails CI on an enabled
key, which is the backstop.

Verify independently, the same way as rules — never from a command's exit status:

```
GET https://firebaseremoteconfig.googleapis.com/v1/projects/geodrop-dfcba/remoteConfig
GET https://firebaseremoteconfig.googleapis.com/v1/projects/geodrop-dfcba/remoteConfig:listVersions
```

`listVersions` is the authoritative history: a publish that happened appears there
immediately, with its `updateOrigin` (`REST_API` vs `CONSOLE`) and author. A console edit
left as an unpublished draft leaves no entry at all.

### Deploy order and the build caveat

When a task changes both rules and a data migration, **rules first, then the migration**
(`docs/account-model.md` spells this out for 2.7). And note what the 2.7 rules did on
release: **older installed builds can no longer create a profile at all**, because they
send `businessName` at create, which is now server-authored. Prod is wiped and pre-pilot,
so nothing is stranded — but Pilot 1 must ship current builds.

---

## Phase 0 — Ground truth

No code changes in this phase. The point is to stop guessing about what you actually
built before anything gets torn out.

### 0.1 — Feature inventory
**Deliverable:** A table mapping every feature in the codebase to one of: *launch scope*,
*deferred*, *unclassified*. Each row cites the files, Firestore collections, Storage
paths, and Cloud Functions involved.
**Acceptance:** Every entry in the direction doc's launch-scope and deferred lists
appears exactly once. Unclassified rows are listed explicitly rather than silently
bucketed.
**Gate:** You confirm the classification, especially the unclassified rows. Everything
downstream depends on this being right.

### 0.2 — Data inventory
**Deliverable:** Counts of existing records that the new scope disallows — anonymous
drops, video drops, NSFW-flagged content, group records, DM threads, stored location
histories. Read-only queries; no mutations.
**Acceptance:** Numbers are broken out by collection, with date ranges. Explicitly states
whether any of it came from real users vs. your own testing.
**Gate:** You review before any disposition decision.

### 0.3 — Migration decision record
**Deliverable:** A short ADR per deferred feature: delete the code, or gate it behind a
disabled flag? Delete the data, orphan it, or archive it? With a recommendation and
reasoning for each.
**Acceptance:** Every deferred feature has an explicit decision. Safety-critical features
(anonymous creation, NSFW, DMs, public groups) must be **disabled at the rules layer**
regardless of what happens to the client code — a flag alone is not an acceptable answer
for these.
**Gate:** You sign the ADR. It becomes the spec for Phases 1 and 2.

---

## Phase 1 — Close the write paths

Server-side first. During Phase 2 you will have half-gutted clients; the rules are what
hold the line while that's true.

### 1.1 — Rules test harness
**Deliverable:** Firebase emulator + security-rules test suite covering current behavior.
Tests assert what the rules do *today*, including the parts you're about to change.
**Acceptance:** Suite runs green against current rules and fails loudly if rules change
underneath it. Runnable in one command, wired into CI if CI exists.
**Gate:** Green suite demoed before any rule is edited.

### 1.2 — Deny anonymous drop creation
**Deliverable:** Firestore and Storage rules require an authenticated, non-anonymous
principal for any drop write. Tests prove unauthenticated and anonymous-auth writes are
rejected.
**Acceptance:** New failing-then-passing tests for each rejection path. Read paths for
guests are unaffected — anonymous *viewing* stays legal per the direction doc.
**Gate:** You review the rules diff line by line. This is the single highest-stakes
change in the plan.

### 1.3 — Deny deferred-feature writes
**Deliverable:** Rules reject writes to video content, NSFW-flagged content, DM threads,
public group creation, and vote records.
**Acceptance:** One test per denied path. Existing documents remain readable — this task
closes writes only.
**Gate:** Rules diff reviewed; test output attached.

### 1.4 — Legacy data disposition
**Deliverable:** Executes the data decisions from 0.3. Dry-run mode first, printing what
*would* change, before anything mutates.
**Acceptance:** Dry-run output reviewed and matches the 0.2 counts. Backup or export
taken before any destructive step. Idempotent and re-runnable.
**Gate:** You approve the dry-run output specifically, then separately approve the live
run. Two gates, not one.

---

## Phase 2 — Remove deferred features from the client

One feature per task, one PR each. Order is roughly highest-risk first.

### 2.1 — Anonymous creation path
**Deliverable:** Remove anonymous/guest drop-creation UI, view models, and repository
methods. Guest *browsing* and guest *unlocking* remain intact.
**Acceptance:** No code path reaches a drop write without an authenticated creator ID.
Guest browse flow still works end to end.
**Gate:** Manual walkthrough of the guest flow on device.

### 2.2 — NSFW detection and content handling
**Deliverable:** Remove the NSFW classification pipeline, associated ML dependencies,
model assets, and any NSFW content states.
**Acceptance:** APK size delta reported. No orphaned model files or permissions left in
the manifest. Prohibition is now policy + moderation, not a client classifier.
**Gate:** Sign-off, plus confirmation that you're comfortable relying on Phase 5
moderation instead of automated detection at this scale.

### 2.3 — Video upload
**Deliverable:** Remove video capture, upload, transcoding hooks, and playback. Storage
rules already reject writes after 1.3.
**Acceptance:** Camera permission usage reduced to photo only if applicable.

### 2.4 — Public group creation
**Corrected 2026-07-26.** This task originally read "Remove group creation, membership,
and group-scoped feeds." That was written before Phase 0 and dropped the word *public*.
**0.1 Decision A** split the concept and the **0.3 ADR** signed it: invite-only groups are
**launch scope** — they are the direction doc's "experiences the user explicitly joined"
mechanism, and `NearbyDropRegistrar` filters push notifications by exactly that membership.
Only **public / discoverable** groups are deferred, and they were never built. Executing the
original wording would have deleted a launch-scope feature and broken scoped notifications.

**Deliverable:** Verify that no public/discoverable group surface exists, and that both
organizer-scoped event drops and invite-only group scoping survive intact. Do **not** remove
invite-only groups, membership, or group-scoped drops. The rules-layer denies that keep
public groups unbuilt (`groups` create/update/delete/`list` denied, membership callable-only)
already shipped in 1.3 and **must not be loosened**.
**Acceptance:** Organizer-scoped event drops are unaffected — verify this explicitly, as
the two often share code.
**Gate:** Confirmation that organizer/event scoping survived intact.
**Open follow-up (deferred to Phase 5):** gate group *creation* to organizer/business
accounts in the `manageGroup` callable, per 0.1 Decision A.

### 2.5 — Direct messaging
**Note added 2026-07-26.** Nothing to remove. 0.1 classified DMs `D·absent` and 2.5
re-confirmed it across `app/src`, `ios/GeoDropIOS` and `functions/src`: no DM UI, no thread
model, no conversation collection, and no DM notification handler. The client routes exactly
two push events, `DROP_COLLECTED` and `REPORT_STATUS_UPDATED`. 1.3 already denied every
DM-shaped path at the rules layer. The task therefore reduces to its Acceptance.
**Deliverable:** Confirm no DM UI, threads, or notification handlers exist, and that the
DM-shaped rules denies from 1.3 hold.
**Acceptance:** Notification routing still works for the scoped notifications that remain.

### 2.6 — Voting and upvotes
**Note added 2026-07-26.** Executed on **both clients plus the rules layer**, unlike
2.1–2.3 which deferred iOS. iOS could not be deferred here: `Drop.swift` sent
`dislikeCount`/`dislikedBy` on every create and `FirestoreService.setDropLike` sent them on
every like, so *any* rules tightening would have broken iOS drop creation and liking. 1.3
had deliberately left dislike *retraction* legal for exactly that reason; removing the
client payloads is what unblocked the final tightening the 0.3 ADR called for.

**Deliverable:** Remove the voting system. Direction doc permits simple likes as a
supporting feature; complex vote weighting goes.
**Acceptance:** Explicitly states what was kept vs. removed, since "likes" and "upvotes"
are likely tangled in the current schema.

**Kept:** `likeCount`/`likedBy` and the full like path, including nested `likedBy.<uid>`
writes and the counter-must-move transition rules; `reportCount`/`reportedBy`;
`collectedBy`; `DropLikeStatus`, reduced to `NONE`/`LIKED` on both platforms.
**Removed:** `dislikeCount`, `dislikedBy`, `isDisliked` (drops and inventory notes), both
Android thumbs-down surfaces, the iOS dislike button, the `MOST_POPULAR` sort's
`likeCount - dislikeCount` net score (now ranks on likes alone — this was the actual vote
weighting), the unused `Drop.likeScore()`, `dislikedBy` from the account-deletion scrub,
and every dislike allowance in `firestore.rules` — the fields are no longer allowed keys,
so casting, retracting, or restating a zero is denied. 1.3's `hasNoSeededVotes` and
`isDislikeRemovalOnly` were deleted as redundant.
**Judgment call, reversible:** iOS's *only* reaction control was the Dislike button (it
never had a Like button), so it was converted to a Like toggle rather than deleted, which
would have left iOS with no way to use a launch-scope feature.

**Gate:** You confirm the like/vote line was drawn where you want it.

### 2.7 — Collapse account types
**Note added 2026-07-29.** 0.1 and the 0.3 ADR had already established there is **no
extended matrix to remove** — the model was `EXPLORER` + `BUSINESS` on both clients before
this task started, so nothing was deleted. What the audit found instead was that the
two-type model was *conventional rather than enforced*, in three ways, and the task became
closing those. Full write-up: **`docs/account-model.md`**.

**Deliverable:** Reduce the account model to the two the launch scope needs — explorer
and business/organizer — removing extended permission matrices.
**Acceptance:** Migration path for existing accounts documented. No role checks reference
removed types.

**Found and fixed:**
1. **A client could grant itself the business surface.** `businessName` was client-writable
   and Android *inferred* `BUSINESS` from business metadata, bypassing the
   deployed `updateBusinessProfile` callable's verified-email gate. R2 changes local source
   to approval-only. At the time, the server never agreed (the drop rules read the stored
   `role`), so the result was business UI on an account with explorer
   permissions. Business metadata is now server-authored and the inference is gone.
2. **Clients and rules disagreed about the same stored value.** Both clients case-folded
   `role`, so `"business"` read as BUSINESS on the client and EXPLORER on the server. Both
   now match exactly, and anything off-model resolves to the least-privileged type.
3. **Server-written profile fields locked the account out of its own profile.** Moderation
   leaves `moderationStatus`/`reinstatedAt` on the document and `acceptLegalPolicies` stamps
   `legalAcceptanceVersion`/`legalAcceptedAt`; none were in the user allow-list, so
   `hasOnlyAllowedUserFields()` — which sees the *merged* document — refused every later
   profile update. Pre-existing and latent, but the legal-acceptance half is on the path
   **every** account takes, and both are squarely in the account model, so fixed here. The
   rules now state the invariant: every Admin-SDK writer of `users/{uid}` must be listed.

Also: iOS's `GroupRole` carried an `EDITOR` case the server stopped emitting (it writes
only `OWNER`/`SUBSCRIBER`) and defaulted an unrecognized value to `.owner`; removed and
flipped to least-privilege, matching Android. Both clients' guest→account migration copied
the whole previous profile document, which now (and partly already) violates the rules;
narrowed to `displayName`, with the username still transferred by its callable.

**Migration path:** `functions/scripts/normalize-account-roles.js`
(`roles:check`/`roles:apply`), dry-run by default, `--confirm-project` gated, idempotent,
writes only `role`. It backfills the deleted client-side inference once, server-side;
normalizes non-canonical values; flags off-model values instead of guessing; and reconciles
the 25-profiles-vs-22-Auth-users mismatch 0.2 left for this phase. `roles:rehearse` proves
all seven safety properties on the emulator and is wired into CI.

**Gate:** Review the account model diff and the migration path before it runs.
Specifically: (a) business metadata becoming server-authored, (b) an off-model `role`
locking that profile until normalized, and (c) the prod `roles:check` dry run, which needs
credentials this session did not have.

### 2.8 — Dependency and manifest sweep
**Deliverable:** Remove now-unused Gradle dependencies, permissions, services, and
feature flags left behind by 2.1–2.7.
**Acceptance:** Manifest diff reviewed. Build still passes. APK size delta reported.

**Done 2026-08-06.** APK **−386,365 bytes** (−377.3 KiB, −0.95%), clean-to-clean debug
builds measured through a `git worktree` at a short path.

**The manifest result is bigger than the size result.** `androidx.work:work-runtime-ktx`
was declared but never used — no `Worker`, no `WorkManager`, anywhere in `app/src`. Its
manifest contributions were shipping in every build:

- 2 permissions — `RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE`. A location app
  declaring both, for a library it does not use, is exactly what draws Play Console
  scrutiny and user suspicion.
- 4 services, one of them `exported` (`SystemJobService`), plus Room's
  `MultiInstanceInvalidationService` — Room arrived transitively with WorkManager.
- 8 receivers, including an exported `DiagnosticsReceiver` and a boot-completed
  `RescheduleReceiver`.
- A `WorkManagerInitializer` startup provider entry.

**NSFW flag removed** (deferred here from task 2.2). `FEATURE_NSFW_ENABLED`, the
`pilot_nsfw_enabled` Remote Config key, and `PilotFeatureFlags.nsfwEnabled` are gone from
both clients; every guard they fed collapses to "server-flagged content is always
hidden," which is the policy position anyway. The viewer-facing half of the feature was
already dead code: the Android "Mature content" switch was hard-coded `false`, disabled,
and wired to an empty callback, `updateNsfwPreference` ignored its argument and had no
caller, `canViewNsfw()` returned a constant, and DropDetailActivity computed an
`nsfwAllowed` value nothing read. iOS had the same shape (`allowNsfw`, `setAllowNsfw`,
`toggleAllowNsfw`, a read-only "Mature content" row). Both clients also passed an
`allowNsfw` argument down to a query layer that ignored it — the Firestore query filters
`isNsfw == false` unconditionally on both platforms.

**Kept, deliberately:** `Drop.isNsfw` (rules require the field present and `false` on
create; list queries filter on it), the server SafeSearch enforcement path, `media3-ui`
alongside `media3-exoplayer` (both back `DropAudioPlayer`), `CAMERA` and `RECORD_AUDIO`
(photo and audio drops), and `material-icons-extended` and `com.google.android.material`
(both genuinely used — the latter supplies the `Theme.Material3.*` parent). Also removed:
two unreferenced Android Studio template stubs, `backup_rules.xml` and
`data_extraction_rules.xml`, which the manifest never pointed at.

**`ACCESS_BACKGROUND_LOCATION` was left in place** and annotated. It is load-bearing for
geofenced nearby alerts today; Phase 3 replaces that design rather than just dropping the
permission.

**Owner actions.** `pilot_nsfw_enabled` was removed from `remoteconfig.template.json`,
the validator's key list, and the release-evidence template on 2026-08-07, and the template
was published — Remote Config is managed as code here, so this was a repo change plus a
publish rather than a console edit.

> **Corrected 2026-08-09: "plus a deploy" was wrong, and cost four failed attempts.**
> The Firebase CLI has **no Remote Config publish command** — only
> `remoteconfig:get`, `remoteconfig:rollback`, and `remoteconfig:versions:list`. Worse,
> `firebase deploy --only remoteconfig` *accepts* the target, because `firebase.json`
> carries a `remoteconfig` key, and then publishes **nothing while printing
> "Deploy complete!"**. The v1 publish recorded above has `updateOrigin: REST_API`, which
> is the tell: it went through the API, not a deploy. Use
> `functions/scripts/publish-remote-config.js` — see below. **Still owner-only:** drop
`GEODROP_FEATURE_NSFW_ENABLED` from whatever build configuration the release pipeline
selects. It lives outside the repo (the example xcconfig no longer lists it, and the
Android `buildConfigField` went at 2.8), so nothing here can reach it. It is inert either
way — no code reads it.

### Phase 2 — gate resolutions

Settled 2026-08-06/07. Phase 2 is closed; nothing below blocks Phase 3.

**2.1 — deferred to pilot prep, not waived.** The on-device guest walkthrough is real
verification that no test replaces, but it cannot be performed usefully today: prod has
zero drops after the 1.4 wipe, so a guest sees an empty map. It also now has to run on a
**current build**, since the deployed 2.7 rules refuse profile creation from older ones.
Move it to the Pilot 1 seeding checklist and run it against seeded content.

**2.2 — closed by decision.** The 0.3 ADR chose moderation over automated detection and
the direction doc states NSFW is prohibited by policy and implementation; the classifier
is deleted and merged. The operative constraint is not a sign-off but the direction doc's
rule that **reporting, blocking, and a documented moderation queue exist before any public
exposure** — that is Phase 5, already scheduled and already a dependency of the pilot.

**2.4 — closed on test evidence.** `firestore-tests/organizerScopeRules.test.js` asserts
organizer drop creation, no `businessId` spoofing, invite-only scoping, and non-member
refusal on every CI run, and is green. That is stronger evidence than a human review pass.
The deferred follow-up (gate group *creation* to organizer/business in `manageGroup`)
remains scheduled in Phase 5.

**2.6 — closed; the iOS Like toggle stays** (owner decision, 2026-08-06). Android has had
a real Like control all along and still does, on two surfaces: `DropDetailActivity.kt`
(`LikeToggleButton`, `ThumbUp`, next to the like count, gated by `canLike`) and
`DropHereScreen.kt` (collected notes). iOS had *only* a Dislike button and no Like button,
so converting it brought the clients to parity rather than adding a feature; removing it
would have left iOS users able to see a like count they cannot contribute to.

**2.7 (a) and (b) — closed retrospectively.** Both were enacted by the 2026-08-07 rules
deploy before they were signed off: business metadata is server-authored in production,
and an off-model `role` locks that profile until normalized. Recording this plainly
because the deploy front-ran the gate. (b) has no live victims — the prod `roles:check`
dry run found **0 off-model roles** across all 25 profiles. Reversing either now costs a
rules change rather than a code edit.

**2.8 — no gate.** Its two owner actions are hygiene, listed above.

---

## Phase 3 — Location privacy rework

The direction doc's six-step model is the spec. This is a behavioral change, not a
cleanup, and it will touch permission timing across the app.

### 3.1 — Location call-site audit
**Deliverable:** Every location request in the codebase, with its trigger, precision,
lifetime, and what it's used for.
**Acceptance:** Read-only. Flags every site that requests precise location outside a
drop-unlock attempt, and every background or continuous request.
**Gate:** You review before any behavior changes.

**Done 2026-08-06 — read-only, nothing changed. Full audit: `docs/location-audit.md`**
(15 call sites across both clients).

**Three things are already right** and 3.2–3.5 must not break them: nothing requests
location at app launch (`ContextualPermissionPolicy` gates every prompt behind an explicit
user intent and refuses to prompt before onboarding completes); Android's unlock check is
*already* the Phase 3 shape (`DropDecisionReceiver` takes a one-shot high-accuracy fix at
pickup, validates staleness and accuracy, and **fails closed**); and **no location trail is
persisted anywhere** — the only coordinates in Firestore are the drops' own, so step 6
holds by construction and 3.5 is mostly verification.

**The core violation is browsing, not unlocking.** Both clients hold a continuous
high-accuracy stream for as long as the explorer surface is open, purely to render
distances and sort lists — `DropHereScreen.kt:2701` (5 s interval) and iOS
`LocationService.startUpdating` (stopped only on de-authorization). Steps 1 and 5 exclude
exactly this.

**Also flagged:** background location is *load-bearing* for geofenced nearby alerts, so 3.4
must decide what happens to that feature rather than just dropping the permission; Android
requests FINE and COARSE together at browse time, before any unlock (3.2/3.3 should split
them); and iOS never handles reduced accuracy at all — no
`requestTemporaryFullAccuracyAuthorization`, no `accuracyAuthorization` — so with Precise
Location off, every unlock silently fails the 30 m check.

**One finding is a correctness bug, not a privacy one:** iOS `markCollected` used
`if let distance = distanceToDrop(drop)`, and `distanceToDrop` returns `nil` when there is
no fix — so **the proximity check was skipped entirely and the collect proceeded**. Android
rejects in the same situation. **Fixed in the same PR** (pulled forward ahead of 3.2/3.3):
`markCollected` is now fail-closed and mirrors Android's rejections — missing fix, fix older
than 2 minutes, or accuracy negative or worse than the pickup radius. Consequence: a user
with "Precise Location" off can no longer collect until F4 is addressed, which is correct
(the 30 m check was previously passing at random against a 1–5 km fix) and matches Android.

**Sequencing note:** 3.2 and 3.3 are coupled and should be planned together —
`pickUpDrop`'s range gate reads the browse stream's cached value, so removing the stream
without rerouting it breaks the user-facing "move closer" message (the real check in
`DropDecisionReceiver` still holds).

### 3.2 — Approximate location for browse
**Deliverable:** Map and nearby-list use coarse location only.
**Acceptance:** Verified on device with precise location denied — browsing still works.

**Done 2026-08-06, together with 3.3** — see the combined note under 3.3. They could not
be separated: the browse location also drove the content-preview and collect gates, so
coarsening it alone would have denied preview and collect to a user standing at the drop.

### 3.3 — Precise location on unlock only
**Deliverable:** Precise location requested at the moment of an unlock attempt, released
after the proximity check resolves. Prefer Android's one-time precise-location mechanism.
**Acceptance:** Instrumented proof that precise access is not held after the check
completes. Unlock still works at your target GPS accuracy.
**Gate:** On-device demo. This is the flow most likely to frustrate pilot users — worth
testing outdoors before signing off.

**Done 2026-08-06 with 3.2, both clients.** The two tasks were merged after the 3.1 audit
showed they share a mechanism: browse location fed the proximity gates, so 3.2 alone would
either break those gates or force them to tolerate ±100 m — which would have loosened
proximity gating, the product's core mechanic, to roughly a 130 m radius.

**The model now matches the direction doc's six steps:**

| Step | Android | iOS |
| --- | --- | --- |
| Browse on approximate | `getApproximateLocation()`, `PRIORITY_BALANCED_POWER_ACCURACY`, one-shot per list load | `refreshApproximateLocation()`, `kCLLocationAccuracyHundredMeters`, one-shot |
| Precise only at unlock | `attemptUnlock` → `getPreciseFixForUnlock()` | `markCollected` → `requestPreciseFix` |
| Check proximity | `distance <= 30 m + fix accuracy`, fail-closed | same comparison, fail-closed |
| Record the unlock | `unlockedDropIds` (ids only) | `unlockedDropIDs` (ids only) |
| Stop afterwards | one-shot; nothing retained | accuracy returns to approximate after each fix |

**Removed:** the continuous `PRIORITY_HIGH_ACCURACY` stream on Android (5 s interval, held
for as long as the explorer surface was open) and `startUpdatingLocation()` on iOS (stopped
only on de-authorization). Neither client streams location now.

**Permission split (3.1's F5).** Browsing asks for `ACCESS_COARSE_LOCATION` only;
`ACCESS_FINE_LOCATION` is requested at the first unlock attempt. On iOS,
`requestTemporaryFullAccuracyAuthorization` is finally used — with the `UnlockDrop`
purpose key added to `Info.plist` — so a user who keeps Precise Location off can still
unlock, which closes the hard stop that 3.1's F4 and the F3 fix had left open.

**Two traps worth recording, both found and closed during implementation:**

1. **Nearby alerts would have silently broken.** Geofencing needs FINE
   (`NearbyDropRegistrar` refuses to register without it), and once browsing was satisfied
   by a coarse grant, the alerts flow read "location granted" and proceeded to fail. The
   alerts flow now reads the *precise* permission state, and its dialog requests precise
   directly instead of sending the user back to a Nearby control that only asks for coarse.
2. **The iOS Collect button was gated on `canPreview`.** Since previewing now requires an
   unlock, and that button *is* the unlock attempt, leaving the gate would have made every
   drop permanently uncollectable. It is now gated on collected/in-flight only.

**UX change to review:** content is no longer revealed by merely standing near a drop. The
card shows an **Unlock drop** button; a successful check reveals the content and offers
**Pick up drop**. This is a truer reading of the direction doc — the unlock is an attempt
the user makes — but it is a visible change to the pilot's core loop and is the thing most
worth watching in the on-device demo.

**Acceptance not yet met — this needs your device.** Both acceptance criteria are physical:
browsing with precise denied, and instrumented proof that precise access is not held after
the check. The code holds no fix beyond the check by construction, but "verified on device"
is your call, outdoors, at your target GPS accuracy. iOS is compile-verified only.

**Android device follow-up 2026-08-07.** Coarse-only browsing worked, but the UI rendered
the intentionally approximate fix as an exact "current location" pin, auto-selected the
first drop before centring the user, and offered a precise-location control wired to the
already-satisfied coarse flow. The follow-up keeps the 3.2/3.3 boundary intact: browsing is
still a balanced-power, one-shot coarse fix, now shown as an accuracy area with an
accuracy-aware zoom; no drop is selected automatically; and the coarse-to-precise Android
permission upgrade is requested only at the existing unlock boundary. The rebuilt internal
APK was installed on the coarse-only test device and showed the approximate area with its
reported ~2 km radius, satisfying the 3.2 device check. The 3.3 gate remains open
until an outdoor drop unlock proves the target GPS accuracy and one-shot lifetime on device.

**Coarse-location presentation refinement 2026-08-07.** Browse surfaces no longer expose
exact metre/kilometre distances from an approximate fix or promise a strict nearest-first
order. Distances are presented as **Nearby**, **A short walk**, or **Farther out**, and the
saved `NEAREST` sort key now groups by those bands with newest-first ordering inside each
band. The hard 300 m client-side browse cutoff was also removed so a coarse fix cannot hide
a genuinely nearby drop. Both maps now draw only a soft accuracy area (with a conservative
fallback/minimum radius) for the user, never a centre pin or a precise-looking numeric
accuracy. These are browse-only presentation and candidate-list changes; the one-shot
precise fix and 30 m unlock enforcement from 3.3 are unchanged.

### 3.4 — Remove background and continuous location
**Deliverable:** Background location permission and any continuous tracking removed from
manifest and code.
**Acceptance:** Manifest no longer declares background location. No foreground service
exists solely for location.

**Approach decided 2026-08-06 — see P5 in `docs/migration-decisions.md`.** Background
location exists for exactly one feature: the geofences that fire nearby-drop
notifications. It is **not** needed for unlocking (3.3 moved that to a one-shot precise
fix) and **not** needed for the scavenger-hunt trail, which advances from the collect path
and reads `currentStepIndex` back from Firestore — no geofence is consulted anywhere in
the chain.

**Chosen: remove the permission and the geofence machinery, and re-base notifications on
membership** — a server-side send when a drop is added to an experience the user
explicitly joined, which is what the launch list actually promises. Accepted cost: no
passive buzz when a user passes an unrelated drop.

Scope when this task runs:

- Remove `ACCESS_BACKGROUND_LOCATION` from the manifest.
- Remove `NearbyDropRegistrar`'s geofence registration, `GeofenceManager`,
  `GeofenceReceiver`, and the `GeoFencePendingIntent` plumbing.
- Drop background location from `ContextualPermissionPolicy` (and its tests), and remove
  the precise-location requirement the alerts flow inherited at 3.2/3.3.
- **Keep** `DropDecisionReceiver` — it is the authoritative proximity check for in-app
  pickups, not geofence-specific — plus `POST_NOTIFICATIONS` and the existing FCM routing.
- Add the server-side membership push in `functions/`, alongside the two events already
  routed (`DROP_COLLECTED`, `REPORT_STATUS_UPDATED`).

**Done 2026-08-06.** Merged-manifest check confirms acceptance: **zero** matches for
`BACKGROUND_LOCATION` or `Geofence`, location permissions reduced to
`ACCESS_COARSE_LOCATION` + `ACCESS_FINE_LOCATION`, and the only remaining services are FCM
and vendor libraries — no foreground service exists for location or anything else we own.

**Deleted:** `NearbyDropRegistrar`, `GeofenceManager`, `GeofenceReceiver`,
`GeoFencePendingIntent`, the `GeofenceReceiver` manifest entry, the background permission,
its launcher, its two dialogs (rationale + recovery), and `backgroundLocationState`.
`ContextualPermissionPolicy` lost `REQUIRE_NEARBY_LOCATION_FIRST`,
`SHOW_BACKGROUND_LOCATION_RATIONALE` and `OPEN_BACKGROUND_LOCATION_SETTINGS`; enabling
alerts now needs `POST_NOTIFICATIONS` and nothing else.

**Added:** `notifyGroupMembersOnDropCreated` in `functions/src/index.ts` — on drop create
with `visibility == "GROUP"`, it resolves members via a `collectionGroup("groups")` query
on the membership `code` and sends `DROP_ADDED_TO_EXPERIENCE` to each member except the
creator. **The payload deliberately carries no location**: it says a drop exists in an
experience you joined, never where the recipient is. The client routes the new event in
`GeoDropMessagingService` with strings in both locales.

**Kept, deliberately:** `DropDecisionReceiver`. It is the authoritative, fail-closed
proximity check for in-app pickups and was never geofence-specific — sweeping it out with
"the geofence machinery" would have deleted the check that makes unlocking trustworthy.

**Tests:** `ContextualPermissionPolicyTest` lost the two cases that no longer exist and
gained the property that replaced them — *alerts need no location grant at all*, asserted
with `foregroundLocation = BLOCKED`. 25 tests / 6 classes green, plus `lintDebug`,
`assembleDebug`, and `tsc` on functions.

**Follow-up left open — corrected 2026-08-07.** This task originally recorded the
**notification radius** as "now inert". **That was wrong.** It no longer controls
notifications, but `DropHereScreen.kt:2904` still uses it to bound the nearby browse list:
non-business drops farther than the radius are filtered out, and the map circle depicts
that reach. So the setting is **misnamed, not dead** — removing it would make the nearby
list unbounded, which is a behaviour change rather than a cleanup.

The real follow-up is therefore a **rename**, not a removal: "Nearby notification radius"
describes a job it stopped doing at 3.4, while the job it actually does — how far the
nearby list reaches — has no name in the UI. Worth settling before the pilot, since an
event organiser will reasonably expect that control to affect what attendees see. Note
iOS has the opposite problem: its equivalent radius filter is commented out
(`AppViewModel.swift:273`), so its Nearby list is already unbounded — see F6 in
`docs/location-audit.md`.

### 3.5 — Unlock receipts, not location history
**Deliverable:** Persist the successful unlock event only. Remove any stored location
trail. Confirm no other user's live position is exposed by default.
**Acceptance:** Schema diff shows what stopped being written. Data disposition for
existing trails follows the 1.4 pattern — dry run, then live, two gates.
**Gate:** Two gates, as in 1.4.

**Done 2026-08-06. There was nothing to remove, and that is the finding.**

**Schema diff — what stopped being written.** Nothing, because no location trail was ever
written. The 3.1 audit established it and 3.5 proved it: the only coordinates in Firestore
are the drops' own `lat`/`lng`, which are authored content. A collect writes exactly one
field, `collectedBy.{uid}`. The scavenger-hunt receipt (`users/{uid}/huntProgress/{huntId}`)
stores `currentStepIndex` and `completedStepIds` — ids and counts. 3.3's unlock record is a
set of drop ids held in memory for the session. **A user-scoped document records which drop
was unlocked and when, never where the user was.**

**Gate 1 — prod dry run (read-only), 2026-08-06:**

```
Root collections: usernames, users
Root-level trail collections: 0
User documents: 25
User subcollections seen: inventory(1), legalAcceptances(1), notificationTokens(1)
User docs carrying position-shaped fields: 0
Trail documents found: 0
RESULT: no location trail exists. Nothing to dispose of.
```

**Gate 2 — live run: not applicable.** There is nothing to delete. The 1.4 pattern is
satisfied by demonstrating an empty target rather than by executing a deletion.

**The real deliverable is enforcement, not removal.** An absence that holds only because
nobody happened to write a trail is worth little, so it is now pinned by
`firestore-tests/locationPrivacyRules.test.js` (wired into the CI suite, 11 files):

- the profile rejects `lat`/`lng`, `lastKnownLocation`, `locationHistory`, `lastSeenLat`,
  `currentLatitude` — `hasOnlyAllowedUserFields()` closes the field list;
- trail-shaped subcollections (`locationHistory`, `locations`, `positions`, `breadcrumbs`,
  `visits`) and a root `locationHistory` are unwritable. These have no match block, so the
  default deny covers them — the assertions exist so that adding a permissive wildcard
  later **fails here** instead of silently opening a trail;
- hunt progress accepts step ids but **rejects the same receipt carrying
  `unlockedAtLat`/`unlockedAtLng`**;
- no user can write a position onto another user's document or subcollection.

**One deliberate exception, asserted as allowed:** `users/{uid}/inventory/{dropId}` stores
`lat`/`lng`. Those are the *collected drop's* coordinates — a copy of content the user has
already unlocked, not a position reading. The test asserts it succeeds so a future
tightening cannot silently break collecting, and separately asserts that the inventory's
closed field list rejects `collectorLat`/`collectorLng`.

**"No other user's live position is exposed by default" holds by construction:** no user
position is stored anywhere, so there is nothing to expose. `users/{uid}` is readable by
any signed-in user, which is why the closed field list — not the read rule — is what keeps
positions out.

**PHASE 3 COMPLETE.**

---

## Phase 4 — Complete the launch scope

Only now do you build. Scope is closed; if something isn't on the direction doc's launch
list, it doesn't get added here.

### 4.1 — Drop expiration

**Done 2026-08-06.** Expiration existed, but only in the clients: `Drop.isExpired()` hid
expired drops and refused to collect them across 77 sites in `app/src`. **No rule ever
evaluated `decayDays`**, so an expired drop stayed collectable and redeemable by anything
that did not run that client check. Expiration is now enforced in `firestore.rules` by
`isNotExpired()`: a drop with a positive `decayDays` stops being collectable and
redeemable once `createdAt + decayDays` has passed; drops without a decay never expire.
Liking and reporting stay legal on expired drops — moderation cannot depend on a drop
still being live — and the creator can still soft-delete one.

**The drops `allow update` rule had to be split to make room.** It was a single condition
OR-ing four write shapes, and it had been logging *"maximum of 1000 expressions to
evaluate has been reached"* since before 2.6 (recorded then as a pre-existing gotcha).
Adding the expiry check tipped it from a warning into **refusing legitimate likes**. It is
now four `allow update` statements — like/report, collect, owner soft-delete, redeem —
which are OR'd exactly as before but evaluated under separate expression budgets. Keep new
conditions inside the branch they belong to.

**Tests:** new `firestore-tests/dropExpirationRules.test.js` (12th suite) covers expired
vs fresh, absent and zero `decayDays`, both sides of the boundary (one hour short, one hour
past), likes/reports still working on an expired drop, and owner soft-delete. Full suite
green.

**Known issue, now visible: the redemption branch still exceeds the expression budget.**
The emulator reports the 1000-expression limit on that statement specifically, which means
a real redemption write could be refused. No test asserts a *successful* redemption today,
which is why this has stayed hidden. **4.3 must fix this before redemption codes ship** —
it is the paid-organizer feature, so it cannot go out on a rule that may not evaluate.

**Not done here:** no scheduled cleanup of expired drops exists. Expired drops remain in
Firestore, merely uncollectable and hidden. Worth adding before the pilot so the map does
not accumulate dead content.

### 4.2 — Collect / claim

**Done 2026-08-07.** Same shape as 4.1: the feature worked, and the question was what the
server actually guaranteed.

**Collecting was already confined to the collector's own key** — `hasOnlyUserCollectedChange`
limits the diff of `collectedBy` to `[userId]`, so you cannot collect on anyone's behalf or
remove another collector. **What it was not, was one-way.** Nothing stopped a user removing
or falsifying their own `collectedBy` entry and collecting again, because
`hasUserCollectedBeforeOrAfter` accepts the write if the user collected *before or after*.

That matters because a claim is the pilot's unit of value — the prize, the trail step, and
the collect counts 4.4 will report to organisers. A reversible claim can be farmed, and the
organiser's numbers are the thing they pay for.

**A claim is now one-way:** once your entry exists it cannot be removed, nulled, or set
false, and the whole-map rewrite that would drop it is refused too. Collecting stays
idempotent, and collecting alongside an existing collector leaves theirs intact.

**Tests:** new `firestore-tests/collectClaimRules.test.js` (13th suite) covers the happy
path, idempotency, all three un-collect shapes (null, false, whole-map rewrite), collecting
on someone else's behalf, removing another collector, and writing into another user's
inventory. Full suite green.

**Current deployed fact:** proximity is still client-enforced on both platforms — no rule
or callable verifies the collector was near the drop, and rules cannot check location. A
claim is honest about *who* and *once*, not about *where*.

**Superseded as the target on 2026-08-09 (F1):** R1/R2 must split readable discovery
metadata from a server-only payload and move receipt creation behind a server-authoritative
`unlockDrop` callable. The callable receives one precise fix plus age/accuracy, persists no
coordinates, and returns content only on success. Do not rewrite the paragraph above as if
this is already deployed; it remains the migration's starting state.

### 4.3 — Redemption codes for business rewards

**Superseded target semantics (F2, approved 2026-08-09):** the callable described below is
still the deployed implementation, but it conflates code issuance with actual business
use. R1/R2/R8 must replace that meaning with separate `issuedAt` and confirmed `usedAt`
states, pre-generated reward code pools, organizer-authorized use/correction operations,
and distinct `issuedCount`/`usedCount` rollups. Existing pre-pilot receipts migrate as
issued, not proven used.

**Prerequisite cleared 2026-08-07** — the redemption rule could never succeed: the
allowed-keys list omitted the top-level `redeemedBy`, which is what a nested-map write
reports as affected. Fixed, deployed as ruleset `f319574d`, and pinned by tests.

**Design decided 2026-08-07 — see P6 in `docs/migration-decisions.md`: codes are
server-issued.** `redemptionCode` sat on the drop document, and drop documents are readable
by any signed-in user and by signed-out guests for public drops — Firestore cannot hide a
single field. `allow list` made bulk harvesting possible. Codes could be collected without
visiting a location or holding an account, which defeats the premise and devalues the paid
feature.

Scope when this task runs:

- Remove `redemptionCode` from the drop document and the rules allow-list.
- Add a `redeemDrop` callable that validates coupon/expiry/already-redeemed/limit, writes
  `redeemedBy.<uid>` and `redemptionCount` with the Admin SDK, and returns a per-user code
  to that caller only.
- Point both clients at the callable instead of their own transaction.
- **Delete** the rules' redemption branch — with redemption server-only, no client write to
  those fields is legitimate. This also removes the statement behind the expression-budget
  pressure.

### 4.4 — Organizer analytics

**Redesign decision (F4):** keep the existing private aggregate dashboard. Do not hide or
replace it with a founder-only report. The report is an operational supplement. When F2 is
implemented, replace the ambiguous redemption total with separate code-issued and
code-used totals; attendee identity remains excluded.

**Design decided 2026-08-07 — see P7 in `docs/migration-decisions.md`: a server-side
rollup.** Per-drop stats already exist on both clients and read off the drop document; what
is missing is the per-experience aggregate a paying organiser wants. Computing that
client-side would mean reading every drop on every dashboard open, and would give a
different answer per device.

Scope when this task runs:

- A rollup document per experience: Admin-SDK-written, owner-read, client-write-never.
- Maintained incrementally by `drops` triggers (`FieldValue.increment` on collect and redeem
  transitions), **plus a scheduled reconcile** that recomputes from source — incremental
  counters drift on trigger failure or retry, and a quietly wrong number is worse than none.
- Report **aggregates, not per-attendee identity**.

**Do not absorb Phase 6.** The pilot's funnel metrics are questions about people, not drops.

**Implementation complete 2026-08-07 — awaiting the 4.4 gate.** The server half was
deployed as ruleset `24b8bd69-6e39-446d-a2b0-eeaa6a3ff688`: `rollUpExperienceActivity`
maintains `drops`, `collects`, and `redemptions` at
`groups/{groupCode}/analytics/summary`, while `reconcileExperienceActivity` recomputes
those documents daily. The existing rules suite proves the parent experience owner can
read the summary, members and signed-out users cannot, and no client can write it.

Both clients now consume that rollup instead of trying to recreate experience totals
from whichever drops happen to be visible locally. Android and iOS resolve only
memberships where the signed-in business is the owner, treat a missing summary as a
zero-activity experience, and show one aggregate card per owned experience alongside
the existing per-drop dashboard. The client models carry counts and timestamps only —
never attendee identity.

**Verification:** Android `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass. The
two focused model tests cover the server field shape plus fail-safe handling of missing,
malformed, and negative counters. This Windows workstation has no Swift/Xcode toolchain,
so the iOS compile and compact-device layout review remain gate evidence rather than
being inferred from the Android result.

**Gate:** Review an owner dashboard with known collect/redemption activity on Android
and iOS; confirm that a non-owner cannot see the summary; record a successful macOS/iOS
build. Do not begin 4.5 until that evidence is accepted.

**Gate resolution — closed 2026-08-08 on Android evidence; iOS visual review deferred.**

Evidence accepted:

- **Known activity, reviewed on Android.** `functions/scripts/seed-experience-activity.js`
  seeded experience `EATZ` (owner `robertp8@hawaii.edu`) with a decided-in-advance shape:
  4 drops, 6 collects, 2 redemptions. The script writes only drops; the deployed
  `rollUpExperienceActivity` trigger computed the summary, and `--verify` confirmed it
  matched source. The owner reviewed that dashboard and accepted it.
- **Non-owner denial.** `firestore-tests/organizerRollupRules.test.js`, green on every CI
  run, asserts the owner can read the summary and that members, strangers, and signed-out
  users cannot, with ownership proven against the parent group document.
- **iOS build.** The `iOS simulator build` job passed on `master` at `a7cf346`.

Deferred, with reasons rather than waivers:

- **The iOS dashboard was never opened.** There is no macOS or Xcode toolchain available
  to this project today, so no one has seen the iOS owner dashboard render this rollup.
  CI proves it compiles; nothing proves it looks right. Whoever gains Mac access should
  run the review before the pilot, not because the gate is open but because it is real
  verification that was skipped.
- **iOS dashboard parity.** The review found the per-drop list hid flag-gated drops while
  the aggregate counted them, so an organiser saw "4 drops" over a list of 2 with 2
  redemptions attributed to drops that were not on screen. Android now lists every drop and
  labels the unreachable ones (PR #51). iOS `ProfileView` still hides them, so the two
  clients currently disagree. Filed as follow-up, not fixed here.

Two defects surfaced by this review and fixed on the way through, both pre-existing:

- Owner-facing drop queries were refused outright by the `drops` list rule for want of an
  `isNsfw` filter, breaking the business dashboard and My Drops for every account (PR #50,
  covered by `firestore-tests/ownerQueryShapeRules.test.js`).
- `migrateExplorerAccount` cannot work as written — rules refuse both enumerating the
  previous account's drops and rewriting `createdBy`, the latter correctly. A guest signing
  into a real account still loses their drops. Needs an Admin-SDK callable; unfixed and
  documented in place. This blocks nothing in 4.5 but sits directly under 4.6's onboarding.

### 4.5 — Scoped push notifications (explicitly joined experiences only)

**Deliverable:** the membership-scoped alert working behind the notifications flag.
**Acceptance:** a demoed happy path plus the failure cases — opted out, no registered
device, non-member.
**Gate:** your sign-off before the flag flips on.

**Implemented 2026-08-08 (PRs #52, #53), deployed 2026-08-09. The gate is still open —
see "Still open" below.**

**The feature was built at 3.4 and had never once worked in production.**
`notifyGroupMembersOnDropCreated` resolves recipients with
`collectionGroup("groups").where("code", "==", …)`, and no collection-group index existed
for that field, so **every invocation since 3.4 ended in `FAILED_PRECONDITION`** —
confirmed in the live logs against the drops seeded into `EATZ` for the 4.4 review.
Nothing caught it: notifications are flag-off, so no device held a token and no delivery
was expected anyway. No test could have caught it either — the emulator creates indexes on
demand, so the query the emulator answers is not the query production refuses.

**Three things shipped:**

1. **`firestore.indexes.json`, wired into `firebase.json`** — the collection-group index
   for `groups.code`. The field's three default collection-scope indexes are listed
   alongside it because **a `fieldOverrides` entry replaces the defaults rather than adding
   to them**; omitting them would have silently broken ordinary `groups` queries while
   fixing the trigger. This is the project's first index config, and like rules, **merging
   does not create it** — it needs a deploy, which is why the deployment log now has an
   *indexes* row.
2. **A server-visible opt-out** at `users/{uid}/notificationSettings/preferences`, read by
   the trigger before each send. Membership decides who *may* be notified; this decides who
   still wants to be. **Absent means opted in** — joining an experience is the opt-in, per
   the launch scope — and **a read failure also means opted in**, because a transient
   Firestore error must not silently unsubscribe an attendee mid-event. Rules keep the
   document owner-only and shaped (`experienceAlertsEnabled` bool, `updatedAt` int, nothing
   else), pinned by `firestore-tests/notificationPreferenceRules.test.js` (17th suite).
3. **Turning alerts off now reaches the server twice over.** Android deletes the FCM token
   *and* writes the preference. It used to clear a local sync marker only, which left both
   the send and the delivery intact — the user's choice was honoured by accident, and only
   on the device where it was made. Two writes are needed rather than one: the token stops
   FCM delivering to *this* device, the preference stops the trigger sending to tokens
   registered on *others*.

**Delivery counts now report deliveries, not attempts (PR #53).** During verification the
trigger logged `Notified 1 member(s)` while the only registered token was stale and the
push reached nobody — FCM had answered per token and the code discarded that answer. That
is the number an operator reads mid-pilot to decide whether attendees were reached, and it
was wrong in the direction that hides failure. `sendToUserTokens` now returns delivered /
failed / pruned counts and both callers log outcomes, with **"no registered device" counted
separately** because it is the quiet failure: alerts are on, the send is skipped, and
nothing anywhere reports a problem. Both test accounts were in exactly that state.

**Acceptance tooling:** `functions/scripts/demo-experience-notification.js
--code=<CODE> --audit` reports who would be notified and why the rest would not, writing
nothing; `--code=<CODE> --owner=<uid> --apply` creates a real drop so the deployed trigger
fires end to end; `--retire --apply` soft-deletes the demo drops afterwards. Not wired into
`functions/package.json`, unlike the other operational scripts — invoke it by path.

**The payload still carries no location**, as at 3.4: it says a drop exists in an
experience you joined, never where the recipient is.

**Still open — this needs your phone.** Nobody has yet seen a scoped push land on a
device. The demo needs **both** switches, since `PilotFeatureFlags.notificationsEnabled`
is `BuildConfig.FEATURE_NOTIFICATIONS_ENABLED && remoteConfig.getBoolean(…)`: an internal
build with `KITHE_FEATURE_NOTIFICATIONS_ENABLED=true`, *and* Remote Config
`pilot_notifications_enabled` flipped on. That key is global and `conditions` is empty in
`remoteconfig.template.json`, so flipping it is a production change — every other client
is protected only by its build flag defaulting to false. Flip it for the demo, then flip it
back. Two things to confirm while you are there:

- **iOS has no opt-out and no token removal.** PR #52 touched Android only. iOS registers
  tokens (`FirestoreService.swift`) but never writes `notificationSettings/preferences` and
  never deletes its token, so an iOS user who turns alerts off keeps being sent to. The
  server honours the preference for whoever writes it; iOS cannot write it. Parity
  follow-up, not a blocker for the Android demo.
- **Which commit the live functions release carries is unproven.** The deploy completed
  04:56:22Z, two minutes after #53 merged (04:54:12Z), but `firebase deploy` uploads its
  source before the functions finish updating, so the timing is suggestive rather than
  conclusive, and this project's credentials cannot verify deployed function source. **The
  log line is its own tell:** if the demo prints `reached 1/2 member(s)`, #53 is live; if it
  prints `Notified 1 member(s)`, redeploy functions before trusting any delivery number.

### 4.6 — QR entry point and low-friction onboarding

**Absorbed by redesign task R5 (approved 2026-08-09):** Experience preview precedes auth
and permissions; anonymous-auth guests may browse but cannot unlock, collect, create, or
redeem; the first unlock attempt gates account creation and resumes the same target after
link/merge. Entry uses an owned-domain Android App Link, `/e/<code>` web fallback, Play
Install Referrer, and a human event code. Firebase Dynamic Links are not a fallback.

**Deliverable** is the working feature behind a flag; **Acceptance** is a demoed happy path
plus the failure cases you care about; **Gate** is your sign-off before the flag flips on.

#### Prerequisite — guest→account continuity (done, gate open)

`migrateExplorerAccount` could not work: rules refuse both enumerating the previous
account's drops and rewriting `createdBy`, the latter correctly — a client able to reassign
authorship could steal or disown drops. **Implemented 2026-08-09; not yet deployed.**

**The audit found the cause was upstream of the broken repair.** Sign-in called
`signInWithCredential` (Android `DropHereScreen.kt`, iOS `AuthService.swift`), which issues
a *new* uid, so guest content was orphaned by construction. And the repair ran in the wrong
direction: its effect was gated on the *current* user being anonymous, so guest→account
never migrated at all — what it actually did was copy a real account's `displayName` onto a
fresh guest session at sign-out, then throw when rules refused the drops half. iOS's copy
of the same function **had no caller**.

**Link first; merge only when linking is impossible.** `linkWithCredential` upgrades the
anonymous account in place and keeps the uid, so the ordinary case needs no server call and
nothing moves. The new `mergeGuestAccount` callable covers the one case linking cannot —
the credential already belongs to an account, i.e. a returning attendee — where Firebase
must issue a different uid.

**The uid is never taken from the request.** The callable verifies the guest's own ID token,
requires the resolved account to be genuinely anonymous (`providerData` empty), refuses a
guest that is the caller, honours revocation, and deletes the guest auth user last. A
callable that accepted a named uid would let any account claim any other account's drops.

Moved: authored drops (`createdBy`, plus `createdByUsername` where present), collect claims
and likes (`collectedBy`/`likedBy` keys), `inventory`, `huntProgress`, `groups`, and
`blockedCreators`. **The destination wins every collision** — a claim is one-way per 4.2, so
a merge must not become a way to un-collect. `displayName`/`username` move only into empty
space. Not moved: `role`, business metadata, moderation state, `legalAcceptances`,
`notificationSettings`, `notificationTokens`, `reportStatuses`.

**`blockedCreators` moves, which is one item beyond the agreed list** — losing a block list
on sign-in is a safety regression rather than lost convenience.

Provenance goes to a new `accountMergeReceipts` collection (digests and counts, never raw
uids), **not onto the profile**: per 2.7, `hasOnlyAllowedUserFields()` sees the *merged*
document, so a new server-written profile field would refuse every later client profile
update. Client-unreadable and unwritable, pinned in `accountRoleRules.test.js`; added to the
wipe script's `PRESERVE_ROOT_COLLECTIONS` (that rail refuses to run on an unclassified root
collection) and swept by the existing receipt purge.

Also removed: the `allowTransferFrom` argument both clients sent to `claimExplorerUsername`.
The callable never read it, so the username transfer it implied always failed
`already-exists`.

**Verification.** `functions/scripts/rehearse-guest-merge.js`, wired into the CI
`p0-rehearsals` chain, asserts four refusals (naming a uid with no token, a garbage token, a
real but non-anonymous token, the caller's own token), that every content type moves, that
account-scoped state stays behind, that the destination's claim survives a collision, and
that a retry after completion is a no-op rather than an error. Green locally, with the full
17-suite rules run and the other five rehearsals. Android `testDebugUnitTest`, `lintDebug`,
`assembleDebug` pass; iOS is compile-verified by CI only.

**Deployed 2026-08-09** — ruleset `f4aa366b-d23d-49d6-af09-816e33fd1a3c` and
`mergeGuestAccount`, both verified live and logged above. **The device demo below is still
outstanding**, so the merge path has never run against a real pair of accounts.

**The 4.5 index check was repeated here, and it passes for the right reason.** The merge's
`collectedBy.<uid> != null` queries are **collection-scoped**, and `drops` carries no field
overrides, so they inherit the default single-field indexes and production answers them.
4.5 broke because its query was a `collectionGroup` — a scope the default config does *not*
cover. That is the distinction to carry forward: **collection-scoped map-key queries are
free; collection-group queries need a declared index.**

**Follow-up found by that check — `deleteAccount` is broken in production.**
`deleteOwnedInventoryCopies` (`accountLifecycle.ts:121`) runs
`collectionGroup("inventory").where("id", "in", …)`, and **`inventory.id` has no
`COLLECTION_GROUP` index**, so that query fails `FAILED_PRECONDITION`. It runs inside the
`Promise.all` ahead of the destructive steps, so:

- an account that **owns no drops** deletes normally — the loop never issues a query;
- an account that **created any drop** fails to delete at all, after
  `anonymizeSubmittedReports` and `scrubUserFromDropMaps` have already partly run. A
  **partial deletion**, on the path that exists to satisfy a deletion promise.

**Fixed and deployed the same session.** `firestore.indexes.json` now declares
`inventory.id` with the `COLLECTION_GROUP` entry **and** the three collection-scope indexes
alongside it, because a `fieldOverrides` entry *replaces* the field's defaults rather than
adding to them — getting that wrong would have broken ordinary `inventory` queries while
fixing deletion. Deployed and logged above; nobody was stranded, since prod is pre-pilot and
wiped.

**The class of bug now has a guard, which matters more than the fix.**
`functions/scripts/validate-collection-group-indexes.js` (`npm run indexes:validate`, wired
into the CI functions job) statically pairs every filtered
`collectionGroup(...).where(...)` in `functions/` with a declared `COLLECTION_GROUP` index
and fails the build when one is missing. **No emulator test can do this** — the emulator
creates indexes on demand, so the query it answers is not the query production refuses,
which is exactly why both instances shipped. Verified by deleting the new entry and watching
it fail on `accountLifecycle.ts`, then restoring it. A dynamic field path it cannot read
statically is reported rather than passed.

---

## Phase 5 — Safety and operations

The direction doc treats these as preconditions for public exposure, not follow-ups.

### 5.1 — Report and block
### 5.2 — Moderation queue (documented process + tooling)
### 5.3 — Creation rate limits and account reputation
### 5.4 — Account deletion and data export

**Gate for the phase as a whole:** you can operate this yourself, sustainably, at pilot
volume. If the moderation queue needs more attention than one person can give during an
event, that is a finding worth acting on — the direction doc names exactly that as a
reposition trigger.

---

## Phase 6 — Pilot instrumentation

### 6.1 — Funnel events
**Deliverable:** Analytics events covering the loop: invitation seen → app opened → drop
discovered → travelled → unlocked → value received → next unlock.
**Acceptance:** Every threshold in the direction doc's metrics table is computable from
the events. Not downloads — activations and unlocks.
**Gate:** You confirm each of the seven metrics can actually be produced.

### 6.2 — Qualitative capture
**Deliverable:** In-app feedback prompt plus a way to log GPS-accuracy failures and
explanation-needed moments during the pilot.
**Acceptance:** Answers the softer questions the direction doc asks — did people
understand what a drop was, did the prize drive everything.

---

## Phase 7 — Store readiness

Data-safety declarations, permission rationales, UGC policy compliance, privacy policy,
and a moderation-response commitment. Sequenced last because Phases 1–5 change every
answer on those forms.

**Approved distribution decision (F7):** qualify the real-event QR flow through a
fail-closed production Play listing on a never-installed device. Open testing is the
fallback only if its extra opt-in is measured and disclosed. Closed testing and sideloads
remain useful engineering channels but cannot close the attendee funnel gate.

---

## Sequencing notes

- **Phase 1 before Phase 2, always.** Rules are the enforcement layer; the client is
  untrusted. Reversing this leaves a window where the feature is hidden but live.
- **Phase 3 after Phase 2.** Don't refactor location handling across code paths you're
  about to delete.
- **Phase 5 is not optional before the pilot.** Report, block, and a working moderation
  queue gate any public exposure.
- Phases 4 and 6 can overlap. Nothing else should.
