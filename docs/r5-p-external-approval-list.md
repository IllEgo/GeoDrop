# Kithe R5-P external approval list

Status: none of the external actions below are implied by local bundle preparation.

R5-P remains open until every group is completed and evidenced. Approvals should be given in
order so an earlier dependency cannot be mistaken for permission to perform a later public
action.

## A1 — Play identity and signing input

- [x] Google approved the personal developer identity documents.
- [x] The Play Console Android-device and contact-phone checks passed.
- [x] The `com.kitheapp` app record was created and Play App Signing was enabled on
  2026-08-25 (Play app ID `4974868835867500240`).
- [x] Play Console's signing page independently confirmed all three **App signing key
  certificate SHA-256** values required by its new quantum-ready signing model: the prior
  classical certificate for Android 16 and earlier plus the current classical and
  post-quantum certificates for Android 17+. The exact values are recorded only in the
  local production `hosting/.well-known/assetlinks.json`; no upload/debug certificate was
  accepted.

Approval effect: permits preparing the final `assetlinks.json`; it does not permit Hosting,
DNS, Firebase, or Play publication.

## A2 — Firebase billing and operating limits

- Approve attaching a named Google Cloud billing account and upgrading `kithe-production`
  from Spark to Blaze.
- Name the billing owner and the person who responds to alerts.
- Approve budget-alert thresholds before activation. Budget alerts notify; they are not a
  hard spending cap.
- Confirm the Cloud Functions region remains `us-central1` for this release.

Approval effect: permits the billed project change only. Deployment still requires A3.

Completion recorded 2026-08-25 UTC:

- The owner explicitly authorized billing activation. The dedicated **Kithe Production
  Billing** account is active and linked to `kithe-production`; the project is now on Blaze.
- Robert Peralta is the billing owner and alert responder. The project-scoped monthly budget
  is **$25 USD**, with administrator email alerts at **50%, 90%, 100%, and 150%**. These
  alerts are not a hard spending cap.
- The reviewed Cloud Functions region remains `us-central1`.
- A2 is complete. No Functions, Hosting, DNS, Play release, or production-data deployment
  was authorized or performed.

## A3 — Backend/Hosting deployment window

- [x] Review and approve the exact R5/R6 Functions allowlist; never deploy every exported
  function implicitly.
- [x] Review the Firestore rules and indexes required for preview, membership, safe discovery,
  collection reads, and the exact resumed unlock.
- [x] Approve one safe schema-v2 test Experience and the minimum test accounts.
- [x] Name the deployer, monitor, deployment window, abort conditions, and rollback owner.
- [x] Capture predeployment state and approve the rollback plan in `deployment/r5-p/README.md`.

Approval effect: permits the reviewed Firebase mutation on its generated Firebase host. It
does not permit custom-domain DNS or QR distribution.

Completion recorded 2026-08-25 UTC: the owner explicitly approved the dedicated fail-closed
package in `../deployment/r5-p/A3-APPROVAL.md`. Exactly seven Functions are active in
`us-central1`; the A3 rules and one index are live; the safe six-document `R5PTEST2` fixture
passed authenticated verification; and the generated Firebase host passed entry, header,
404, and Digital Asset Links checks. `../deployment/r5-p/A3-EVIDENCE.md` records the live
evidence. No custom domain, DNS, Remote Config, legal-policy Function, Play release, or legacy
GeoDrop project action occurred.

## A4 — `join.kitheapp.com` custom domain and DNS

- Verify the generated Firebase host first.
- Approve adding `join.kitheapp.com` to Firebase Hosting.
- Approve the exact Cloudflare DNS record Firebase requests.
- Verify HTTPS, no redirect at `/.well-known/assetlinks.json`, JSON content type, the single
  `com.kitheapp` statement, and the Play signing fingerprint.

Approval effect: permits the custom-domain and DNS changes. It does not permit a Play release
or real-event QR distribution.

A4 completion recorded 2026-08-25 UTC: after separate explicit A4a and A4b approvals,
Firebase's direct-serving association and exactly one Cloudflare record are active: CNAME
`join.kitheapp.com` to `kithe-production.web.app`, DNS only, TTL Auto. Cloudflare and Google
resolvers return the exact record, Firebase reports **Connected**, and the custom host passes
certificate-validated HTTPS, root/404/security-header, safe entry-page, and exact Digital
Asset Links parity checks. No root, email, DNSSEC, redirect, Worker, SSL, Play, Remote Config,
or QR-distribution setting was changed. Full evidence and rollback are in
`../deployment/r5-p/A4-APPROVAL.md`. A5a is complete locally; A5b through A5e remain
unapproved.

## A5 — Play route and external matrix

Preparation audit recorded 2026-08-25 UTC: Play shows 0/3 internal-test tasks, 0/11 setup
tasks, 0 opted-in testers, and disabled production access. This account must publish a closed
test with at least 12 opted-in testers for at least 14 days before applying for production.
A5a is now complete locally: the repo targets API 36, all 130 release-variant tests pass,
release lint passes, and the configured diagnostic AAB is unsigned with its privacy audit
recorded in `../deployment/r5-p/A5A-EVIDENCE.md`. The required policy URLs still return 404,
Remote Config is uninitialized, and no final screenshots, Maps key, upload key, or reviewer
fixture exist. A5 remains split into A5a local candidate remediation, A5b signing/internal
test, A5c policy/setup, A5d closed test, and A5e production/external funnel. **Only A5a is
approved and complete; A5b through A5e are not authorized.** Exact scope, abort criteria,
and rollback are in `../deployment/r5-p/A5-APPROVAL.md`.

- Approve the Play track: the signed direction prefers a fail-closed production listing;
  open testing is the accepted alternative with its extra opt-in measured.
- Approve the release App Bundle and listing submission separately.
- Approve a never-installed test device/account path and a returning-account fixture.
- Run installed QR, never-installed QR through Play, stripped referrer, manual code, LINK,
  MERGE, permission denial/recovery, coarse-only, precise denial/grant, and exact resumed
  unlock checks.
- Confirm analytics continuity by entry session without raw location, user identity in the
  URL, payload, token, or credential leakage.

Approval effect: permits only the reviewed release/test operation. Pilot or public launch
still requires the remaining pre-pilot gates and R10-P authorization.

## Current state

- Local Hosting bundle structure: **pass**.
- Unapproved policy drafts excluded from Firebase Hosting: **pass**.
- Dynamic entry renderer and Android QR generation: **pass locally**.
- A1 Play identity, app record, signing, and local release `assetlinks.json`: **complete**.
- Release upload-key certificate: unavailable until the first app bundle is uploaded; it
  is not the Digital Asset Links certificate and does not block A1.
- A2 Blaze billing and operating limits: **complete**.
- A3 generated-host Firebase deployment: **approved, deployed, and verified**.
- Generated Firebase entry host: **pass** at `https://kithe-production.web.app`.
- `join.kitheapp.com`: **connected and verified through A4**.
- A5a API-36 unsigned diagnostic release gate: **complete**.
- Play release and R10-P: **not authorized**.

References:

- [Android Digital Asset Links configuration](https://developer.android.com/training/app-links/configure-assetlinks)
- [Firebase Hosting configuration](https://firebase.google.com/docs/hosting/full-config)
