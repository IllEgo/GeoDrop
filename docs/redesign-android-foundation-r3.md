# Kithe R3 — Android Design Foundation and Gate Evidence

Status: **Approved — R3 gate complete; R4 subsequently approved**  
Date: 2026-08-10  
Authority: approved sequence in `redesign-alignment-proposal.md`  
Production state: **no deployment, Remote Config change, production read/write, data
migration, navigation migration, or M4 cutover was performed**

Approval record: on 2026-08-10 the owner approved every remaining R3 gate item after the
verified debug APK was installed in place and launched successfully on the connected
SM-S938U. This closes R3 and explicitly authorizes R4. No separate bitmap-capture artifact
or TalkBack traversal transcript was retained; the owner accepted the physical-device
review as sufficient for this gate. R10 still requires recorded real-device screenshot,
TalkBack, compact-screen, 200% font, sunlight, network, permission, and outdoor evidence.

## 1. Outcome

R3 establishes the Android visual and interaction foundation required by the approved
design and accessibility specifications. It adds exact design tokens, fixed brand themes,
localized component copy, the nine shared components, a development-only component
catalog, and automated gate checks. It does not implement R4 navigation or migrate an R5+
screen flow.

No R3 implementation decision opposes `product-direction.md` or `migration-plan.md`.
Where the former theme differed from the approved design specification, R3 follows the
approved design tokens: teal/amber/grey state colors, one offline-safe platform sans
family, a 13 sp text floor, 12/16 dp geometry, zero shadow elevation, fixed brand colors,
system dark mode, and reduced-motion behavior.

## 2. Implemented foundation

### Tokens and themes

- Exact approved light primary `#0B5D5D`, dark primary `#4FD1C5`, locked `#5B6470`, near
  `#E07B24`, error `#B3261E`, light surface `#FDFCFA`, and dark surface `#14171A` tokens.
- Complete light and dark Material color schemes, with state-specific container and
  foreground pairs. State meaning is always paired with text and an icon.
- System light/dark selection by default. Wallpaper-derived dynamic color is retained only
  as a source-compatible parameter and intentionally does not replace the brand/state
  palette.
- Platform sans/Roboto-compatible typography with Hawaiian character coverage and the
  approved 34/26/20/17/15/13 sp scale. Reward codes use a 28 sp monospaced style.
- Approved 4/8/12/16/20/24/32/48 dp spacing, 48 dp minimum touch target, 8/12/16 dp shape
  tokens, and tint/border hierarchy with zero shadow elevation.
- Motion tokens use 250 ms unlock, 200 ms state change, and 150 ms crossfade defaults.
  Android's zero animator-duration setting resolves to reduced motion; unlock and pin
  movement durations then become zero and the crossfade becomes 80 ms.

### Copy and localization

- All R3 component-owned user copy is in Android string resources.
- English and Spanish R3 resources are complete; Android lint reports no missing
  translations.
- Catalog fixtures preserve `Kīlauea`, `Hawaiʻi`, and `Liliʻuokalani` rather than replacing
  the kahakō or ʻokina.
- A compact-component test renders deliberately expanded copy, and the catalog provides a
  320 dp/200% font preview. Permission actions remain outside the scrollable explanation
  so they stay reachable under text growth.
- Participant terminology follows the approved vocabulary: Nearby, Experience, Trail,
  Found, and Unlock. New R3 audience copy does not introduce Explorer, collect, or coupon.

## 3. Shared component catalog

The development-only `GeoDropComponentCatalog` contains every required R3 component and is
not connected to app navigation:

1. `DropCard`
2. `DropPin`
3. `UnlockButton`
4. `ResultSheet`
5. `PermissionPrimer`
6. `TrailStrip`
7. `StatCard`
8. `CodeDisplay`
9. `EmptyState`

The catalog covers locked, near, and found states; idle, checking, and reasoned-disabled
unlock states; loading, found, failure, and empty result states; and shared loading, empty,
error, and disabled component frames. Dedicated previews cover light, dark, compact 200%
font, and reduced-motion configurations.

Accessibility behavior includes merged state descriptions where appropriate, explicit
button roles, icon plus text plus color state cues, polite/assertive result live regions,
spoken reward-code characters, descriptive copy controls, full-width 48 dp actions, and a
scroll-safe permission primer.

## 4. Verification evidence

Automated checks run locally on 2026-08-10:

| Check | Result |
| --- | --- |
| Debug Kotlin compilation | Pass |
| Android unit and Robolectric suite | Pass: 55 tests after the final expansion check |
| R3 palette, typography, spacing, motion, and WCAG contrast assertions | Pass |
| State text/icon semantics, disabled reason, live-region, and 48 dp assertions | Pass |
| Compact 200% font and expanded-copy composition checks | Pass |
| English/Spanish resource completeness and Hawaiian diacritics | Pass |
| Android lint | Pass |
| Debug APK assembly | Pass |
| On-device screenshot-test APK compilation | Pass |

The instrumentation suite contains light and dark catalog bitmap-capture smoke tests and
compiled successfully. It was not executed during the automated R3 run because no device
was attached at that point and the SDK had no configured AVD. A physical SM-S938U was later
connected; the verified debug APK installed in place, preserved app data, launched, and
remained the top resumed activity without a detected launch crash. The owner accepted that
physical-device review without requiring a separate capture artifact at this gate.

## 5. R3 gate checklist

- [x] Exact visual, type, spacing, shape, elevation, state, and motion tokens implemented.
- [x] Light, dark, and system theme behavior implemented and locally tested.
- [x] Nine shared components and all required component states exist in the catalog.
- [x] Non-color cues, 48 dp actions, live regions, and descriptive semantics tested.
- [x] WCAG AA text pairs and 7:1 primary unlock pairs verified programmatically.
- [x] English/Spanish resources, Hawaiian diacritics, compact 200% font, and expanded copy
  verified locally.
- [x] Unit tests, lint, debug assembly, and screenshot-test APK compilation pass.
- [x] Owner accepted the physical-device light/dark and compact visual review after the
  updated app was installed and launched on the SM-S938U; no separate bitmap artifact was
  retained.
- [x] Owner accepted the physical-device accessibility review for the R3 gate; a recorded
  TalkBack/200% traversal remains mandatory in R10 qualification.
- [x] Founder approved the R3 catalog and explicitly authorized R4 on 2026-08-10.

R3 stops here. R4 was subsequently implemented and approved; its evidence is in
`redesign-navigation-shell-r4.md`. R2 production deployment/audit, production data access,
Remote Config enablement, and M4 remain outside this approval.
