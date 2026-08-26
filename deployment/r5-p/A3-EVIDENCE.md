# A3 Firebase deployment completion evidence

Status: **complete on the generated Firebase host; A4 and later gates remain closed**.

The owner explicitly approved the exact A3 package in `A3-APPROVAL.md`. The attended
production operation ran against `kithe-production` on 2026-08-25 UTC and closed at
2026-08-25T14:31:54Z.

## Deployed and verified

- Secret Manager API is enabled. `ANALYTICS_HMAC_SECRET` version 1 is enabled, and the
  random 32-byte value was never displayed, logged, or committed. The Functions runtime
  service account has secret-accessor permission only for this secret.
- `deployment/r5-p/firestore.rules` is the live ruleset. Console verification confirmed the
  intended `experienceDrops` rule and final fail-closed catch-all.
- Firestore has exactly one composite index: `experienceDrops` by `experienceCode ASC`,
  `state ASC`, `moderationState ASC`, and `publishedAt DESC`. There are zero field overrides.
- Exactly these seven Node.js 22 first-generation Functions are `ACTIVE` in `us-central1`:
  `experienceEntryPage`, `resolveExperience`, `joinExperience`, `recordClientEvent`,
  `recordAuthCompletion`, `unlockDrop`, and `mergeGuestAccount`. No other Function is
  deployed. A final inventory also confirmed that the unapproved legacy
  `GEODROP_POLICY_BASE_URL` variable is absent from every deployed A3 Function. The generated
  Artifact Registry has a one-day cleanup policy for old images.
- The first Functions pass enabled the required first-use APIs but did not leave a Function
  behind. The attended retry completed with seven deployed, zero errored, and zero aborted.
- Requests to `resolveExperience` and `joinExperience` without App Check were rejected with
  HTTP 401. A successful device-attested participant call remains part of the later A5
  Play/device matrix; A3 did not enable the release-client backend flag.
- `R5PTEST2` was created with one atomic six-document commit using `exists: false`
  preconditions. The utility selected the sole enabled non-anonymous Kithe test account,
  reused the established repository test point, and emitted neither identifier nor
  coordinates. A separate authenticated read verified all six documents and the safe,
  published text-only shape.
- Firebase Hosting released successfully at `https://kithe-production.web.app`. Live checks
  passed for the noindex root, intentional 404, CSP/Permissions/Referrer/nosniff/frame
  headers, direct JSON `assetlinks.json`, exact parity with all three approved Play signing
  fingerprints, and no redirect.
- `https://kithe-production.web.app/e/R5PTEST2` returns HTTP 200, renders the safe fixture and
  Google Play link, and exposes neither the test point nor internal owner/payload fields.

## Explicitly unchanged

- No custom domain or Cloudflare DNS record was added.
- Remote Config and the participant backend release flag were not changed.
- Legal-policy Functions were not deployed and policy drafts were not hosted.
- No Play App Bundle, listing, track, QR distribution, pilot, or public release action ran.
- Legacy GeoDrop Firebase/Cloud projects were not changed or removed.

The next production gate is **A4**, which requires a new explicit approval for
`join.kitheapp.com` and the exact Firebase-requested Cloudflare DNS record.
