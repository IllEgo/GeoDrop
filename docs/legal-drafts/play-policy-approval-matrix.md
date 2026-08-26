# Kithe Play policy approval matrix

Status: **requesting-owner decisions recorded for LP-1 through LP-9, conditional LP-10,
and LP-11; assigned role signoffs remain pending. LP-12, exact policy copy,
publication, and Play submission remain open**

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
| LP-1 | Legal operator/controller shown publicly | Robert Micah Lee Peralta is the current individual / sole-proprietor operator of the Kithe brand; there is no separate LLC, corporation, partnership, or E3HI ownership. | If a business entity is formed later, update the policy and complete Play's personal-to-organization conversion workflow. | Individual operator/Legal | Requesting-owner decision recorded; legal/privacy verification pending |
| LP-2 | Public jurisdiction/address disclosure | State Hawai'i, United States, and add only the address legally required for the selected account/regions. | Use an approved business mailing address if privacy and Play requirements permit. | Operator/Legal | Requesting owner approved; legal/address review pending |
| LP-3 | Minimum age and target audience | Adult-only (18+) U.S. Pilot 1. | A 13+/16+ posture adds child/minor consent, content, store-rating, and operations work. | Owner/Legal/Product | Requesting owner approved; role signoffs pending |
| LP-4 | Pilot regions | United States only for Pilot 1. | Additional regions require rights, transfer, and disclosure review. | Owner/Legal/Product | Requesting owner approved; role signoffs pending |
| LP-5 | Safety-report/evidence retention | 180 days after final case closure; extend only for a logged legal hold, emergency, or active appeal. | Indefinite retention is inconsistent with the privacy-minimal direction. | Legal/Trust & Safety | Requesting owner approved; role/hold process pending |
| LP-6 | Provider backup restoration window | No Firestore scheduled backup or PITR is enabled. Disclose provider deletion completion of up to 180 days unless law requires storage; do not describe it as a restoration window. | Recheck production configuration immediately before publication. | Engineering/Legal | Requesting owner approved; legal/recheck pending |
| LP-7 | Reward/fraud/dispute/legal retention | 180 days after Experience end, deidentified on account deletion; extend only for a documented legal/tax/dispute hold. | Removing all records immediately can break reward disputes; retaining all account data is excessive. | Legal/Operations/Privacy | Requesting owner approved; role/hold process pending |
| LP-8 | Deletion receipt | Approve the implemented pseudonymous 30-day window. | Shorten only after verifying retry/dispute operations; lengthen only with documented need. | Legal/Privacy | Requesting owner approved; role/parity check pending |
| LP-9 | Support and deletion response targets | Acknowledge within 5 business days; complete verified deletion within 30 calendar days unless a disclosed exception applies. | Micah, the individual operator, is primary and Kerise is the operational backup for `support@kitheapp.com`; preserve coverage and document exceptions. | Operations | Requesting owner approved; coverage complete |
| LP-10 | Service-provider classification | Provisional Play No sharing for the exact current release only while providers remain limited service providers and all collection is disclosed. | The 2026-08-26 review found Analytics, Firebase BigQuery/Cloud Logging links, custom external log sinks, and all Maps APIs disabled. The owner then explicitly approved disabling non-Firebase improvement use of Firebase Service Data; Firebase reported success and a reload showed the setting unchecked. Reclassify if Maps/another SDK or recipient role changes. | Legal/Privacy | Privacy-minimal production posture verified; legal/privacy, contract/account, and post-Maps exact-release closure pending |
| LP-11 | Organizer visibility | Approve aggregate/results disclosure and verify no exact participant position/path is exposed. | Any person-level organizer view would conflict with product direction and require redesign. | Product/Privacy | Requesting owner approved; role/exact verification pending |
| LP-12 | Policy version and URLs | Issue an approved version, publish HTTPS `/privacy` and `/account-deletion`, then configure the backend manifest. | Placeholder pages or local files do not satisfy Play. | Legal/Engineering/QA | Open |
| LP-13 | Play account ownership posture | Keep the current personal account while Kithe is individually operated. Revisit organization conversion only after a legal entity, website verification, organization payments profile, and D-U-N-S record are ready. | Google provides an official personal-to-organization conversion flow; policy/operator details must be updated with the transition. | Individual operator/Operations | Current personal posture recorded; future conversion deferred |

## Publication gate

Do not publish either page until every placeholder is resolved, the policy and Data safety
worksheet agree line by line, an authorized reviewer is recorded, the external deletion route
works without app installation, all policy routes return HTTPS 2xx, and a signed release build
opens the same approved URLs and policy version.
