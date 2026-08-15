# Kithe R4 — Navigation Shell and Gate Evidence

Status: **Approved — R4 gate complete; R5-L approved and R5-P deferred**  
Date: 2026-08-10  
Authority: approved sequence in `redesign-alignment-proposal.md`  
Production state: **no deployment, Remote Config change, production read/write, data
migration, backend enablement, or M4 cutover was performed**

Approval record: on 2026-08-10 the owner approved every R4 gate item after the verified
R4 APK was reinstalled in place and launched successfully on the connected SM-S938U. This
closes R4 and explicitly authorizes R5. No R5 implementation or production action was
performed as part of this approval.

## 1. Outcome

R4 replaces the Android participant shell with **Nearby**, **Collection**, and **Account**
as its only participant tabs. Nearby has one active Experience, an Experience switcher,
and a join path when none is active. Approved organizers retain the same participant shell
and reach their existing tools through Account; organizer access is not a fourth tab or a
separate permanent mode.

No R4 implementation decision opposes `product-direction.md` or `migration-plan.md`.
Existing `groups` memberships remain the storage adapter and are presented as Experiences,
as approved in R0. Existing aggregate organizer tools remain available, as required by F4.
The complete QR/auth/permission entry flow remains R5, participant screen rebuilding
remains R6, and organizer application/authoring redesign remains R7.

## 2. Implemented shell

### Participant navigation

- A typed `ParticipantDestination` policy permits only Nearby, Collection, and Account.
- Legacy saved defaults migrate safely: Collected becomes Collection; Discover, My Drops,
  missing, and unknown values become Nearby.
- The legacy Profile / Drop Something / Manage Groups bottom actions and floating
  Discover / My Drops / Collected controls are no longer rendered.
- Collection is combined across all joined Experiences rather than filtered by the active
  Nearby Experience.

### Experience context

- Nearby always shows the active Experience in its top bar and exposes a switcher for all
  joined Experiences.
- A missing or removed selection falls back deterministically to the first membership;
  no memberships resolve to no active Experience.
- The no-Experience state explains the next step and provides the required join action.
- The transitional R4 code dialog can only join an existing Experience. It calls the
  existing adapter with `allowCreateIfMissing = false` and exposes no creation or removal
  action. R5 owns the complete QR, App Link, preview, and manual-code flow.
- Until the later Experience contract supplies display names, the header uses the honest
  transitional label `Experience <code>` rather than inventing a name.

### Account and organizer authorization

- Account contains identity, permission recovery, organizer status, joined Experiences,
  privacy/data actions, profile/sign-in, and sign-out.
- Accounts without approved organizer access see status copy only; they do not receive an
  organizer-tools action.
- Approved organizers (the existing internal `BUSINESS` role) receive **Open Organizer
  tools** inside Account. The existing aggregate dashboard is nested behind that action,
  has an explicit Back to Account path, and does not replace participant navigation.
- Selecting any participant tab while organizer tools are open returns to the shared
  participant shell.

### State preservation

- Selected participant destination and active Experience are saveable.
- A saveable-state host retains independent Nearby UI state per Experience and shared
  Collection and Account state across tab/Experience switches.
- Sign-out, account deletion, or role removal closes organizer tools and returns to a safe
  participant destination.

## 3. Gate verification

Automated and build checks run locally on 2026-08-10:

| Check | Result |
| --- | --- |
| R4 policy and Robolectric UI suite | Pass: 10 tests |
| Complete Android unit/Robolectric suite | Pass: 65 tests, 0 failures, 0 errors, 0 skipped |
| Three-tab reachability and legacy-action absence | Pass |
| Experience switch/join/no-Experience reachability | Pass |
| Approved/unapproved organizer authorization | Pass |
| 48 dp R4 action targets | Pass |
| Tab and per-Experience state restoration | Pass |
| Android lint | Pass: 0 errors; 129 existing advisory warnings, none in the R4 navigation source |
| Debug APK assembly | Pass |
| Android test APK compilation | Pass |

The final debug APK SHA-256 is
`D2D4BB73124655FE2C2D15AE3FB9B23B8086EF7762B6065CB5DD9B5E9DF25278`.
It installed in place successfully on the connected SM-S938U, preserving app data. The
process started and `MainActivity` was the focused app beneath the device notification
shade. This is an install/launch smoke check, not a recorded manual traversal of both
account roles.

## 4. R4 approval checklist

- [x] Nearby, Collection, and Account are the only participant tabs.
- [x] Active Experience header/switcher and no-Experience join state are implemented.
- [x] Participant join UI cannot create or remove Experiences.
- [x] Collection is combined across Experiences.
- [x] Approved organizer tools are under Account and never become a fourth tab or permanent
  mode.
- [x] Unapproved accounts receive no organizer-tools action.
- [x] Participant and approved-organizer reachability is covered by policy and UI tests.
- [x] Tab and Experience state preservation is covered by UI tests.
- [x] Full tests, lint, debug assembly, instrumentation APK compilation, and physical-device
  install/launch smoke check pass.
- [x] Owner accepted the complete R4 gate and explicitly authorized R5 on 2026-08-10.

R4 stops here complete. R5 entry/auth/permission work is implemented and its R5-L
local/device gate is approved. R5-P is deferred but remains mandatory before a pilot or
public release. The owner explicitly authorized R6 participant-screen rebuilding for local
implementation on 2026-08-10; R7 organizer access/authoring, production deployment, and M4
remain outside that authorization.
