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
**Deliverable:** Remove group creation, membership, and group-scoped feeds.
**Acceptance:** Organizer-scoped event drops are unaffected — verify this explicitly, as
the two often share code.
**Gate:** Confirmation that organizer/event scoping survived intact.

### 2.5 — Direct messaging
**Deliverable:** Remove DM UI, threads, and notification handlers.
**Acceptance:** Notification routing still works for the scoped notifications that remain.

### 2.6 — Voting and upvotes
**Deliverable:** Remove the voting system. Direction doc permits simple likes as a
supporting feature; complex vote weighting goes.
**Acceptance:** Explicitly states what was kept vs. removed, since "likes" and "upvotes"
are likely tangled in the current schema.
**Gate:** You confirm the like/vote line was drawn where you want it.

### 2.7 — Collapse account types
**Deliverable:** Reduce the account model to the two the launch scope needs — explorer
and business/organizer — removing extended permission matrices.
**Acceptance:** Migration path for existing accounts documented. No role checks reference
removed types.
**Gate:** Review the account model diff and the migration path before it runs.

### 2.8 — Dependency and manifest sweep
**Deliverable:** Remove now-unused Gradle dependencies, permissions, services, and
feature flags left behind by 2.1–2.7.
**Acceptance:** Manifest diff reviewed. Build still passes. APK size delta reported.

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

### 3.2 — Approximate location for browse
**Deliverable:** Map and nearby-list use coarse location only.
**Acceptance:** Verified on device with precise location denied — browsing still works.

### 3.3 — Precise location on unlock only
**Deliverable:** Precise location requested at the moment of an unlock attempt, released
after the proximity check resolves. Prefer Android's one-time precise-location mechanism.
**Acceptance:** Instrumented proof that precise access is not held after the check
completes. Unlock still works at your target GPS accuracy.
**Gate:** On-device demo. This is the flow most likely to frustrate pilot users — worth
testing outdoors before signing off.

### 3.4 — Remove background and continuous location
**Deliverable:** Background location permission and any continuous tracking removed from
manifest and code.
**Acceptance:** Manifest no longer declares background location. No foreground service
exists solely for location.

### 3.5 — Unlock receipts, not location history
**Deliverable:** Persist the successful unlock event only. Remove any stored location
trail. Confirm no other user's live position is exposed by default.
**Acceptance:** Schema diff shows what stopped being written. Data disposition for
existing trails follows the 1.4 pattern — dry run, then live, two gates.
**Gate:** Two gates, as in 1.4.

---

## Phase 4 — Complete the launch scope

Only now do you build. Scope is closed; if something isn't on the direction doc's launch
list, it doesn't get added here.

### 4.1 — Drop expiration
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
