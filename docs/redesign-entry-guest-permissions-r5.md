# Kithe R5 — Entry, Guest, Account, and Permissions

Status: **R5-L approved and complete; R5-P prerequisite work authorized and in progress**
Date: 2026-08-14
Next task: **Review and explicitly approve or reject A5b upload-key and internal-test candidate**

Parallel local task: **Review and approve the prepared Play listing and Data safety
package in `play-listing-data-safety-package.md`; no Play submission is authorized**

This record covers only R5 from `redesign-alignment-proposal.md`. A3 deployed the reviewed
fail-closed backend, safe fixture, and generated Firebase host; A4 connected and verified the
dedicated custom domain. No Remote Config release flag, legal-policy backend, Play release,
QR distribution, or M4 cutover was deployed or changed.

Approval record: on 2026-08-10 the owner approved splitting R5 into a local/device gate
and a production-funnel gate. The evidence in this record closes **R5-L**. The owned host,
App Links association, Play distribution, deployment bundle, production-safe fixtures, and
external cold-install matrix remain together in **R5-P**, which is in progress and mandatory
before any pilot or public release.

On 2026-08-14 the owner directed work to continue with R5-P. This authorizes its read-only
production audit and local fail-closed bundle preparation. It is not blanket authorization
to purchase services, accept Play/Firebase terms, change account security, write DNS,
deploy Hosting/Functions/rules, create production data, or publish a Play release; each such
mutation remains an explicit action-time gate.

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
`DEMO2026` as **Kithe Device Demo** and supplies a visibly labeled
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

## R5-P production-funnel gate — in progress

The local implementation gate is approved. The following product and production inputs are
not code defects, but every one of them must be completed before R5-P can close:

1. **Configure the registered app-owned HTTPS host.** The owner registered
   `kitheapp.com` through Cloudflare on 2026-08-13, with `join.kitheapp.com` as the planned
   dedicated App Link host. Registration is active, auto-renewal and WHOIS redaction are
   enabled, and a 2026-08-14 read-only check confirmed DNSSEC is active. Base-domain
   ownership is therefore satisfied;
   DNS, HTTPS hosting, and the dedicated entry subdomain remain open and the client must stay
   fail-closed until those checks pass. The app is an independent product that E3HI may use;
   its App Link identity must not depend on or imply ownership by E3HI. `geodrop.app` remains
   prohibited.
2. **Provide the Play App Signing SHA-256 certificate fingerprint.** Publish the release
   fingerprint in `/.well-known/assetlinks.json`. If debug-domain testing is wanted, use
   a separate staging host/fingerprint; do not add a debug key to the production claim. The
   current debug SHA-1 and SHA-256 are registered only on the Kithe Firebase Android app for
   local device testing; the Play release fingerprint is still outstanding.
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

### R5-P resumption audit recorded 2026-08-14

- Cloudflare shows DNSSEC active. No `join.kitheapp.com` DNS record exists yet, so the
  dedicated entry host remains safely disconnected.
- Kithe Production remains on Spark. Firebase Hosting is uninitialized, and neither the
  `experienceEntryPage` landing function nor the R2 callables are deployed. The approved
  dynamic-preview design requires Blaze/Cloud Billing before a deployment can be reviewed.
- The independent Kithe Support Google identity has no Play Console developer account. On
  2026-08-14 the owner enabled two-step verification and Play accepted the prerequisite.
  The owner then confirmed that Kithe has no legally registered entity or D-U-N-S number,
  so an Organization account cannot yet be truthfully verified. The owner initially chose
  to pause Play enrollment and form an independent Hawaii entity, then chose to proceed with
  a personal Play developer account for now and revisit business ownership later. Hawaii's
  current public registry returned no record containing `Kithe`, but the portal warns that
  search results are not definitive name-availability or trademark clearance. The personal
  enrollment wizard accepted `Kithe` as the public developer name. The owner completed and
  linked a personal Google Payments profile, and Play advanced to the public developer
  profile. `support@kitheapp.com` is the approved public developer email. Play requires a
  public website at this step. The owner approved the root-site deployment and DNS change on
  2026-08-14. The dependency-free site in `website/` is now deployed through the Cloudflare
  Pages project `kithe` and live with SSL at `https://kitheapp.com`; the root proxied CNAME
  targets `kithe-370.pages.dev`. The existing email-routing MX, SPF, and DKIM records remain
  present. The site is intentionally separate from the Firebase entry host and the unapproved
  draft policy pages in `public/`, and `join.kitheapp.com` remains unconfigured. The owner
  completed the registration payment on 2026-08-14, creating the personal Play developer
  account **Kithe** (account ID `8042348230312832614`). The Play dashboard currently blocks
  app creation until the owner completes official-document identity verification, verifies
  access to a real Android device through the Play Console mobile app, and verifies the
  contact phone number after Google approves the identity documents. Those owner-controlled
  verification steps remain open. Any future organization conversion requires its own
  verified process.
- While Google identity verification is pending, the local Play listing and Data safety
  package was prepared in `play-listing-data-safety-package.md`,
  `play-listing-draft.md`, and `play-data-safety-draft.md`. It is not submitted. The audit
  found two pre-upload release decisions: remove the deferred audio recorder/microphone
  boundary from Pilot 1 and remove Firebase Analytics automatic collection. The owner then
  directed local work to continue. Both corrections are implemented locally: Firebase
  Analytics and five legacy events are removed, audio authoring/recording is unreachable,
  and the regenerated debug manifest has no Advertising ID, AdServices, microphone, or
  recorder component. A clean 2026-08-14 gate passed 127 unit tests, lint, and debug APK
  assembly. The exact release App Bundle still requires the same audit. Legal approval and
  publication of `/privacy` and `/account-deletion` remain blocking.
- The next local-only listing pass audited the visual identity and found the legacy Android
  launcher still depicts photo, video, and audio inside the old turquoise media pin. A
  Kithe-specific `K`-as-trail icon candidate, a 1,024-by-500 feature graphic, and a six-shot
  real-device capture manifest are prepared under `../play-assets/store-listing/`. The PNGs
  are correctly sized and sRGB; the icon/launcher is not adopted until the owner approves
  the mark. Substantive local privacy and account-deletion drafts are now in `../public/`,
  with unresolved decisions recorded in `legal-drafts/play-policy-approval-matrix.md`.
  Nothing was published, deployed, uploaded, or entered into Play.
- The owner then approved the Kithe mark, feature graphic, and six-shot plan. Android's
  adaptive foreground, round icon, and all density fallbacks now use the approved mark; the
  local website source has matching favicon/touch assets without a deployment. The updated
  APK passed 127 unit tests, lint, and assembly, installed successfully on Samsung
  `R5CY114LNCE`, displayed the masked Kithe icon and label correctly in Android App info,
  and launched `com.kitheapp/.MainActivity` without a fatal log. Existing app data was
  preserved. The live participant surface correctly remained in the List fallback because
  Maps setup is still an unauthorized pre-pilot dependency. No policy, site, backend, or
  Play action occurred.
- A local internal APK built successfully with `com.kitheapp`, `join.kitheapp.com`, the
  verified `/e/` intent filter, and `APP_LINK_CONFIGURED=true`. It is debug/internal-signed;
  its certificate must never be placed in the production `assetlinks.json`. The Play-held
  app-signing SHA-256 remains required.
- The existing Firebase Hosting rewrite and landing function remain the approved design.
  Replacing the dynamic Experience preview with a static landing page would amend the
  accepted R5-P funnel and is not being done implicitly.
- Local R5-P bundle preparation now includes the real organizer QR/share surface and the
  redesigned dynamic web fallback. The organizer detail creates the QR entirely on-device,
  shows the human event code alongside it, and can copy the link, share a branded portrait
  QR image, or save that image through Android's document picker without a storage
  permission. The payload contains only the canonical Experience path, a random 128-bit
  entry session, and the `QR` channel; it contains no user id, location, drop payload, auth
  token, or credential. QR presentation remains behind `APP_LINK_CONFIGURED`, so the normal
  build still shows the honest event-code-only state while `join.kitheapp.com` is
  disconnected.
- The undeployed `experienceEntryPage` now renders branded active, upcoming, ended, and
  not-found states from safe preview metadata, preserves a valid QR entry session through
  the Play Install Referrer handoff, escapes host-authored text, uses no client analytics or
  external assets, and denies camera, geolocation, and microphone access at the page header.
  Local verification passed Functions lint/build/contract checks and the Android gate with
  **130 tests, zero failures**, lint, and debug APK assembly. The QR test performs an actual
  encode/decode round trip. This is source and local test evidence only: no DNS, Hosting,
  Functions, Firebase billing, Play listing, production data, or live-site change occurred.
- The next predeployment audit found that `firebase.json` still targeted `public/`, which
  contains nine local policy drafts, including pages visibly marked unapproved. That was an
  accidental-publication risk, not an accepted R5-P requirement. Firebase Hosting now
  targets a dedicated minimal `hosting/` directory containing only a noindex root and 404;
  the dynamic `/e/**` rewrite remains unchanged. The Hosting ignore rule no longer excludes
  the future `.well-known/assetlinks.json`, and the exact path is configured for JSON plus a
  short rollout cache lifetime. Static security headers deny active content, framing,
  referrers, camera, geolocation, and microphone access.
- The local preflight refuses malformed, multiple, wrong-package, unexpected-file, or
  policy-containing Hosting bundles and has a production-required mode. The ordered
  external decisions are recorded in `r5-p-external-approval-list.md`. At this point no
  billing, Firebase, Hosting, DNS, policy publication, Play release, or production-data
  mutation had occurred.
- On 2026-08-25 Google showed the personal Play developer identity and contact phone as
  verified. The owner explicitly accepted the Developer Program Policies, Play App Signing
  terms, and US export declaration. The `Kithe` / `com.kitheapp` Play record was created as
  a free app with automatic protection active (Play app ID `4974868835867500240`). Play App
  Signing is active. An independent signing-page check found Play's new quantum-ready model
  uses three production certificates: the prior classical key for Android 16 and earlier
  plus current classical and post-quantum keys for Android 17+. All three public SHA-256
  fingerprints are now present in the local, undeployed
  `hosting/.well-known/assetlinks.json`; no debug or upload certificate was used. A1 is
  therefore complete. No bundle was uploaded and no release, Hosting, DNS, Firebase,
  billing, or production-data action occurred.
- On 2026-08-25 UTC the owner explicitly authorized A2. A dedicated **Kithe Production
  Billing** account was activated and linked only to `kithe-production`, moving that project
  from Spark to Blaze. Robert Peralta is the billing owner and alert responder. A
  project-scoped **$25 USD monthly budget** emails billing administrators at 50%, 90%, 100%,
  and 150%; these notifications are not a hard cap. The reviewed Functions region remains
  `us-central1`. A2 is complete. No Functions, Hosting, DNS, Play release, or production-data
  deployment was authorized or performed.
- The subsequent A3 audit confirmed the live target still has zero Functions, uninitialized
  Hosting, an empty `nam5` Firestore database, the original deny-all rules, and no indexes.
  App Check lists Kithe Android / `com.kitheapp` as registered with Play Integrity. The audit
  also found that the root rules/index manifest is broader than R5-P and that Secret Manager
  is not initialized. A dedicated fail-closed A3 config now limits deployment to seven entry,
  join, continuity, unlock, and merge functions; an A3-specific client ruleset; one discovery
  index; and the already validated Hosting bundle. Its emulator and local release checks pass.
  `deployment/r5-p/A3-APPROVAL.md` records the exact proposal and rollback. No secret, Function,
  rule, index, fixture, Hosting release, DNS, Remote Config, or Play mutation occurred.
- On 2026-08-25 UTC the owner explicitly approved that exact A3 package. Secret Manager was
  initialized without exposing the secret value; the dedicated fail-closed rules and one
  discovery index were deployed; and exactly seven reviewed Functions became active in
  `us-central1`. The safe, text-only `R5PTEST2` fixture was created as one guarded six-document
  commit and passed authenticated verification without logging its owner or test point.
  `https://kithe-production.web.app` passed live root, 404, security-header, direct Digital
  Asset Links, and redacted entry-page checks. Missing-App-Check requests to the participant
  callables returned HTTP 401. `deployment/r5-p/A3-EVIDENCE.md` records the result. A3 stopped
  at the A4 gate: no custom domain, Cloudflare DNS, Remote Config, legal-policy Function, Play
  release, QR distribution, or legacy GeoDrop project action occurred.
- The subsequent A4 read-only audit confirmed Firebase Hosting still lists only the two
  generated default domains. Independent public DNS checks found no CNAME, A, or AAAA answer
  for `join.kitheapp.com` and no ACME TXT answer, while the Cloudflare nameservers and DNSSEC
  remain present for `kitheapp.com`. Because Firebase reveals its exact validation/routing
  record set only after association creation, `deployment/r5-p/A4-APPROVAL.md` splits A4 into
  A4a (create the pending Firebase association and stop) and A4b (separately approve/apply the
  exact returned DNS records). No A4 mutation occurred.
- On 2026-08-25 UTC the owner separately approved A4a and A4b. Firebase added the
  direct-serving `join.kitheapp.com` association with no redirect, then exactly one Cloudflare
  record was added: CNAME `join.kitheapp.com` to `kithe-production.web.app`, DNS only, TTL
  Auto. Cloudflare and Google resolvers returned the exact target, Firebase reported
  **Connected**, certificate-validated HTTPS passed, and the root, 404, security headers,
  safe `R5PTEST2` page, and one-statement/three-fingerprint Digital Asset Links contract all
  passed. No root, email, DNSSEC, redirect, Worker, SSL, Play, Remote Config, legal-policy,
  QR-distribution, or legacy-project setting was changed. A4 is complete; A5 remains gated.
- The subsequent A5 preparation audit found no uploadable release candidate. Play reports
  0/3 internal-test tasks, 0/11 app-setup tasks, 0 opted-in testers, and a mandatory
  12-tester/14-day closed test before production access. The owner explicitly approved A5a,
  which is now complete locally: the project targets API 36, all 130 release-variant tests
  pass, release lint and bundle generation pass, and the unsigned diagnostic artifact/privacy
  audit is recorded in `deployment/r5-p/A5A-EVIDENCE.md`. Policy URLs still return 404,
  Remote Config is uninitialized, and Maps, upload-key, screenshots, and reviewer-fixture
  gates remain open. `deployment/r5-p/A5-APPROVAL.md` splits the work into A5a through A5e.
  A5b through A5e and every Play field edit remain unauthorized.

### Independent Firebase identity and Android package prepared 2026-08-13

The owner created **Kithe Production** (`kithe-production`) on Firebase's Spark plan and
registered **Kithe Android** as `com.kitheapp`. The debug SHA-1/SHA-256 fingerprints are
registered, Android now builds and installs as `com.kitheapp`, and the active Firebase config
targets Kithe. Anonymous Auth is enabled and an empty deny-all Standard Firestore database is
created in `nam5`. The physical-device `DEMO2026` route passed through preview and policy to
the participant surface.

The physical debug build's App Check token is allowlisted without being written to the
repository. The owner explicitly accepted the Google APIs and Play Integrity API terms on
2026-08-13, and Kithe Android is registered with Play Integrity using a one-hour token
lifetime. Firestore and Authentication enforcement remain off. Crashlytics detects the Kithe
Android app and is waiting for a first crash; Messaging is available, but no test campaign was
sent and Analytics remains disabled. Local verification passes Functions lint/build,
redesign and fail-closed Remote Config contracts, local operations readiness, all 18
Firestore/Storage emulator suites, and the collection-group index check. Five missing
`schemaVersion` index declarations found by that check were added to the local manifest only.

This does not close R5-P. Cloudflare Email Routing is enabled for `kitheapp.com`, and the
active `support@kitheapp.com` rule forwards to the verified owner inbox with its MX, SPF, and
DKIM records publicly resolvable. The alias is now the Google identity **Kithe Support**, is
an Editor on Kithe Production, and is the Firebase OAuth support email. Google Authentication
is enabled with public-facing name **Kithe**; the refreshed Android configuration passed a
physical-device Explorer sign-in and preserved the local `DEMO2026` Collection state.
Maps SDK enablement requires explicit Maps terms and billing approval, while Firebase Storage
and Cloud Functions require Blaze. An initial device run confirmed the Maps authorization
failure; the installed build now defaults to List, disables Map, and shows a pending-setup
message without making an unauthorized Maps request. No billing, production data, Firestore
rules/index deploy, Functions, Storage, Hosting, Remote Config, App Check enforcement,
Analytics, Play release, or App Link association was enabled.

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
