# Kithe Pilot 1 policy owner ballot

Status: **Kithe confirmed unregistered; formation decision required before LP-1; no
publication or Play submission authorized**

Owner decision 2026-08-14: hold off on LLC/entity formation. LP-1 and Play organization
conversion remain open and must not be inferred from the Kithe brand name or two-owner intent.

Prepared: 2026-08-14

This ballot reduces the policy matrix to the choices the owner can make now. It is an
engineering/product recommendation, not legal advice. Approval updates only the local draft
policy package. Publishing pages, entering Play answers, or deploying backend policy
configuration requires a later action-time approval.

## Information the owner must provide

### O-1 — Two-owner legal structure and name

The owner has clarified that Kithe will be owned by two people and confirmed that Kithe is
not registered. Do not insert one individual's name as Kithe's sole operator. Before
publication, provide:

1. the exact legal form: **Hawaiʻi general partnership, Hawaiʻi LLP, two-member Hawaiʻi
   LLC, or another specified form**;
2. the exact registered legal name shown in the Hawaiʻi BREG record;
3. the effective/formation date; and
4. the partner or member authorized to approve policy and control the Play account.

The word **members** normally points toward an LLC, while a partnership has **partners**;
casual wording is not enough to select the legal form. Hawaiʻi DCCA describes a general
partnership as two or more co-owners who are personally liable for partnership debts. An LLP
is a general partnership that elects limited liability, while an LLC uses members and files
Articles of Organization. This ballot does not recommend a legal form; the two owners should
make that choice with a qualified Hawaiʻi attorney and tax professional.

Formation work is currently deferred. The comparison and non-filing preparation sequence are in
`two-owner-formation-readiness.md`. A two-member LLC is the recommended default to evaluate
for an app business, but it is not selected or authorized.

Recommended sequencing: keep LP-1 and public policy publication open until the entity or
partnership is formed and its BREG record is accepted. Then name the registered operator—not
E3HI and not only one partner—in the policy. Use `support@kitheapp.com` as the public privacy
contact; do not put either owner's home address into the policy unless a qualified reviewer
confirms it is required.

The current Play account is personal. Google currently provides an official individual-to-
organization conversion flow after the organization website is verified and an organization
payments profile, D-U-N-S number, organization/contact details, and any requested identity
verification are complete. Whether to convert before the first release is a separate owner
gate; do not silently publish a partnership-operated app under a personal-use posture.

## Recommended defaults the owner can approve now

| Ballot | Matrix IDs | Recommended Pilot 1 decision | Result |
|---|---|---|---|
| O-2 | LP-2, LP-4 | Hawaiʻi, United States operator; United States-only Pilot 1 availability. | Avoids implying worldwide launch or unreviewed international transfer/rights coverage. |
| O-3 | LP-3 | Target **Ages 18 and over**; not designed for children; no child-directed store imagery or copy. | Keeps Pilot 1 outside the Play Families path. The app must still receive an accurate IARC rating. |
| O-4 | LP-5 | Retain safety case records for **180 days after final case closure**, then delete or deidentify; extend only for a documented legal hold, emergency, or active appeal. | Fixed, privacy-minimal window with an exception that must be logged and reviewed. |
| O-5 | LP-7 | Retain deidentified reward/fraud/dispute audit for **180 days after the Experience ends**; remove account linkage on deletion; extend only for a documented legal/tax/dispute hold. | Matches the implemented R1 default without preserving the full participant account. |
| O-6 | LP-8 | Retain a pseudonymous deletion receipt for **30 days**, then purge it. | Matches the implemented retry/dispute window. |
| O-7 | LP-9 | Acknowledge emailed privacy/deletion requests within **5 business days** and complete verified deletion within **30 calendar days** unless a disclosed legal exception applies. | A concrete operational promise; requires an owner-monitored support inbox and backup coverage. |
| O-8 | LP-11 | Experience hosts receive Experience-level aggregates and operational results only—never a participant's exact live position or travel path. | Preserves the approved product/privacy direction. |

## Items that must remain open after owner approval

| Matrix ID | Why it cannot be approved by assumption | Closure evidence |
|---|---|---|
| LP-6 | The maximum Firebase/Google backup-restoration lifecycle is not verified for the exact production configuration. | Current provider/configuration evidence and a truthful maximum or qualified disclosure. |
| LP-10 | Play's service-provider exception is configuration- and contract-dependent. Google/Firebase and Cloudflare must remain limited to operating Kithe, with no advertising/profile reuse or optional export that changes the classification. | Exact release SDK audit, provider terms/DPA review, Firebase integration review, and Cloudflare routing/hosting review. |
| LP-12 | Version, effective date, HTTPS URLs, backend manifest, and signed-build links do not exist as one approved production bundle yet. | Approved copy, publication authorization, HTTPS 2xx checks, backend configuration, and signed-build verification. |

## Recommended provisional Play position

Until LP-10 closes, keep **Data sharing: Provisional No**. Google Play states that transfers
to a service provider processing data on the developer's behalf do not need to be declared
as sharing, while SDK collection still must be disclosed. If any provider uses Kithe data
outside that role, or an optional integration exports it elsewhere, change the answer to
match the actual release.

## Approval language

The owners can use this instruction after selecting and registering the legal form:

> Legal form: [EXACT FORM]. Registered operator name: [EXACT REGISTERED NAME]. Authorized
> policy approver: [NAME/ROLE]. Approve O-2 through O-8 as written. Keep LP-6, LP-10,
> LP-12, and Play account conversion open. Do not publish or submit anything yet.

## Current official policy references

- [Google Play User Data and privacy-policy requirements](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Google Play account-deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en-EN)
- [Google Play Data safety form and service-provider exception](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en-EN)
- [Google Play target audience and Families requirements](https://support.google.com/googleplay/android-developer/answer/9867159?hl=en-EN)
- [Firebase Android data-disclosure guidance](https://firebase.google.com/docs/android/play-data-disclosure)
- [Hawaiʻi DCCA business entity types](https://cca.hawaii.gov/breg/legalinfo/)
- [Hawaiʻi DCCA registration FAQ](https://cca.hawaii.gov/breg/faqs/)
- [Google Play personal-to-organization conversion](https://support.google.com/googleplay/android-developer/answer/16260648?hl=en)
