# A5c policy-decision review

Status: **requesting-owner approval recorded for the LP-1 individual-operator decision and
the recommended LP-2 through LP-9, conditional LP-10, and LP-11 local decisions. LP-12,
required role-specific signoffs, exact policy-copy approval, publication, Play answers, and all external configuration remain
unauthorized**

Reviewed: 2026-08-26

Decision recorded: 2026-08-26

This is an engineering/product decision package, not legal advice. The owner approved only
the review of LP-1 through LP-12. This review did not publish policy pages, edit Google Play,
change Firebase, Cloudflare, Maps, Remote Config, or reviewer data, or approve any policy
decision by implication.

## Review result

- The requesting owner approved the recommended Pilot 1 direction for LP-2 through LP-8 and
  LP-11. Legal, privacy, operations, and implementation-verification signoffs identified in
  the checklist are still required; this is not final policy-copy approval.
- LP-9 is approved locally: Micah, the individual operator, is primary and Kerise is the operational backup for
  monitoring `support@kitheapp.com`, with a five-business-day acknowledgment target and
  30-calendar-day verified-deletion completion target unless a disclosed legal exception
  applies.
- The read-only production check resolves the technical uncertainty behind LP-6: Kithe's
  `(default)` Firestore database has point-in-time recovery disabled, no scheduled backup,
  and no existing Firestore backup. Provider deletion terms still need an approved, precise
  disclosure.
- The requesting owner approved LP-10's provisional Play **No data sharing** direction for
  the exact current release, subject to the conditions below. It is not a final legal
  determination and must be rechecked after any Maps integration or other SDK/provider
  change.
- The requesting owner subsequently identified **Robert Micah Lee Peralta** as Kithe's
  current individual / sole-proprietor legal operator, with Kithe as the brand and no
  separate LLC, corporation, or partnership. This supersedes the earlier two-owner legal-form
  assumption. Authorized legal/privacy review and exact-copy approval remain open.
- A read-only Play **About you** check on 2026-08-26 confirmed account type **Personal** and
  developer name **Kithe**, but the linked Payments profile currently presents the shorter
  legal name **Robert Peralta**. No address or other private account detail is recorded here.
  The operator-name mismatch must be resolved before exact-copy approval or publication.
- The requesting owner then confirmed that **Robert Micah Lee Peralta** is the authoritative
  government-identity name. Do not store identity-document details in the repository. The
  linked personal Payments profile must be updated and reverified to that exact name before
  identity parity can pass.
- LP-12 remains blocked until an authorized approver approves exact final copy and the later
  publication, backend-manifest, HTTPS, and signed-build parity checks pass.

## Verified technical and provider evidence

### Firestore retention posture

Read-only Firebase CLI queries against project `kithe-production` on 2026-08-26 returned:

- database: `projects/kithe-production/databases/(default)`, Native mode, location `nam5`;
- `pointInTimeRecoveryEnablement`: `POINT_IN_TIME_RECOVERY_DISABLED`;
- `versionRetentionPeriod`: `3600s`;
- backup schedules: none; and
- existing Firestore backups: none.

No configuration command was issued. This means Kithe currently has no customer-restorable
Firestore scheduled-backup or seven-day PITR window. It does **not** mean that every residual
provider copy disappears instantly. Google's current Firebase/Cloud Data Processing terms
state that customer-requested deletion is completed as soon as reasonably practicable and
within a maximum of 180 days, unless applicable law requires storage. That 180-day period is
a provider deletion-completion bound, not a Kithe restoration promise.

### Service-provider posture

- Google Play says a transfer to a service provider processing data on the developer's
  behalf need not be declared as Play “sharing,” although the app must still accurately
  disclose collection.
- Current Google/Firebase data-processing terms describe Google as a processor when it
  processes customer data to provide the contracted services.
- Cloudflare's current DPA and U.S. privacy statement describe its processor/service-provider
  role for customer data processed to provide contracted services.
- The exact uploaded release contains Firebase Auth, Firestore, Functions, Storage, FCM,
  Remote Config, Crashlytics, and Play Integrity App Check. Firebase Analytics, Google Mobile
  Ads, Advertising ID, and AdServices are absent.
- Maps libraries are compiled but the uploaded release has `MAPS_CONFIGURED=false`; therefore
  this review does not classify a future enabled Maps data flow.
- Cloudflare currently provides root-site Pages/DNS/email routing. `join.kitheapp.com` is a
  DNS CNAME to Firebase Hosting.

LP-10 remains conditional on the contracted accounts retaining this limited posture: no
advertising/profile reuse, no optional export or integration that changes the recipient's
role, no undisclosed SDK collection, and a fresh review of the exact signed candidate after
Maps or any other provider/SDK is enabled.

## LP-1 through LP-12 decision checklist

| ID | Review recommendation | Evidence or condition required to close | State after review |
|---|---|---|---|
| LP-1 | Name Robert Micah Lee Peralta as the current individual / sole-proprietor operator of the Kithe brand; do not imply that Kithe is an LLC, corporation, partnership, or E3HI property. | Update and reverify the linked personal Payments profile from the shorter `Robert Peralta` presentation to the confirmed authoritative name, then obtain authorized legal/privacy and exact-copy approval. | **Requesting-owner decision recorded; Payments identity/legal verification pending** |
| LP-2 | State Hawaiʻi, United States; publish only the address legally required, preferably an approved business mailing address. | Legal review of the individual-operator address posture. | **Requesting owner approved; legal/address review pending** |
| LP-3 | Pilot 1 is for adults aged 18 and over and is not designed for children. | Required role signoff and consistent Play target-audience/IARC answers. | **Requesting owner approved; legal/product closure pending** |
| LP-4 | Limit Pilot 1 availability to the United States. | Required role signoff and matching Play country availability. | **Requesting owner approved; legal/product closure pending** |
| LP-5 | Keep safety case records for 180 days after final case closure, then delete or deidentify; extend only for a logged legal hold, emergency, or active appeal. | Legal/Trust & Safety signoff and an operational hold log. | **Requesting owner approved; role closure pending** |
| LP-6 | Disclose that no Firestore scheduled backup or PITR is enabled; state that provider deletion may take up to 180 days unless law requires storage, and do not describe that period as a restoration window. | Legal approval of exact final wording; recheck configuration immediately before publication. | **Requesting owner approved; legal/recheck pending** |
| LP-7 | Keep deidentified reward/fraud/dispute audit records for 180 days after an Experience ends; remove account linkage on deletion; extend only for a documented legal, tax, or dispute hold. | Legal/Operations/Privacy signoff and hold procedure. | **Requesting owner approved; role closure pending** |
| LP-8 | Keep a pseudonymous deletion receipt for 30 days, then purge it. | Legal/Privacy signoff and implementation-parity check. | **Requesting owner approved; role closure pending** |
| LP-9 | Acknowledge emailed requests within 5 business days and complete verified deletion within 30 calendar days unless a disclosed legal exception applies. | Micah, the individual operator, is primary and Kerise is the operational backup for monitoring `support@kitheapp.com`; preserve coverage and document any legal exception. | **Requesting owner approved; coverage complete** |
| LP-10 | Use provisional Play “No data sharing” only for service-provider processing under the conditions above; still disclose all collection. | Analytics/exports/Maps posture is verified, and the owner-approved non-Firebase Firebase Service Data use was disabled and verified after reload. Authorized legal/privacy and contract/account confirmation and a post-Maps exact-release recheck remain required. | **Privacy-minimal production posture verified; legal/post-Maps closure pending** |
| LP-11 | Give Experience hosts only Experience-level aggregates and operational results—never an exact participant live position or travel path. | Product/Privacy signoff and exact candidate/UI/backend verification. | **Requesting owner approved; role/verification closure pending** |
| LP-12 | Use the actual authorized publication date as the policy version/effective date; publish approved HTTPS `/privacy` and `/account-deletion` pages and keep app/backend versions identical. | LP-1 through LP-11 closure as applicable, exact-copy approval, separate publication/deployment authorization, HTTPS 2xx checks, backend manifest, and signed-build link/version parity. | **Blocked until final bundle** |

LP-13 (Play account ownership posture) is outside this LP-1 through LP-12 review. The current
personal Play account is consistent with the recorded individual operator; any later
personal-to-organization conversion remains deferred until a business entity exists.

## Non-public policy draft preparation — 2026-08-26

After LP-9 approval, the owner instructed the workflow to continue while formation remained
deferred. That instruction authorized non-public draft preparation only. The local files
`public/privacy.html`, `public/account-deletion.html`, and `public/data-retention.html` now
incorporate the requesting-owner LP-1 through LP-9 and LP-11 directions and clearly label
LP-10's remaining provider/account recheck.

The drafts now state the 18+ and U.S.-only Pilot 1 posture; 180-day safety and deidentified
reward/fraud/dispute windows; 30-day deletion receipt; five-business-day acknowledgment and
30-calendar-day verified-deletion targets; aggregate-only organizer visibility; no scheduled
Firestore backup/PITR; provider deletion completion of up to 180 days; and Firebase's current
90-day Crashlytics retention before removal begins.

All three pages retain `noindex, nofollow`, a visible **Draft — not approved for
publication** warning, unresolved version/effective-date fields, and a publication stop.
The privacy draft retains the unresolved registered-operator/address field and explicit
LP-10 contract/account confirmation. The retention draft records the verified absence of
optional Crashlytics, BigQuery, Cloud Logging, and custom-sink exports and requires a fresh
pre-publication recheck.

These files remain outside the dedicated `hosting/` entry bundle. No live site, Google Play
field, Firebase/Cloudflare/Maps setting, backend policy manifest, or signed build was changed.
Official diagnostic-retention evidence:
[Firebase Privacy and Security](https://firebase.google.com/support/privacy).

Verification results:

- a local tag-balance/required-marker check passed for all three HTML files;
- each page contains `noindex, nofollow`, a visible draft warning, and unresolved
  version/effective-date fields;
- `functions/scripts/validate-r5-hosting-bundle.js` passed with zero errors and confirmed
  that `public/` is the excluded policy-draft directory while `hosting/` remains the dedicated
  entry bundle;
- no private support forwarding address appears in the policy drafts or decision evidence;
  and
- direct visual rendering of the local `file:` pages was blocked by the browser URL safety
  policy, so visual layout QA remains pending and is not claimed as passing evidence.

## LP-10 read-only production verification — 2026-08-26

The owner instructed the gated workflow to continue. The resulting inspection was read-only:
no button that enables, links, creates, edits, reveals, or saves a service or credential was
used.

- Firebase Project settings → Integrations showed **Enable Google Analytics**, **Link
  BigQuery**, and **Link Cloud Logging**. Therefore those integrations are not enabled or
  linked. AdMob, Google Ads, and Display & Video 360 all require the currently disabled
  Analytics integration.
- Google Cloud Log Router contained only the system-created `_Default` and `_Required` sinks,
  both routed to internal Google Cloud Logging buckets. There was no custom Storage,
  BigQuery, Pub/Sub, or other external sink and no linked BigQuery dataset.
- Logs Storage showed `_Default` at 30 days and `_Required` at 400 days. The former contains
  application/service logs; the latter contains required administrative/system audit logs.
- Firebase Data privacy showed **Let Google use your Firebase Service Data** enabled. The same
  screen and Firebase's official definition state that Firebase Service Data excludes
  Customer Data, but Firebase also says Service Data is personal information and can include
  operational details such as IP addresses. Because the enabled option authorizes analysis,
  recommendations, and improvement of non-Firebase Google services, this review does not
  assume it is irrelevant to Play. Disabling it would be a separate privacy-minimal account
  mutation requiring explicit approval; otherwise an authorized legal/privacy reviewer must
  determine the Play effect.
- Google Maps Platform showed every Maps API, including Maps SDK for Android, with an
  **Enable** action, so none is enabled. Two generic Firebase-created API keys exist, but
  there is no dedicated Maps key. No key value or full credential identifier was viewed or
  recorded. The exact uploaded candidate independently has `MAPS_CONFIGURED=false`.
- Firebase Data privacy showed no privacy representative assigned, consistent with the open
  LP-1 legal-operator/authorized-approver gate.

Assessment: Analytics, optional customer-data exports, Ads, and Maps do not currently broaden
the recipient posture. At the time of this read-only review, LP-10 could not close while the
optional Firebase Service Data use remained enabled without an authorized legal/privacy
determination. The owner later approved the privacy-minimal disable action, recorded below.
Contract/account confirmation and another exact SDK/configuration review after any Maps
key/API or other provider is enabled are still required before saving Play answers.

### Owner-approved Firebase Service Data mutation — 2026-08-26

The owner explicitly instructed: `Approve disabling Firebase Service Data non-Firebase use
in kithe-production`.

- Immediately before the action, project `kithe-production` showed **Let Google use your
  Firebase Service Data** checked.
- Only that checkbox was cleared. Firebase displayed **Success: Service data sharing
  disabled**.
- The Data privacy page was reloaded. Its DOM showed the checkbox without a checked state, and
  direct read-only inspection returned `checked: false`.
- No Analytics, BigQuery, Cloud Logging, Maps, privacy-representative, credential, Play, or
  other project setting was changed. No credential value was revealed or recorded.

Current assessment: the optional non-Firebase Service Data reuse blocker is closed. The
privacy-minimal production posture technically supports keeping the Play No-sharing answer
provisional. LP-10 still requires authorized legal/privacy and contract/account confirmation
and must be repeated against the exact release after Maps or another provider/SDK is enabled.

Official references:

- [Firebase Privacy and Security](https://firebase.google.com/support/privacy)
- [Cloud Logging system buckets and retention](https://docs.cloud.google.com/logging/docs/store-log-entries)
- [Google Play Data safety service-provider exception](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)

## Recorded requesting-owner direction

After being presented with option 1, the requesting owner replied `approve` on 2026-08-26.
The recorded scope is exactly:

- keep LP-1 deferred;
- approve the recommended local direction for LP-2 through LP-8 and LP-11;
- approve LP-10 only as a conditional, provisional Play posture;
- keep LP-9 open until named primary and backup support coverage is supplied;
- keep LP-12 and publication closed; and
- authorize no external mutation.

This instruction does not establish that both owners or a qualified/authorized legal,
privacy, Trust & Safety, operations, or product approver has signed the decisions assigned to
those roles.

Subsequent coverage instruction on 2026-08-26: Micah is primary and Kerise is backup for
monitoring the public support inbox. The later LP-1 decision clarified that Micah is the sole
legal/equity owner and Kerise's role is operational only. The private forwarding destination was not
recorded in this repository. This coverage instruction alone does not approve LP-9's response
times.

Subsequent LP-9 approval on 2026-08-26: the requesting owner explicitly instructed `Approve
LP-9`, accepting the five-business-day acknowledgment and 30-calendar-day verified-deletion
completion targets with the recorded coverage roles and disclosed-legal-exception condition.
This remains a local policy decision and does not approve exact public copy or publication.

Subsequent LP-1 instruction on 2026-08-26: the requesting owner identified **Robert Micah Lee
Peralta** as the current operator, selected **individual / sole proprietor** as the legal form,
confirmed **Kithe** as the brand, and confirmed there is no separate LLC, corporation, or
partnership. This supersedes the earlier two-owner legal/equity assumption. Kerise remains
the LP-9 operational backup only. The instruction does not constitute independent legal or
privacy review and does not authorize publication or any external account change.

Subsequent read-only identity check on 2026-08-26: Play **About you** shows a Personal account,
developer name Kithe, and linked-Payments legal name **Robert Peralta**. Because that does not
exactly match the supplied LP-1 operator name, publication remains blocked until the operator
chooses and verifies one exact legal-name presentation. No Play field was edited or saved.

Authoritative-name instruction on 2026-08-26: the requesting owner confirmed **Robert Micah
Lee Peralta** as the exact government-identity name. The shorter Payments-profile presentation
is not the selected public/operator form. No identity-document detail is stored here. Updating
or reverifying Google Payments remains a separate sensitive-data action requiring action-time
confirmation.

Google Payments correction attempt on 2026-08-26: the requesting owner gave the required
action-time approval to transmit and submit the full legal name, then completed private
passkey verification. The self-service **Change name** route did not present a text-only
correction form; it required a document evidencing a legally documented name change and
listed marriage certificate, divorce decree, adoption decree, or court order, with possible
address-document follow-up. The workflow stopped before **Start name change**. No name,
document, address, or form was uploaded or submitted. Resolve the original-name omission
through Google Payments/Play support or an owner-controlled accepted-document process.

## Exact owner/legal response template

The authorized decision-makers may use the following only after filling every bracketed
field. Approval of this block updates local policy decisions only; it does not authorize
publication, Play submission, Maps setup, backend deployment, Remote Config, reviewer data,
or a release.

> Legal operator decision for LP-1: Robert Micah Lee Peralta, individual / sole proprietor,
> operating the Kithe brand with no separate entity. Authorized legal/privacy approver:
> [NAME/ROLE/DATE]. Approve LP-2 Hawaiʻi/United States with
> [approved address posture]; LP-3 adults 18+; LP-4 United States only; LP-5 180 days after
> final safety-case closure with logged exceptions; LP-6 no scheduled Firestore backup or
> PITR and provider deletion-completion of up to 180 days unless law requires storage, not a
> restoration promise; LP-7 180-day deidentified audit after Experience end with documented
> holds; LP-8 30-day pseudonymous deletion receipt; LP-9 five-business-day acknowledgment
> and 30-calendar-day verified deletion, with Micah as primary and Kerise as operational
> backup; LP-10 provisional Play No data sharing subject to the service-provider
> conditions and a post-Maps exact-release recheck; and LP-11 aggregate organizer visibility
> only.
> Keep LP-12 open until exact-copy approval and a separately authorized publication bundle.
> This decision does not authorize any external mutation.

Required approval record:

- requesting-owner direction/date: **approved as scoped above, 2026-08-26; name/role not
  separately provided in this approval**;
- second equity owner: **not applicable under the recorded individual-ownership decision;
  Kerise remains operational backup only**;
- authorized legal/privacy approver name/role/date: **not provided**;
- LP-9 support roles and response-time decision: **approved 2026-08-26; Micah (individual
  operator/primary), Kerise (operational backup), five-business-day acknowledgment, 30-calendar-day
  verified deletion unless a disclosed legal exception applies**; and
- exact LP-1 legal operator details: **Robert Micah Lee Peralta; individual / sole
  proprietor; brand Kithe; no separate entity; recorded 2026-08-26**.

## Official references used for this review

- [Google Play Data safety and service-provider exception](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Google Cloud Data Processing Addendum](https://cloud.google.com/terms/data-processing-addendum)
- [Firebase Data Processing and Security Terms](https://firebase.google.com/terms/data-processing-terms)
- [Firebase Terms of Service](https://firebase.google.com/terms)
- [Cloudflare U.S. privacy compliance](https://www.cloudflare.com/trust-hub/us-privacy-compliance/)
- [Cloudflare Customer DPA](https://www.cloudflare.com/en-gb/cloudflare-customer-dpa/)
- [Cloud Firestore backups](https://docs.cloud.google.com/firestore/native/docs/backups)
- [Cloud Firestore PITR/database fields](https://docs.cloud.google.com/firestore/docs/reference/rest/v1/projects.databases)

## Gate

Stop here. The requesting-owner decisions listed above are recorded locally, but authorized
legal/privacy review, the missing role-specific signoffs, exact-copy approval, and LP-12
publication evidence remain open. Do not treat these
decisions as approval of exact policy copy; resolve draft placeholders; publish policy
routes; save Play setup answers; enable Maps; deploy legal functions or a policy manifest;
initialize Remote Config; create reviewer data; or begin A5d without the corresponding later
approval.
