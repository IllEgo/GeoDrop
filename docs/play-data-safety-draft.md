# Kithe Google Play Data safety draft

Status: **evidence-linked working draft; privacy-minimal cleanup verified locally; do not submit**  
Prepared: 2026-08-14  
Artifact assumed: Android Pilot 1 release, package `com.kitheapp`

This is an engineering inventory, not approved legal policy. The final Play answers must
match the exact App Bundle, enabled Firebase/Google services, production backend, public
privacy policy, retention schedule, and vendor terms at submission time.

## Recommended global answers

| Play question | Draft answer | Confidence / condition |
| --- | --- | --- |
| Does the app collect or share required user data types? | **Yes, collects** | Confirmed by Auth, Functions, Firestore, Storage, Crashlytics, FCM, Maps, and App Check flows |
| Is user data encrypted in transit? | **Yes** | Firebase and Google SDK traffic uses TLS; recheck any organizer web form and custom domain before submission |
| Can users request deletion? | **Yes, after blocker closes** | In-app permanent deletion exists; Play also requires a working external URL |
| Is data shared with third parties? | **Provisional No** | Only if Google/Firebase/Cloudflare are confirmed as service providers for these flows and no optional Ads/Signals/vendor reuse is enabled |
| Independent security review? | **No** | No qualifying independent assessment is recorded |
| Ads or data sale? | **No** | Product direction prohibits display ads and location-data sale |

Do not select **No data collected**. Off-device transmission through an SDK counts as
collection even when Kithe does not persist the value itself.

## Data-type worksheet

`Required/optional` below describes the final form recommendation. A conditional note is
included where a feature can be declined but a narrower Kithe experience remains.

| Play category / type | Collected | Shared | Required or optional | Ephemeral | Purpose | Evidence and notes |
| --- | --- | --- | --- | --- | --- | --- |
| Location / Approximate location | Yes | Provisional No | Optional | No | App functionality | Coarse permission supports Nearby. Maps collects IP/request and map-interaction metadata. Firebase Analytics has been removed locally. No background permission exists. |
| Location / Precise location | Yes | Provisional No | Optional; required only to unlock or place at current position | No at the data-type level | App functionality; fraud/security for the proximity check | `R6ParticipantGateway` sends one fix with accuracy/age to `unlockDrop`. The function checks it and stores no submitted coordinates. Organizer-selected drop coordinates are stored, so the type cannot be called wholly ephemeral. |
| Personal info / Name | Yes | Provisional No | Optional | No | App functionality; account management | Firebase/Firestore stores display name or username. Organizer application also collects a contact name. |
| Personal info / Email address | Yes | Provisional No | Optional overall; required for email/Google account paths | No | Account management; fraud/security | Firebase Authentication collects email for email/password and Google sign-in. Organizer application collects contact email. Guest preview does not require it. |
| Personal info / User IDs | Yes | Provisional No | Required | No | App functionality; account management; fraud/security; analytics | Firebase creates a UID for anonymous and identified accounts. Firestore/Functions attach it to private state; the custom analytics ledger stores an HMAC-protected actor key. |
| Personal info / Other info | Yes | Provisional No | Optional | No | App functionality; account management | Business/organization name, categories, organizer description, and host label. Do not claim that Kithe collects a postal address or phone number unless the production form changes. |
| Photos and videos / Photos | Yes | Provisional No | Optional | No | App functionality | Approved organizers may upload a photo drop to Cloud Storage. Pilot 1 does not upload video. |
| Audio files / Voice or sound recordings | **No for target Pilot 1** | No | Not applicable | Not applicable | Not applicable | Audio is deferred by product direction. The microphone permission, recorder component, audio authoring option, and audio templates have been removed or blocked locally. Reconfirm the exact release manifest before submission. |
| App activity / App interactions | Yes | Provisional No | Required | No | App functionality; analytics; fraud/security | Joins, unlocks, receipts, Trails, rewards, report/block actions, notification interactions, map interactions, and the 180-day pseudonymous event ledger. Firebase Analytics automatic lifecycle/session collection has been removed locally. |
| App activity / Other user-generated content | Yes | Provisional No | Optional | No | App functionality; developer communications; fraud/security | Experience/drop text, photo captions and alt text, organizer application description, reports, and feedback narratives. There is no direct messaging or public anonymous creation. |
| App info and performance / Crash logs | Yes | Provisional No | Required unless an opt-out/config change is made | No | Analytics; app functionality | Crashlytics automatically collects stack traces and relevant application state. Kithe also records selected non-fatal exceptions. |
| App info and performance / Diagnostics | Yes | Provisional No | Required unless SDK configuration changes | No | Analytics; app functionality; fraud/security | Crashlytics device metadata, Firebase Sessions, Maps crash metrics, network/app metadata, and App Check/Play Integrity diagnostics. |
| App info and performance / Other app performance data | Yes | Provisional No | Required unless SDK configuration changes | No | Analytics; app functionality | Firebase Sessions quality/session metrics and Maps SDK abnormal-termination/usage metadata. |
| Device or other IDs / Device or other IDs | Yes | Provisional No | Required unless SDK configuration changes | No | App functionality; analytics; fraud/security | Firebase installation ID, Crashlytics installation UUID, FCM token, Maps SDK identifier, App Check integrity token, and Install Referrer/session identifier. The Firebase Analytics app-instance and Advertising IDs have been removed locally. |

## Types not collected in the Pilot 1 target

- financial or payment information;
- health and fitness information;
- contacts;
- messages between users;
- videos;
- arbitrary files or documents;
- calendar data;
- web browsing history;
- installed-app inventory;
- background or continuous location history.

Reward codes are server-issued Experience content, not consumer payment information. If a
future release adds purchases, merchant accounts, scanners, subscriptions, audio, video,
or a different integrity verdict, this inventory must be repeated.

## Location handling evidence

The target participant path matches the approved privacy design:

1. Approximate location is requested contextually for Nearby, not at app launch.
2. Precise location is requested after the user selects Unlock.
3. Android sends `lat`, `lng`, `accuracyM`, and `capturedAt` to the protected
   `unlockDrop` callable.
4. The callable validates age/accuracy, calculates distance in memory, and stores a
   receipt with drop/payload identifiers and time. It does **not** store the submitted
   participant coordinates.
5. A failed distance event stores only a coarse distance bucket, not coordinates.

The Play row remains **not ephemeral** because approved organizers can choose a current
position for a drop and Kithe stores that selected precise drop coordinate as content.
The public policy should distinguish participant check coordinates from authored drop
locations.

## SDK and manifest audit

| Component | Automatic or configured data relevant to Play | Release action |
| --- | --- | --- |
| Firebase Authentication | IP address, app/user-agent metadata, Firebase app ID; UID; display name/email depending on provider | Keep and disclose |
| Cloud Firestore / Functions / Storage | UID on authenticated requests; function name, caller IP, FCM token; all developer-defined profile, UGC, receipt, report, and media data | Keep and disclose; verify production rules and retention |
| Firebase Cloud Messaging / Installations | App version, Firebase user agent, installation ID; notification interactions would be added if Analytics were included | Keep; ensure alerts remain joined-Experience-only and honor opt-out |
| Firebase Crashlytics / Sessions | Stack traces, app state, device metadata, installation UUID, session/quality metrics, custom keys and non-fatal exceptions | Keep for Pilot reliability; verify no raw location, email, code, token, or UGC enters custom keys/logs |
| Firebase App Check with Play Integrity | Firebase user agent and integrity token; app/device/license metadata used for abuse prevention | Keep; disclose fraud/security purpose |
| Firebase Remote Config | App/OS/SDK metadata and configuration values | Keep fail-closed flags; prohibit Analytics targeting and personalization for Pilot 1 |
| Google Maps SDK | Device/SDK/request metadata, stack traces/crash metrics, IP address, Maps SDK identifier, and map interactions such as pan/zoom | Keep and disclose app interactions, diagnostics, approximate location, and device ID conservatively |
| Play Services Location | Foreground coarse/precise device position requested by Kithe | Keep contextual permissions; no background location |
| Play Install Referrer | Referrer payload and click/install timing read from Play | Keep for R5-P continuity; referrer may contain only Experience code, random entry-session ID, and channel |
| Firebase Analytics | App-instance ID, Advertising ID, masked-IP coarse location, lifecycle/session events, and developer events | **Removed locally for Pilot 1; reconfirm the exact release dependency graph** |

## Local verification: Analytics and advertising IDs removed

The audit found an explicit `firebase-analytics-ktx` dependency and five legacy
`Firebase.analytics.logEvent` calls. The previous merged manifest contained:

- `com.google.android.gms.permission.AD_ID`;
- `android.permission.ACCESS_ADSERVICES_ATTRIBUTION`;
- `android.permission.ACCESS_ADSERVICES_AD_ID`.

Manifest-merger evidence attributed those permissions to Google measurement libraries.
This drift duplicated Kithe's purpose-built pseudonymous event ledger and did not match
the intended privacy-minimal production setup.

The local Pilot 1 cleanup now:

- removes the Analytics dependency and five legacy log calls;
- keeps the approved custom event ledger, its field allowlist, HMAC actor keys, and
  180-day expiry;
- retains Crashlytics crash reports and deliberate non-fatal reporting without Analytics
  breadcrumb events; and
- produces a regenerated debug merged manifest with no `AD_ID` or AdServices permission.

The clean Android gate passed on 2026-08-14: 127 unit tests, lint, and debug APK assembly.
The exact signed release App Bundle must still be inspected before the Play form is final.

Restoring Firebase Analytics later would reopen Advertising ID, derived coarse location,
app-instance ID, automatic lifecycle/session events, consent, retention, and Play form
review. It requires a separate product/privacy decision.

## Local verification: audio authoring and microphone removed

Pilot 1 permits text and photo drops; audio is deferred. The local cleanup:

- removes `RECORD_AUDIO` from the manifest;
- removes `AudioRecorderActivity` from registered Android components;
- removes the recorder launch/permission path;
- removes Audio from the legacy authoring selector and filters audio templates; and
- keeps defensive read-only decoding/rendering for legacy data without exposing audio
  authoring.

The regenerated debug merged manifest contains neither `RECORD_AUDIO` nor the recorder
activity. The source file remains unregistered and unreachable so a later audio decision
does not silently become Pilot 1 functionality. Reconfirm the exact release App Bundle.

Keeping audio would require an explicit product-direction reversal, a user-facing
permission rationale, transcript/accessibility requirements, Data safety disclosure, and
new test coverage. It is not a listing-only choice.

## Account deletion and retention blockers

Kithe has an in-app permanent deletion path, but Play requires both in-app deletion and a
functional external web resource. The external page must prominently reference Kithe,
explain how to request deletion, and disclose retained exceptions. It must not merely
freeze the account.

Before submission:

- approve and publish `https://kitheapp.com/privacy`;
- approve and publish `https://kitheapp.com/account-deletion`;
- keep both links reachable without installing or signing into the app;
- approve the 30-day deletion-receipt exception, report pseudonymization/retention, and
  any legal/fraud retention;
- confirm account export objects delete after 24 hours and signed URLs expire after 15
  minutes;
- run a production-safe export/deletion rehearsal and verify Auth, Firestore, Storage,
  usernames, authored drops/media, receipts, and notification tokens are handled as the
  public policy promises.

Publishing draft legal text is not authorized by preparation of this package.

## Pre-submission verification

1. Generate the exact signed release App Bundle configuration.
2. Export its merged manifest and dependency graph.
3. Confirm that Firebase Analytics, advertising-ID permissions, the microphone permission,
   and the recorder component remain absent; confirm feature flags and Firebase project
   identifiers.
4. Re-audit every SDK against the current Play SDK Index and official disclosure page.
5. Verify that Firebase Analytics and Ads/Signals linking remain disabled; verify
   Crashlytics, Remote Config, FCM, App Check, Maps, and retention settings in
   `kithe-production`.
6. Compare the public privacy/deletion pages with the worksheet row by row.
7. Exercise guest preview, email/Google account creation, precise unlock, photo authoring,
   reporting, feedback, notifications, export, and deletion on the candidate build.
8. Have the owner and legal/privacy reviewer approve the final answers.
9. Copy answers into Play Console, review the generated summary, and stop for an explicit
   submission approval.

## Official references

- [Google Play Data safety form](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en-EN)
- [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Google Play account-deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en)
- [Firebase Android data disclosure](https://firebase.google.com/docs/android/play-data-disclosure)
- [Google Analytics Firebase SDK disclosure](https://support.google.com/analytics/answer/11582702?hl=en)
- [Google Analytics default collection](https://support.google.com/analytics/answer/11593727?hl=en)
- [Maps SDK for Android disclosure](https://developers.google.com/maps/documentation/android-sdk/play-data-disclosure)
- [Play Integrity terms and data safety](https://developer.android.com/google/play/integrity/terms)
