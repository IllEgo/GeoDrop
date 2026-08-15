# R5-P entry-host deployment bundle

Status: local preparation only; production actions are not authorized.

## Bundle boundary

- `hosting/` is the only Firebase Hosting static directory.
- `public/` contains unapproved legal drafts and is deliberately excluded.
- `/e/**` rewrites to the dynamic `experienceEntryPage` function.
- `/.well-known/assetlinks.json` must not exist in `hosting/` until the Play App Signing
  SHA-256 fingerprint is available and approved.
- The production association contains only `com.kitheapp` and the Play-held app-signing
  fingerprint. Never add a debug or internal certificate to the production host.

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

## Release asset preparation

1. Copy `assetlinks.json.template` to `hosting/.well-known/assetlinks.json` only after the
   Play fingerprint is known.
2. Replace the placeholder with the uppercase, colon-delimited Play App Signing SHA-256.
3. Run:

   `npm.cmd --prefix functions run r5:hosting:check -- --require-release-assets`

4. Review the exact diff and confirm that `hosting/` contains no policy drafts, credentials,
   test certificates, source maps, or production Experience data.

## Authorized deployment sequence (not yet authorized)

1. Capture current Functions, Hosting, rules/index, App Check, billing, and DNS state.
2. Deploy only the reviewed R5/R6 function allowlist needed by the external matrix.
3. Deploy Firebase Hosting to its generated Firebase domain first; do not change DNS yet.
4. Verify the landing states, security headers, `assetlinks.json` content type, lack of
   redirects, and install-referrer URL using a safe fixture.
5. Add and verify `join.kitheapp.com` in Firebase Hosting, then apply the exact approved
   Cloudflare DNS record.
6. Verify HTTPS and Digital Asset Links before distributing any QR.
7. Build the Play release with `KITHE_APP_LINK_HOST=join.kitheapp.com`, upload it through
   the approved Play track, and run the complete never-installed/installed matrix.

## Rollback

- Stop QR distribution and keep the human Experience code path available.
- Restore the prior Firebase Hosting release and the prior approved function revisions.
- Revert or disable the `join.kitheapp.com` DNS record if the host is unsafe or inconsistent.
- Do not delete or reverse schema-v2 data as part of a web rollback.
- Record timestamps, operator, symptoms, commands, and verification evidence in the release
  evidence template.
