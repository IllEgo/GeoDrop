# GeoDrop — Migration Decision Record (Task 0.3)

An ADR per deferred feature: **delete the code or gate it behind a disabled flag? delete,
orphan, or archive the data?** — with a recommendation and reasoning for each. This is the
last Phase 0 task; once signed it becomes the spec that Phases 1 (rules) and 2 (client
removal) execute against.

- **Status:** Proposed — awaiting owner sign-off (the 0.3 gate).
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
| Complex voting (dislikes) | No | DELETE dislike UI + fields; KEEP likes | **Remove `dislikedBy`/`dislikeCount` from allowed reaction transitions** (`hasValidSocialTransitions`, both update helpers) | wipe (2 test drops) | 1.3 → 2.6 |
| Algorithmic recommendations | No | none exists | n/a | none | — (do not build) |
| Multiple account types w/ extensive matrices | No | **KEEP** explorer + business only; no extended matrix to remove | Role immutability already enforced; keep | wipe resets roles | 2.7 (verify) |
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
