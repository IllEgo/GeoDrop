# A5 Play route and external-funnel approval package

Status: **A5a and A5b are complete. The requesting owner's local operator decision and
recommended decisions are recorded for LP-1 through LP-9, conditional LP-10, and LP-11.
LP-12, required role-specific signoffs, exact policy copy, and all A5c external mutations remain
unauthorized. Version `1 (1.0)` is active only on Internal testing for the two owner-approved
testers. A5d and A5e remain unauthorized**.

A5 cannot safely be approved as one action. The current Play account requires setup and a
qualifying closed test before production access, while the local project does not yet produce
an uploadable release candidate. This package splits the remaining work into separately
approved stages and keeps Play, policies, Remote Config, reviewer data, and QR distribution
closed until their own gates pass.

## Initial read-only Play baseline — 2026-08-25 UTC

This baseline was captured before A5b item 3. The later draft-upload evidence below
supersedes its statements that no release existed and no upload certificate was shown.

- Play app: **Kithe**, package `com.kitheapp`, Play app ID `4974868835867500240`.
- Dashboard progress is **0 of 3** internal-testing tasks and **0 of 11** app-setup tasks.
  No release has been created, previewed, confirmed, or rolled out.
- Closed testing is locked until app setup is finished. Production access then requires a
  published closed-testing release, at least **12 opted-in testers**, and at least **14 days**
  of qualifying testing. The dashboard currently reports **0 opted-in testers**.
- Play App Signing is active and quantum-ready. The upload-key certificate is intentionally
  absent because no first App Bundle has been uploaded.
- The eleven open setup tasks are: privacy policy, sign-in/reviewer access, ads, content
  rating, target audience, Data safety, government-app declaration, financial-features
  declaration, health declaration, app category/contact details, and store listing.
- Production access is disabled. A5 preparation did not change any Play field or tester list.

## Current external baseline

- Firebase Hosting reports `join.kitheapp.com` **Connected**, and the A4 HTTPS/App Links
  checks remain complete.
- Firebase Remote Config is uninitialized and shows **Create configuration**. There is no
  live release template to enable; the tracked template remains fail-closed.
- `https://kitheapp.com/privacy` and `https://kitheapp.com/account-deletion` both return
  HTTP 404. Their local files remain visibly marked engineering drafts with unresolved
  operator, version/date, retention, age, and response-time placeholders.
- Production still contains only the seven A3 Functions. `getLegalPolicyManifest` and
  `recordLegalAcceptance` are not in the deployed allowlist, and no legal-policy base URL is
  configured.
- No dedicated Maps key is configured in Gradle or the environment. The release variant
  therefore builds with `MAPS_CONFIGURED=false`.
- The approved Play icon and feature graphic exist. There are **zero final screenshots**;
  the six-shot manifest still requires an exact signed candidate and a reviewed fixture.
- The safe `R5PTEST2` rehearsal has one non-sensitive drop. It is not the proposed Play
  reviewer account/photo fixture and does not authorize new reviewer data.

## Pre-A5a diagnostic release audit — not an upload candidate

A local diagnostic build used `KITHE_APP_LINK_HOST=join.kitheapp.com` and made no external
change. It generated the untracked bundle
`app/build/outputs/bundle/release/app-release.aab` with SHA-256
`17D79AE58BF7C183FBC7A55AE799CEA6A9512F82B6922F121E0AB6ADD761A10B`.

The diagnostic established:

- package `com.kitheapp`, version code `1`, version name `1.0`;
- `APP_LINK_CONFIGURED=true`, host `join.kitheapp.com`, region `us-central1`, Play Integrity
  App Check enabled, every build-time feature upper bound false, and Maps false;
- no Advertising ID, AdServices attribution/ID, microphone permission, or registered audio
  recorder component in the merged release manifest;
- release lint passed with zero errors and 124 warnings; and
- the bundle is **unsigned** because the release build has no upload signing configuration or
  workspace upload key.

The release unit-test gate does not pass: **105 of 130 passed and 25 failed**. All failures
are Compose/ActivityScenario suites that cannot resolve `androidx.activity.ComponentActivity`
under the release test variant. The debug-only test-manifest dependency is not available to
that variant. This is release-test configuration debt, not passing release evidence.

The manually retained `app/release/app-release.apk` is not a fallback: it is a stale legacy
artifact labeled GeoDrop with package `com.e3hi.geodrop` and must never be uploaded for Kithe.

## Target API blocker — closed by A5a

The project compiles and targets API 34. Google Play already requires new mobile apps to
target at least API 35, and on **2026-08-31** new apps and updates must target API 36 unless
Play grants an extension. Because the mandatory 14-day test cannot finish before that date,
the A5 candidate must target API 36 rather than relying on a six-day window or an ungranted
extension.

Official requirement:
<https://support.google.com/googleplay/android-developer/answer/11926878?hl=en>

## A5a completion — 2026-08-25 UTC

The owner explicitly approved A5a. The local remediation now targets API 36, all 130 release
unit tests pass, release lint and bundle generation pass, and the post-build
manifest/dependency/privacy audit is clean. The new unsigned diagnostic AAB has SHA-256
`C3B7EA328607936E609D26C7A3EEE5B46B67AAED1AD300427328E5FB35286963`.

The exact code/configuration changes, test counts, lint result, artifact size/signing state,
release flags, Firebase/App Links identity, and no-Analytics/no-ad-ID/no-microphone/no-recorder
results are recorded in `A5A-EVIDENCE.md`. No external system was changed.

## Staged approvals

### A5a — local release-candidate remediation

**Approved and complete.** A5a authorized local code/configuration work only:

1. Raise compile/target SDK to API 36 and make only the compatibility changes required by
   that migration.
2. Fix the release unit-test runtime so all 130 tests execute against the release variant.
3. Re-run release unit tests, lint, bundle generation, manifest/dependency inspection, and
   the no-Analytics/no-ad-ID/no-microphone/no-recorder audit.
4. Preserve `join.kitheapp.com`, Play Integrity App Check, `kithe-production`, and fail-closed
   feature flags. Do not add a Maps key, upload key, policy URL, test credential, or secret.
5. Produce only an unsigned local diagnostic AAB and its hash. Stop before key generation,
   signing, upload, Play field edits, policy publication, backend/Remote Config changes,
   reviewer data, or tester enrollment.

### A5b — upload-key and internal-test candidate

**Approved and complete.** The owner separately approved item 3 on 2026-08-25. The exact
bundle was uploaded, its Play-reported upload certificate was confirmed, two owner-approved
testers were assigned without recording personal addresses in the repository, and version
`1 (1.0)` was released only to Internal testing on 2026-08-26:

1. Select or create a dedicated private upload key outside the repository and define its
   owner, backup, rotation, and recovery handling without exposing passwords or key material.
2. Build the exact signed candidate and record its AAB hash, upload certificate, package,
   version, target API, Firebase identity, App Link host, feature flags, merged manifest,
   dependency graph, and device smoke evidence.
3. Separately approve adding an internal tester list, uploading that exact AAB, confirming
   Play's reported upload certificate, and rolling out only to Internal testing.
4. Stop before completing the eleven setup forms or creating a closed/production release.

Current item-3 evidence:

- uploaded AAB SHA-256:
  `BF7CF87B17F1FBF2EEE954D375060E0FEFBEA2DC77F6EA34A88F28E09214222D`;
- Play artifact: version `1 (1.0)`, minimum API 26, target SDK 36;
- Play upload-certificate SHA-256:
  `2B:7B:4D:86:B8:C4:70:A2:1D:18:1B:42:8D:98:F6:DB:A8:4D:6F:93:8A:69:0A:8B:42:63:47:30:94:5E:9C:B1`;
- state: Internal testing track **Active**, release **Available to internal testers**, and
  Play review state **Not reviewed** under temporary name `com.kitheapp (unreviewed)`;
- tester list: `Kithe's Internal Testing`, two owner-approved users; and
- opt-in URL: `https://play.google.com/apps/internaltest/4701473779914778401`.

### A5c — policies, reviewer fixture, Maps, listing, and Play setup

**Policy-decision review and requesting-owner option-1 approval are complete.** The evidence,
recommended direction, recorded local decisions, unresolved role signoffs, and remaining
approval template are in `A5C-POLICY-DECISION-REVIEW.md`. The requesting owner approved
LP-2 through LP-9, conditional LP-10, and LP-11 locally. The instructions made no external
change and do not authorize any item below. At that stage LP-1 remained deferred; LP-10 still needs
legal/exact-release closure; and LP-12 remains blocked until an approved publication bundle
exists.

The requesting owner subsequently identified the current legal operator as **Robert Micah
Lee Peralta**, operating Kithe as an **individual / sole proprietor**, with no separate LLC,
corporation, or partnership. This supersedes the earlier two-owner legal-formation assumption;
Kerise remains the LP-9 operational backup, not a legal/equity owner. The decision is recorded
locally only. Exact policy-copy approval, authorized legal/privacy review, publication, Play
answers, account conversion, and every other external mutation remain unauthorized.

A subsequent read-only Play **About you** check confirmed the Personal account and Kithe
developer name but found that the linked Payments profile currently shows the shorter legal
name **Robert Peralta**. No private address is recorded here. Resolve this exact-name mismatch
before LP-1 identity verification, exact-copy approval, or publication. No Play change ran.

The requesting owner then confirmed **Robert Micah Lee Peralta** as the authoritative
government-identity name. No identity-document detail is stored in the repository. The
linked personal Payments profile must be updated and reverified before identity parity can
pass; that sensitive external action is not authorized by this local decision record.

The requesting owner later provided action-time authorization for the name update and
completed private passkey verification. Google Payments then required documentary evidence
of a legally documented name change rather than offering a simple legal-name correction.
The workflow stopped before **Start name change** and uploaded or submitted nothing. LP-1
identity parity therefore remains open pending Google Payments/Play support or an
owner-controlled accepted-document process.

LP-9 coverage was subsequently supplied: Micah, the individual operator, is primary and
Kerise is the operational backup for monitoring `support@kitheapp.com`. The private forwarding address is intentionally
not stored here. The five-business-day acknowledgment and 30-calendar-day verified-deletion
promise was explicitly approved on 2026-08-26 unless a disclosed legal exception applies.

The subsequent `continue` instruction authorized non-public policy draft preparation only.
The local privacy, account-deletion, and data-retention drafts now incorporate the recorded
owner directions while preserving visible draft/noindex markers and LP-12 placeholders,
LP-10 provider/export verification, and the publication stop. They remain outside the
dedicated live entry-host bundle; no external system was changed.

A later `continue` instruction authorized LP-10 read-only production verification only.
Firebase Analytics, BigQuery linking, optional Firebase Cloud Logging linking, custom external
log sinks, and all Maps APIs were confirmed disabled. Only internal 30-day `_Default` and
400-day `_Required` Cloud Logging buckets exist. Firebase's optional non-Firebase improvement
setting for Firebase Service Data is enabled, but that provider-defined category expressly
excludes Customer Data while still including some personal operational data such as IP
addresses. Do not finalize Play No sharing until a later approval disables this optional use
or an authorized legal/privacy reviewer determines its Play effect. Contract/account approval
and a post-Maps exact-release recheck also remain required. No setting or credential was
changed, enabled, linked, or revealed.

The owner then explicitly approved disabling only **Let Google use your Firebase Service
Data** for non-Firebase analysis, recommendations, and improvement in `kithe-production`.
Firebase reported **Service data sharing disabled**, and a reload plus direct DOM inspection
confirmed `checked: false`. No other Firebase, Google Cloud, Maps, credential, Play, or policy
setting changed. The optional-use blocker is closed; LP-10 still needs authorized
legal/privacy and contract/account confirmation and a post-Maps exact-release recheck.

Requires resolved owner/legal decisions and exact action-time approvals:

1. Close LP-1 through LP-12, record the authorized legal/privacy approver, and approve the
   exact privacy/account-deletion copy and retention promises.
2. Publish the approved policy routes and verify public HTTPS 2xx access without app install
   or sign-in. Deploy only an explicitly reviewed legal-function allowlist and policy base
   URL; keep client/backend acceptance versions identical.
3. Approve Maps terms/billing use, create a package-and-signing-certificate-restricted Maps
   key, and verify the exact candidate without exposing the key.
4. Approve and create a non-privileged reviewer account plus stable safe text/photo fixture;
   keep its password and coordinate only in Play's reviewer-access field.
5. Capture and approve six sanitized screenshots from the exact candidate.
6. Review and save the eleven Play setup tasks using the final artifact and public policies.
   The working recommendations remain: Events, free, no ads, U.S.-only, adults 18+, not a
   government/financial/health app, and provisional no data sharing only if LP-10 closes.
7. Initialize Remote Config with the reviewed fail-closed template. Enabling any participant
   flag remains a separate supervised mutation with an exact revert.

### A5d — mandatory closed test

Requires a later explicit release approval after A5b and A5c pass:

1. Create only the approved closed-testing release from the exact signed/hash-matched AAB.
2. Enroll at least 12 owner-approved testers and confirm opt-in; never publish credentials or
   personal tester data in the repository.
3. Run the test for at least 14 qualifying days, monitoring crashes, App Check, support,
   privacy/deletion, install/referrer continuity, and the participant loop.
4. A closed-test enrollment or sideload is not the required cold Play-install funnel evidence.
5. Stop after recording Play's qualification state and test evidence. Do not apply for
   production access or distribute a real-event QR.

### A5e — production access and external funnel

Requires separate approval after Play recognizes the qualifying closed test:

1. Review and submit the production-access application.
2. If access is granted, separately approve the fail-closed U.S. production listing and its
   exact release/rollout settings.
3. Only after the production listing is installable, run the never-installed normal Play
   path plus installed QR, stripped referrer, manual code, LINK, MERGE, permission-denial,
   coarse/precise, and resumed-unlock matrix.
4. Keep all real-event QR distribution and pilot/public launch closed until the full matrix,
   operations, and rollback evidence pass.

## Abort and rollback

Abort on a wrong package/project/track, API below 36, signing mismatch, key exposure,
unexpected SDK/permission/component, Analytics/ad-ID/microphone drift, release-test or lint
failure, App Link/Firebase identity mismatch, policy/Data safety disagreement, unpublished
or placeholder policy, unsafe reviewer data, less than Play's required tester/time state,
certificate or HTTPS failure, or any request to broaden A5 beyond its approved stage.

For an internal or closed-test failure, halt the rollout and replace it only with a separately
approved corrected version; do not delete the Play app or signing keys. Keep Remote Config
fail-closed, retire only the dedicated reviewer fixture/account if needed, preserve the A4
generated/custom hosts, and distribute no real-event QR.

## Gate

A5a and A5b are complete. The A5c review, requesting-owner option-1 local decisions, and
LP-1 individual-operator decision are recorded, but required role signoffs, authorized
legal/privacy review, exact policy-copy approval, and LP-12 remain open. The exact
signed candidate, physical API 36 smoke, encrypted
removable key/password archives, restored private-key signing test, sealed offline recovery
code, second-custodian designation, exact Play upload, upload-certificate confirmation,
approved tester assignment, and Internal-only rollout all pass. Stop here: A5c through A5e
external actions remain closed, as do A5d and A5e.
