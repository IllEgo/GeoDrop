# A3 Firebase deployment approval package

Status: **approved and completed on 2026-08-25 UTC**. See `A3-EVIDENCE.md` for the live
deployment and verification record. This approval does not extend to A4 or any later gate.

This package is deliberately narrower than the repository-wide Firebase configuration. It
uses `firebase.r5-p.json`, the dedicated A3 ruleset, one required composite index, and the
seven-name Functions allowlist. Do not substitute `firebase.json` or deploy all Functions.

## Read-only production baseline — 2026-08-25 UTC

- Project: `kithe-production`, Blaze, Firestore `(default)` in `nam5`.
- Functions: none deployed; the console still shows first-use onboarding.
- Hosting: uninitialized; the console still shows first-use onboarding.
- Firestore data: empty; the console shows **Your database is ready to go. Just add data.**
- Firestore rules: the original deny-all ruleset published 2026-08-13.
- Firestore indexes: zero composites and zero field overrides.
- App Check: Kithe Android / `com.kitheapp` is registered with Play Integrity. Firestore and
  Authentication console enforcement remain off. Each selected callable still enforces App
  Check in source.
- Secret Manager API: not enabled. `ANALYTICS_HMAC_SECRET` therefore does not exist yet.
- Blaze budget: $25 USD monthly alerts at 50%, 90%, 100%, and 150%; alerts are not a cap.

Because this is the first deployment, there is no prior Functions revision or Hosting release
to restore. Rollback means deleting/disabling the new resources and restoring deny-all rules.

## Exact authorized target — deployed

### Functions

Deploy only the names in `functions.allowlist.txt`, all in `us-central1`:

1. `experienceEntryPage`
2. `resolveExperience`
3. `joinExperience`
4. `recordClientEvent`
5. `recordAuthCompletion`
6. `unlockDrop`
7. `mergeGuestAccount`

This covers web preview, guest preview/join, funnel continuity, exact resumed unlock, and the
returning-account MERGE path. Organizer authoring, reports, moderation, rewards, media, account
export/deletion, schedulers, and every other exported function stay undeployed.

`getLegalPolicyManifest` and `recordLegalAcceptance` are intentionally excluded. Their policy
version is still marked draft and `GEODROP_POLICY_BASE_URL` is not approved/configured. The Play
cold-install matrix cannot proceed until the policy package is approved and those two functions
receive a separate reviewed deployment authorization.

### Firestore

Deploy `deployment/r5-p/firestore.rules`, not the broader root ruleset. It permits only:

- a principal to read and safely initialize/update its own Explorer profile;
- a member or owner to get its own Experience document, without collection enumeration;
- a member to read active, published, moderation-safe discovery metadata;
- a principal to read its own membership, unlock, reward-receipt, Trail-progress, blocked-host,
  and legal-acceptance records; and
- no client writes to server-authoritative R2 records.

The catch-all denies every legacy drop, payload, reward, analytics, organizer, moderation, and
account-lifecycle path. Deploy exactly one composite index: `experienceDrops` by
`experienceCode ASC`, `state ASC`, `moderationState ASC`, and `publishedAt DESC`. No field
override is part of A3.

### Hosting

Deploy the already validated `hosting/` bundle only to the generated Firebase domains. It
contains the noindex root/404, the dynamic `/e/**` rewrite, security headers, and the three-key
Play `assetlinks.json`. A3 does not add `join.kitheapp.com`, change Cloudflare DNS, distribute a
QR, or publish a Play release.

### Safe production fixture

Create exactly one 72-hour schema-v2 Experience with code `R5PTEST2`, one standard text drop,
and no photo, reward, Trail, promotion, personal data, or real event content. Its 25 m test
location reuses the established repository test point and the fixture utility never prints
the coordinates. The owner is an existing enabled non-anonymous Kithe test account.

`functions/scripts/manage-r5-p-fixture.js` is dry-run by default, pins and confirms
`kithe-production`, refuses overwrites, verifies the owner in Firebase Auth, and can retire the
fixture by marking the Experience cancelled and discovery removed without deleting receipts.

## Attended window and roles

- Window: the explicitly approved attended session on 2026-08-25 UTC. The operation closed at
  2026-08-25T14:31:54Z.
- Deployer: Robert Peralta's project-owner session, with Codex executing only the approved
  commands.
- Monitor and billing-alert responder: Robert Peralta.
- Rollback owner: Robert Peralta; Codex executes the recorded rollback when directed or when an
  abort condition is met.

Abort immediately if the resolved project is not `kithe-production`; a live resource or data
record differs from the baseline; the Functions set or region differs from the allowlist;
Secret Manager would expose a value; rules or index validation changes; any deploy/test command
fails; the generated host has a redirect, wrong content type, missing header, certificate drift,
or exposes payload/location/user data; an unexpected billing alert fires; or the owner asks to
stop.

## Commands after explicit A3 approval

Preflight remains read-only/local:

```powershell
npm.cmd --prefix functions run lint
npm.cmd --prefix functions run test:redesign:unit
npm.cmd --prefix functions run r5:hosting:check -- --require-release-assets
npm.cmd --prefix functions run r5:a3:check
firebase.ps1 emulators:exec --only "firestore" --project demo-kithe-a3 "npm.cmd --prefix firestore-tests run test:r5-p"
```

Then, and only then:

1. Enable Secret Manager API and create a random 32-byte `ANALYTICS_HMAC_SECRET` without
   displaying, logging, or committing its value.
2. Deploy the A3 rules and one index with `--config firebase.r5-p.json`.
3. Wait until the index is ready, then deploy only the seven named Functions with the same
   config and `--only functions:<name>,...`.
4. Dry-run and apply the fixture with runtime-only owner and public test-point coordinates.
5. Deploy Hosting with the A3 config to the generated Firebase host.
6. Verify the root, 404, `assetlinks.json`, `/e/R5PTEST2`, App Check failures/successes, callable
   region, fixture shape, logs, and billing before ending the window.

No Remote Config value changes in A3. The redesign participant backend stays fail-closed in the
release client until its later matrix authorization.

## Completion

All authorized A3 mutations and live checks completed. The exact seven Functions are active,
the unapproved legacy policy-base variable is absent from their runtime environments, the
dedicated rules and one index are live, `R5PTEST2` passed an authenticated six-document
verification, and the generated Firebase host passed its entry, header, 404, and Digital
Asset Links checks. The operation stopped at the A4 custom-domain/DNS gate.

## Rollback

- Stop testing; no custom DNS or QR distribution exists to unwind.
- Retire `R5PTEST2` in place; never delete unlock or audit evidence.
- Disable the first Firebase Hosting release.
- Delete only the seven first-deployed Functions in `us-central1`.
- Deploy `firebase.r5-p-rollback.json` with `--only firestore:rules` to restore the captured
  deny-all baseline.
- Keep the inert composite index and secret unless a separate cleanup is approved; neither
  grants access by itself.
- Recheck that Functions and Hosting show no active deployment, Firestore is deny-all, and the
  fixture is not discoverable. Record timestamps and symptoms in the release evidence.
