# Kithe R5-P external approval list

Status: none of the external actions below are implied by local bundle preparation.

R5-P remains open until every group is completed and evidenced. Approvals should be given in
order so an earlier dependency cannot be mistaken for permission to perform a later public
action.

## A1 — Play identity and signing input

- Google approves the personal developer identity documents.
- The Play Console Android-device and contact-phone checks pass.
- The `com.kitheapp` app record is created and Play App Signing is enabled.
- The Play-held **App signing key certificate SHA-256** is copied from Play Console and
  independently checked. A local upload/debug certificate is not accepted.

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

## A3 — Backend/Hosting deployment window

- Review and approve the exact R5/R6 Functions allowlist; never deploy every exported
  function implicitly.
- Review the Firestore rules and indexes required for preview, membership, safe discovery,
  collection reads, and the exact resumed unlock.
- Approve one safe schema-v2 test Experience and the minimum test accounts.
- Name the deployer, monitor, deployment window, abort conditions, and rollback owner.
- Capture predeployment state and approve the rollback plan in `deployment/r5-p/README.md`.

Approval effect: permits the reviewed Firebase mutation on its generated Firebase host. It
does not permit custom-domain DNS or QR distribution.

## A4 — `join.kitheapp.com` custom domain and DNS

- Verify the generated Firebase host first.
- Approve adding `join.kitheapp.com` to Firebase Hosting.
- Approve the exact Cloudflare DNS record Firebase requests.
- Verify HTTPS, no redirect at `/.well-known/assetlinks.json`, JSON content type, the single
  `com.kitheapp` statement, and the Play signing fingerprint.

Approval effect: permits the custom-domain and DNS changes. It does not permit a Play release
or real-event QR distribution.

## A5 — Play route and external matrix

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
- Play App Signing SHA-256: **blocked on A1**.
- Blaze billing: **not approved**.
- Firebase deployment: **not approved**.
- `join.kitheapp.com` DNS/HTTPS: **not configured**.
- Play release and R10-P: **not authorized**.

References:

- [Android Digital Asset Links configuration](https://developer.android.com/training/app-links/configure-assetlinks)
- [Firebase Hosting configuration](https://firebase.google.com/docs/hosting/full-config)
