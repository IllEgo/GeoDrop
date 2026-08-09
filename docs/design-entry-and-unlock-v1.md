# GeoDrop — Entry Flow & Unlock States (Design v0.1)

> **Owner:** Product Designer. **Status:** draft, 2026-07-26 — founder review required.
> **Answers:** `user-stories-v1.md` story 1 (QR with no app installed) and story 4 (unlock failure).
> **Upload to:** Frontend Engineer, Backend Engineer, Product Manager, DevOps project knowledge bases.
> **Platform:** Android only, per `PROJECT-STATE.md` v0.3. iOS notes marked `[iOS later]`.

---

## Part 1 — Entry: what the QR does for someone without the app

### 1.1 The finding

Story 1 frames this as (a) store → install → deep-link back, (b) deep link lost, (c) browser
entry. **(b) isn't a design option, it's a failure mode — and it's what you get by default.**
But the real problem is one layer down:

**Firebase Dynamic Links no longer exists.** Google shut it down on 25 August 2025; existing
links return 404 and deferred deep linking through it is gone. Every "scan QR → install →
land in the right experience" tutorial written before mid-2024 assumes it. If the prototype
wired entry through FDL — plausible, given it's Firebase-native and LLM-generated — that path
is dead code and nobody has noticed because the QR flow has never been tested end-to-end on a
device that didn't already have the app.

**→ Frontend/Backend: confirm whether `com.google.firebase:firebase-dynamic-links` or any
`*.page.link` domain appears anywhere in the repo. This should be a row in the Phase 0.1
feature inventory and I don't believe it currently is.**

The paid replacements (Branch, AppsFlyer, and the smaller FDL clones) all solve this, and all
are wrong for a two-person side project at pilot 1 volume: an SDK dependency, a vendor domain,
and a monthly bill to serve ~150 people at one event.

### 1.2 Recommended design: (a), built natively, with a code fallback

Three layers. Each one catches what the one above it drops.

| Layer | Mechanism | Catches |
|---|---|---|
| 1. App installed | **Android App Links** — verified `assetlinks.json` on your own domain, `https://<domain>/e/<code>` | The clean case. Opens straight into the experience. |
| 2. App not installed | Web landing page → Play Store URL carrying `&referrer=` → app reads it on first launch via the **Play Install Referrer API** | Deferred deep link, no vendor, no SDK, no cost |
| 3. Anything else | **Human-readable event code**, typed into a first-run field | Referrer stripped, sideloaded install, borrowed phone, someone photographed the sign and installed at home two days later |

Layer 3 is the important one and it's the cheapest thing in this document: **one text field and
a Join button.** It is also the only layer that survives Open Decision #3 going the wrong way
(see 1.5). Build it first, not last.

`ASSUMPTION:` GeoDrop controls a web domain suitable for App Links verification. If not, this
plan doesn't work as written — flag it now, it's a prerequisite with a lead time.

### 1.3 The event code

A short, speakable, unambiguous code — `ORCHID-42` rather than `a7f3-9c2e`. Requirements:

- Case-insensitive, whitespace-tolerant on entry
- No `0/O`, `1/I/l`
- Two words or one word + digits — it has to survive being read aloud across a room and
  written on a whiteboard
- Scoped per experience, not per guest (per-guest identity is the invite link's job, §1.5)

The code is printed **on the same physical sign as the QR**, in plain text, at a readable size.
Not as fine print. This is a design instruction for the printed artifact, and it's the single
highest-leverage detail in the whole entry flow: it converts a total dead end into ten seconds
of typing.

### 1.4 Screens

**S1 — Pre-install web landing page** (`/e/<code>`, no app detected)

Nobody currently owns this and it is the first impression of GeoDrop for most pilot guests.

- **Primary action:** Get the app
- Names the actual event ("Kalani & Mei's wedding — 14 drops hidden around the property")
  pulled from the experience record. A generic "Download GeoDrop" page converts far worse.
- One sentence on what happens: content is hidden around the venue; walk to it to open it.
- Event code shown on screen, so a guest who bounces to the Play Store manually still has it.
- No account creation on the web. No feature list. No screenshots carousel.
- States: loading; **invalid/expired code** ("This event code isn't active. Check with the
  organizer."); experience-not-yet-started.

**S2 — First launch, referrer resolved**

Skip straight to S4. Do not show a generic welcome screen to someone who arrived via an invite
— they have already been told what this is, twice.

**S3 — First launch, no referrer** (layer 3)

- **Primary action:** enter event code
- Single field, large, numeric-friendly keyboard, autofocus, paste-aware
- Secondary, lower and quieter: "Scan a code" (camera) — needs camera permission priming, so
  it is not the default path
- Error state: "We couldn't find that code" + the code they typed shown back to them + retry.
  Never clears the field.

**S4 — Experience preview (pre-account)**

The one screen that decides the ≥30% threshold. Anonymous auth, view-only (Phase 1.2).

- Shows the experience: name, organizer, drop count, a map with locked drop markers
- **Primary action:** Join
- Account creation is triggered by Join, not by launch. The guest sees what they're joining
  before being asked for anything.
- No location permission requested here. No push permission requested here. See §1.6.

### 1.5 This is blocked on Open Decision #3, and worse than it looks

`PROJECT-STATE.md` lists pilot 1 distribution as open and blocking. The entry flow above assumes
a **public or open-testing Play listing**. If pilot 1 ships via Play Console **closed testing**,
the real guest flow becomes:

> scan QR → web page → *"you need to be invited as a tester"* → guest supplies the Google
> account they use on their phone → organizer adds it to a list → guest waits → accepts the
> tester invite → installs → referrer may or may not survive

That is not a funnel, it's an obstacle course, and it happens while the guest is standing at a
party holding a drink. Invite→activation will not reach 30% through it, and the failure will
look like a product failure when it's a distribution failure.

**Design position: pilot 1 needs open testing or production distribution.** If that's not
possible, the pilot's activation threshold has to be renegotiated before the event, not after —
and the event code (layer 3) becomes the primary path rather than the fallback.

**→ Handoff to DevOps + Product Manager. Blocking for onboarding design and for the metrics
spec's §4 denominator choice.**

### 1.6 The invite-link version, and why it's better

`metrics-spec-v1.md` §4 recommends Option A (per-guest invite links) for a measurable
denominator. It has a second benefit that matters more to me than the analytics:

**A link sent days ahead means guests install at home, on wifi, unhurried.** A venue QR means
50–150 people install simultaneously on venue LTE at the moment the event starts. That's a
first-impression disaster with no product cause, and it's the kind of thing that only shows up
on the day.

If Option A is chosen, the invite copy should ask for pre-install explicitly, and the venue QR
stays as a same-day fallback for people who didn't. Both entry points then exist and both need
designing — they do, above, and they converge at S4.

### 1.7 Permission sequencing (unchanged by any of the above)

| Permission | Requested at | Priming screen |
|---|---|---|
| Location (approximate) | After Join, before the map first renders | "GeoDrop shows what's near you. It never shares where you are with anyone." |
| Location (precise, one-time) | First unlock attempt only | Inline in the unlock sheet — see Part 2 |
| Camera | Only if the guest taps "Scan a code" or a photo drop requires capture | Inline |
| Notifications | After the **first successful unlock**, not before | "Want a nudge when a new drop appears at this event? Only this event." |

Push after first unlock is deliberate: the guest has now experienced the value, and story 11's
"a moment where the value is obvious" is exactly that moment. Asking at launch buys a denial
that is effectively permanent.

---

## Part 2 — The unlock failure state

### 2.1 Design position

The current spec says "not close enough, roughly X away." That's right, but it's one of seven
distinct failures, and four of them are **not the guest's fault**. Telling someone "you're not
close enough" when their phone's GPS is drifting is how you manufacture the "GPS unlocking
frustrates consistently" reposition trigger out of a working product.

Two rules:

1. **Never report a distance you don't trust.** If reported accuracy is worse than the unlock
   radius, the app genuinely does not know where the guest is, and must say so instead of
   guessing.
2. **Failure is never a dead end.** Every failure state carries exactly one obvious next
   action, and retry is one tap, in place — not back-out-and-start-over.

### 2.2 The unlock sheet

One component handles the attempt and every outcome. A bottom sheet, thumb-reachable, drop
title visible, primary action bottom-anchored.

**Live distance readout.** While the sheet is open, show approximate distance updating from
coarse location, without a server call: *"About 40 ft away"* → *"About 15 ft away"* →
*"You're here — tap to unlock."* This converts the whole interaction from a binary reject into
a getting-warmer game, which is the mechanic the product is selling. It also means most guests
never see a failure state at all, because they don't tap until they're there.

**→ Blocking question for Backend: what's the rate limit on server-side unlock attempts?** If
attempts are cheap, the button stays live at all distances. If they're expensive, the button
gates until coarse distance is within ~2× the radius. I need the number to finish this screen.

### 2.3 Failure taxonomy and copy

Provisional copy — see §2.5 on voice. Distances in feet/miles (`ASSUMPTION:` US/HI pilot).

| # | Condition | Message | Action | Notes |
|---|---|---|---|---|
| 1 | Too far, accuracy good | **Not there yet** — "You're about 120 ft away. The map's showing you where." | Show me → returns to map, drop highlighted | Round: <20 ft "a few steps", <100 ft nearest 10, else nearest 50 |
| 2 | Too far, accuracy worse than radius | **Your phone isn't sure where you are** — "Step into the open, away from buildings, and try again." | Try again | Never quote a distance here |
| 3 | No network | **No signal here** — "GeoDrop checks the spot on our end, so this needs a connection. Try again in a moment." | Try again | See §2.4 |
| 4 | Precise permission denied | **GeoDrop needs your exact location just for this check** — "It's used once, then released. We never keep a trail." | Allow once → system dialog | Also reachable from settings deep-link if permanently denied |
| 5 | One-time permission expired | Silent re-request, no error screen | — | This is a system behaviour, not a user mistake; don't surface it as failure |
| 6 | Expired | **This one's closed** — "The organizer set it to end at 9:00 PM." | Back to map | Must read as *different* from #1 (story 5 requirement) |
| 7 | Already unlocked | Not a failure. Open it. | — | |

Rules the copy follows: no apologies, no exclamation marks, no blame, never a raw error code,
never the word "error," and **never a bare "no."** Every message names what happened and what
to do next.

### 2.4 The one that isn't a design problem

Failure 3 has no good design answer. If proximity validation is server-side and the venue has a
dead zone, guests standing on the exact right spot get rejected. No copy fixes that.

Story 4 already flags this. Restating it with design weight: **either the pilot venue is
walk-tested for coverage at every drop location before drops are placed, or there is a defined
offline path.** A third option worth considering — an authoring-time warning when a drop is
placed where the authoring device had no signal — is cheap and catches most of it, but only
covers drops placed on-site.

**→ Handoff to Backend + Product Manager. Blocking before the pilot venue is booked.**

### 2.5 Success, briefly

Failure states are worthless if success is flat. On a passing check: haptic, the payload
revealed with a short reveal (not a modal celebration), and — critically — **what to do next**:
*"3 of 14 found. Nearest one: about 200 ft, by the banyan."* Story 1's loop is "unlock another,"
so the success state is where that loop is either continued or dropped.

Respect reduce-motion: the reveal degrades to a cross-fade, never to nothing.

### 2.6 Voice, provisionally

All copy above is written in a voice that isn't decided yet — `APP-BRIEF.md` §9 is
`NOT YET ANSWERED` and I need it before this becomes canonical. Provisional stance, for you to
confirm or overrule:

**Warm and plain in ordinary moments; entirely plain in failure moments.** Playfulness belongs
to the organizer's content, not the app's chrome — the app is the box the surprise comes in,
not the surprise. Concretely: GeoDrop never jokes at someone who is standing in the rain
looking for a drop that won't open.

Three questions I need answered to close the voice guide (from `APP-BRIEF.md` §9):
if GeoDrop greeted you at the event, how would it sound? What would it never say? Closer to
"clever scavenger-hunt host" or "quiet, trustworthy local guide"?

---

## Terminology (first entries in the glossary)

Per my lane — every role writes to these. Flagging because two are already inconsistent across
the existing docs.

| Term | Use | Never |
|---|---|---|
| **drop** | The placed content | pin, marker, item, geodrop (lowercase, one word) |
| **experience** | The joinable collection of drops | event, hunt, trail — *"trail"* is a specific ordered experience, not a synonym |
| **unlock** | The act of opening a drop by being there | collect, claim, find, redeem |
| **Explorer / Organizer** | The two user-facing roles | user, guest, customer, creator |

**Unresolved:** *unlock* vs *collect*. `user-stories-v1.md` story 6 and `metrics-spec-v1.md` §8
both ask; the metrics spec assumes they're one act. **Design's position: make them one act, and
delete "collect" from the product vocabulary.** Two words for one action costs a glossary entry,
an analytics event, and a moment of guest confusion, and buys nothing. If they must stay
distinct, "collect" needs its own deliberate gesture and its own reason to exist, which I'd have
to design and which nobody has asked for.

Guests will say "found" regardless of what we ship. Worth knowing when writing organizer-facing
copy.
