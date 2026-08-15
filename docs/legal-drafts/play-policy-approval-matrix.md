# Kithe Play policy approval matrix

Status: **engineering draft; no policy page is approved or published**

Owner-facing ballot: `owner-approval-ballot.md`. Official Play/Firebase guidance was
rechecked on 2026-08-14. Google requires an in-app account-deletion path and an external web
request path when any account creation is available; the web path must work without forcing
reinstallation. The privacy notice must be public, non-geofenced, non-PDF, identify the app
or listed developer, and accurately cover data access, collection, use, sharing, security,
retention, and deletion. Firebase's current SDK guide confirms collection remains the app
developer's disclosure responsibility even when a service-provider transfer is not counted
as Play “sharing.”

This matrix turns the remaining privacy and account-deletion work into explicit decisions.
The editable local pages are `public/privacy.html` and `public/account-deletion.html`. They
contain a visible draft warning, `noindex`, and unresolved placeholders. The live
`website/` directory was not changed.

## Evidence-backed statements ready for review

| Statement | Local evidence | Review still required |
|---|---|---|
| Kithe has no background-location permission. | Android manifest and merged debug manifest audit; `docs/location-audit.md` | Repeat against the exact signed release bundle. |
| A participant's unlock fix is evaluated server-side and its coordinates are not stored in the unlock record. | `unlockDrop` contract and R1/R6 tests | Production log/telemetry inspection. |
| Organizer-authored drop coordinates are stored as content placement. | R7 authoring contract and Data safety worksheet | Confirm final organizer UI and disclosure language. |
| Pilot 1 supports text and photo authoring, not audio/video. | Product direction; microphone/audio-authoring removal; release audit still pending | Repeat dependency/manifest/UI audit on the signed release. |
| In-app export and permanent deletion exist behind recent reauthentication. | `AccountDataDialog.kt`, `AccountLifecycleRepo.kt`, callable/emulator rehearsal | Signed-build production-like rehearsal for password and Google accounts. |
| Export links expire after 15 minutes and objects after 24 hours. | Account-lifecycle implementation and emulator rehearsal | Verify scheduled production cleanup. |
| Raw pseudonymous pilot events expire after 180 days and actor data is removed on deletion. | R1 contract and redesign purge/deletion code | Verify Firestore TTL/index/config in production. |
| Firebase Analytics and advertising-ID permissions are absent from the local target. | Dependency and merged-manifest audit | Repeat on the exact release App Bundle. |

## Decisions required before publication

| ID | Decision | Recommended Pilot 1 option | Alternatives / consequence | Owner | Status |
|---|---|---|---|---|---|
| LP-1 | Legal operator/controller shown publicly | Kithe is confirmed unregistered and the owner has deferred entity formation. Keep the public operator placeholder unresolved. | General partnership, LLP, and two-member LLC have materially different liability/tax/filing consequences. Do not name one person as sole operator or imply E3HI owns Kithe. | Both owners/Legal | Deferred by owner |
| LP-2 | Public jurisdiction/address disclosure | State Hawai'i, United States, and add only the address legally required for the selected account/regions. | Use an approved business mailing address if privacy and Play requirements permit. | Owner/Legal | Open |
| LP-3 | Minimum age and target audience | Adult-only (18+) U.S. Pilot 1. | A 13+/16+ posture adds child/minor consent, content, store-rating, and operations work. | Owner/Legal/Product | Open |
| LP-4 | Pilot regions | United States only for Pilot 1. | Additional regions require rights, transfer, and disclosure review. | Owner/Legal/Product | Open |
| LP-5 | Safety-report/evidence retention | 180 days after final case closure; extend only for a logged legal hold, emergency, or active appeal. | Indefinite retention is inconsistent with the privacy-minimal direction. | Legal/Trust & Safety | Open — O-4 |
| LP-6 | Provider backup restoration window | Publish the verified maximum window from Firebase/Google configuration and contract. | Do not guess or reuse a generic vendor number. | Engineering/Legal | Open |
| LP-7 | Reward/fraud/dispute/legal retention | 180 days after Experience end, deidentified on account deletion; extend only for a documented legal/tax/dispute hold. | Removing all records immediately can break reward disputes; retaining all account data is excessive. | Legal/Operations/Privacy | Open — O-5 |
| LP-8 | Deletion receipt | Approve the implemented pseudonymous 30-day window. | Shorten only after verifying retry/dispute operations; lengthen only with documented need. | Legal/Privacy | Open |
| LP-9 | Support and deletion response targets | Acknowledge within 5 business days; complete verified deletion within 30 calendar days unless a disclosed exception applies. | Do not promise a response time without a monitored inbox and backup coverage. | Operations | Open — O-7 |
| LP-10 | Service-provider classification | Confirm Google/Firebase/Maps/Play and Cloudflare roles and current terms before answering Play “shared.” | If any provider uses data outside a service-provider role, update the Play answer and policy. | Legal/Privacy | Open |
| LP-11 | Organizer visibility | Approve aggregate/results disclosure and verify no exact participant position/path is exposed. | Any person-level organizer view would conflict with product direction and require redesign. | Product/Privacy | Open |
| LP-12 | Policy version and URLs | Issue an approved version, publish HTTPS `/privacy` and `/account-deletion`, then configure the backend manifest. | Placeholder pages or local files do not satisfy Play. | Legal/Engineering/QA | Open |
| LP-13 | Play account ownership posture | Keep the current personal account unchanged while formation is on hold. Revisit organization conversion only after a legal structure, website verification, organization payments profile, and D-U-N-S record are ready. | Publishing first under the personal posture creates an ownership/verification transition. Google provides an official conversion flow, but it must be planned and verified. | Both owners/Operations | Deferred by owner |

## Publication gate

Do not publish either page until every placeholder is resolved, the policy and Data safety
worksheet agree line by line, an authorized reviewer is recorded, the external deletion route
works without app installation, all policy routes return HTTPS 2xx, and a signed release build
opens the same approved URLs and policy version.
