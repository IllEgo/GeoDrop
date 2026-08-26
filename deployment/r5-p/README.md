# R5-P entry-host deployment bundle

Status: A1 through A5a are complete. Live A3 evidence is in `A3-EVIDENCE.md`; the connected
custom domain and A4 verification are in `A4-APPROVAL.md`; and the local API-36 release gate
is in `A5A-EVIDENCE.md`. A5b through A5e remain staged and unapproved in `A5-APPROVAL.md`.

## Bundle boundary

- `hosting/` is the only Firebase Hosting static directory.
- `public/` contains unapproved legal drafts and is deliberately excluded.
- `/e/**` rewrites to the dynamic `experienceEntryPage` function.
- `/.well-known/assetlinks.json` contains all three Play-provided production App Signing
  SHA-256 fingerprints for `com.kitheapp` and is verified on the generated Firebase host.
- The production association contains only `com.kitheapp` and its legacy-classical,
  quantum-classical, and post-quantum Play-held signing fingerprints. Never add a debug,
  internal, or upload certificate to the production host.

## External approvals required before any mutation

1. Google completes the Play developer identity/device/contact checks, the Kithe app record
   exists, Play App Signing is enabled, and its SHA-256 fingerprint is copied from Play.
2. The owner approves attaching Cloud Billing and upgrading `kithe-production` to Blaze,
   including a budget/alert threshold and named billing owner.
3. Engineering reviews the exact Functions allowlist. Do not use a blanket
   `firebase deploy --only functions` command: this repository exports functions outside
   the R5 entry path.
4. The owner approves a safe schema-v2 test Experience and any required test accounts.
5. The owner approves the deployment window, monitoring owner, abort criteria, and rollback
   window.
6. The owner separately approves the Firebase Hosting/custom-domain operation and the
   Cloudflare DNS record for `join.kitheapp.com`.

Approval progress as of 2026-08-25 UTC: items 1 through 6 are complete through A4. The
single approved Cloudflare CNAME is live, Firebase reports the custom domain connected, and
the HTTPS and Digital Asset Links checks pass. A5a is complete locally; A5b through A5e
remain separately gated.

The A3 package uses `firebase.r5-p.json`, `deployment/r5-p/firestore.rules`, the single-index
`deployment/r5-p/firestore.indexes.json`, and `functions.allowlist.txt`. The root rules/index
configuration is intentionally broader and must not be substituted.

## Release asset preparation

1. Confirm `hosting/.well-known/assetlinks.json` contains the three exact uppercase,
   colon-delimited Play App Signing SHA-256 values shown in Play Console: the prior
   classical certificate used on Android 16 and earlier plus the current classical and
   post-quantum certificates used by Android 17's hybrid signing. Never substitute the
   upload, debug, or internal certificate.
2. Run:

   `npm.cmd --prefix functions run r5:hosting:check -- --require-release-assets`

3. Review the exact diff and confirm that `hosting/` contains no policy drafts, credentials,
   test certificates, source maps, or production Experience data.

## Deployment sequence and remaining gate

1. [x] Capture current Functions, Hosting, rules/index, App Check, billing, and DNS state.
2. [x] Deploy only the reviewed seven-Function A3 allowlist.
3. [x] Deploy Firebase Hosting to its generated Firebase domain without changing DNS.
4. [x] Verify landing states, security headers, `assetlinks.json`, redirects, and the safe
   `R5PTEST2` entry fixture.
5. [x] After separate A4 approval, add and verify `join.kitheapp.com`, then apply only the
   exact Firebase-requested Cloudflare DNS record.
6. [x] Verify custom-domain HTTPS and Digital Asset Links before distributing any QR.
7. [ ] After the staged A5 approvals, remediate and sign the exact release candidate, finish
   Play setup, complete the mandatory closed test, obtain production access, and run the full
   never-installed/installed matrix.

## Rollback

- Stop QR distribution and keep the human Experience code path available.
- Restore the prior Firebase Hosting release and the prior approved function revisions.
- Revert or disable the `join.kitheapp.com` DNS record if the host is unsafe or inconsistent.
- Do not delete or reverse schema-v2 data as part of a web rollback.
- Record timestamps, operator, symptoms, commands, and verification evidence in the release
  evidence template.
