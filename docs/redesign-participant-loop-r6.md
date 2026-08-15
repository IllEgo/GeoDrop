# Kithe R6 — Participant Loop

Status: **Local/device participant experience owner-approved; outdoor/server qualification gate open**  
Date: 2026-08-11  
Production state: **no deployment, backend enablement, Remote Config change, Play
publication, production-data mutation, or M4 cutover was performed**

## Authorization and scope

The owner explicitly authorized R6 local implementation on 2026-08-10 after approving the
R5-L/R5-P split. R6 covers the participant discovery, unlock, Collection, Trail, safety,
accessibility, and outdoor-device gate in `redesign-alignment-proposal.md`. R5-P and R7+
remain separate gates.

On 2026-08-11, after reviewing the updated physical-device experience with working map
tiles and relocated demo drops, the owner stated that R6 looks good and approved it. This
records acceptance of the implemented local/device participant experience. It does not
convert unrun qualification checks into passing evidence or authorize deployment, backend
enablement, production configuration/data changes, Play publication, M4, or R7.

## R6.0 — crash stability and observability

Firebase recorded two foreground Android 16 emulator crashes in Compose pointer hit-testing
with `Offset is unspecified`. The participant list panel supplied
`AnchoredDraggableState.offset` directly to `graphicsLayer.translationX`. That offset is
`Float.NaN` before anchors are installed, creating a non-finite layer transform that can
later fail during touch hit-testing.

Implemented locally:

- added `finitePanelTranslation`, which preserves a real drag offset and otherwise uses the
  finite collapsed-panel position (or zero as a final defensive fallback);
- prevented NaN/infinite values from reaching the participant panel graphics layer;
- added focused regression coverage for NaN, infinity, and normal drag offsets; and
- added privacy-safe Crashlytics keys for participant destination, active-Experience
  presence, unlock-in-progress state, and precise-permission state. No Experience code,
  location, payload, or account identifier is recorded by these keys.

Verification:

- `compileDebugKotlin`: passing;
- `R6ParticipantPanelGeometryTest`: 3 passing, 0 failed; and
- no broad Compose dependency upgrade was included because the concrete app-side
  non-finite transform was isolated and fixed directly.

## R6.1 — local participant-loop implementation

Implemented behind a fail-closed target-backend contract gate:

- Android discovery reads only `experienceDrops` metadata and does not retain title, body,
  photo metadata, reward instructions, or reward codes before unlock;
- explicit unlock uses the `unlockDrop` callable with a one-shot precise fix and never
  falls back to the legacy client-authoritative collect path while the target gate is on;
- stable server failure reasons map to actionable participant copy without exposing exact
  coordinates or distance;
- Nearby provides equally reachable map/list controls plus locked, near, found, expired,
  and Trail-locked text/icon states;
- successful unlock immediately inserts the immutable receipt into Collection, reveals
  text/photo alt copy, displays an issued reward code, and provides the next Trail step;
- Collection combines receipts across Experiences and displays Experience/date, reward
  status/code, Trail status, and report access;
- report and block-host actions use the R2 callables; and
- reduced-motion mode replaces participant tab slides with a short crossfade.

The rollout switch is `pilot_redesign_backend_enabled` plus minimum contract version 1.
Its default remains off/fail-closed. Supplying an injected test gateway activates only the
local test path and does not change Firebase or production configuration.

## Verification on 2026-08-10

- `compileDebugKotlin`: passing;
- complete `testDebugUnitTest`, `lintDebug`, and `assembleDebug`: passing;
- `compileDebugAndroidTestKotlin`: passing;
- two focused `R6ParticipantDeviceTest` checks passed on physical Android 16 hardware
  (`SM-S938U`), covering the list/select/unlock path and combined
  Collection/reward/report path;
- the verified debug APK was reinstalled after instrumentation and cold-launched normally
  in 668 ms; and
- the launch log contained no fatal exception and no `Offset is unspecified` crash.

## Debug-device fixture follow-up on 2026-08-11

`DEMO2026` now has a `BuildConfig.DEBUG`-only local `R6ParticipantGateway`. This prevents
the undeployed target server and current Firestore rules from making the local demo appear
empty for guests or emit `PERMISSION_DENIED` for signed-in explorers. The physical-device
check confirmed the Google Map, trail, four List discoveries, and a seeded Collection
receipt. The release/internal gateway and all Firebase configuration remain unchanged.

This fixture improves local UI/UX review but does not close the real-server, security-rule,
Remote Config, private-media, or outdoor-location portions of the R6 gate.

The same follow-up initially exposed only a blank Google map surface: the widget and
controls mounted, but the generated `google_maps_key` value was empty. Read-only Cloud
Console inspection confirmed that the existing Firebase-generated Android key already has
Maps SDK for Android enabled and is restricted to `com.e3hi.geodrop` with the connected
debug certificate fingerprint. The Gradle default now aliases `google_maps_key` to that
generated `google_api_key` unless a build supplies a dedicated `GOOGLE_MAPS_API_KEY`.
After rebuilding and reinstalling, physical-device visual inspection confirmed real Hilo
map tiles and all four local demo pins; no Maps authorization error remained. No cloud key,
billing, API, or restriction setting was changed.

## Remaining R6 gate work and blockers

1. The R2 target server has not been deployed and the target Remote Config gate remains
   off by design, so a real end-to-end server unlock cannot yet be claimed.
2. The physical-device launch reported Firebase Installations HTTP 403
   `API_KEY_SERVICE_BLOCKED`. This prevents a reliable installation token and therefore
   blocks Remote Config refresh and messaging-token behavior. Correcting API-key/service
   restrictions is an external production-configuration change and was not performed.
3. Private photo rendering still needs the authorized `getCollectionMedia` URL flow plus
   app-private offline Collection caching; current local UI presents the immutable photo
   alt text but not the image bytes.
4. Manual TalkBack, 200% font, compact-screen, and outdoor GPS/drift checks remain, as do
   physical recovery checks for every server unlock failure and confirmation that no
   precise fix is retained.

The R6 production/outdoor qualification gate remains open until these checks pass. The
owner's local/device acceptance does not authorize or claim pilot readiness, and R5-P
remains separately blocking before pilot or public release.
