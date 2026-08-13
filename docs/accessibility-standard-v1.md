# GeoDrop — Accessibility Standard v1

> **Owner:** Product Designer. **Status:** draft, 2026-07-26 — founder sign-off required.
> **Answers:** `user-stories-v1.md` global-states checklist, open `ASSUMPTION:` "no accessibility
> standard set for v1."
> **Upload to:** Frontend Engineer, QA project knowledge bases.
> **Testing is QA's, not mine.** This file defines the bar; QA defines how it's verified.

> **R0 alignment decision (2026-08-09; see `redesign-alignment-proposal.md`):** accepted as
> the Pilot 1 accessibility bar.
> Participant surfaces target WCAG 2.2 AA; the component and outdoor-device evidence in
> redesign tasks R3, R6, and R10 are required gates. The document remains draft only where
> it still needs concrete QA cases, not because the standard itself is undecided.

---

## The standard, in one line

**WCAG 2.2 Level AA on all Explorer-facing surfaces. Best-effort on Organizer authoring
surfaces in v1.**

That split is deliberate and I want it recorded rather than discovered. Explorers are 50–150
strangers at someone's wedding who did not choose this app and cannot be trained. Organizers are
a handful of paying customers you talk to directly and can help. Holding both to the same bar
with 20 hrs/week produces a standard that gets quietly ignored, which is worse than a scoped one
that gets met. Revisit when organizer self-service authoring ships.

---

## 1. Non-negotiable, v1

Nine items. All are cheap if done from the start and expensive to retrofit. If a screen fails
one of these, it isn't done.

| # | Rule | Number |
|---|---|---|
| 1 | **Tap targets** | 48×48 dp minimum, 8 dp minimum spacing between adjacent targets `[iOS later: 44×44 pt]` |
| 2 | **Text contrast** | 4.5:1 body, 3:1 for large text (≥18.66 sp bold / ≥24 sp) |
| 3 | **Non-text contrast** | 3:1 for icons, map markers, focus rings, and input borders that carry meaning |
| 4 | **Dynamic type** | Every screen usable at **200% font scale**. All text sized in `sp`, never `dp`. No fixed-height text containers, no `maxLines` that truncates a primary action's label |
| 5 | **Never color alone** | Locked vs unlocked, expired vs available, success vs failure each carry an icon *and* a text label, not just a colour |
| 6 | **Screen reader** | Every non-decorative control has a `contentDescription`; decorative images explicitly `null`. Unlock outcomes announced via live region |
| 7 | **Map has a text equivalent** | The nearby-drop list is the accessible alternative to the map, and is therefore **not optional and not a secondary view** |
| 8 | **Reduce motion respected** | Any reveal, celebration, or marker animation degrades to a cross-fade. The success state never depends on motion to be understood |
| 9 | **One-handed reach** | Every primary action sits in the bottom third of the screen. Nothing required to complete an unlock is in a top corner |

### Notes on the two that will actually bite

**#7 — the map.** A map is close to unusable with a screen reader no matter how it's built.
Story 2 lists the list view alongside the map as if they're interchangeable presentation
choices; they aren't. The list is the accessible surface and the map is the pleasant one. That
means the list has to carry everything the map carries — distance, locked state, drop type — and
has to be reachable in one tap from the map, permanently. Not a settings toggle.

**#4 — dynamic type.** Android 14+ uses non-linear font scaling up to 200%, and it will break
any layout with a fixed-height card. The unlock sheet, the drop card, and the distance readout
are the three places this will show up first. Cheap now, a rebuild later.

---

## 2. The GeoDrop-specific one that no generic checklist contains

**Sunlight legibility.**

This app is used outdoors, walking, in Hawai'i, mid-afternoon. That is not an edge case — it is
the primary usage condition, and it is a situational impairment that affects 100% of pilot
guests rather than a minority.

Consequences:

- **The Explorer surfaces default to a light, high-contrast theme.** Dark mode is supported and
  respects the system setting, but is not the default and is not what pilot screenshots or the
  store listing should show.
- **Target 7:1 on the unlock sheet's primary text and button**, not the 4.5:1 floor. This is the
  one screen that has to be readable at arm's length in glare while someone is walking.
- **No thin weights and no light-grey-on-white secondary text anywhere in the Explorer flow.**
  Secondary text is a darker grey than you'd choose on a desk.
- No large flat areas of pure white behind the map — glare off a white screen is worse than the
  contrast ratio predicts.

I'd rather over-index here than ship a beautiful app nobody can read at the event it was built
for.

---

## 3. Explicitly out of scope for v1

Recorded so it's a decision, not an oversight:

- Full TalkBack certification of Organizer authoring (map pin placement in particular is hard to
  make screen-reader-complete; the "place at my current location" path is the accessible
  alternative and **must** remain available)
- Switch Access and external keyboard navigation
- Localization / RTL — English only in v1, but layouts avoid fixed text widths so text expansion
  doesn't require a rebuild later `[see §5]`
- Captions/transcripts for audio drops — **only because audio is Now-excluded**. If Open
  Decision #8 puts audio back in scope, a text transcript becomes mandatory on every audio drop
  and that is a real authoring cost for the Organizer, not a client feature. Worth pricing into
  that decision.

---

## 4. Per-flow checklist

Applies alongside the six global states in `user-stories-v1.md`.

**Entry / onboarding**
- Event-code field: labelled, not placeholder-only; errors announced, not just coloured red
- Permission priming screens are readable at 200% scale without scrolling to reach the button

**Map / nearby list**
- List reachable in one tap from map, always
- Each list row announces: drop title, locked state, distance — in that order
- Distance in a screen-reader-friendly form ("about 120 feet away", not "120ft")

**Unlock**
- Live distance readout is a polite live region — it must not interrupt continuously as the
  number ticks; announce on meaningful change only
- Failure messages announced via assertive live region; failure is never conveyed by haptic or
  colour alone
- Success announced with the payload *and* the next-step sentence

**Collection / profile / report**
- Report reachable in ≤2 taps *including* via screen reader — verify the path, since "long-press
  the drop" is not screen-reader reachable

---

## 5. Localization readiness (not localization)

Not translating in v1, but two habits now cost nothing and save a rebuild:

- No hardcoded strings in layouts — everything through resources
- Assume **+35% text expansion**; no button whose label is sized to fit exactly

Relevant sooner than it sounds: Hawai'i events plausibly want Hawaiian or Japanese before they
want anything else.

---

## 6. What I need

1. **Sign-off on the AA / best-effort split** (§0). If you want one bar for everything, say so
   and I'll rewrite — but it will slow authoring-tool work.
2. **A named brand colour set**, so I can specify a palette that meets §1 and §2 rather than
   picking one and having it replaced later. If there isn't one yet, that's fine — tell me and
   I'll propose one built to hit the contrast numbers from the start, which is much easier than
   fixing a palette chosen for looks.
3. **Confirmation that this goes to QA as a testable list**, not just to Frontend as a guideline.
   Half of these are only real if someone checks them.
