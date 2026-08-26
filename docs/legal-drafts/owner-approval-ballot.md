# Kithe Pilot 1 policy owner ballot

Status: **requesting owner recorded O-1/LP-1 and approved O-2 through O-8, the verified LP-6
wording, and the conditional LP-10 posture on 2026-08-26. LP-12, required role-specific signoffs, exact
policy copy, publication, and Play submission remain open**

Owner decision 2026-08-14: hold off on LLC/entity formation. At that time LP-1 and Play
organization conversion remained open and could not be inferred from the Kithe brand name.

Requesting-owner decision 2026-08-26: after being presented with option 1, the owner replied
`approve`. Record the recommended local direction for LP-2 through LP-8, conditional LP-10,
and LP-11. Keep LP-1 deferred, LP-9 open until primary/backup support coverage is named, and
LP-12/publication closed. The approving owner's name/role, the second owner's approval, and
the required legal/privacy/operations/product role signoffs were not supplied by this
instruction and remain open.

Coverage instruction 2026-08-26: Micah is primary and Kerise is the operational backup
for monitoring `support@kitheapp.com`. Their private forwarding destination is intentionally
not recorded here. The later O-1/LP-1 decision clarified that Micah is the sole legal/equity
owner. This supplies the missing coverage roles but does not by itself approve
O-7/LP-9's five-business-day acknowledgment or 30-calendar-day deletion target.

LP-9 decision 2026-08-26: the requesting owner explicitly approved O-7/LP-9. Micah remains
primary and Kerise backup; acknowledge emailed privacy/deletion requests within five business
days and complete verified deletion within 30 calendar days unless a disclosed legal
exception applies. Exact public copy and publication remain separately gated.

O-1/LP-1 decision 2026-08-26: the requesting owner identified **Robert Micah Lee Peralta**
as Kithe's current operator, selected **individual / sole proprietor** as the legal form,
confirmed **Kithe** as the brand, and confirmed there is no separate LLC, corporation, or
partnership. Kerise remains an operational backup, not a legal/equity owner. Independent
legal/privacy review, exact-copy approval, and publication remain separately gated.

Prepared: 2026-08-14

This ballot reduces the policy matrix to the choices the owner can make now. It is an
engineering/product recommendation, not legal advice. Approval updates only the local draft
policy package. Publishing pages, entering Play answers, or deploying backend policy
configuration requires a later action-time approval.

## Information the owner must provide

### O-1 — Current individual operator and future conversion

The requesting owner identified **Robert Micah Lee Peralta** as Kithe's current individual /
sole-proprietor operator. Kithe is the brand; there is no separate LLC, corporation, or
partnership. Before publication:

1. verify that the exact legal name matches the personal Play/Payments identity;
2. obtain authorized legal/privacy review of the operator and address wording;
3. approve the exact policy copy; and
4. record the final policy version and effective date under LP-12.

The current personal Play account is consistent with this individual posture. If a business
entity is formed later, update the operator and policy copy and complete Google's official
personal-to-organization conversion flow after website verification, an organization
payments profile, D-U-N-S number, organization/contact details, and any requested identity
verification. The superseded multi-owner comparison remains in
`two-owner-formation-readiness.md` for future reference only.

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
| LP-6 | The requesting owner approved the verified direction: no Firestore scheduled backup or PITR, with provider deletion completion of up to 180 days unless law requires storage—not a restoration promise. | Legal approval of exact copy and a production configuration recheck immediately before publication. |
| LP-10 | The requesting owner approved only a conditional, provisional Play No-sharing posture. A 2026-08-26 review found Analytics, optional Firebase BigQuery/Cloud Logging links, custom external log sinks, and all Maps APIs disabled. The requesting owner then explicitly approved disabling non-Firebase improvement use of Firebase Service Data; the setting was verified unchecked after reload. | Obtain authorized legal/privacy and contract/account confirmation, then repeat the exact-release review after Maps or any other provider/SDK is enabled. |
| LP-12 | Version, effective date, HTTPS URLs, backend manifest, and signed-build links do not exist as one approved production bundle yet. | Approved copy, publication authorization, HTTPS 2xx checks, backend configuration, and signed-build verification. |

## Recommended provisional Play position

Until LP-10 closes, keep **Data sharing: Provisional No**. Google Play states that transfers
to a service provider processing data on the developer's behalf do not need to be declared
as sharing, while SDK collection still must be disclosed. If any provider uses Kithe data
outside that role, or an optional integration exports it elsewhere, change the answer to
match the actual release.

## Remaining approval language

The operator and assigned approvers can use this instruction after completing the remaining
legal/privacy and exact-copy review:

> Legal form: individual / sole proprietor. Operator: Robert Micah Lee Peralta. Brand:
> Kithe. No separate entity. Authorized policy approver: [NAME/ROLE/DATE]. Confirm the
> already recorded requesting-owner direction for
> O-2 through O-8, LP-6, and conditional LP-10, including Micah as LP-9 primary
> and Kerise as operational backup. Keep LP-12 and any future Play account conversion open. Do not
> publish or submit anything yet.

## Current official policy references

- [Google Play User Data and privacy-policy requirements](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Google Play account-deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en-EN)
- [Google Play Data safety form and service-provider exception](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en-EN)
- [Google Play target audience and Families requirements](https://support.google.com/googleplay/android-developer/answer/9867159?hl=en-EN)
- [Firebase Android data-disclosure guidance](https://firebase.google.com/docs/android/play-data-disclosure)
- [Hawaiʻi DCCA business entity types](https://cca.hawaii.gov/breg/legalinfo/)
- [Hawaiʻi DCCA registration FAQ](https://cca.hawaii.gov/breg/faqs/)
- [Google Play personal-to-organization conversion](https://support.google.com/googleplay/android-developer/answer/16260648?hl=en)
