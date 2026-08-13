# GeoDrop — Visual System v1

> **Owner:** Product Designer. **Status:** draft v0.1, 2026-07-26.
> **Upload to:** Frontend Engineer (primary), Product Manager.
> **Companion files:** `design-flows-and-ia-v1.md`, `voice-and-glossary-v1.md`.

> **R0 alignment decision (2026-08-09; see `redesign-alignment-proposal.md`):** accepted as
> the Android-first visual foundation.
> R3 must turn these tokens and components into verified light/dark, dynamic-type,
> screen-reader, contrast, reduced-motion, and compact-device behavior before screen
> migration. Preserve iOS and shared-backend compatibility, but iOS visual parity follows
> the Android outdoor pilot gate.

---

## 0. Platform approach — corrected against PROJECT-STATE v0.3

The handoff asked for a visual system built for two genuinely separate apps, Material and HIG, in parallel. **PROJECT-STATE.md v0.3 reverses that sequencing**: Android first end-to-end, iOS on hold and later ported from the finished Android app.

Designing a complete HIG system now would be drafting iOS screens against Android screens that don't exist yet, and it would sit unread for months while the schema it assumes moves underneath it. So:

| Layer | Status now |
|---|---|
| **Foundations** — palette, type scale semantics, spacing, state model, all copy, IA | Platform-neutral. Written once, below. Both apps inherit. |
| **Android / Material 3 expression** | Full, now. Concrete values are Android-first. |
| **iOS / HIG expression** | Divergence list only (§7), filled in when porting starts. |

This is not "one design skinned twice" — §7 is where the real difference lives, and it's a list of decisions rather than a set of tokens, because that's what a port actually needs. Say the word if you'd rather have the full HIG pass now anyway and I'll do it; I think it would be wasted work at this moment.

---

## 1. Palette

The constraint that decides this palette: **most of the explorer app sits on top of a map.** Anything in map-blue (roads, water) or map-green (terrain) fights the basemap and loses. And the app is read outdoors in strong sun, which kills subtle mid-tones.

| Token | Hex | Use |
|---|---|---|
| `brand/primary` | **#0B5D5D** | Brand, primary actions, **found** state. Deep enough to hold 7:1 on light surfaces; distinct from map blue and terrain green at this saturation. |
| `brand/primary-on-dark` | **#4FD1C5** | Same role, dark mode. |
| `state/locked` | **#5B6470** | Locked drops. Deliberately quiet — locked is the resting state of most of the map. |
| `state/near` | **#E07B24** | **Unlockable now / you're close.** The one warm, loud colour in the system. Amber, not red — red reads as error at a glance. |
| `feedback/error` | **#B3261E** | Failures only. Never used for locked or too-far, which are not errors. |
| `surface/light` | **#FDFCFA** | Light background. Near-white, faintly warm — pure white is punishing at full brightness outdoors. |
| `surface/dark` | **#14171A** | Dark background. **Not #000** — pure black smears on OLED when panning a map. |
| `ink/primary` | **#14171A** / `#F2F4F5` on dark | Body text |
| `ink/secondary` | **#4A525C** / `#A8B2BC` on dark | Metadata, distances |

**The state progression is the system's core idea:** grey (locked) → amber (you're close enough to try) → teal (found). It's legible at a glance across a map at arm's length. It is also **always paired with an icon and a text label** — never colour alone, per the accessibility floor.

Deliberate omissions: no gradients, no glassmorphism, no secondary accent. One loud colour, spent on the only thing that matters — *go here now*.

## 2. Type

**Font must render ʻokina (ʻ) and kahakō (ā ē ī ō ū).** Organizer content will be full of place names — Liliʻuokalani, Kīlauea, Hawaiʻi — and a display face that drops the macron or substitutes a curly apostrophe is not shippable in this market. This eliminates a lot of otherwise-fine display faces; verify before adopting any.

- **Android:** Roboto Flex (system, full coverage, variable weight).
- **iOS:** SF Pro (system, full coverage).
- One family, worked hard with weight and size. A second display face is the kind of decoration that costs load time and gains nothing on a screen that's mostly map.

| Role | Size / line (sp) | Weight | Use |
|---|---|---|---|
| Display | 34 / 40 | 600 | Headline stat on organizer Results; reward code |
| Headline | 26 / 32 | 600 | Screen titles, found-state payload heading |
| Title | 20 / 26 | 600 | Drop names, sheet titles |
| Body L | **17 / 26** | 400 | Drop body, permission explainers, error copy |
| Body | 15 / 22 | 400 | Secondary content |
| Label | 13 / 18 | 500 | Distances, badges, metadata — **floor, nothing smaller** |
| Mono | 28 / 34 | 500 | Reward codes only |

Body L is one step above the Material default deliberately: this is read while walking, in sun, at arm's length. Nothing below 13sp exists in the system, so nobody has to decide.

**Reward code glyphs:** exclude 0/O, 1/I/l, 5/S, 8/B from generated codes, or use a mono face that distinguishes them clearly. Someone will read this aloud across a counter.

## 3. Spacing & layout

4dp base: `4 · 8 · 12 · 16 · 24 · 32 · 48`. Screen gutter **20dp**.

- **Primary actions bottom-anchored**, within thumb reach. Top-right primary actions don't exist in the explorer app.
- Sheets over full-screen navigation for drop detail and unlock — the map stays visible behind, which is orientation the user needs.
- Corner radius: 16dp cards/sheets, 12dp buttons, full-round chips. One step of softness, applied consistently.
- Elevation via surface tint, not drop shadow — shadows disappear over a map.
- Max one primary action per screen. If a screen seems to need two, the flow is wrong (see the working agreement on primary actions).

## 4. Core components

`DropCard` (list) · `DropPin` (map, 3 states) · `UnlockButton` (idle / checking / disabled-with-reason — never silently disabled) · `ResultSheet` (found / each failure) · `PermissionPrimer` (full screen and sheet variants) · `TrailStrip` (progress, "Next:") · `StatCard` (organizer headline) · `CodeDisplay` · `EmptyState` (illustration optional, copy mandatory).

Every component ships with: default, loading, empty, error, disabled, and dark. A component without its error state isn't done.

## 5. Dark mode

Not an option, a co-equal mode — evening events are the pilot's likely setting. Auto-follow system, no in-app toggle in v1.

- Dark surface #14171A, elevated surfaces lighten in 4% steps.
- Map style switches with it. A light map in a dark room is the brightest object in the room.
- `state/near` amber holds up on dark unchanged; primary teal shifts to #4FD1C5 for contrast.
- Re-verify all contrast ratios in dark; they do not carry over.

## 6. Motion

Sparse and purposeful. Two moments earn animation:
1. **The unlock reveal** — a short, non-bouncy reveal of the payload (~250ms). This is the product's one emotional beat; it should feel like something opening, not like a notification arriving.
2. **Pin state change** grey → amber as you approach a drop, on the next check.

Everything else: standard platform transitions. All motion respects reduced-motion and degrades to cross-fade. No looping ambient animation — battery matters when someone is walking a venue for an hour.

## 7. iOS divergence list — decisions, not tokens

To be filled in when porting starts (PROJECT-STATE Open Decision #12). These are the places where a straight translation of the Android build will feel wrong to an iPhone user:

1. **Navigation** — tab bar semantics differ; iOS back is edge-swipe + nav bar, Android has system back. The unlock sheet must dismiss correctly under both.
2. **Location permission model** — iOS asks While Using / Once, and precise is a *separate toggle* on the same dialog. There is no direct equivalent of Android's one-time precise grant, so §3b priming copy changes materially.
3. **Push permission** — one shot on iOS, effectively permanent if denied. The priming moment matters more, not less.
4. **Sheets** — iOS detents behave differently; the drop detail sheet's half/full behaviour needs its own spec.
5. **Typography** — SF Pro metrics differ from Roboto Flex; the scale in §2 needs re-fitting, not copying.
6. **Haptics** — richer vocabulary on iOS; the unlock confirmation should use it.
7. **Dynamic Type** — iOS users actually change it. Test the four critical screens at accessibility sizes.
8. **App Tracking Transparency / privacy nutrition labels** — different declarations from Android's data safety form, and they're a design surface (the pre-prompt) as well as a compliance one.

---

## 8. Localization readiness

Not a v1 feature; a v1 *constraint*, because retrofitting is expensive.

- Externalize every string from day one, including error and empty-state copy.
- No text baked into images.
- Design tolerates +40% text expansion. Japanese is a plausible second locale for Hawaiʻi Island tourism (the direction doc's third vertical) and Japanese also *contracts* — layouts must survive both.
- Diacriticals: see §2. This one bites in v1, not v2, because organizer-authored place names are already Hawaiian.
