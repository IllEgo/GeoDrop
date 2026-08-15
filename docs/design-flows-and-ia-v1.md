# Kithe — Flows, IA & Permission Design v1

> **Owner:** Product Designer. **Status:** draft v0.1, 2026-07-26 — founder review required.
> **Scope source:** `product-direction.md` launch scope, `user-stories-v1.md`, `migration-plan.md` Phase 3.
> Adds no scope. Every screen below maps to a committed story.
> **Upload to:** Frontend Engineer, Backend Engineer, Product Manager project knowledge bases.
> **Companion files:** `design-system-v1.md`, `voice-and-glossary-v1.md`.

> **R0 alignment override (approved 2026-08-09; see
> `redesign-alignment-proposal.md`):** use one shared shell with
> Nearby/Collection/Account. Approved organizer tools are reached through Account, not a
> permanent mode or fourth tab. Guest browsing gates an account only at the first unlock.
> Location permission is contextual, precise permission begins after Unlock, and push
> permission follows the first success. Keep the existing private aggregate Results view;
> founder reporting supplements it. Shareable Results images are deferred polish.

---

## 0. Three things that shaped every decision below

**a) This app is used outdoors, walking, one-handed, in sunlight, with a drink in the other hand.**
Nothing in the source docs says this out loud, but it's the actual usage context and it drives more design decisions than any feature on the list: high contrast over subtlety, bottom-anchored actions, big targets, no thin type, no hover-equivalents, no multi-step forms in the explorer app. It also means **dark mode is not a nicety** — weddings and festivals happen at night, and a bright screen in a dark venue is hostile.

**b) The list view is not a fallback for the map. It is the accessible path and the low-signal path.**
Map pins are the classic screen-reader dead end, and they're also useless when location is denied or GPS is fuzzy. Treat list and map as genuinely equal surfaces (user-stories §2 already says the same set — this adds *why*).

**c) A failed unlock must never look like the user's fault when it's GPS's fault.**
"GPS unlocking consistently causes frustration" is a named reposition trigger in `product-direction.md`. Most of that frustration is not accuracy — it's being told "no" without being told why or how far. The failure copy in §4 is doing more work for the pilot than any success screen.

---

## 1. Information architecture

### Explorer app — 3 tabs, nothing buried

```
Nearby            Collection          You
├ Map ⇄ List (toggle, shared state)   ├ profile
├ experience switcher (header)        ├ notification settings
├ drop detail (sheet)                 ├ blocked hosts
└ unlock flow (sheet)                 ├ delete account / export
                                      └ help
Collection
├ grouped by experience
├ found item detail (incl. reward codes)
└ expired-but-found items stay here
```

Rules:
- **Trail progress lives inside Nearby**, as a strip above the map/list, not as a fourth tab. It's context, not a destination.
- **Reward codes live in Collection**, reachable in two taps from anywhere. A guest at a bar counter needs the code fast (story 9: "can find their code again after closing the app").
- **The experience switcher is a header control, not a tab.** Most guests will only ever have one. Don't build a whole surface for the multi-experience case that doesn't exist yet — but don't hardcode single-experience either, since participant retention (a 2nd experience within 90 days) is a 90-day go/no-go criterion.

### Organizer app — same binary, separate mode

Entered by account type, not a toggle. An organizer who is *also* attending their own event switches to the explorer view from the You tab; that path exists because you will use it constantly during the pilot.

```
Experiences (list)
└ Experience detail
   ├ Drops        — list + map, add, reorder into a trail, duplicate
   ├ Rewards      — reward drops, code settings, redemption marking
   ├ Results      — live analytics (§6)
   └ Settings     — dates, timezone, join link/QR, guest list
Drop editor (full screen)
└ Preview as guest (locked state ⇄ unlocked state)
```

---

## 2. Flow — entry and first browse

```
QR scanned / link tapped
   │
   ├─ app installed ──────────────────────► [Experience preview]
   └─ app not installed ─► ⚠ UNRESOLVED (see Handoff A) ─► [Experience preview]

[Experience preview]  ← the first screen anyone sees. No map yet, no permission yet.
   Host name · experience name · "12 drops hidden around Liliʻuokalani Gardens"
   · dates · one line of what to expect
   Primary: "Start exploring"
   Secondary: "What is this?" (one short sheet, gloss of drop/trail/unlock)
   │
   ▼
[Location priming — approximate]  (§3a)
   │
   ├─ granted ────► [Nearby — map] with distances
   └─ denied ─────► [Nearby — list] no distances, honest banner, still usable
   │
   ▼
[Nearby]
   Locked drops show teaser + distance. Payload never on device before the check.
   │
   ▼
[Drop detail sheet]  → primary action: "Unlock"
```

**Account creation is deliberately not here.** Anonymous view-only browse is legal (Phase 1.2), and asking for an account before the guest has seen anything is the fastest way to miss the ≥30% activation threshold. The account gate lands at the **first unlock attempt**, which is the first moment the value is obvious and the first moment a write actually happens.

```
[Unlock pressed, not signed in]
   └─► [Sign-up sheet]  "Make an account to keep what you find."
        minimum fields only · one method · no profile prompts · no carousel
        └─► returns straight into the unlock attempt, not to the map
```
Returning to the map after sign-up instead of resuming the unlock is the single most common drop-off bug in flows like this. Call it out in QA.

**States for Experience preview:** loading = skeleton of host/title. Error (bad or dead link) = "This invitation doesn't work anymore. Ask your host for a new one." Expired experience = "This event has ended" + Collection link if they took part.

---

## 3. Permission priming — first-class, twice

### 3a. Approximate location (at "Start exploring", never at launch)

Full-screen, one job, one button:

> **Show me what's nearby**
> Kithe uses your rough location to show which drops are around you and how far.
> When you try to open a drop, we'll ask for your exact location for a few seconds to check you're really there — then we let go of it.
> We never track where you go, and no one else sees your location.
>
> [ Show nearby drops ]   [ Not now ]

Why this shape: it pre-announces the *second* request. The precise-location ask at unlock is the one that gets denied, and it gets denied because it feels like an escalation. Saying it here first makes it a promise kept rather than a surprise.

"Not now" is a real option and goes to the list view. Do not disable it, do not grey it, do not re-ask on the next screen.

**Denied path is not degraded to uselessness:** list view, author-defined order, teasers and photos intact, no distances. Persistent inline banner, not a modal: *"Distances are off because location is off. Turn on"* → app settings deep link.

### 3b. Precise location (at the first unlock attempt only)

Bottom sheet, on the **first** unlock attempt in an experience. Not on every drop — ten drops must not mean ten explainers.

> **Checking you're here**
> Kithe needs your exact location for a moment to confirm you're at this spot. We use it for the check and forget it — nothing is saved but the fact you found this.
>
> [ Check now ]   [ Cancel ]

Then the system dialog. On Android prefer **"Only this time"** (one-time precise; migration 3.3). Subsequent unlocks in the same session skip the sheet and go straight to the check.

**Android 12+ edge case Frontend must handle:** the user can grant location but choose *Approximate* in the same dialog. That is not a denial and must not be treated as one — it needs its own message: *"Your phone is sharing a rough location only. Kithe needs the exact one for a second to check you're here."* + settings path.

**Precise denied entirely:** browsing continues to work. The unlock button stays enabled — it opens an explainer rather than doing nothing. A permanently greyed button with no explanation is the worst possible outcome, because the guest concludes the app is broken and tells the host.

### 3c. Push (after the first successful unlock, inside the success sheet)

Never at launch, never before value.

> **Want a nudge from this event?**
> Only for [Event name] — when a new drop goes live or the trail moves on. Nothing else, ever. It stops when the event does.
>
> [ Turn on ]   [ No thanks ]

### 3d. Camera (organizer only, at photo attach)

Standard, in-context. Offer library-first — most organizer photos already exist.

---

## 4. Flow — the unlock, and every way it fails

This is the core mechanic (user-stories §4) and the only flow worth over-designing.

```
[Drop detail sheet]
   teaser · type · distance · "Unlock"
   │
   ▼  press
[Checking…]   inline in the sheet, ≤ 2s target, never a full-screen spinner
   │
   ├─ PASS ─────────► [Found] success state
   ├─ TOO FAR ──────► distance + "Check again"
   ├─ FUZZY GPS ────► accuracy explanation + "Check again"
   ├─ EXPIRED ──────► closed state, no retry
   ├─ NO NETWORK ───► ⚠ depends on Handoff B
   ├─ NOT SIGNED IN ► sign-up sheet, then resume
   └─ NO PRECISE ───► permission explainer (§3b)
```

**Failure copy — draft, own it as a set, not individually:**

| Case | Copy | Actions |
|---|---|---|
| Too far | "Not there yet — you're about **40 m** away." *(one-shot distance, from the check just made)* | Check again · Show on map |
| Fuzzy GPS | "Your phone isn't sure where it is right now (give or take 60 m). Step into the open and try again." | Check again |
| Expired | "This one closed when the event ended." | Back |
| No signal | *see Handoff B — two variants drafted, neither shippable until Backend answers* | — |
| Precise denied | "Kithe needs your exact location for a second to check you're here." | Allow · Not now |
| Approximate-only granted | "Your phone is sharing a rough location only." | Open settings · Not now |

Non-negotiables in that table:
- **Too-far and fuzzy-GPS must be different messages.** Blaming a guest for standing in the wrong place when their phone is off by 60 m is exactly the frustration the reposition trigger describes.
- **"Check again" is a button, not a background retry.** Continuous re-checking would mean holding precise location, which violates the privacy model. One press, one check, one release.
- **No exclamation marks, no apologies, no error codes.** State what happened and what to do.
- The distance shown is from the check that just ran and is not live. Don't animate it downward as they walk — that implies tracking we deliberately don't do.

**Success state ("Found"):**
Full-height sheet. Payload first, chrome second. Haptic + visual + text (never haptic alone — see accessibility).
- Content (text / photo / reward code)
- If reward: the code, large, monospaced, with "Show this at [business]" and a Collection link
- If in a trail: *"Next: [teaser of the following drop]"* — this is the "→ unlock another" half of the loop and the tightest metric you have (in-experience retention). Give it the most visual weight after the payload itself.
- If last in trail: completion state, prize instructions, feedback prompt (migration 6.2)
- Push priming lands here on first success only (§3c)

**Loading:** skeleton within 200ms, always. **Offline browse:** last-loaded teasers render from cache with a banner; unlocking is the only thing that hard-requires the network.

---

## 5. Flow — organizer authoring (and the biggest unaddressed cost in the project)

```
[Experiences] → + New experience
   name · dates · timezone · location centre
   ▼
[Drops]  map + list
   + Add drop ──► [Drop editor]
                    location: "Use my location" | "Drop a pin"
                    title · body · photo · type · unlock radius · expires
                    ▼ Preview as guest (locked ⇄ unlocked)
                    ▼ Save · Save & add another
   ⋯ per drop: Duplicate · Reorder into trail · Delete
   ▼
[Trail]  drag to order · "guests unlock these in sequence"
   ▼
[Rewards] · [Results] · [Share] (QR + link)
```

**Say plainly: authoring 10–30 drops on a phone is the largest unpriced cost in this build.**
`user-stories-v1.md` §3 already notes most authoring won't happen on-site, and `metrics-spec-v1.md` §7 names authoring hours as "the most important unmeasured number in the project" — with *"quality experiences require too much manual authoring"* as a reposition trigger. There is no web console in launch scope, so today the answer is: a phone, at a desk, thirty times.

Design mitigations inside current scope (no new features, all editor-level):
- **"Save & add another"** returns to a fresh editor with location/radius/expiry retained. Turns 30 forms into 30 fields.
- **Duplicate drop** — most drops in an experience differ only by location and body.
- **Defaults inherited from the experience** — radius, expiry, timezone set once, overridable per drop.
- **Pin-drop placement with a draggable map and a search field**, since the organizer is at a desk in a different building.

If those aren't enough during pilot 1, the finding isn't "the editor needs polish" — it's that the authoring surface is wrong, and that's a Product/scope conversation, not a design one. Log the hours from drop one.

---

## 6. Organizer results — design the sentence, not the dashboard

Direction doc says organizer analytics and redemption are what people actually pay for. `user-stories-v1.md` §10 asks for "at least one number presentable in a sentence the Organizer would repeat." Take that literally and make it the top of the screen:

```
┌────────────────────────────────┐
│  38 of 52 guests               │   ← headline stat, display size
│  found at least one drop       │
│                                │
│  21 finished the trail         │
│  14 of 19 rewards redeemed     │
│                                │
│  [ Share these results ]       │   ← renders an image
└────────────────────────────────┘
   Per drop ▾
   ● Welcome message      36 found
   ● Sponsor discount     19 found
   ○ Hidden lookout        2 found   ← surfaced, not buried
   Live · updates during the event
```

- **The share action is the point.** Organizers will send this to the next client. If it isn't shareable as an image, it becomes a screenshot of a cluttered dashboard — build the shareable version deliberately.
- **The drop nobody found is the most actionable row** (per §10) — visually distinguish it, don't just sort it last.
- **Available during the event.** The organizer is checking their phone mid-reception.
- **Empty state, which they will see first:** "No one's arrived yet. Results appear as guests start finding drops." Not a zeroed dashboard — a zeroed dashboard at 6pm reads as broken.
- **Never any individual guest's path.** Aggregates and receipts only, per the privacy principle. This is also a selling point worth stating in the UI: *"Kithe doesn't track where guests go."*

**Redemption:** business marks a code redeemed manually for pilot 1 (`ASSUMPTION:` per story 9). Design: a search/enter-code field + a big confirm, plus already-redeemed and expired states. This runs at a busy counter — one field, one button, 56dp, high contrast.

---

## 7. Accessibility checklist (applies to every flow above)

Nothing in the source docs sets a v1 standard. `user-stories-v1.md` flags the gap and it's mine to close, so: **WCAG 2.2 AA as the floor**, with these specifics.

- Text contrast ≥ 4.5:1; non-text UI and map pin states ≥ 3:1. Target 7:1 for primary text — outdoor glare, not compliance.
- Minimum touch target 48dp/44pt. Primary actions 56dp.
- **Never colour alone for locked / unlockable / found.** Every state carries icon + text label as well.
- Dynamic type to 200% without truncation on: drop detail, unlock failure, reward code, sign-up. These four are non-negotiable; test them, don't assume.
- **The list view must be fully operable with a screen reader, including unlock.** Map pins get labels, but the list is the supported path — say so in the QA brief.
- Announce unlock results via live region. Haptics and animation are supplements, never the only signal.
- Respect reduced-motion: the unlock reveal degrades to a cross-fade.
- Reward codes: high-contrast monospace, generous letter spacing, no ambiguous glyph pairs (see `design-system-v1.md`).

---

## 8. Outbound handoffs

### HANDOFF → Backend Engineer  *(A — blocking design)*
**Context:** Unlock failure states, `design-flows-and-ia-v1.md` §4.
**What I need:** Does an unlock work with no cell signal? Two mutually exclusive designs follow from the answer — (i) queued: "You found it. We'll confirm when you're back online," with a pending state in Collection and a resolution path if the check later fails; (ii) hard fail: "Kithe can't reach the internet. Move somewhere with signal and try again." I will not ship a promise the server won't keep.
**Constraints from Design:** whichever it is, it must be distinguishable in copy from *too far* and from *expired*. Also confirm the check returns the distance delta to the client — the "about 40 m away" message depends on it.
**Blocking:** yes, for failure copy only. Rest of the flow proceeds.

### HANDOFF → Product Manager  *(B)*
**Context:** Four product decisions that change screens, not just wording.
**What I need:**
1. **QR with no app installed** — (a) store → install → deep-link back, or (c) browser entry. `user-stories-v1.md` §1 is right that (b) is what you get by default. I can't design the entry flow's first screen until this is chosen; it's the front door to the ≥30% threshold.
2. **Default unlock radius** — needs a number. Design consumes it in the editor default and the failure copy. Informed by migration 3.3 field testing.
3. **Unlock vs collect** — I've designed them as one action (verb *unlock*, noun-place *Collection*). Confirm, and metrics-spec §8 Q1 resolves the same way.
4. **Organizer authoring surface** — see §5. Phone-only authoring is currently assumed; confirm that's accepted going into pilot 1.
**Blocking:** no, except item 1 for the entry screen.

### HANDOFF → Founder  *(C)*
**Context:** `APP-BRIEF.md` §9 (tone) is `NOT YET ANSWERED` and §11's seed glossary wasn't in the PROJECT-STATE version I have.
**What I need:** the three questions in APP-BRIEF §9, plus the existing seed list so I'm extending it rather than replacing it. A provisional voice is proposed in `voice-and-glossary-v1.md` §1 in the meantime — correct it rather than accept it by default.
**Blocking:** no.
