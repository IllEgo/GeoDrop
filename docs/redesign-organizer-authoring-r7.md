# Kithe R7 — Organizer Access and Core Authoring

Status: **R7 local/device gate approved; pre-pilot dependencies remain open**  
Date: 2026-08-11  
Scope: Android local/device implementation only. No backend deployment, Remote Config
change, application-form publication, production data, Play release, or M4 cutover is
authorized by this work.

## Authorization and governing decisions

After approving the R6 local/device experience, the owner directed the redesign to
continue. R7 local implementation therefore began under the signed R0 decisions:

- only an existing account that is both internal role `BUSINESS` and workflow status
  `APPROVED` receives target organizer capability;
- `PENDING` and `DENIED` are server-authored workflow states, not a third account role;
- Experience and drop mutations are online/authoritative and fail clearly instead of
  entering an offline write queue;
- text and one photo are the R7 content types; reward and Trail authoring belong to R8;
- scheduled publishing remains deferred; and
- drafts remain conditional on the timed 8–20-drop venue walkthrough demonstrating that
  interruptions make them necessary.

The legacy Organizer surface remains reachable only when the target R7 backend flag is
off. The R7 surface replaces it when the target boundary or debug fixture is active. This
preserves the staged migration instead of changing release behavior before rollout.

## Implemented locally

### Account approval gate

- Account now renders distinct `NOT_APPLIED`, `PENDING`, `APPROVED`, and `DENIED` states.
- Guests are told to sign in before requesting access.
- The not-applied explanation hands off through the target
  `createOrganizerApplicationLink` callable; no Organizer capability exists before
  approval.
- Pending shows a submission timestamp when available, makes email the expected decision
  channel, and has no fake progress or countdown.
- Denied has no automated reapply action, matching the unresolved policy decision.
- The approved action remains nested under Account and opens Experiences; it does not add
  another account type or participant tab.

### Experiences

- Approved organizers have an Experiences list with an honest first-use empty state.
- Create and edit support name, short description, start/end date and time, editable venue
  timezone, default unlock distance, and cancellation state.
- Device timezone is the default. Picker conversion and display use the entered venue
  timezone.
- Experience detail presents the human event code as selectable text, plus Copy and Share.
- The app states explicitly that QR sharing is unavailable while the app-owned HTTPS host
  is deferred. It does not draw a decorative or non-resolving QR.

### Text/photo drop authoring

- Placement supports a tappable/draggable map pin, manual latitude/longitude, and the
  screen-reader-accessible **Use my location** alternative.
- Unlock distance defaults to 25 m and is adjustable from 15–100 m in 5 m steps, with an
  overlap warning.
- The same editor creates and edits text/photo drops with title, message/caption, camera,
  photo chooser, automatic app-side compression, required photo alt text, no expiry,
  Experience-end expiry, or custom expiry.
- Chosen photos apply their EXIF rotation and mirror metadata before preview/compression.
  Portrait previews use a taller, uncropped `Fit` presentation instead of forcing every
  image into a cropped landscape frame.
- A newly created Experience now starts immediately by default, with a four-hour default
  duration. An organizer who deliberately selects a future start sees an explicit notice
  that guests may join but published drops remain hidden until that time.
- Save validates locally, calls the target authoritative boundary, keeps fields in place
  on failure, disables double submission, and reports errors honestly.
- Delete is confirmed and explains that existing unlock receipts keep their immutable
  snapshot.
- No scheduled publishing, audio/video, reward, Trail, templates, bulk authoring, or
  offline write queue was added.

### Debug-only review fixture

`DEMO2026` receives an in-memory R7 gateway only in a debug APK. It exposes a visibly
labelled **Local demo organizer** and never calls Firebase for its create/edit/delete
operations. The debug R5, R6, and R7 boundaries share that same in-memory source, so an
Experience created in organizer tools can be joined through the participant entry flow
and its safe text drops can appear in discovery without falling through to Firebase.
New fixture Experiences use the production eight-character, ambiguity-free alphabet and
a shared issued-code guard rather than restarting from `LOCAL002` with every new store.
The local fixture simulates successful photo moderation so its organizer-to-participant
review can complete without a moderation service; the debug banner names that simulation
and states that local content does not sync to another device. Production photos continue
to require the governed moderation operation before discovery.
The fixture is injected only while `DEMO2026` is active, so another debug Experience does
not accidentally grant an Explorer the approved surface.

## Verification evidence

- `compileDebugKotlin`: passed.
- `testDebugUnitTest`: 29 suites, 107 tests, 0 failures.
- `lintDebug`: passed.
- `assembleDebug`: passed.
- Added policy coverage for date ordering, radius limits, text/photo requirements, and a
  full debug-gateway create/drop/delete cycle.
- Added a cross-boundary regression that creates through R7, joins through R5, reads the
  published drop through R6, and asserts that the Firebase delegate was not called.
- The in-app **Join another Experience** action now uses the R5 entry boundary instead of
  the legacy authenticated-group repository call, so guest entry and locally authored
  debug Experiences follow the same contract as code entry.
- APK installed successfully on Samsung SM-S938U, serial `R5CY114LNCE`.
- Physical-device smoke path passed:
  Account → Organizer tools → Experiences → `DEMO2026` detail → Add a drop.
- On the device, the Experience code/copy/share state, HTTPS/QR deferral, seeded drop list,
  working map tiles, venue pin, manual coordinates, current-location action, radius
  controls, content fields, expiry choices, and publish action all rendered.
- A valid text drop was published through the local gateway on the device and immediately
  appeared in the Experience drop list.
- No `AndroidRuntime` fatal event was recorded during the walkthrough. Crashlytics logged
  only a settings-request network failure while the device reported no connection.
- The create-to-join unauthenticated regression build was installed successfully on the
  same device. The physical path created `R7JoinRegression` with code `LOCAL002`, joined
  it through **Join another Experience**, and switched Nearby to `Experience LOCAL002`
  without an authentication error.
- No app fatal or entry-boundary authentication failure was logged during that path.
  Firebase Installations separately returned HTTP 403 `API_KEY_SERVICE_BLOCKED`; that
  existing API-key restriction did not block local Experience entry, but installation
  tokens and dependent Firebase services still require configuration before production.
- The duplicate-code/photo-orientation regression build was installed on the same device.
  Two consecutive Experiences received distinct codes `AELVK7JL` and `M9A5BN7Z`. A photo
  selected from the device picker rendered upright, uncropped, and in the portrait-height
  preview; the verification drop was neither saved nor uploaded.
- Added organizer-to-participant regression coverage for both text and photo discoveries,
  scheduled-Experience hiding, and immediate default activation. Joining from inside the
  participant shell now also persists the selected code as the active Experience.
- The regression build was installed on Samsung `SM-S938U`. The physical path created
  active Experience `DiscoveryBridge` (`5P5CKSME`), published `ExplorerVisibleDrop` as
  `SAFE`, joined the code as an Explorer, and the owner confirmed that the drop appeared.
  No app fatal was logged during the walkthrough.

## R7 gate result and deferred evidence

On 2026-08-11 the owner approved the R7 local/device implementation after the physical
organizer → Explorer discovery regression passed. This approval closes the R7
implementation gate; it does not assert that the following timed/authorization evidence
was performed during the approval session. These items remain required before pilot
readiness and are explicitly carried forward rather than recorded as passed:

1. Start from the approved Organizer Experiences list.
2. Create one Experience and one valid text or photo drop in under three minutes.
3. Confirm a not-applied/pending/denied account cannot create either through UI or server.
4. Walk the real venue and time authoring 8–20 drops. Record total time, per-drop range,
   interruptions, GPS/map friction, and photo-upload failures.
5. Add basic drafts only if that measured walkthrough triggers the conditional need.

The owner accepted the current Pilot 1 behavior of requiring a replacement image when an
existing photo drop is edited. Reusing an organizer-owned prior asset remains an optional
future server-contract improvement, not an R7 blocker.

The following items require a later explicit decision or already-deferred dependency:

- **QR/print artifact:** R5-P must configure an owned HTTPS host, generate a real App Link
  QR/share image, and qualify clean install through Play. Event-code-only is acceptable for
  this local R7 review, not for pilot entry acceptance.
- **Application form:** Legal wording, contact path, reapplication policy, and the real
  `ORGANIZER_APPLICATION_URL` must close before production enablement.
- **Existing photo edits:** the approved R2 callable currently requires a new staging
  upload for every photo payload version. R7 retains replacement-only photo edits for
  Pilot 1; securely reusing an organizer-owned prior asset remains a later option.
- **Organizer drop list scaling:** the current target contract has detail-per-drop reads
  because discovery records intentionally omit payload titles. This is acceptable at the
  8–20-drop pilot size; a private owner-list callable is the appropriate follow-up if
  measured latency becomes material.

R5-P, real server/outdoor R6 qualification, production deployment, and production data
remain separate open gates.

### Cross-device boundary

The debug organizer fixture is intentionally process-local. It can prove the complete
organizer → join → participant-discovery contract on one running device, but an Explorer
on a second device cannot receive an Experience or drop created there. Cross-device proof
requires deploying the approved R1/R2 Experience/drop callables and rules to an authorized
Firebase environment, enabling the redesign backend for allowlisted testers, and operating
photo moderation. That integration is required by the product direction, but is not
implicitly authorized by the R7 local/device approval. R8 and production actions require
separate authorization.
