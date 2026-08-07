# Location call-site audit (task 3.1)

Read-only. Every location request in both clients, with its trigger, precision, lifetime,
and purpose. No behavior was changed. Audited 2026-08-06 at commit `f7869b9`.

The spec is the direction doc's six-step model:

1. Show nearby content using **approximate** location.
2. Request **precise** location only at the moment the user attempts to unlock a drop.
3. Check proximity.
4. Record the successful unlock — not a continuous location history.
5. Stop requesting precise location afterward.
6. Never show a user's live position to other users by default.

---

## Summary

**Three things are already right**, and 3.2–3.5 should be careful not to break them:

- **Nothing requests location at app launch.** `MainActivity.onCreate` only *checks*
  permissions; `ContextualPermissionPolicy` sequences every prompt behind an explicit user
  intent (`NEARBY_DISCOVERY`, `ENABLE_NEARBY_ALERTS`) and refuses to prompt at all until
  onboarding completes.
- **Android's unlock check is already the shape Phase 3 wants.**
  `DropDecisionReceiver.validateProximity` takes a one-shot high-accuracy fix at the moment
  of pickup, rejects stale readings, rejects readings whose accuracy is worse than the
  pickup radius, and **fails closed** when location is unavailable.
- **No location trail is persisted anywhere.** The only coordinates written to Firestore
  are the drop's own `lat`/`lng`. There is no `locationHistory`, no `lastKnownLocation`, no
  per-user position field, on either client or in `functions/`. Step 6 holds by
  construction: no user's live position is ever written or shown.

**The core violation is the same on both platforms and it is not the unlock path — it is
browsing.** Each client holds a continuous high-accuracy location stream the entire time
the explorer surface is open, purely to render distances and sort lists. That is precise
location for browse, held indefinitely, which steps 1 and 5 exclude.

---

## Android

| # | Site | Trigger | Precision | Lifetime | Purpose |
| --- | --- | --- | --- | --- | --- |
| A1 | `MainActivity.kt:106` `hasNearbyAlertPermissions` | Auth state change | — (check only) | — | Gates geofence registration on FINE + BACKGROUND + POST_NOTIFICATIONS |
| A2 | `DropHereScreen.kt:1310-1440` permission launchers | User intent (browse nearby / enable alerts) | FINE + COARSE requested together | — | Runtime prompts, sequenced by `ContextualPermissionPolicy` |
| A3 | `DropHereScreen.kt:4366` background rationale | User enables Nearby alerts | BACKGROUND | — | Rationale then system prompt |
| **A4** | **`DropHereScreen.kt:2701`** | **Explorer home visible + foreground permission** | **`PRIORITY_HIGH_ACCURACY`** | **Continuous, 5 s interval / 2 s fastest, until the screen leaves composition** | **Keeps `otherDropsCurrentLocation` fresh for distance labels, sorting, and the pickup range gate** |
| A5 | `DropHereScreen.kt:1687` `getLatestLocation()` | Called on demand | `PRIORITY_HIGH_ACCURACY`, falls back to `lastLocation` | One-shot | Six callers, split by purpose — see below |
| A6 | `DropHereScreen.kt:1787` `pickUpDrop` | User taps pick up | Reads **A4's cached value**, no new request | — | 30 m client-side gate before dispatching the pickup |
| **A7** | **`DropDecisionReceiver.kt:224`** | **Pickup attempt** | **`PRIORITY_HIGH_ACCURACY` one-shot** | **Released when the check resolves** | **The real proximity check: staleness + accuracy validated, fails closed** |
| A8 | `NearbyDropRegistrar.kt:277` `getLocation` | Geofence (re)registration | `PRIORITY_HIGH_ACCURACY`, falls back to `lastLocation` | One-shot | Origin for the geofence set |
| A9 | `NearbyDropRegistrar.kt:99/132` + `GeofenceManager.kt:45` | Nearby alerts enabled | OS-side monitoring | **Continuous and backgrounded** | Geofence triggers for nearby-drop notifications |
| A10 | `GeofenceReceiver.kt` | Geofence transition | None — consumes the trigger | — | Builds the notification |

**A5's callers, by purpose:**

| Caller | Purpose | Verdict |
| --- | --- | --- |
| `:2558` `submitDrop` | Place a new drop at the user's position | **Legitimate.** Creation genuinely needs precise position. Not covered by the unlock-only rule, but should still be a momentary request. |
| `:3994`, `:4022` hunt builder | Place hunt step coordinates | **Legitimate**, same reasoning. |
| `:2644`, `:2668` | Refresh distance for the browse list | **Violates step 1.** |
| `:2764` | Distance on the "My drops" tab | **Violates step 1.** |
| `:2906` | Distance/sort for collected notes | **Violates step 1.** |

---

## iOS

| # | Site | Trigger | Precision | Lifetime | Purpose |
| --- | --- | --- | --- | --- | --- |
| **I1** | **`LocationService.swift:44` `startUpdating`** | **Any time authorization becomes when-in-use or always** | **`kCLLocationAccuracyNearestTenMeters`** | **Continuous — `stopUpdating` is only ever called on de-authorization** | **Feeds `currentLocation`, which every distance calculation, the nearby list, drop creation, and the collect gate read** |
| I2 | `DropFeedView.swift:122` | Feed appears | When-in-use authorization | — | First prompt |
| I3 | `MainBottomBar.swift:623` | User enables alerts | **Always (background) authorization** | — | Background triggers |
| I4 | `AppViewModel.swift:192` `distanceToDrop` | Rendering + gating | Reads I1's cached value | — | Distance labels, `canPreview` (30 m), collect gate |
| I5 | `AppViewModel.swift:533` create drop | User creates a drop | Reads I1's cached value | — | Drop coordinates |

---

## Findings

### F1 — Continuous precise location for browsing (both platforms) · the main Phase 3 target

Android **A4** and iOS **I1** each hold a high-accuracy stream for the whole time the
explorer surface is open. Neither is tied to an unlock. This is what 3.2 and 3.3 exist to
replace: distances and sorting are a *browse* concern and should run on coarse location,
with precision requested only at A7's moment.

Note the coupling before changing A4: **A6 reads A4's cached value**, so removing the
stream without rerouting `pickUpDrop` would break the pre-dispatch range gate. A7 would
still catch it — it re-validates independently and fails closed — but the user-facing
"move closer" message would stop working.

### F2 — Background location is load-bearing for nearby alerts, not incidental

`ACCESS_BACKGROUND_LOCATION` exists for A9's geofences, which are the entire delivery
mechanism for nearby-drop notifications. 3.4 cannot simply drop the permission; it has to
decide what happens to geofenced alerts. Options worth weighing at 3.4, not now:
foreground-only geofences, server-side proximity fan-out, or accepting that nearby alerts
require the permission and gating them behind an explicit opt-in (which
`ContextualPermissionPolicy` already implements).

### F3 — iOS `markCollected` fails **open** · asymmetric with Android, and a pilot risk

```swift
if let distance = distanceToDrop(drop), distance > Self.dropPreviewRadiusMeters {
    return .invalidInput("Move within \(radius) meters to pick up this drop.")
}
```

`distanceToDrop` returns `nil` when `currentLocation` is `nil`, so the `if let` simply
falls through and **the collect proceeds with no proximity check at all**. Android's A7
rejects in exactly that situation. Any iOS user who collects before the first fix arrives
— or with location denied — unlocks from anywhere. This is a correctness bug in the
product's core mechanic, not just a privacy issue.

**Fixed 2026-08-06**, pulled forward out of Phase 3 sequence. `markCollected` is now
fail-closed and mirrors A7's rejections: no fix, a fix older than 2 minutes (the same
threshold as Android's `LOCATION_STALE_THRESHOLD_MILLIS`), or a `horizontalAccuracy` that
is negative or worse than the pickup radius all reject the pickup. The distance comparison
allows the radius plus the reading's own accuracy, matching Android. Both callers already
surface the returned error in an alert, so each rejection reaches the user.

One consequence worth knowing: **a user with "Precise Location" off can no longer collect
at all**, and is told to turn it on. That is the correct outcome — a 30 m check against a
1–5 km fix was previously passing or failing at random — but it is a hard stop until F4 is
addressed. Android already behaves this way.

### F4 — iOS never handles reduced accuracy

Neither `requestTemporaryFullAccuracyAuthorization` nor `accuracyAuthorization` appears
anywhere in the iOS source. With "Precise Location" off, iOS supplies fixes accurate to
roughly 1–5 km, and `markCollected`'s 30 m gate will reject every unlock with no
explanation the user can act on. 3.3's "prefer the one-time precise mechanism" has a
direct iOS counterpart that has to be built, not just ported.

### F5 — Android requests FINE and COARSE together, before any unlock

A2 requests both permissions at the `NEARBY_DISCOVERY` intent — that is, at browse time.
Step 2 wants precise deferred to the unlock attempt. 3.2/3.3 should split this: COARSE at
browse, FINE (or the one-time grant) at unlock.

### F6 — Two unrelated iOS defects found while tracing the call sites

Neither is a location-privacy issue; recording them so they are not lost.

- `AppViewModel.swift:273` — the nearby-radius filter is commented out, so iOS's "Nearby"
  destination lists **every** drop regardless of distance.
- `AppViewModel.swift:207` `previewRestrictionMessage` returns `nil` on both branches of
  its guard, so it can never produce a message.

### F7 — Proximity is client-enforced only, on both platforms

`markDropCollected` and the Android pickup write are ordinary client writes; no rule or
callable verifies the collector was ever near the drop. Rules cannot check location, so
this is inherent rather than a defect — but it bounds what Phase 3 can promise, and it is
worth stating before the pilot: a determined user can always unlock remotely. F3 matters
because it makes that trivial rather than deliberate on iOS.

---

## Status after 3.2/3.3 (2026-08-06)

F1, F4, and F5 are closed, and F3 was closed earlier: neither client streams location,
browsing runs on approximate one-shot fixes, precision is requested only at an unlock
attempt, and iOS now requests temporary full accuracy. F2 (background location for
geofenced alerts) is untouched and belongs to 3.4. F6 and F7 stand as recorded.

## Suggested sequencing for the rest of Phase 3

3.2 and 3.3 are genuinely coupled through A4/A6 and should be planned together even if
they ship as separate tasks. F3 is a one-line fix and does not need to wait for either —
it is arguably worth pulling forward, since it is currently the cheapest way to unlock any
drop in the product from anywhere.
