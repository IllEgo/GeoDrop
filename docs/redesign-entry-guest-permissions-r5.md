# GeoDrop R5 — Entry, Guest, Account, and Permissions

Status: **R5-L approved and complete; R5-P production funnel deferred**  
Date: 2026-08-10  
Next task: **R6 local implementation authorized; no production action is authorized**

This record covers only R5 from `redesign-alignment-proposal.md`. No target functions,
hosting, App Links association, Remote Config, production data, Play release, or M4
cutover was deployed or changed.

Approval record: on 2026-08-10 the owner approved splitting R5 into a local/device gate
and a production-funnel gate. The evidence in this record closes **R5-L**. The owned host,
App Links association, Play distribution, deployment bundle, production-safe fixtures, and
external cold-install matrix remain together in **R5-P**, which is deferred but mandatory
before any pilot or public release.

## Outcome

The app now has the approved entry orchestration:

- strict `https://<owned-host>/e/<code>` parsing for installed App Links;
- canonical QR/share and Play Install Referrer payloads with one 128-bit entry session;
- referrer recovery, 24-hour pending-entry continuity, and manual code fallback;
- Experience preview before visible account, permission, onboarding, or map surfaces;
- silent anonymous Firebase session for view-only guest join/browse;
- first-Unlock account gate using the existing link-in-place/new-account and
  merge/returning-account paths;
- persisted exact drop target and automatic resume after account and precise permission;
- contextual approximate primer, one-shot precise primer, denial/settings recovery, and
  a once-per-Experience notification primer after the first successful collection;
- best-effort branch analytics that never blocks entry or recovery;
- an undeployed `/e/**` hosting rewrite and metadata-only landing function with an encoded
  Play referrer; and
- English and Spanish entry, account-gate, permission, recovery, and notification copy.

The build is deliberately fail-closed when no approved host is supplied. Its host is
`r5-unconfigured.invalid`, `APP_LINK_CONFIGURED` is false, Android reports link handling
disabled for that host, and Gradle rejects `geodrop.app`. That domain belongs to an
unrelated product and is not an option.

The human-code compatibility decision is also explicit: input accepts spaces and visual
dashes but compacts them to the R2 alphanumeric document ID. An eight-character code is
displayed as `ABCD-EFGH`. The future host-side generator remains R7 work; R5 does not
pretend the current random ID is a word-based code such as `ORCHID-42`.

## Implementation map

| Requirement | Implementation |
| --- | --- |
| Installed App Link | Manifest `autoVerify` filter plus `MainActivity.onNewIntent`; strict configured-host parser |
| Not-installed continuity | Play Install Referrer 2.2 reader, canonical encoded referrer, entry session and pending store |
| Web fallback | `experienceEntryPage` function and Firebase Hosting `/e/**` rewrite, both undeployed |
| QR/share artifact | Canonical `R5EntryLinks` QR payload and Play URL; host presentation remains part of the R7 organizer surface |
| Preview first | `R5EntryFlow` resolves metadata into a dedicated preview before the participant shell |
| Guest | Anonymous Auth session can resolve, join, and browse; guest buttons reach the account gate instead of becoming dead controls |
| New/returning account | `GuestAccountUpgrade` LINK/MERGE paths retained; unlock gate hides account types and username/profile setup |
| Exact resume | Experience/drop target persists across process death; account and precise grants resume that target |
| Approximate | User invokes location from Nearby, sees primer, then gets a coarse-only system request |
| Precise | First Unlock persists target, then shows one-shot privacy primer and request/settings recovery |
| Push | Feature-flagged, Experience-scoped, once after first collection; no join/map prompt |
| Analytics | `app_first_open`, auth completion, unlock-account-gate, and permission-result events use the R2 contracts |

## R5-L verification evidence

- Android compilation, debug APK, and instrumentation APK: passing.
- Final Android unit suite: 81 passing, 0 failed, including 16 R5-specific parser, store,
  QR/referrer, preview-ordering, and exact-resume policy tests.
- Android lint: 0 errors, 130 warnings, and 11 information items. The existing warning
  backlog remains; the R5 entry/share/resume files add no lint issue.
- Functions: TypeScript build and ESLint pass.
- Redesign function contract: pass, 21 canonical events, 25 m new-drop radius, 50 m
  maximum accepted boundary.
- Physical device: Samsung SM-S938U (`R5CY114LNCE`). Final APK SHA-256
  `8D1EE45599AFD2A6C8EDB906D67E7822BC92820046C9CD79CC083245C3A7A8C0`; reinstalled with
  `-r`, so existing app data was preserved.
- Focused device test: `R5EntryDeviceTest` passed (`OK (1 test)`), proving the installed QR
  request displays Experience preview while Sign in and location-permission surfaces are
  absent.
- Explicit HTTPS intent reached `MainActivity` and showed the reason-specific, retry-safe
  unavailable state because the R2 target is intentionally undeployed. The recovery
  action cleared the persisted test link and left the app on manual Experience entry.
- Android package inspection confirmed the configured `/e/` `autoVerify` filter and the
  expected disabled/unverified state for `r5-unconfigured.invalid`.

## Debug-only device continuation added 2026-08-10

Because the target R2 callables remain undeployed and the connected device currently
receives Firebase Installations `API_KEY_SERVICE_BLOCKED`, a clean debug reinstall had no
real code capable of resolving the manual-entry screen. The debug APK now recognizes
`DEMO2026` as **GeoDrop Device Demo** and supplies a visibly labeled
`DEBUG-DEMO-NOT-PRODUCTION` policy manifest. Both fixtures are selected only when
`BuildConfig.DEBUG` is true; release and internal builds still require the Firebase entry
and legal-policy gateways and fail closed. The demo records only local membership and
local debug-policy acknowledgement, and is not production acceptance evidence.

The code-to-preview path was exercised on the physical Samsung and the membership was
saved. A follow-up device check found that authenticated legacy sync could immediately
replace the local fixture with an empty remote membership list; the debug demo now restores
`DEMO2026` on launch and pauses that remote membership sync only while the local demo is
active. Two cold launches confirmed the active header remains **Experience DEMO2026**.

On 2026-08-11, a second device follow-up found that entry was local but the participant
surface still used the legacy Firebase query. Guests therefore had no useful demo map data,
while a signed-in explorer could receive `PERMISSION_DENIED: Missing or insufficient
permissions`. The debug build now injects a complete local R6 participant fixture for
`DEMO2026`: four non-secret discoveries, trail progress, one saved Collection receipt,
local unlock/reward behavior, reporting, and host blocking. Discovery is identity-neutral,
so the guest Nearby map/list does not require Firebase authentication; Collection remains
account-only in the UI. Release and internal builds do not receive this fixture.

The updated debug APK was installed on the physical Samsung and cold-launched. Device UI
inspection confirmed a rendered Google Map, the local trail, all four List discoveries,
and the seeded signed-in Collection receipt, with neither `PERMISSION_DENIED` nor its
missing-permissions message present. This is local demo evidence only and does not satisfy
the R5-P production fixture or deployment gates.

## R5-P production-funnel gate — deferred

The local implementation gate is approved. The following product and production inputs are
not code defects, but every one of them must be completed before R5-P can close:

1. **Choose an app-owned HTTPS host after the app identity is settled.** The app is an
   independent product that E3HI may use; its App Link identity must not depend on or imply
   ownership by E3HI. Prefer a dedicated subdomain of the eventual app-owned domain because
   it isolates App Link and landing-page operations. `geodrop.app` is prohibited.
2. **Provide the Play App Signing SHA-256 certificate fingerprint.** Publish the release
   fingerprint in `/.well-known/assetlinks.json`. If debug-domain testing is wanted, use
   a separate staging host/fingerprint; do not add a debug key to the production claim.
3. **Approve the production installation route.** The signed F7 recommendation is a
   fail-closed production Play listing while Experiences remain invite-only. The accepted
   alternative is Play open testing with the extra opt-in step disclosed and measured.
   Sideloading and closed-test-only enrollment cannot satisfy the cold-install gate.
4. **Approve a deployment bundle and rollback window.** R5 preview needs the undeployed R2
   callables, the new landing function, Hosting rewrite, owned-domain binding, and correct
   `assetlinks.json` to ship as one reviewed bundle. This requires a separate production
   authorization; it was not inferred from approval to implement R5.
5. **Approve a safe test Experience.** The full matrix needs an invite-only schema-v2
   Experience with metadata and non-secret discovery records. Creating or migrating that
   production data is a separate mutation approval; a staging Experience is safer for the
   first rehearsal.
6. **Provide a never-installed Android test path and account fixtures.** The gate still
   needs a clean second device (preferred) or explicit permission to clear a dedicated
   test device, plus one new account credential and one returning account credential.
7. **Run the external matrix.** After items 1-6: installed QR, never-installed QR through
   Play, stripped referrer, manual code, LINK, MERGE, denied approximate, coarse-only,
   denied/blocked precise, granted precise, and exact resumed unlock. Analytics continuity
   must be checked by entry session, without logging raw location or payload.

## Direction and migration alignment

The R5-L/R5-P split does not oppose `product-direction.md`, because the full external funnel
remains required before the pilot. It intentionally amends the strict stop-before-R6
sequence previously recorded in `migration-plan.md`; that procedural exception is approved
only for local redesign work and is recorded in both documents.

- Play is an installation handoff, not the acquisition strategy. Events and the existing
  E3HI relationship remain the distribution/acquisition channel.
- The R5 exact-resume orchestration currently hands off to the legacy client unlock path.
  R6, if later approved, replaces that participant loop with the already-built
  server-authoritative boundary. R5 does not pull R6 forward.
- Host QR presentation is prepared as a canonical artifact contract but remains in the R7
  organizer Experience surface, preserving the ordered migration.

- [x] **R5-L — local/device implementation:** approved and complete on 2026-08-10.
- [ ] **R5-P — production funnel:** deferred; blocks pilot/public release and R10 pilot
  authorization until every item above passes.

The owner explicitly authorized R6 local implementation on 2026-08-10. Later local
design/implementation tasks still require their normal gates. Do not deploy the external
funnel, enable the target backend, change Remote Config, mutate production data, publish a
Play release, or perform M4 under this split or the R6 authorization.
