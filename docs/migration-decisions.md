# Kithe — Migration Decision Record (Task 0.3)

An ADR per deferred feature: **delete the code or gate it behind a disabled flag? delete,
orphan, or archive the data?** — with a recommendation and reasoning for each. This is the
last Phase 0 task; once signed it becomes the spec that Phases 1 (rules) and 2 (client
removal) execute against.

- **Status:** Approved. P1–P7 were subsequently implemented as recorded in
  `migration-plan.md`; P8 was approved by the owner on 2026-08-09 and defines the redesign
  target where it supersedes an earlier implementation decision.
- **Inputs:** `docs/feature-inventory.md` (0.1), `docs/data-inventory.md` (0.2).
- **Load-bearing fact from 0.2:** all 100 drops are the owner's own test data; **zero
  real third-party users**. Data-disposition risk is therefore low, which is what makes
  the wipe decision below acceptable.

---

## Decision principles

- **P1 — Rules-first for safety-critical features.** The four safety-critical items
  (anonymous creation, NSFW, DMs, public groups) get a **rules-layer deny that holds
  regardless of client code**. A feature flag is never the sole control for these — per
  the 0.3 acceptance criterion. Rules land in **Phase 1**, before any client is gutted in
  Phase 2, so there is no window where the feature is hidden but still writable.
- **P2 — Flag-gated now, deleted later.** Deferred client code is already fail-closed via
  `PilotFeatureFlags` (Remote Config, fail-closed). The **target is deletion** in Phase 2,
  not a permanent flag — the direction doc says to remove deferred code, not extend it.
  The flag is the interim safety while rules are the durable enforcement.
- **P3 — Full prototype data wipe, then seed.** Owner decision: wipe all prototype
  content and seed fresh, curated content for Pilot 1. Executed in **1.4** with dry-run +
  backup + two gates. See the Data disposition section for exact scope.
- **P4 — Keep the server SafeSearch scan as a moderation signal only.** Decoupled from any
  user-facing NSFW gating; it feeds the (launch-scope) moderation queue for human review.

Code-decision vocabulary: **DELETE** (remove code), **GATE** (flag off, keep code short-
term), **KEEP** (launch scope), **REWORK** (change, don't remove).

---

## Per-feature decisions

| Feature (deferred list) | Safety-critical? | Code | Rules-layer | Data | Phase |
|---|---|---|---|---|---|
| Public anonymous-posting toggle | **Yes** | DELETE toggle | **Force `isAnonymous==false` on drop create** (currently whitelisted but unconstrained) | wipe | 1.2 → 2.1 |
| NSFW content + detection | **Yes** | DELETE client classifier/toggle/viewing; **KEEP** server SafeSearch as moderation signal (P4) | **Keep `isNsfw==false` forced on create; keep `nsfwEnabled==false` forced** (already enforced) — add tests | wipe | 1.3 → 2.2 |
| Open direct messaging | **Yes** | none exists | **Add explicit deny** for any `messages`/`threads`/`conversations`/`dm*` path (defense-in-depth; currently default-deny only) | none | 1.3 |
| Public group creation | **Yes** | KEEP invite-only; DELETE nothing | **Keep `groups` client writes denied (callable-only) and `list`/enumeration denied — must not loosen.** Public/discoverable groups stay unbuilt | wipe (9 test groups) | 1.3 (assert) |
| Video uploads | No | DELETE capture/upload/playback | **Deny `contentType=='VIDEO'` on create; remove `videos/` branch + `video/*` from storage.rules** | wipe (8 test videos + objects) | 1.3 → 2.3 |
| Broad / background location | No | **REWORK, do not delete** — proximity unlock depends on it | n/a (client/manifest) | none | Phase 3 |
| Complex voting (dislikes) | No | DELETE dislike UI + fields; KEEP likes — **DONE 2.6** (both clients) | **Removed `dislikedBy`/`dislikeCount` from every allowed transition and from the create/inventory key allow-lists — DONE 2.6.** `hasNoSeededVotes` and `isDislikeRemovalOnly` deleted as redundant | wipe (2 test drops) | 1.3 → **2.6 complete** |
| Algorithmic recommendations | No | none exists | n/a | none | — (do not build) |
| Multiple account types w/ extensive matrices | No | **KEEP** explorer + business only; no extended matrix to remove — **verified at 2.7**, nothing deleted | Role immutability was enforced; **2.7 also made business metadata server-authored, made off-model roles fail closed, and added the moderation fields to the user allow-list** so a reinstated account can still edit its profile | wipe preserved 25 profiles → `roles:check`/`roles:apply` normalizes them once | **2.7 complete** — see `docs/account-model.md` |
| National / global discovery | No | none exists | Discovery stays proximity/group-scoped | none | — (do not build) |
| Subscriptions / ads / data-sale / crypto | No | none exists | n/a | none | — (do not build) |

---

## Safety-critical detail (the four that require rules-layer enforcement)

Per acceptance, these cannot rely on a client flag. Each has a durable rules control.

### 1. Anonymous creation
- **Threat:** the direction doc's core harm model — public, hard-to-attribute,
  location-tied content.
- **Current state:** anonymous *auth* is already impossible (drop create requires
  `createdBy==auth.uid` + a role). The gap is the **`isAnonymous` display toggle**, which
  create permits with any value.
- **Decision:** rules force `request.resource.data.isAnonymous == false` on create (or drop
  the field from the whitelist). Client toggle deleted in 2.1. Every drop shows a display
  name.

### 2. NSFW
- **Current state:** `isNsfw==false` is already forced on create and `nsfwEnabled` is
  forced false — clients cannot produce or view NSFW; only backend moderation sets
  `isNsfw`.
- **Decision:** keep both rules and add explicit tests (1.1/1.3). Delete the client
  classifier/toggle/viewing (2.2). **Keep** the server SafeSearch scan (`analyzeOnUpload`)
  as a moderation-queue signal (P4). Prohibition is policy + moderation, with an automated
  pre-flag retained.

### 3. Direct messaging
- **Current state:** no DM collection; access is default-deny.
- **Decision:** add an **explicit deny** match for DM-shaped paths so a future accidental
  collection can't become writable. Belt-and-suspenders; no client code exists to remove.

### 4. Public groups
- **Current state:** already locked — `groups` are callable-only (client create/update/
  delete denied) and **enumeration (`list`) is denied**, so no public discovery is
  possible. Invite-only groups are launch scope (0.1 Decision A).
- **Decision:** **preserve these denies as the enforcement** and assert them in the rules
  suite; do not loosen. Public/discoverable groups remain unbuilt behind the full Phase 5
  safety stack. Follow-up (not a rules item): gate group *creation* to organizer/business
  accounts in the `manageGroup` callable.

---

## Data disposition (executed in 1.4, not now)

**Decision: full prototype wipe + fresh seed.** All content is owner test data, so
delete rather than orphan or archive. Disposition per store:

- **Delete (content):** `drops` + Storage `drops/*` objects; `groups` +
  `users/*/groups`; `reports`; `users/*/inventory`; `users/*/huntProgress`; `huntChains`;
  backend moderation collections (`dropModerationQueue`, `moderationCases`,
  `moderationAppeals`, `moderationAuditEvents`).
- **Preserve (accounts):** the owner's Auth users and minimal `users/{uid}` profiles, for
  login continuity. Exact keep-list (which of the 11 accounts, whether to retain any
  `users/{uid}` fields) is finalized at the 1.4 **dry-run**.
- **`usernames` — RESOLVED at 1.4 (2026-07-25): preserve.** 0.3 left this open as
  "as needed". Owner's call: a username is account identity, not prototype content, and
  the accounts are being kept — wiping the mapping would strip display names off profiles
  we are deliberately retaining. `wipe-prototype-data.js` preserves them by default and
  exposes `--wipe-usernames` if this is ever reversed. Also preserved for the same reason:
  `users/*/blockedCreators`, `users/*/notificationTokens`, `users/*/reportStatuses`.
- **Not archived:** no long-term archive of test data. The 1.4 rule still applies — take a
  Firestore/Storage **export as a rollback backup** immediately before the destructive
  run, retained short-term only.
- **Method:** the existing `backfill-launch-fields.js` handles field-level cleanup
  (NSFW/anonymous/visibility), but a full wipe wants a dedicated dry-run script following
  the same read-only-first pattern. Two gates at 1.4: approve dry-run output, then approve
  the live run.

Because of the wipe, the per-record transforms 0.2 anticipated (strip dislike/anonymous
flags on 2+2 drops, quarantine 4 NSFW, delete 8 videos) are **subsumed** — those records
are deleted wholesale, not transformed.

---

## Coverage check

All 11 direction-doc deferred items have an explicit decision above. The four
safety-critical items each have a rules-layer control that does not depend on a flag
(P1). Background location is the one **REWORK** (not delete) — flagged because launch-
scope proximity unlock depends on it (Phase 3). Reclassified-as-launch items (invite-only
groups, simple likes, two account types) are marked KEEP, consistent with 0.1.

## Gate

Per the migration plan, this ADR is the spec for Phases 1–2 **once you sign it.** Sign-off
means you accept:

1. The four rules-layer denies as the durable enforcement (P1).
2. The flag-now-delete-later sequencing (P2).
3. The full prototype wipe with fresh seed, backup-then-delete at 1.4 (P3).
4. Keeping the server SafeSearch scan as a moderation signal (P4).

This closes **Phase 0**. Next is **1.1 — Rules test harness** (Firebase emulator + a
security-rules suite that pins today's behavior before any rule below is edited).

---

## P5 — Background location and nearby alerts (added 2026-08-06, Phase 3)

**Supersedes the "REWORK, not delete" note above.** That note assumed launch-scope
proximity unlock depended on background location. Tasks 3.1–3.3 showed it does not:
unlocking now takes a one-shot precise fix at the moment of the attempt, and the
scavenger-hunt chain — the pilot's "trail" — never touched geofencing at all.
`advanceHuntProgress` is called from the collect path (`DropDecisionReceiver.kt:161`,
`DropHereScreen.kt:2035`) and the next step becomes visible because
`fetchLockedHuntDropIds` reads `currentStepIndex` back from Firestore. Pure data.

**What background location actually buys** is one thing: a passive notification when a
user wanders near a drop they were not looking for. That is a discovery convenience, not
a mechanic.

**Decision: remove it.** `ACCESS_BACKGROUND_LOCATION` and the geofence machinery
(`NearbyDropRegistrar` registration, `GeofenceManager`, `GeofenceReceiver`) go at task
3.4. Notifications are re-based on **membership**, not proximity: a server-side send when
a drop is added to an experience the user explicitly joined.

**Why this is not a scope cut.** The launch list promises "push notifications **only** for
experiences the user explicitly joined" — a membership-scoped send satisfies it exactly.
Proximity alerts were solving a problem the launch scope never posed, at the cost of the
one permission the direction doc singles out as sensitive and Play Console scrutinises
hardest. The pilot's measured loop is in-app anyway: *see invitation → open Kithe →
discover drop → walk to location → unlock → get value → unlock another*.

**Accepted cost.** No buzz when a user passes an unrelated drop. If the pilot shows
discovery suffers without it, foreground-only geofences remain available as a follow-up —
that option was considered and set aside as retaining most of the complexity for a
fraction of the feature, since Android Q+ geofences without the background permission are
unreliable in the background by design.

**Rejected:** keeping background geofences behind the existing explicit opt-in. It
conflicts with 3.4's acceptance and with the direction doc's deferred list ("broad or
background location tracking"), so it would have been a deliberate reversal rather than a
migration step.

---

## P6 — Redemption codes are server-issued (added 2026-08-07, task 4.3)

**The problem.** `redemptionCode` was a field on the drop document. Firestore rules cannot
restrict individual fields: `allow get` grants signed-in users any drop they can browse and
grants **signed-out guests** any public, active, non-NSFW drop, so every reader receives
every field — the code included. `allow list` compounds it: public drops are queryable, so
codes could be **harvested in bulk** without visiting a location, without an account, and
without unlocking anything.

That defeats the product's premise — content you must physically visit to unlock or redeem
— and it undermines precisely the feature businesses pay for. It cannot be fixed by
tightening a rule; the exposure is inherent in storing a secret on a readable document.

**Decision: the code never lives on the drop.** A callable issues it at redemption time.

- `redemptionCode` is removed from the drop document and from the rules allow-list, so it
  cannot be written by a client or read by anyone.
- A `redeemDrop` callable performs the whole transaction server-side: confirm the drop is a
  coupon, unexpired and not deleted; confirm the caller has not already redeemed; confirm
  `redemptionCount < redemptionLimit`; write `redeemedBy.<uid>` and increment
  `redemptionCount` with the Admin SDK; return a **per-user** code to that caller only.
- The clients stop running their own redemption transaction and call the callable.
- The rules' redemption branch is then **deleted**, not tightened — with redemption
  server-only, no client write to `redeemedBy`/`redemptionCount` is legitimate. That also
  removes the statement that has been the source of the expression-budget pressure.

**Why this over the alternatives.** A gated subcollection (`drops/{id}/redemptions/{uid}`)
would also hide the code, but splits a coupon's identity across two documents and still
leaves redemption as a client write. Accepting the exposure for the pilot was considered and
rejected: the codes are the paid feature, and a harvestable code is worse than no code,
because it looks like it works.

**Bonus:** issuing per-user codes gives organisers real redemption tracking — who redeemed,
when, and how many remain — which is the B2B tooling the direction doc calls load-bearing.
A single shared code can never support that.

**Migration:** prod holds zero drops after the 1.4 wipe, so no existing coupon codes need
rotating. Any build predating the callable will fail to redeem once the rules branch is
removed; the pilot must ship current builds, as it already must for 2.7.

### P6 addendum — the typed code goes (decided 2026-08-07)

Pointing the clients at the callable turned out not to be plumbing. The existing
signature is `redeemDrop(dropId, userId, providedCode)`: the user **types a code** and the
client validates it. That only makes sense while a code exists in advance — the shared one
that used to sit on the drop, which is exactly what P6 removes.

**Decided: drop `providedCode` entirely.** Tap → callable → a per-user code is displayed,
and staff verify the code the customer presents.

Consequences to build against:

- `FirestoreRepo.redeemDrop` (Android) and `FirestoreService.redeemDrop` (iOS) lose the
  `providedCode` parameter and stop running their own transaction; both call `redeemDrop`.
- The "enter the code" screen becomes "here is your code, show it at the counter" on both
  clients.
- **This changes what the business does at the counter** — instead of one shared code every
  customer types, staff verify a code the customer presents. Worth telling pilot partners
  before the event rather than at it.
- It is also what makes redemption tracking real: who redeemed, when, how many remain. A
  single shared code can never support that, and organiser analytics (4.4) depends on it.

**Rejected:** keeping a typed code as a second factor. It adds friction, and the shared
code would have to live somewhere readable again — reintroducing the exposure P6 exists to
close.

**Sequence for the implementation session:** point both clients at the callable first, then
remove `redemptionCode` from the drop document and delete the rules redemption branch. Doing
the rules step first would break redemption for every build in the field.

---

## P7 — Organizer analytics are a server-side rollup (added 2026-08-07, task 4.4)

**Where 4.4 starts.** Per-drop stats already exist on both clients — iOS `ProfileView` renders
redemptions and collects per drop, Android has a business dashboard — and both read straight
off the drop document. What does not exist is any view *across* an experience: no "your event
had N unlocks from M attendees, X redemptions across Y offers." That aggregate is what a
paying organiser wants, and the direction doc names organiser analytics and redemption
tracking as load-bearing for B2B revenue.

**Decision: compute it server-side into a rollup document, not client-side across drops.**

Why not client-side: aggregating would mean reading every drop in the experience on every
dashboard open — expensive, slow, and bounded by what the querying client is allowed to
read. It also produces a different answer per device depending on what happens to be
cached, which is a poor basis for a number an organiser is paying for.

Shape to build against:

- A rollup document per experience, written **only** by the Admin SDK, readable **only** by
  the experience owner. Client writes denied outright, as with redemption receipts.
- Maintained incrementally by Firestore triggers on `drops` — `FieldValue.increment` on
  collect and redeem transitions — rather than recomputed on read.
- Plus a scheduled reconcile that recomputes from source and corrects drift. Incremental
  counters drift when a trigger fails or retries; without reconciliation the number quietly
  becomes wrong, which is worse than being absent.

**Why this is only worth doing now.** These counts were not trustworthy before today. 4.2
made claims one-way, so `collectedBy` can no longer be farmed by collect/un-collect cycling.
4.3 moved `redemptionCount` server-side, so only the callable can increment it. Building a
rollup on top of forgeable counters would have produced numbers not worth reporting.

**Boundary — this is not pilot instrumentation.** The direction doc's success metrics
(invite → activation, ≥1 unlock, ≥3 unlocks, trail completion, would-use-again) are funnel
questions about *people*, and no amount of per-drop counting answers them. That is Phase 6.
4.4 must not absorb it.

**Privacy note for implementation:** `collectedBy` and `redeemedBy` already expose collector
uids to anyone who can read the drop, so the organiser can see them today. The rollup should
report **aggregates**, not per-attendee identity — there is no reason a dashboard needs to
name who redeemed, and doing so would sit badly beside the location-privacy work in Phase 3.

---

## P8 — Approved redesign alignment (added 2026-08-09, task R0)

The owner approved all seven fundamental recommendations and the inconsistency-resolution
table in `redesign-alignment-proposal.md`. These decisions are binding targets; they do not
claim the current clients or deployed backend already implement them.

1. **Server-authoritative unlock (supersedes P6 only where payload release/receipt creation
   still depends on a client-only proximity decision).** Split readable discovery metadata
   from the locked payload. A callable validates a one-shot precise fix, accuracy, age,
   active window, membership, configured radius, idempotency, and abuse controls before it
   returns content and writes an immutable receipt. Never persist or log the coordinates.
2. **Issued is not used (supersedes P6's use of “redeemed” at code generation and P7's
   matching counter).** Assign a server-held, pre-generated code on reward unlock and
   reserve inventory. Record confirmed business use separately through an owner-authorized,
   audited operation. Migrate existing pre-pilot receipts as issued, not proven used.
3. **Approved-organizer-only Pilot 1 creation.** Keep exactly two internal roles. `BUSINESS`
   means approved organizer; it is not a merchant employee role. Only an approved organizer
   that owns an Experience may create or mutate its event drops. Organizer application
   status, if stored, is server-authored workflow state rather than a third role.
4. **Keep private aggregate Results.** The already-built owner dashboard remains launch
   scope and reports no attendee identity. A founder report supplements it. When decision 2
   lands, Results separates codes issued from codes used.
5. **Android-first, iOS preserved.** Redesign and outdoor-qualify Android first. Preserve
   iOS source, security behavior, and backend compatibility; defer iOS visual parity until
   the Android pilot gate.
6. **Scheduled drops remain deferred.** Immediate publish and expiration stay in v1. Do
   not build a date picker without the server transition, notification, timezone, and
   recovery system an explicit later reversal would require.
7. **Qualify QR onboarding through real Play distribution.** Prefer a fail-closed
   production listing with owned-domain App Links, Install Referrer, and event-code fallback.
   Open testing is a disclosed fallback. Closed-test enrollment and sideloading cannot close
   the attendee-funnel gate.

Additional binding resolutions: participant tabs are Nearby/Collection/Account; guest
browsing precedes an account gate at the first unlock; precise permission begins only after
the unlock action; push permission follows the first success; Trail is the user-facing
ordered mechanic; `groups` may remain the Pilot 1 storage name behind an Experience facade;
earned payload snapshots are immutable; and Firebase Dynamic Links are not used.

**Sequence:** R1 defines contracts and migrations. R2 changes the server/rules boundary
before redesigned clients depend on it. Every later R task stops at the gate in
`redesign-alignment-proposal.md`. R0 changes documentation only.

---

## P9 — Split R5 local implementation from the production funnel (added 2026-08-10)

The owner approved postponing the HTTPS host and production-funnel work while the app is
being renamed and its independent identity is being established. This is a sequencing
change, not a waiver of the real-install funnel required by P8.

**Decision: split R5 into two gates.**

- **R5-L — local/device implementation:** automated verification, physical-device install,
  preview-first entry, guest/account transitions, permission recovery, and exact-resume
  behavior. Approved and complete on 2026-08-10.
- **R5-P — production funnel:** app-owned HTTPS host, release App Links association, real
  Play installation continuity, deployment and rollback approval, safe fixtures, and the
  complete external matrix. Deferred, but blocking before any pilot or public release.

This exception permits later local UI/UX design and implementation to proceed under each
task's normal approval gate. It authorizes no production deployment, backend enablement,
Remote Config change, Play publication, production-data mutation, or M4 cutover. R6 is the
next eligible task; eligibility is not automatic authorization.

**Authorization addendum:** after approving this split, the owner explicitly authorized R6
local implementation on 2026-08-10. That authorization includes the participant-loop
crash-stability checkpoint and the R6 scope in `redesign-alignment-proposal.md`; it does not
expand any production or later-task authority listed above.

**R5-P resumption addendum:** on 2026-08-14 the owner directed R5-P work to continue.
Read-only production audit and local fail-closed bundle preparation are authorized. This
does not pre-approve billing, Play developer enrollment or terms, account-security changes,
DNS writes, deployments, production-data mutation, or Play publication; those remain
explicit action-time decisions.

---

## P10 - Experience-first entry replaces the legacy account-choice landing (added 2026-08-12)

The old `GeoDrop / Step into a world of hidden drops` page is not a redesign target. It
placed an account choice before Experience context, which conflicts with P8 and the approved
R5 preview-first guest flow.

**Decision: remove the legacy landing and its duplicate registration screen.** Entry remains
Experience code or link -> Experience preview -> controlled anonymous guest browsing. Account
sign-in and creation remain available from Account and are required at the first unlock. If a
guest dismisses sign-in, signs out, or completes account deletion, the app returns to guest
browsing instead of revealing an account-choice wall.

This is an implementation alignment, not a scope change to `product-direction.md` or
`migration-plan.md`. Verified with the Android unit/lint/build gate and on the physical-device
DEMO2026 participant route.

---

## P11 - Product name is Kithe; technical identity migrates separately (added 2026-08-13)

The owner selected **Kithe** as the product name. Current user-visible Android, preserved iOS,
web/legal, notification, demo-fixture, and canonical documentation copy uses Kithe.

**Decision: separate the brand rename from the technical-identity migration.** During the
brand-only stage, compatibility identifiers remained unchanged, including
`com.e3hi.geodrop`, the `geodrop-dfcba` Firebase project, source-level `GeoDrop*` symbols,
stable storage/preferences, and existing App Link configuration. Renaming those prematurely
would have risked installed sessions, Firebase registration, data access, notification
delivery, and deferred R5-P work.

Before pilot or public release, the technical-identity gate must replace the E3HI-associated
application/bundle identifiers, register Kithe with its final Firebase and signing setup, and
complete the owned HTTPS/App Links path. This sequencing does not change the product behavior
or override `product-direction.md`; it keeps the already-approved independent-product decision
while avoiding a premature backend cutover.

---

## P12 - Kithe production domain is kitheapp.com (added 2026-08-13)

The owner selected **`kitheapp.com`** as Kithe's production base domain. The dedicated R5-P
entry host is planned as **`join.kitheapp.com`**, keeping Experience entry, App Links,
landing-page fallback, and rollback operations isolated from the root website.

The owner registered `kitheapp.com` through Cloudflare on 2026-08-13. Cloudflare reports the
registration active through 2027-08-13, with auto-renewal scheduled for 2027-07-14. WHOIS
redaction is enabled and DNSSEC activation is pending. No invoice, account, registrant, or
payment details belong in the repository.

Registration proves control of the base domain but does not complete the R5-P host gate. The
R5 client must retain the fail-closed `r5-unconfigured.invalid` placeholder until
`join.kitheapp.com` has working DNS and HTTPS, the hosting bundle is approved, and the
release App Link association is ready. No production hosting, App Link association, or
deployment is authorized by domain registration alone.

The matching Android application ID is **`com.kitheapp`**. The owner confirmed that
permanent identifier by authorizing creation of Kithe's independent Firebase identity on
2026-08-13. Package migration still requires the staged configuration and compatibility
checks below; the domain decision alone did not authorize a backend cutover.

---

## P13 - Independent Kithe Firebase identity is registered (added 2026-08-13)

The owner authorized and created the independent Firebase project **Kithe Production** with
project ID **`kithe-production`** on the Spark plan. Google Analytics was left disabled
because the creation wizard could not finish loading the Analytics-account selector; it can
be enabled later from Project Settings > Integrations after an account is confirmed.

The Android app **Kithe Android** is registered in that project with package
**`com.kitheapp`**. The current local debug certificate's SHA-1 and SHA-256 fingerprints are
registered for device testing. This initially staged the generated configuration separately
without changing the working client; P14 records the later authorized Android cutover.

---

## P14 - Android technical identity moved to Kithe; paid services remain gated (added 2026-08-13)

After the owner said to continue, Android's namespace, application ID, Kotlin packages,
instrumentation packages, FileProvider authority, notification actions, Play fallback ID,
Firebase default alias, and active `google-services.json` moved to **`com.kitheapp`** and
**`kithe-production`**. Stable preference filenames and source-level `GeoDrop*` UI symbols
remain compatibility details; they are not package ownership claims.

Kithe Firebase Authentication is initialized with Anonymous and Google enabled. The default
Standard Firestore database exists in **`nam5`**, matching the legacy project's location,
with no data and deny-all production rules. The old project's web client ID was removed from
the Android resources. On 2026-08-13, the owner authorized Cloudflare Email Routing DNS
for `kitheapp.com` and an active `support@kitheapp.com` rule forwarding to the verified owner
inbox. Cloudflare reports routing enabled, and public DNS resolves its three MX records plus
the expected SPF and DKIM records. The forwarding alias was registered as the Google identity
**Kithe Support**, added to Kithe Production as an Editor (the least project role eligible for
the OAuth support-email selector), and configured with privacy-minimal Google account settings.
After the owner explicitly accepted the separate Firebase terms for that identity, Google
sign-in was enabled with public-facing name **Kithe** and support email
`support@kitheapp.com`. The refreshed Android Firebase configuration and generated web client
ID are now active in the local project.

The migrated debug APK passed 118 unit tests, lint, and assembly, then installed and launched
on the physical Samsung as `com.kitheapp`. `DEMO2026` reached preview, policy acceptance, and
the participant surface without a fatal crash. Because the application ID changed, Android
installs it alongside the legacy package and does not transfer that package's local test state
or Firebase session automatically. This fresh test state is compatible with P3's prototype
reset decision and does not oppose `product-direction.md`.

After Google Authentication was enabled and the refreshed OAuth configuration was installed,
the physical Samsung opened the Google account chooser, completed Explorer sign-in, and
returned to Kithe without an Android runtime failure. The Account surface no longer showed the
Guest state, and the local `DEMO2026` Collection progress remained available after the account
upgrade. No account address or credential is stored in this evidence.

App Check is initialized but remains non-enforcing. The physical debug build's secret is
allowlisted in Firebase without storing it in the repository. On 2026-08-13, the owner
explicitly accepted the Google APIs and Play Integrity API terms and authorized registration;
Kithe Android is now registered with the Play Integrity attestation provider using a one-hour
token lifetime. Firestore and Authentication remain unenforced. Crashlytics has detected the
Kithe Android app and is waiting for its first crash; no deliberate production crash was
generated. Firebase Messaging is available for the project, but no campaign was sent and
Analytics remains disabled.

Local backend verification passed Functions lint/build, the redesign contract, fail-closed
Remote Config validation, local operations readiness, and all 18 Firestore/Storage emulator
rule suites. The collection-group index validator exposed five missing `schemaVersion`
single-field declarations used by the redesign migration audit; the local index manifest now
contains them and the validator passes. Nothing from that backend bundle has been deployed.

The remote cutover is not complete. Maps SDK for Android is disabled in the Kithe Google Cloud
project and requires accepting Google Maps Platform terms plus an approved billing setup; the
first migrated build therefore reported a Maps authorization failure. The app now fails
closed to the usable List view, disables the Map selector, and explains that map setup is
pending until a dedicated `GOOGLE_MAPS_API_KEY` is provided. Firebase Storage and Cloud
Functions also require a Blaze upgrade. No billing account was linked, no terms were accepted, no
Firestore data or indexes were created, and no rules, Functions, Storage, Hosting, Remote
Config, App Check enforcement, Analytics, or Play release was deployed. The production App
Link host remains the fail-closed placeholder.

---

## P15 - R10 is split into local qualification and pilot qualification (added 2026-08-13)

The owner approved splitting R10 into **R10-L** and **R10-P** and authorized R10-L only.
This is a procedural sequencing amendment, not a change to `product-direction.md` or the
Pilot 1 acceptance bar.

On 2026-08-14 the owner reviewed the completed R10-L automated, accessibility, and
physical-device evidence and explicitly approved and closed R10-L. R10-P remains
unauthorized and requires a separate owner authorization.

R10-L may audit and fix the local Android UI/UX, run automated and accessibility checks,
exercise debug-only poor-network/permission/GPS states, install debug review builds, and
collect physical-device evidence. It may not enable or purchase Maps/Firebase services,
deploy backend or configuration, mutate production data, publish Analytics or Remote Config,
enforce App Check, create a Play release, configure HTTPS/App Links, sign a release, use
external pilot accounts, or run Pilot 1.

R10-P retains every paid-service, production, Play, HTTPS, outdoor/cross-device, operational,
release, and pilot gate. R10-L evidence cannot close or waive R5-P or the open R6-R9
pre-pilot dependencies. A separate owner authorization is required before R10-P begins.

---

## P16 - Kithe visual mark approved and adopted locally (added 2026-08-14)

The owner approved the local Kithe visual package: a cream `K` drawn as a three-point trail
on deep teal, an amber nearby destination, the restrained feature graphic, and the six-shot
participant-first Play capture plan. This replaces the legacy turquoise media-pin identity,
whose photo/video/audio symbols opposed the Kithe rename and Pilot 1's text/photo boundary.

Android now uses the approved geometry for the adaptive foreground, round icon, and every
density fallback. The matching local website favicon and touch-icon assets are prepared but
not deployed. iOS adoption remains deferred under the Android-first sequence. The Play icon
and feature graphic are not uploaded.

The updated debug APK passed 127 unit tests, lint, and assembly, installed on physical Samsung
`R5CY114LNCE`, displayed the Kithe icon and label correctly through Android's masked App info
surface, and launched the Kithe participant UI with no fatal AndroidRuntime or Crashlytics log.
Existing device data was preserved. Final store screenshots remain gated on the exact signed
release candidate, approved reviewer fixture, Maps setup, production policies, and release
audit. This decision does not authorize billing, deployment, policy publication, Play upload,
or submission.

---

## P17 - Kithe is intended for two-owner operation; legal form remains open (added 2026-08-14)

The owner clarified that Kithe will be a partnership with two owners. This supersedes the
temporary policy recommendation to name one individual as Kithe's sole operator. It does not
yet establish whether the Hawaiʻi structure will be a general partnership, LLP, or two-member
LLC; “members” is LLC terminology, while partnerships have partners.

The owner subsequently confirmed that Kithe is not registered. The formation-readiness
comparison is recorded in `legal-drafts/two-owner-formation-readiness.md`; it recommends a
two-member LLC only as the default form to evaluate, not as a selected or authorized filing.

No public policy, Play listing, or backend legal manifest may assert a final operator until
the owners select the form, complete the applicable Hawaiʻi BREG filing, supply the exact
registered legal name and formation date, and designate an authorized policy approver. Kithe
remains independent from E3HI.

The existing Play developer account remains personal. Google currently supports an official
personal-to-organization conversion after website verification and organization identity,
payments-profile, D-U-N-S, contact, and verification requirements are satisfied. Conversion
timing is a new pre-release owner gate; this decision does not authorize registration,
payments changes, D-U-N-S work, account conversion, publication, or submission.

The owner placed LLC/entity formation on hold on 2026-08-14. Until explicitly resumed, do
not perform or imply entity selection, registration, name reservation, EIN, D-U-N-S, banking,
organization payments-profile work, or Play account conversion. The two-owner intent remains
context only; it is not a registered legal operator. Local app, UI, listing, and Data safety
preparation may continue without closing LP-1.

