# Kithe Legal Policy Readiness

Status: **Blocking — the individual operator is identified, but URLs, exact policy copy,
authorized legal/privacy approval, and publication are not established**

Local progress on 2026-08-14: substantive, visibly marked engineering drafts now exist at
`../public/privacy.html` and `../public/account-deletion.html`. The evidence and unresolved
owner/legal decisions are separated in `legal-drafts/play-policy-approval-matrix.md`. These
files are intentionally outside the live `website/` deployment and do not satisfy the gate.

Local decision/draft update on 2026-08-26: the requesting owner approved the recommended
LP-2 through LP-9, conditional LP-10, and LP-11 directions. Micah, the individual operator,
is primary and Kerise is the operational backup for the support inbox. The non-public privacy, account-deletion,
and data-retention HTML drafts now incorporate those directions while retaining visible
draft warnings, `noindex`, LP-10 provider confirmation, LP-12 version/date, and publication
gates. No policy route was published.

LP-10 production review on 2026-08-26: read-only console evidence shows Firebase Analytics
disabled; BigQuery and the optional Firebase Cloud Logging integration unlinked; only the
internal `_Default` and `_Required` Cloud Logging sinks; 30-day and 400-day bucket retention,
respectively; no linked BigQuery dataset; and all Maps APIs disabled. Firebase's optional use
of Firebase Service Data for non-Firebase service improvement is enabled, but the console and
official provider definition expressly exclude Customer Data from Firebase Service Data.
Firebase also says Service Data can include personal operational data such as IP addresses.
The requesting owner subsequently approved disabling this optional non-Firebase use. Firebase
reported **Service data sharing disabled**, and the setting remained unchecked after reload.
The privacy-minimal production posture is now technically verified, but authorized
legal/privacy and contract/account confirmation plus a post-Maps exact-release recheck remain
blocking before the Play answer is finalized.

Live recheck on 2026-08-25: `https://kitheapp.com/privacy` and
`https://kitheapp.com/account-deletion` both return HTTP 404. The A4 custom entry domain is
connected, but it does not publish or approve these policies. Play setup and A5 remain blocked.

Ownership update on 2026-08-26: the requesting owner superseded the 2026-08-14 two-owner
formation assumption and identified **Robert Micah Lee Peralta** as Kithe's current legal
operator, with the form **individual / sole proprietor**, the brand **Kithe**, and no separate
LLC, corporation, or partnership. Kerise remains the operational backup for LP-9, not a
legal/equity owner. The current personal Play account remains consistent with this posture;
any later entity formation and personal-to-organization conversion remain separately gated.

Identity-parity check on 2026-08-26: Play **About you** confirms account type Personal and
developer name Kithe, but the linked Payments profile shows the shorter legal name **Robert
Peralta** rather than the supplied operator name **Robert Micah Lee Peralta**. No private
address is recorded in this evidence. Exact-copy approval and publication remain blocked
until one authoritative legal-name presentation is selected and verified.

The requesting owner subsequently confirmed **Robert Micah Lee Peralta** as the authoritative
government-identity name. No identity-document details are recorded. The linked personal
Payments profile must be updated from its shorter name presentation and reverified before
identity parity, exact-copy approval, or publication can pass.

The owner authorized the correction and completed private passkey verification on
2026-08-26, but Google Payments' self-service route required proof of a legally documented
name change rather than presenting a text-only correction form. The workflow stopped before
the document process began; no name, document, address, or form was submitted. Identity
parity remains blocking pending Google Payments/Play support or an owner-controlled
accepted-document process.

The owner placed LLC/entity formation on hold on 2026-08-14. Entity registration, EIN,
D-U-N-S, organization payments-profile work, and Play account conversion remain deferred.
The operator identity is now locally resolved, but authorized legal/privacy review, exact
copy, address posture, and publication approval remain blocking.

The mobile app currently references `geodrop.app`, but a July 2026 live check
showed that domain presenting a different DropZone/DeFi product. Those URLs must
not be treated as approved Kithe mobile-app disclosures.

The backend therefore fails closed until `GEODROP_POLICY_BASE_URL` is configured
to an approved HTTPS origin. It exposes a versioned manifest for Terms, Privacy,
Community Guidelines, Promotion Terms, retention, subprocessors, minors, and
support, and records the accepted version and server timestamp for signed-in
users.

## Required launch evidence

| Gate | Owner | Status/evidence |
|---|---|---|
| Legal operator and pilot regions identified | Legal/Product | Individual operator and U.S.-only direction recorded; authorized legal/privacy and address review pending |
| Intended minimum age (13+, 16+, or 18+) selected | Legal/Product | 18+ direction owner-approved; assigned-role/Play verification pending |
| Terms approved and published | Legal | Blocking |
| Privacy notice and data inventory reconciled | Legal/Engineering | Non-public draft updated; exact release/provider/legal review blocking |
| Community Guidelines and moderation taxonomy reconciled | Legal/Trust & Safety | Blocking |
| Promotion/merchant terms approved | Legal/Operations | Blocking |
| Retention and deletion exceptions approved | Legal/Privacy | Requesting-owner directions recorded; assigned-role signoffs and production verification blocking |
| Subprocessor list approved and published | Legal/Security | Blocking |
| Minors policy approved and published | Legal | Blocking |
| Support path staffed and published | Operations | Micah primary/Kerise backup and response targets recorded; publication blocking |
| All eight URLs return 2xx over HTTPS | QA | Blocking |
| Android/iOS show the same policy version and URLs | QA | Blocking |
| Server acceptance version/timestamp verified | QA | Backend implemented; client/deployment verification pending |

No embedded draft copy or placeholder URL constitutes Legal approval. After the
production origin is selected, set the environment value, verify all routes,
and record the approver and evidence links in the P0 status report.

