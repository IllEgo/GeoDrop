# Kithe — Account model

Status: current as of task **2.7** (2026-07-29). Spec source: `docs/product-direction.md`
(deferred list, "Multiple account types with extensive permission matrices") and the 0.3
ADR row of the same name in `docs/migration-decisions.md`.

This document is the answer to "who can do what, and where is that decided." It also
records the migration path for accounts that predate the model, which is task 2.7's
acceptance criterion.

---

## The model

There are exactly **two account types**, and one field holds the answer: `users/{uid}.role`.

| Type | Stored value | What it adds |
| --- | --- | --- |
| Explorer | `EXPLORER` | The default. Discover, unlock, collect, like, report, join invite-only groups, create community drops. |
| Business / organizer | `BUSINESS` | Everything an explorer can do, plus non-`COMMUNITY` drop types (`TOUR_STOP`, `RESTAURANT_COUPON`, …) attributed to itself via `businessId == uid`. |

There is no third type, no tier, and no permission matrix. Everything else that varies by
principal is deliberately **not** an account type:

| Axis | Where it lives | Why it is not an account type |
| --- | --- | --- |
| Group owner vs. subscriber | `users/{uid}/groups/{code}.role` — `OWNER` \| `SUBSCRIBER` | Scoped to one group, not to the account. Written only by the `manageGroup` callable. |
| Moderator / admin | Auth custom claims `moderator`, `admin` | Operational staff access to the moderation callables. Not a user-facing type, not stored on the profile, not self-grantable. |
| Suspended | Auth custom claim `suspended` (+ `moderationStatus` on the profile for display) | A state, not a type. `isSignedIn()` in `firestore.rules` fails closed on the claim, so a suspended principal loses every write path at once. |
| Guest | Anonymous Firebase Auth, no `role` at all | Viewing only. `isNonAnonymousUser()` gates creation (task 1.2). |

## Approved redesign target (not live until R2)

Task R0 approved a narrower Pilot 1 creation contract without adding an account type:

- `EXPLORER` remains the default internal role, but Pilot 1 participants do not receive a
  public/community creation surface or authorization path. They browse joined Experiences,
  unlock, collect, report, and manage their account.
- `BUSINESS` remains the second and only elevated role. In redesigned product copy it means
  an approved **Organizer**. It is not a merchant employee account.
- Only a `BUSINESS` account that owns the Experience may create or mutate that Experience's
  drops. Ownership remains scoped membership state, not a role.
- Organizer applications use founder review. If the app needs `pending`/`approved`/`denied`
  display state, add a server-authored `organizerAccessStatus`; this is workflow state, not
  a role, and it must be added to every profile allow-list/writer test in the R2 change.
- Guest sessions remain anonymous Firebase Auth for continuity, but are view-only: preview,
  join, and browse are allowed; unlock, Collection writes, creation, and reward issuance
  require a linked non-anonymous account. Resume the pending unlock after linking/merge.
- "Explorer" never appears as an identity in participant UI. Guest-facing content calls
  the Experience creator the **host**; professional/account tooling may say Organizer.

**Migration boundary:** the table above and the writer descriptions below still document
the deployed task-2.7 implementation. In particular, authenticated Explorer community
creation and verified-email `updateBusinessProfile` promotion remain deployed behavior
until R2 is separately approved and deployed. R2 has changed local source so profile edits
require prior organizer approval and only an admin/operator decision can elevate the role.
Do not present that source change as live security evidence before the deployment gate.

## Who may write what

Profile fields are split by author. `firestore.rules` enforces the split; the split is
named in the rules themselves so the two lists cannot drift apart silently.

**Client-authored** — `displayName`, `username`, `nsfwEnabled`, `nsfwEnabledAt`,
`createdAt`.

`nsfwEnabled`/`nsfwEnabledAt` are **legacy**. No client writes them any more (task 2.8
removed the NSFW preference along with the pilot flag) and nothing reads them. They stay
in the allow-list because documents created before 2.8 still carry them, and
`hasOnlyAllowedUserFields()` sees the merged document — dropping them from the list would
lock every one of those profiles out of its own updates. `hasPilotSafeContentPreference()`
still pins any surviving value to `false`.

**Server-authored** — `role`, `organizerAccessStatus`,
`organizerAccessSubmittedAt`, `organizerAccessReviewedAt`, `businessName`,
`businessCategories`, `moderationStatus`, `suspendedAt`, `suspendedBy`,
`suspensionReason`, `suspensionCaseId`, `reinstatedAt`, `legalAcceptanceVersion`,
`legalAcceptedAt`.

**Every Admin-SDK writer of `users/{uid}` must appear in the server-authored list.** A
field written there but missing from the list locks the account out of its own profile:
`hasOnlyAllowedUserFields()` sees the *merged* document, so one stray key makes every
later client update fail. The writers today are `setOrganizerAccessDecision`,
`ingestOrganizerApplication`, `updateBusinessProfile`, `claimExplorerUsername`,
`acceptLegalPolicies`, and the moderation callables. Add to the list when adding a writer.

Server-authored fields may be **present** on a document a client updates — the Admin SDK
writes them and bypasses rules — but a client can neither introduce, change, nor remove
one (`preservesServerAuthoredFields()`, plus `preservesRole()` for `role`). Two
consequences worth stating plainly:

- **Historical deployed path:** before R2 is deployed, the only path to `BUSINESS` is the
  verified-email `updateBusinessProfile` callable. In R2 source that callable cannot
  elevate; admin/operator approval is the only target path. Before 2.7 a client could
  write itself a `businessName` directly, and
  the Android client then *inferred* `BUSINESS` from that metadata — unlocking the
  business UI for an account the server still treated as an explorer, with no verified
  email. Both halves are gone: the rules refuse the write, and the clients trust only the
  stored `role`.
- **A reinstated account can still edit its profile**, and so can one that accepted the
  legal policies. Moderation writes leave `moderationStatus`/`reinstatedAt` behind after an
  overturned appeal, and `acceptLegalPolicies` stamps
  `legalAcceptanceVersion`/`legalAcceptedAt` on every account that accepts. None of these
  were in the allow-list before 2.7, so their presence silently refused every later profile
  update — the legal-acceptance one on the path every account takes. They are named now.

At create time a profile must be `role: 'EXPLORER'`, may carry only client-authored
fields, and may not claim a `username` (usernames are allocated by the
`claimExplorerUsername` callable, which owns the `usernames/{name}` uniqueness record).

`role` is compared **exactly** in the rules (`role == 'BUSINESS'`), so both clients parse
it exactly too — `UserRole.fromRaw` (Kotlin) and `UserRole.from(raw:)` (Swift) do not
case-fold, and anything off-model resolves to `EXPLORER`, the least-privileged type.
A client that accepted `"business"` would show the business surface to an account the
server treats as an explorer.

Enforcement evidence: `firestore-tests/accountRoleRules.test.js` (rules layer),
`app/src/test/java/com/e3hi/geodrop/data/AccountRoleTest.kt` (client parsing),
`firestore-tests/adversarialRules.test.js` (escalation attempts).

---

## Migration path for existing accounts

The 1.4 wipe deleted prototype content but preserved accounts: **25 profile documents and
22 Auth users** in `geodrop-dfcba` (see `docs/data-inventory.md`). Those documents were
written by clients that inferred roles and may not carry a canonical `role` at all, so the
model above needs one server-side pass over them.

**Tool:** `functions/scripts/normalize-account-roles.js`
(`npm --prefix functions run roles:check` / `roles:apply`). Dry-run by default; `--apply`
requires `--confirm-project=geodrop-dfcba`. It writes **only** the `role` field, only with
one of the two launch values, and never touches business metadata, Auth, or any document
it did not list in the dry run. It is idempotent.

What it does, per profile:

| Case | Action |
| --- | --- |
| `role` already `EXPLORER` or `BUSINESS` | Nothing. |
| `role` recognized but non-canonical (`business`, padded whitespace) | **Normalize** to the uppercase form. Such a value is a silent explorer on the server. |
| `role` absent or empty, business metadata present | **Backfill** `BUSINESS`. This is the one-time server-side replacement for the client-side inference 2.7 deleted — it now happens once, auditably, instead of on every profile load. |
| `role` absent or empty, no business metadata | **Backfill** `EXPLORER`. |
| `role` is anything else (`ADMIN`, a number, …) | **Flag only.** Not rewritten — see below. |

It also reports, read-only, the profile/Auth mismatch that 0.2 flagged for this phase:
profiles with no Auth user (harmless — nobody can sign in as them) and Auth users with no
profile (created on next sign-in). Neither is deleted here; account deletion has its own
audited path (`accountDeletionReceipts`, `npm --prefix functions run account:audit`).

**Off-model roles need a human decision.** Under the 2.7 rules, `preservesRole()` refuses
to carry forward a role that is not one of the two launch types, so such a profile cannot
update itself until the value is corrected. That is deliberate — failing closed beats
guessing which type a stray value meant — but it means a flagged profile is a locked
profile. The dry run lists them individually; each needs an explicit choice of type before
the account is usable. The 0.2 inventory found no such values, so the expected count is
zero.

**Rehearsal, not a promise:** `npm --prefix functions run roles:rehearse` proves all seven
safety properties against the emulator (dry run is inert, wrong-project `--apply` refuses,
each classification lands correctly, business metadata is never touched, second run is a
no-op). It runs in CI's `p0-rehearsals` job. Production credentials were not available in
this session, so the prod dry run is the owner's step — the same split used at 1.4.

### Order of operations

1. Deploy the 2.7 rules (`firebase deploy --only firestore:rules`).
2. Run `roles:check` against prod and review the plan.
3. Run `roles:apply --confirm-project=geodrop-dfcba`.
4. Resolve any flagged profiles by hand.

Steps 1 and 3 are independent for explorers, but **do them in this order for business
accounts**: an older installed build could otherwise write a `businessName` that the new
rules will later refuse, and a `BUSINESS` account whose `role` is still absent reads as an
explorer on a 2.7 client until step 3 runs.

### Operational caveat

Older installed builds (Android and iOS) send `businessName`/`businessCategories` when
they create a profile, and backfill `businessCategories` on a profile that lacks it. After
these rules deploy both writes are refused, so on an old build **first sign-in cannot
create a profile at all**, and a profile missing `businessCategories` fails its load path.
New builds send neither field. This is the same class of caveat as 2.6's ("an old build can
no longer like at all"), one step sharper because it hits onboarding rather than a single
interaction — the pilot must ship current builds.
