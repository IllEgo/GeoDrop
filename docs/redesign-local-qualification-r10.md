# Kithe R10-L — Local Android qualification

Status: **R10-L approved and closed; R10-P remains unauthorized**
Date: 2026-08-14
Scope: R10-L only. R10-P and all production actions remain unauthorized.

## Approval record

On 2026-08-13 the owner approved splitting R10 into R10-L and R10-P and authorized R10-L
only. The split allows local Android UI/UX and accessibility qualification to continue while
the owner intentionally postpones paid services and the HTTPS/App Link host. It does not
change the product boundary or Pilot 1 acceptance criteria.

On 2026-08-14 the owner reviewed the completed automated, accessibility, and physical-device
evidence and explicitly approved R10-L. This closes only the local Android qualification
gate; it does not authorize R10-P or satisfy any open pre-pilot dependency.

## Authorized R10-L work

- Audit the current Android client and tests against the accepted accessibility and UX
  specifications.
- Exercise 200% font scale, TalkBack reachability, compact-screen behavior, denied-permission
  recovery, list-as-map-equivalent behavior, and debug/local poor-network and GPS states.
- Add or correct local Android UI, accessibility semantics, copy, tests, and qualification
  documentation where evidence exposes a defect.
- Run Android unit, lint, assembly, and relevant instrumentation checks.
- Install debug review builds on the approved physical test device and collect sanitized
  evidence without storing account identifiers, credentials, precise locations, or tokens.

## R10-P work that remains prohibited

- Linking billing or enabling paid Maps/Firebase capabilities.
- Deploying Functions, rules, indexes, Hosting, Storage, Remote Config, App Check enforcement,
  Analytics, production data, or production-safe fixtures.
- Creating or publishing a Play listing, obtaining release signing evidence, configuring
  HTTPS/App Links, or running the clean-install external matrix.
- Claiming real-server, private-media, outdoor/cross-device, operational, release, or Pilot 1
  qualification.

## Baseline checklist

- [x] Map/list, entry, account, Collection, unlock, and safety surfaces remain usable at 200%
  font scale without losing a primary action.
- [x] Participant controls and outcomes expose meaningful TalkBack names, roles, states, and
  announcements; Report remains reachable within two actions.
- [x] Compact-screen and system-inset checks keep required actions reachable.
- [x] Denied location/notification states provide a clear recovery action without loops.
- [x] List remains a complete one-tap text equivalent while Map is unavailable.
- [x] Poor-network, unavailable-server, and GPS-unavailable states provide an honest next step.
- [x] Automated Android checks pass and the final debug APK installs and launches cleanly.
- [x] Physical-device evidence is reviewed and approved by the owner.

## Baseline findings and corrections

- Static review found fixed-height primary actions across entry, participant components,
  Account, and navigation surfaces. They were converted to minimum heights so labels can
  grow at 200% font scale.
- The first physical 200% font-scale pass kept Nearby navigation, List mode, permission
  recovery, Account identity, and Edit profile actions visible, but exposed a real blocker:
  the drop-detail sheet was not scrollable. Unlock entered the system-navigation area and
  Report/Block were below the screen. The sheet is now scrollable and the two safety actions
  are stacked, full-width, and minimum-height rather than fixed-height.
- Report reasons exposed an unlabeled radio control plus a separate label button. Each reason
  is now one labeled selectable radio row, and the reason list can scroll at large text.
- Entry, participant-load, and Account-detail error surfaces are marked as assertive
  live regions while retaining their explicit recovery actions.

## Evidence recorded on 2026-08-13

- Focused Compose regressions for entry and participant accessibility passed, including
  compact 200% text-scale scrolling, safety-action reachability, selectable report reasons,
  and assertive error announcements.
- The full `testDebugUnitTest lintDebug assembleDebug` gate completed successfully. Android
  lint produced no blocking issue and the debug APK assembled successfully.
- The resulting debug APK installed successfully on the approved Samsung `SM-S938U` and
  launched as `com.kitheapp`.
- At 200% font scale, the corrected drop-detail sheet scrolled far enough to keep Unlock,
  Report, and Block host reachable above the system navigation area. The report dialog kept
  all reason labels, Send report, and Cancel reachable; selecting a reason updated its
  checked state. No report was submitted.
- A compact `900x1600` display override at 200% exposed clipped headers, a vertically
  collapsed Refresh label, wrapped bottom navigation, and recovery notices that displaced
  the List viewport. The header and navigation now use single-line large-text treatments,
  browse controls stack responsively, and Maps/location recovery notices scroll with List.
  The corrected physical pass kept Nearby, Collection, and Account readable and allowed the
  tester to scroll through both notices to the drop cards above the system navigation area.
- Automated compact regressions cover the one-line header/navigation treatment, responsive
  browse controls, and drop reachability when both Maps and location are unavailable.
- The post-check AndroidRuntime log contained no fatal exception and no `Offset.getX-impl`
  match. This is a local smoke result, not production Crashlytics closure evidence.
- With notification permission denied, Account showed `Not granted — open Settings`; tapping
  Notifications opened Kithe's system notification page and Back returned to Account without
  a permission loop or changing the permission.
- A physical airplane-mode/no-network refresh initially exposed cached drops without saying
  they were cached. Nearby now observes Android's validated default network and presents a
  polite live-region notice: `You're offline. Showing saved drops. Reconnect, then Refresh.`
  The transition brings the notice into view, cached drops remain reachable, and Refresh
  retains the honest state. The full test/lint/assemble gate and repeat device check passed.
- Samsung TalkBack was enabled temporarily. Kithe remained navigable, the drop safety sheet
  and report dialog exposed labeled selectable controls, and Report was reached without
  sending a report. Synthetic ADB gestures do not reproduce human touch-exploration reliably,
  so spoken wording and swipe-order remain an owner-listener check rather than a claimed pass.
- A connected-device instrumentation scenario rendered the real unavailable-server state on
  the Samsung, verified the honest message and visible Try again action, and confirmed retry
  invocation. The instrumentation runner then removed its target package during cleanup,
  clearing Kithe's local app storage. The verified APK was reinstalled and the owner signed
  back in; no replacement anonymous account was created.
- The restored signed-in shell exposed Nearby, Collection, and Account without revealing
  account identifiers. At 200% font scale, Collection's empty state wrapped cleanly, all three
  destinations remained readable, and content stayed above the system navigation area. The
  empty state is consistent with the cleared local app storage; no server data was changed.
- On 2026-08-14, the owner completed the human-listener TalkBack check and reported a pass for
  destination announcements, meaningful control names, focus order, drop safety reachability,
  selectable report-reason state, and cancellation without submitting a report.
- The device font scale was restored to its original `1.0` and the app was relaunched after
  the check. TalkBack was restored to off; airplane mode, Wi-Fi, and mobile data were restored
  to their original states. A final log scan found no fatal exception or `Offset.getX-impl`
  match.

## Gate result

- **Approved and closed by the owner on 2026-08-14.**

R10-L approval closes only this local qualification gate. It does not authorize R10-P or
waive R5-P and the open R6-R9 pre-pilot dependencies.
