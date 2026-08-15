# Kithe Legal Policy Readiness

Status: **Blocking — production operator, URLs, policy copy, and Legal approval
are not established**

Local progress on 2026-08-14: substantive, visibly marked engineering drafts now exist at
`../public/privacy.html` and `../public/account-deletion.html`. The evidence and unresolved
owner/legal decisions are separated in `legal-drafts/play-policy-approval-matrix.md`. These
files are intentionally outside the live `website/` deployment and do not satisfy the gate.

Ownership update on 2026-08-14: the owner stated that Kithe will have two owners in a
partnership arrangement and confirmed that Kithe is not registered. The exact Hawaiʻi form
is not selected; “two members”
could instead describe a two-member LLC. Policy publication must wait for the accepted BREG
legal name, formation date, authorized approver, and a decision about converting the current
personal Play account to an organization account. No draft may name one individual as Kithe's
sole operator or identify E3HI as owner.

The owner placed LLC/entity formation on hold on 2026-08-14. Entity registration, EIN,
D-U-N-S, organization payments-profile work, and Play account conversion are therefore
deferred. This does not waive the legal-operator publication gate; the placeholder remains
unresolved while local product and store-preparation work continues.

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
| Legal entity and pilot regions identified | Legal/Product | Blocking |
| Intended minimum age (13+, 16+, or 18+) selected | Legal/Product | Blocking |
| Terms approved and published | Legal | Blocking |
| Privacy notice and data inventory reconciled | Legal/Engineering | Blocking |
| Community Guidelines and moderation taxonomy reconciled | Legal/Trust & Safety | Blocking |
| Promotion/merchant terms approved | Legal/Operations | Blocking |
| Retention and deletion exceptions approved | Legal/Privacy | Blocking; see `account-lifecycle-retention-draft.md` |
| Subprocessor list approved and published | Legal/Security | Blocking |
| Minors policy approved and published | Legal | Blocking |
| Support path staffed and published | Operations | Blocking |
| All eight URLs return 2xx over HTTPS | QA | Blocking |
| Android/iOS show the same policy version and URLs | QA | Blocking |
| Server acceptance version/timestamp verified | QA | Backend implemented; client/deployment verification pending |

No embedded draft copy or placeholder URL constitutes Legal approval. After the
production origin is selected, set the environment value, verify all routes,
and record the approver and evidence links in the P0 status report.

