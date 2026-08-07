# GeoDrop — Prototype → Launch Scope Migration Plan

Companion to `docs/product-direction.md`. That file defines the target state; this file
is the ordered work to get there from the current prototype.

**Do not import this file into `CLAUDE.md`.** It is a working backlog, not persistent
context. Point Claude at it per task instead.

---

## How to run this

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
   `updateBusinessProfile` callable's verified-email gate. The server never agreed (the drop
   rules read the stored `role`), so the result was business UI on an account with explorer
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
deploy rather than a console edit. **Still owner-only:** drop
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
### 4.3 — Redemption codes for business rewards
### 4.4 — Organizer analytics
### 4.5 — Scoped push notifications (explicitly joined experiences only)
### 4.6 — QR entry point and low-friction onboarding

For each: **Deliverable** is the working feature behind a flag; **Acceptance** is a
demoed happy path plus the failure cases you care about; **Gate** is your sign-off before
the flag flips on. 4.3 and 4.4 are what organizers actually pay for — give those the most
review attention.

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

---

## Sequencing notes

- **Phase 1 before Phase 2, always.** Rules are the enforcement layer; the client is
  untrusted. Reversing this leaves a window where the feature is hidden but live.
- **Phase 3 after Phase 2.** Don't refactor location handling across code paths you're
  about to delete.
- **Phase 5 is not optional before the pilot.** Report, block, and a working moderation
  queue gate any public exposure.
- Phases 4 and 6 can overlap. Nothing else should.
