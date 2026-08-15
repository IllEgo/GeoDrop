# Kithe Account Lifecycle and Retention Policy — Draft

Status: **Engineering draft; Legal and Product approval required before publication**  
Policy version implemented by the backend: `pilot-2026-07-21-draft`

This document records the behavior implemented by the account export and
deletion backend. It is not approved public policy. The P0 remains incomplete
until the retention windows below are approved, the copy is published at the
production policy URL, both apps show the same version, and end-to-end tests
pass against production-like data.

## Export behavior

After recent reauthentication, an account holder can request a JSON export.
The export includes:

- Firebase Authentication account metadata and linked provider names;
- the user profile and all nested user collections, including notification
  tokens, group memberships, inventory, blocked creators, and hunt progress;
- the username ownership record, when it is owned by the requester;
- drops created by the requester; and
- safety reports submitted by the requester.

The backend writes the export to a private Storage path and returns a signed URL
that expires after 15 minutes. A scheduled cleanup removes export objects after
24 hours. Export paths are not client-readable under Storage rules without the
short-lived signed URL.

## Deletion behavior

Deletion requires all of the following:

1. a signed-in account;
2. authentication no more than five minutes old;
3. the exact current policy version; and
4. the explicit confirmation value `DELETE`.

The backend then:

- deletes the profile and all nested user collections;
- releases the username only when the mapping still belongs to that user;
- deletes owned drop documents and their referenced Storage media;
- deletes collected inventory copies of those owned drops;
- removes the user's identifier from reaction, report, and collection maps on
  other drops;
- pseudonymizes safety reports submitted by the user; and
- deletes the Firebase Authentication account last, so a failed earlier step
  can be retried.

The completion response contains a receipt ID, completion time, policy version,
and deletion counts. A pseudonymous server-side copy of the receipt is retained
for 30 days to diagnose failed or disputed deletion requests.

## Proposed retention exceptions requiring approval

| Data | Proposed handling | Decision owner | Approval |
|---|---|---|---|
| Safety reports | Retain the report after removing the reporter account ID; retain evidence only for the approved safety/legal window. | Legal, Trust & Safety | Pending |
| Deletion receipts | Retain pseudonymous receipt and counts for 30 days, then purge. | Legal, Privacy | Pending |
| Export objects | Delete after 24 hours; signed access expires after 15 minutes. | Privacy, Security | Pending |
| Provider backups | Remove through the provider backup lifecycle; publish the maximum restoration window. | Engineering, Legal | Pending — window not selected |
| Fraud/redemption records | Retain only fields required by the approved fraud, tax, or dispute policy. | Legal, Operations | Pending — schema/window not selected |

## Verification evidence and remaining P0 work

The isolated 2026-07-21 emulator rehearsal seeded an authenticated password
account with nested profile data, username ownership, group membership,
inventory, an owned drop and Storage object, another user's collected copy,
reaction/report/collection maps, and a safety report. It then exercised export
and deletion through the callable endpoints. The rehearsal passed export object
and payload checks, Auth deletion, recursive profile deletion, username release,
drop/media/inventory cleanup, report pseudonymization, identifier scrubbing, and
the completion receipt contract.

Reproduce it from the repository root with:

```text
firebase emulators:exec --only auth,firestore,storage,functions --project geodrop-ci "npm --prefix functions run account:rehearse"
```

Remaining before P0 completion:

- Deploy the callables and scheduled cleanup to the approved production-like
  Firebase project with App Check enforced.
- Test password and Google-provider reauthentication through signed Android and
  iOS release builds.
- Verify production signed-URL expiry, scheduled 24-hour cleanup, retry behavior,
  and the approved provider backup lifecycle.
- Approve and publish every retention exception and record Legal approval.

Use `npm run account:audit -- preflight --uid UID --output MANIFEST.json`
before the test, keep that manifest private, then run
`npm run account:audit -- verify --uid UID --manifest MANIFEST.json` after the
in-app deletion completes. Verification fails if Auth, nested profile data,
username ownership, drops, media, inventory copies, raw reporter identity, map
identifiers, or the pseudonymous completion receipt do not match the contract.
