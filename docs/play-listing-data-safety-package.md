# Kithe Play listing and Data safety package

Status: **local package prepared; owner/legal decisions and release verification open**  
Prepared: 2026-08-14  
Submission authorization: **not granted**

Current Play/A5 audit — 2026-08-25: the Play app now exists and signing/identity setup from
A1 is complete, but no release is uploaded. Play reports 0/11 setup tasks, 0 testers, and a
mandatory 12-tester/14-day closed test before production access. A5a now passes locally at
API 36: all 130 release-variant tests, release lint, bundle generation, and the privacy audit
pass. The diagnostic AAB remains intentionally unsigned. An upload key, approved policy
URLs, Maps, reviewer data, screenshots, Play setup, and the qualifying closed test remain
blocking. See `../deployment/r5-p/A5-APPROVAL.md` and
`../deployment/r5-p/A5A-EVIDENCE.md`.

## Package contents

- `play-listing-draft.md` — copy-ready listing metadata and descriptions, asset plan,
  reviewer-access fixture, and unresolved Play Console answers.
- `play-data-safety-draft.md` — evidence-linked Data safety worksheet, SDK/manifest audit,
  privacy/deletion blockers, and release verification sequence.
- `../play-assets/store-listing/` — owner-approved Kithe icon and feature graphic in editable
  SVG and Play-sized PNG formats, plus the real-device screenshot capture manifest.
- `../public/privacy.html` and `../public/account-deletion.html` — local, visibly marked
  policy drafts; neither is part of the live `website/` deployment.
- `legal-drafts/play-policy-approval-matrix.md` — evidence/decision matrix required to
  remove the draft placeholders safely.
- `legal-drafts/owner-approval-ballot.md` — short owner decision form with recommended
  Pilot 1 defaults and the items that must remain open for verification.
- `legal-drafts/two-owner-formation-readiness.md` — Hawaiʻi general partnership/LLP/
  two-member LLC comparison and safe pre-filing sequence; planning only.

## Outcome

The listing direction does not fundamentally oppose `product-direction.md` or
`migration-plan.md`. It keeps the event-first acquisition wedge, invite-only
Experiences, participant-first map/list/unlock loop, approved organizers, text/photo
drops, server-authoritative proximity checks, Collection receipts, reporting/blocking,
and joined-Experience notifications. It does not claim E3HI ownership or turn Play Store
discovery into the acquisition strategy.

The audit found two implementation details that opposed or materially drifted from the
approved Pilot 1 boundary:

1. Audio is deferred, but the previous manifest requested microphone access and exposed
   the legacy recorder.
2. Firebase Analytics was compiled and invoked, adding automatic analytics and
   advertising-ID/AdServices permissions despite the intended privacy-minimal production
   posture and a separate purpose-built analytics ledger.

Both are now corrected locally. A clean run of 127 unit tests, lint, and debug APK assembly
passed on 2026-08-14. The regenerated debug manifest contains none of `AD_ID`, the two
AdServices permissions, `RECORD_AUDIO`, or the recorder activity. The exact signed release
App Bundle must still repeat this evidence before any Play answer or upload.

The visual audit also found the Android launcher still uses the legacy turquoise media pin
with photo, video, and audio symbols. That is inconsistent with the Kithe rename and with
Pilot 1's text/photo-only boundary. The owner approved the flat, high-contrast Kithe mark,
feature graphic, and screenshot plan on 2026-08-14. The mark is now adopted in Android and
the local website source; no website deployment or Play upload occurred.

## Approval list

| ID | Decision | Recommendation | Why it is needed |
| --- | --- | --- | --- |
| PL-1 | Category | **Events** | Matches the primary market and avoids promising global/local guide discovery |
| PL-2 | Short/full listing copy | **Approve draft** | Enables a copy-ready listing while identity verification is pending |
| PL-3 | Target audience | **Adults only for Pilot 1** | Avoids child-directed claims and additional Families obligations; legal/owner decision required |
| PL-4 | Initial availability | **United States, fail-closed production listing** | Matches the approved real Play install route and Hawaii pilot |
| PL-5 | Visual identity and plan | **Approved locally 2026-08-14** | “K as trail” mark adopted in Android and local website source; feature graphic and six-shot participant-first plan approved; upload remains closed |
| PL-6 | Play reviewer fixture | **Approve normal test account + simulated-location instructions** | Gives review access without a proximity bypass or privileged backdoor |
| DS-1 | Firebase Analytics | **Implemented and locally verified** | Dependency and five legacy events removed; debug manifest has no advertising-ID/AdServices permissions |
| DS-2 | Audio recorder/microphone | **Implemented and locally verified** | Recorder is unregistered/unreachable, audio authoring is hidden, and debug manifest has no microphone permission |
| DS-3 | Data sharing | **Provisional No; obtain vendor/legal confirmation** | Service-provider exceptions and optional Google settings must be verified |
| DS-4 | Privacy policy | **Working draft prepared; resolve LP-1 through LP-12, approve, then separately authorize publication at `/privacy`** | Mandatory for all Play apps and must match actual collection |
| DS-5 | External deletion page | **Working draft prepared; approve support/deletion flow, then separately authorize publication at `/account-deletion`** | Mandatory because Kithe creates accounts |
| DS-6 | Retention exceptions | **Decide using the local policy approval matrix before publication** | Deletion receipts, safety reports, analytics, rewards, and fraud/security records need accurate promises |

Approval of this package authorizes only local follow-up implementation explicitly named
by the owner. It does not authorize DNS changes, policy publication, Firebase/Hosting/
Functions/rules deployment, production fixtures, Play App creation, bundle upload, form
submission, testing-track release, or publication.

## Recommended execution order while Play identity verification is pending

1. Approve or revise PL-1 through PL-6 and DS-3 through DS-6; DS-1 and DS-2 are locally
   implemented. PL-5, DS-4, DS-5, and DS-6 now have local review artifacts.
2. The approved launcher is implemented and physically verified. Capture six sanitized phone
   screenshots only from the exact signed release candidate after Maps and reviewer-fixture
   dependencies close.
3. Resolve LP-1 through LP-12 in `legal-drafts/play-policy-approval-matrix.md`; obtain
   privacy/legal review of the local pages without publishing.
4. [Complete 2026-08-25] Complete Play identity/device/phone verification and create the Play app.
5. Generate and inspect the exact signed release App Bundle manifest/dependency graph.
6. Create the reviewer fixture only under separately approved production-data handling.
7. Reconcile the final App Bundle, production settings, legal pages, and Data safety form.
8. Stop for action-time approvals before website publication, production deployment,
   bundle upload, or any Play submission.
