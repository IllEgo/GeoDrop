# GeoDrop — Wireframe Spec v1 (text-based)

**Owner:** Product Designer
**Status:** draft v0.1, 2026-07-26 — founder review required
**Format:** buildable screen breakdowns — purpose, primary action, elements, states. Not visual comps.
**Scope:** onboarding (both branches pending Open Decision #13), core loop, unlock success/failure, collection, redemption, report/block.
**Depends on:** `APP-BRIEF.md`, `product-direction.md`, `user-stories-v1.md`, `geodrop-product-spec-v1.md`, `PROJECT-STATE.md` (all 2026-07-26 versions)
**Updated 2026-07-29:** added Experience Preview (1.0.5), the pre-auth screen shown between QR resolve and account/guest-session onboarding — previously referenced only in earlier IA research, not yet formally spec'd in this document.
**Updated 2026-07-29 (second pass):** added Enter Event Code (1.0b), the code-entry fallback/equal-path for joining an experience without scanning a QR — closes a gap surfaced by `top-level-navigation.md`.

---

## How to read this

Each screen has:
- **Purpose** — why it exists
- **Primary action** — the one thing this screen is for (there is exactly one)
- **Elements** — what's on it, roughly top to bottom
- **States** — loading / empty / error / offline / permission-denied / success, only where applicable
- **Copy notes** — actual wording direction, not placeholder

Screens marked **[SHARED]** exist regardless of which guest-access branch ships. Screens marked **[BRANCH A]** / **[BRANCH B]** only exist in that branch. Everything else is core-loop and doesn't depend on #13.

`ASSUMPTION:` flagged inline wherever I'm filling a gap that isn't yet decided. These are guesses I need checked, not decisions.

---

## Part 1 — Onboarding (branch-dependent, Open Decision #13)

### 1.0 [SHARED] QR Resolve / Deep Link Landing

**Purpose:** entry point for every Explorer. Everything downstream depends on this resolving correctly and fast.

**Primary action:** none — this is a transitional screen, ideally sub-second.

**Elements:**
- Full-bleed brand mark or minimal spinner
- No text if resolution is instant; if it takes >1s, a single line: "Finding your experience…"

**States:**
- *Loading:* spinner, appears within 200ms per the global states checklist
- *Error (bad/expired link):* "This invitation isn't working. [Organizer name]'s event may have ended, or the link may be broken." — CTA: none, or "Contact [organizer]" if we have that string available
- *Offline:* "You'll need a connection to join — try again once you're online." Retry button.

**Copy notes:** never say "404" or "link invalid" — the guest doesn't know what a link is, they know they scanned something at a party.

**⚠️ Depends on Open Decision #3+13 (distribution mechanism) and #9 (experience as first-class entity).** This screen assumes the QR resolves directly to an experience ID. If that's not resolved by the time Frontend builds this, the screen still works the same way — just flag that the resolution target may change.

---

### 1.0b [SHARED] Enter Event Code (added 2026-07-29)

**Purpose:** the second, equally-valid entry path into an experience, for anyone who has the human-readable event code (e.g. `ORCHID-42`, per `experience-creation-flow.md` §3) but hasn't scanned or can't scan the QR — a borrowed phone, a photographed sign read later, a code read aloud over a phone call, or simply someone who prefers typing to scanning. This is not a fallback bolted onto the QR path as an afterthought; the entry-flow research this project is built on treats the code as a first-class, equally-valid layer of the entry mechanism, not an emergency backup.

**Reached from:** the Map tab's empty/no-experience-joined state (per `top-level-navigation.md`), for anyone opening the app cold without having scanned anything first. Also reasonable as a secondary link on the QR-resolve error state (1.0's "bad/expired link" case) — if a QR genuinely doesn't work, offering "Have an event code instead?" turns a dead end into a second chance, rather than stopping at "this invitation isn't working."

**Primary action:** enter the event code and continue.

**Elements:**
- Single input field, formatted to match the code's actual shape (e.g. auto-uppercase, dash placeholder) so someone reading it off a sign doesn't have to guess capitalization or punctuation
- Placeholder/label text shows the format directly: "e.g. ORCHID-42" — removes guesswork about what's expected
- Primary button: "Continue"
- Small link below: "Have a QR code instead? Scan it" — routes to camera/QR scan, since someone might land here first and realize they actually have the QR available too. Keeps the two entry paths visibly connected rather than siloed.

**States:**
- *Loading:* button shows inline spinner while the code resolves
- *Error (code not found / mistyped):* "We couldn't find an experience with that code — double check it and try again." Never a technical "404" or "invalid," matching the same tone already established for 1.0's error state. Field stays populated, doesn't clear on error — don't make someone retype a long code from scratch over a small typo.
- *Error (expired experience):* same message pattern as 1.0's expired-link case: "This event may have ended, or the code may not be active yet." (worth distinguishing "ended" from "not active yet" given scheduled-drops/experience-start-time now exist as a concept — an event that hasn't started yet is a different situation from one that's over, and the message should account for both without over-explaining which one it is)
- *Offline:* same as 1.0's offline state: "You'll need a connection to join — try again once you're online."

**On success:** resolves into 1.0.5 (Experience Preview) — identical downstream experience to the QR path from this point forward. This screen's only job is getting from "I have a code" to "I'm looking at the same Preview screen anyone scanning a QR would see." No divergent treatment based on which entry path someone used.

**Copy notes:** avoid making this feel like the "lesser" way in — no "backup," no "alternative," no apologetic framing. Per the entry-flow research this is built on, the code is meant to carry equal weight to the QR, and the UI should reflect that rather than quietly implying scanning is the "real" way and typing is what you do when that fails.

---

### 1.0.5 [SHARED] Experience Preview (added 2026-07-29)

**Purpose:** the first real screen anyone sees — shows what this is and why it's worth continuing, before asking for an account, a permission, or anything else. This sits between 1.0 (resolve) and both onboarding branches (1.1/1.3) — nobody hits signup or a guest session without seeing this first.

**Why this exists as its own screen rather than folding straight into signup or the map:** per the core principle running through onboarding elsewhere in this spec (show value before asking for something), jumping straight from a QR scan into a signup form or a permission request is the cold-ask problem this spec has otherwise been careful to avoid at every other step. This screen is that same discipline applied to the very first moment, not just to location and push later on.

**Primary action:** continue into the experience (leads into Branch A signup or Branch B guest session, per whichever way Open Decision #13 resolves — this screen's content and behavior are identical either way).

**Elements:**
- Organizer/host name and experience name — "[Organizer]'s [Experience Name]"
- One line describing scale, written from server data, not organizer-authored copy: "12 drops hidden around [venue/area name]" — gives a concrete sense of what's ahead without requiring the organizer to write promotional copy for every experience
- Date range (from experience creation's Basics step)
- Optional one-line description, if the organizer set one at creation — this is the one piece of copy that IS organizer-authored, and it's allowed to be empty
- Primary button: "Start exploring"
- Secondary, lower-weight link: "What is this?" — for the guest who scanned a QR at a party with zero context and isn't sure what they've landed on. Leads to a short, generic (non-experience-specific) explainer of what GeoDrop is — one paragraph, not a full tutorial.

**States:**
- *Loading:* brief skeleton while experience metadata (name, drop count, dates) loads — should feel instant given 1.0 already confirmed the experience resolved; this shouldn't reintroduce a second meaningful wait
- *Error:* shouldn't normally occur here since 1.0 already handled the bad-link/expired case — if experience metadata specifically fails to load after a successful resolve, same retry pattern as elsewhere: "Couldn't load this experience. [Retry]"
- *Offline:* same as 1.0's offline state — if 1.0 succeeded then went offline before this screen loaded, same messaging: "You'll need a connection to join — try again once you're online."

**Copy notes:** "12 drops hidden around [venue]" needs the drop count to be real and current, not a stale cached number from when the organizer created the experience — since drops are added on an ongoing basis (per the ongoing-authoring model), a guest scanning the QR on day two of a multi-day event should see the actual current count, not day-one's.

**What this screen deliberately does NOT do:** request any permission, request account creation, or show the map. Those all come after "Start exploring" is tapped — this screen's only job is showing enough to make that tap an easy yes.

---

---

### 1.1 [BRANCH A] Account Required — Signup

**Purpose:** minimum-friction account creation, required before any write (including first unlock), per Phase 1.2's anonymous-view-only rule.

**Primary action:** create account with one identifier + one credential.

**Elements:**
- Short headline: "One more step before you start exploring" (not "Sign Up" — signals *almost there*, not a wall)
- Single field: phone or email (pick one method, don't offer both — decision fatigue on a screen you want under 30 seconds)
- Password or magic-link/OTP field
- Primary button: "Continue"
- Small print: what happens to their data / link to privacy policy — required, keep it one line
- No profile photo, no username choice, no "tell us about yourself"

**States:**
- *Loading:* button shows inline spinner, fields disabled
- *Error (already exists):* "Looks like you've used GeoDrop before — [log in instead]" — resolves without dead-ending per acceptance criteria
- *Error (validation):* inline, under the field, never a modal
- *Offline:* form stays interactive but submit shows "You'll need a connection to finish this." Don't lose what they typed.

**Copy notes:** "Continue," not "Sign Up" or "Register" — those words signal commitment before value has been shown. This screen is between the QR and the first drop; it should feel like a formality, not a gate.

**⚠️ Open question I need from Product/Backend:** phone or email as the single identifier? Affects whether OTP or magic-link is the credential flow. Flagging rather than guessing — this changes the field count.

---

### 1.2 [BRANCH A] Account Created → Map Load

Transitions straight into 2.1 (Map/List View) with the experience already joined. No interstitial welcome screen — per the acceptance criteria, "a newly authenticated user lands directly on the experience map, not on a tutorial or a feed."

---

### 1.3 [BRANCH B] Guest Session — Straight to Map

**Purpose:** guest gets to value (seeing drops, attempting an unlock) with zero account friction. Device-bound session created silently.

**Primary action:** none on this transition — after tapping "Start exploring" on the Experience Preview (1.0.5), leads directly into 2.1 (Map/List View).

**Elements:** none — this is not a screen, it's the *absence* of screen 1.1. Worth stating explicitly so it isn't accidentally designed as an interstitial.

**Copy notes:** nothing to say here. The whole point of Branch B is that there's nothing between "scanned QR" and "looking at the map."

---

### 1.4 [BRANCH B] Soft Upgrade Prompt

**Purpose:** convert a guest session to a real account, without blocking the loop that got them there.

**Primary action:** create an account, OR dismiss and keep going as a guest.

**Trigger timing — `ASSUMPTION:`** after the *first* successful unlock, not the second. Reasoning: the guest has now proven the mechanic works for them and has something to lose (their first unlocked drop) if they don't upgrade before reinstalling. Waiting until the second unlock risks them closing the app and never coming back to upgrade at all — and per the acceptance criteria, collection persistence is "tied to account," so an un-upgraded guest's first find is one uninstall away from gone. **This timing is a guess and needs Product/Founder sign-off** — it directly trades off against interrupting momentum right after a win.

**Elements:**
- Non-blocking: presented as a bottom sheet or banner, not a full-screen modal — the map stays visible/dismissable underneath
- Headline: "Nice find! Save your progress so it's still here next time." (frames upgrade as loss-prevention, not as a requirement)
- Same single-field signup as 1.1, condensed
- Secondary action, equally visible: "Not now" — must be a real tap target, not a tiny "skip" link buried in a corner
- If dismissed, does not reappear after every subsequent unlock — `ASSUMPTION:` reappears once more near trail completion, then not again this session. **Needs a decision, flagging rather than guessing further.**

**States:**
- *Loading / error / offline:* same as 1.1
- *Dismissed:* guest continues normally, collection stays device-bound

**Copy notes:** never use "before it's too late" or urgency-manipulation phrasing here — the founder's own principles (APP-BRIEF §7) treat trust as core to the product, and a guilt-trip upgrade prompt is the kind of thing that reads as bad faith at an event where the guest is a friend of the organizer.

---

## Part 2 — Core loop [SHARED across both branches]

### 2.1 Map / List View

**Purpose:** show what's discoverable nearby without requesting precise location.

**Primary action:** select a drop to view its detail (locked or unlocked).

**Elements:**
- Toggle: Map / List (persists selection between the two, per acceptance criteria)
- Map: pins for each drop in the joined experience only; locked vs. unlocked visually distinct (not color alone — needs a shape or icon difference too, per accessibility default)
- List: same drops, sorted by distance, each row shows locked/unlocked state + rough distance + drop type icon
- No pins/rows for drops outside this experience
- No other users shown, ever, on the map

**States:**
- *Loading:* skeleton map or list, not blank
- *Empty (no drops yet):* "Nothing here yet — check back once [organizer] adds some drops." — real explanation, not "No drops"
- *Error:* "Couldn't load the map. [Retry]"
- *Offline:* list view still renders from cache if drops were already fetched; map may not tile-load — say so: "You're offline — some map details may not show, but your drop list is up to date as of [time]."
- *Permission-denied (approximate location):* list view still fully usable; map shows a centered/default view with a banner: "Turn on location to see how far things are." App is not degraded to unusable — per acceptance criteria this is a hard requirement, not a nice-to-have.

**Copy notes:** distance shown roughly ("about 200m away"), never falsely precise, since it's approximate location.

---

### 2.2 Drop Detail — Locked

**Purpose:** tease the content, drive the walk, without shipping the payload.

**Primary action:** attempt unlock (only enabled/relevant once the Explorer is physically near).

**Elements:**
- Title (visible even locked)
- Teaser line — short, written by the Organizer at authoring time, never auto-generated filler
- Rough distance + direction if available
- Locked visual treatment (icon, blur, or similar — not literally hiding that a drop exists)
- Button: "Try to unlock" — always present, not just when in range; tapping it from far away should lead into the failure state below rather than being disabled/greyed-out. A disabled button with no explanation is a dead end; a tap that explains "you're 400m away" is informative.

**States:** loading (fetching teaser), error, offline (cached teaser still viewable if previously loaded)

**Copy notes:** "Try to unlock," not "Unlock" as the button label while locked — sets expectation that proximity is being checked, not guaranteed.

---

### 2.3 Unlock Attempt → Precise Location Request

**Purpose:** request precise location at the exact moment of attempt, per the six-step privacy sequence — never earlier.

**Primary action:** grant precise location (system permission dialog) — this is largely OS-native UI, but the *priming* screen before it is ours.

**Elements (priming screen, shown before the OS dialog fires):**
- One sentence: "GeoDrop needs your exact location just for a second to confirm you're here." 
- Button: "Continue" → triggers OS permission dialog
- This priming step is mandatory per the role brief — never let the OS dialog fire cold on first use.

**States:**
- *Permission denied:* "Without precise location, we can't confirm you're at the spot. [Try again] or [go back]" — app remains usable, Explorer can back out to the map.
- *One-time permission expired (Android):* same denial flow, doesn't distinguish for the user why — a fresh prompt either way.

---

### 2.4 Unlock Result — Success

**Purpose:** deliver the payload, make showing up feel earned.

**Primary action:** view the content, then return to map or move to next drop.

**Elements:**
- Full content reveal (text/photo)
- If a reward/redemption code is attached, code shown here too, with a note that it also lives in Collection
- Clear next step: "Find your next drop" or similar, pointing back to map/list
- Progress indicator if part of a trail: "3 of 12 found"

**Copy notes:** per acceptance criteria this state must "tell the Explorer what to do next" — don't end on the content with no forward action.

---

### 2.5 Unlock Result — Failure (highest-priority state in the whole app)

**Purpose:** per PM's flag, this is "the state that decides whether the pilot reads as broken." Must name the actual problem and actual distance.

**Primary action:** understand why it failed and what to do (walk closer, wait, retry).

**Elements — three distinct failure reasons, each with its own copy, not a shared generic message:**

1. **Too far:** "You're about 60m away — head toward [landmark or direction if available]." Distance must be real, not a vague "not close enough."
2. **Weak GPS / accuracy worse than radius:** distinguished explicitly from "too far" — "Your location isn't precise enough right now. Try moving away from buildings or trees." This is a materially different problem from distance and needs its own copy per the acceptance criteria.
3. **No network:** "Can't confirm your location without a connection. Try again once you have signal." — ties to the Backend blocking question about unlocks at dead-zone venues; if there's no defined offline path, this is the honest failure message rather than a silent hang.

**Elements shared across all three:**
- Retry button
- Back to map/list
- Rate-limiting: repeated failed attempts at the same drop don't lock the Explorer out of the experience, but after some threshold (`ASSUMPTION:` 5 attempts in quick succession) show a calmer message: "Still no luck — take a break and try again in a bit," to avoid feeling like a broken slot machine.

**Copy notes:** never "Unlock failed" alone. Always distance + direction + next action, per acceptance criteria this is a named requirement, not a preference.

---

### 2.6 Collection

**Purpose:** persistent record of what was found.

**Primary action:** browse past finds; view a specific unlocked drop again.

**Elements:**
- Grid or list of unlocked drops, combined across every experience the Explorer has joined (added 2026-07-29, per `top-level-navigation.md` — Collection is not filtered to whichever experience is currently active on the Map tab)
- Each entry shows title + thumbnail if photo, **plus which experience it came from** (name + rough date) — necessary now that entries can span multiple experiences; without this label, a combined view would be confusing about where each find happened
- Progress summary — now scoped per-experience rather than one flat number for the whole screen, since "7 of 12 found" only means something relative to one specific experience's total: shown either as a per-experience subheader when the list is grouped/sorted that way, or contextually next to each experience-labeled cluster. Exact grouping/sort presentation follows the sort modes already spec'd in `bounty-mechanic-design.md` (Newest first / Oldest first / Bounties first) — none of those needed redesigning for multi-experience, they already sort on properties that work the same regardless of source experience.
- Trail completion badge/state if applicable — visually distinct, not just another list item
- Redemption codes shown inline if attached to a collected drop (see 2.7)
- **Edited indicator (added 2026-07-29):** if a drop was changed by the Organizer after this Explorer collected it — whether within the 30-minute grace window (content updated) or after it (content frozen as originally collected, but the live drop has since diverged) — the collected entry carries a small, low-key "edited" marker. No diff view, no "originally said" text, just an honest small signal so the Explorer isn't later confused if their memory doesn't match what someone else saw. See `drop-authoring-flow.md` §7 for the full mechanic this reflects.

**States:**
- *Empty:* "Nothing collected yet — go find your first drop." Links back to map.
- *Offline:* collection is cached locally, viewable offline — this should work since it's the Explorer's own past unlocks.

**⚠️ Data note (added 2026-07-29):** per `drop-authoring-flow.md` §7, a collected drop's content must be a **snapshot taken at collection time**, not a live reference to the current drop record, once the 30-minute edit grace window has closed. This is a Backend data-modeling requirement flowing from the ongoing-authoring decision, not just a display rule — flagging here since this screen is where the consequence of getting it wrong would actually surface (an Explorer's Collection silently changing days later).

**Confirmed (2026-07-29) — collect and unlock are one action, not two.** Collection updates the instant an unlock succeeds; there is no separate "claim" or "save" tap. This closes Open Decision #16. (This also matches what `bounty-mechanic-design.md` already stated as settled — "Collect is automatic on unlock... closes that open question" — so this wireframe spec was simply the one place still carrying it as an unconfirmed assumption rather than a locked decision.)

---

### 2.7 Redemption Code Detail

**Purpose:** Explorer can find and show their code to a business; business validates manually (per v1 done-state — no scanner).

**Primary action:** display the code clearly enough to be read by someone else, in person, in daylight.

**Elements:**
- Large, high-contrast code display — this will be read by a business employee, often in bright outdoor light, so treat it like the photo-legibility requirement from authoring
- Status: unredeemed / redeemed, visually distinct (not color-only)
- Which business/reward it's for

**States:**
- *Already redeemed:* "This code has already been used on [date]." — no ambiguity
- *Expired:* "This code expired on [date]."
- *Offline:* code must still display — it's already been issued and stored locally; this cannot depend on a live connection or it's useless at exactly the moment it's needed (standing at a merchant counter).

**Copy notes:** offline-availability here is not a nice-to-have, it's the actual real-world use case (bad venue wifi + standing at a booth).

---

### 2.8 Report / Block

**Purpose:** recourse without contacting anyone directly; store-compliance requirement.

**Primary action:** submit a report, or block a creator.

**Elements:**
- Reachable in ≤2 taps from any drop (acceptance criteria)
- Small, fixed reason list (not free text as the primary path — reduces moderation-queue ambiguity)
- Optional free-text add-on
- Separate action: "Block this creator" — hides their drops from this Explorer going forward

**States:**
- *Success:* "Thanks — we've got this. We don't share who reported it." Confirmation only, no promise of outcome per acceptance criteria.
- *Works even with zero prior unlocks* — an Explorer who's unlocked nothing can still report, per acceptance criteria. Worth stating explicitly since it's easy to accidentally gate this behind "has interacted with a drop."

---

## Part 2.5 — Permission Priming (location)

Two separate asks, deliberately different in framing and timing, but designed to *read* as one consistent privacy story — echoing illustration style and tone so a user who denied the first isn't surprised or extra-guarded when the second appears differently worded. Denying either must never dead-end the app, per APP-BRIEF §7 and the global states checklist.

### 2.5a Approximate Location Priming

**Purpose:** ask for coarse location only when the user would notice the value of granting it — not cold, not before they've seen anything.

**Primary action:** grant approximate location (system dialog) — the priming screen is ours; the OS dialog itself is native.

**Timing — recommendation, not yet confirmed:** do **not** ask before the map first renders. Let 2.1 load in its permission-denied default state first (list view fully usable, map centered/generic, banner present per 2.1's existing spec). Prime the ask from that banner, triggered by the user tapping it — not fired automatically on screen load. Reasoning: APP-BRIEF frames location privacy as a product principle, not a checkbox, and "show the gap, then offer to close it" is a stronger demonstration of that stance than asking on arrival before any value has been shown. It also means a user who came only to browse one drop and never taps the banner is never interrupted by a permission dialog at all.

**Elements (priming screen/sheet, shown on banner tap, before the OS dialog fires):**
- Short illustration or icon — Bobi-adjacent visual language if the mascot system is in play by build time, otherwise a simple map-pin motif. Should visually echo 2.5b's treatment.
- Headline: "See what's nearby"
- One line: "GeoDrop uses your approximate location to show how far away things are. We never show your exact position to anyone."
- Button: "Continue" → triggers OS permission dialog
- Secondary: "Not now" — dismisses back to the map's denied state, no repeat prompt this session

**States:**
- *Granted:* map/list immediately reflect real distances; banner disappears
- *Denied (via OS dialog, not just "Not now"):* same denied-state banner persists, but rewording to acknowledge the choice explicitly rather than repeating the same ask: "You can turn this on anytime in Settings if you change your mind." Never re-prompt automatically.

**Copy notes:** "approximate," not "general" or "rough" — matches the term used elsewhere in specs (APP-BRIEF, product-direction) so the glossary stays consistent. Never say "we need your location" — say what it's *for*, per the permission-priming brief requirement.

**⚠️ Needs confirmation:** the banner-triggered timing above is my recommendation, not a locked decision — flagging for Product/Founder sign-off, since it's a real alternative to "ask immediately after account creation," and it changes when Frontend wires the permission call.

---

### 2.5b Precise Location Priming (unlock attempt)

This is screen 2.3 from Part 2, restated here so both priming moments sit together for review — no content change from 2.3.

**Purpose:** request precise location at the exact moment of unlock attempt, never earlier, per the six-step privacy sequence.

**Primary action:** grant precise location (system dialog).

**Elements (priming screen, shown before the OS dialog fires):**
- Same illustration/icon family as 2.5a, but paired with a "just for a second" visual cue (e.g. a brief pulse or timer motif) to distinguish "one-time, momentary" from 2.5a's "ongoing while browsing"
- Headline: "Confirm you're here"
- One line: "GeoDrop needs your exact location just for a second to confirm you're at this spot. It's not stored, and we stop checking right after."
- Button: "Continue" → triggers OS permission dialog

**States:**
- *Granted:* proceeds directly into the unlock check (2.4 success or 2.5 failure, per Part 2 numbering)
- *Denied:* "Without precise location, we can't confirm you're at the spot." — Retry / Go back. App remains usable; user returns to map.
- *One-time permission expired (Android):* same denial flow and copy — no need to explain the technical distinction to the user, a fresh prompt either way.

**Copy notes:** "just for a second" and "not stored" are load-bearing phrases — they're the two concrete facts (duration, retention) that differentiate this ask from a generic location permission and match the product's actual behavior (6-step sequence: check, don't persist, release immediately). Don't soften this into vague reassurance ("we respect your privacy") — say the specific mechanism instead.

**Relationship to 2.5a:** a user could see 2.5b having denied 2.5a, or having never been asked 2.5a at all (if they never tapped the banner). Both paths must work — 2.5b does not depend on 2.5a having been granted or even shown.

---

### 2.5c Push Notification Priming

**Purpose:** ask for push permission scoped to exactly what the product spec permits — organizer broadcasts and new-drop alerts for this joined experience — and nothing that implies proximity or ongoing tracking.

**Primary action:** grant push notifications (system dialog) for this experience.

**Timing — recommendation, following the same logic as 2.5a.** Spec §4.11 says "requested at join, with a reason string, not at first launch." That rules out cold-asking before the app opens, but "at join" still leaves open whether it fires the instant `experience_joined` triggers server-side, or slightly after — once the map has rendered and the Explorer has actually seen there's something here worth being notified about. I'd recommend the latter: fire this right after 2.1 (Map/List View) first renders with ≥1 drop visible, not before. Asking for a notification permission before someone has seen anything to be notified *about* is the same cold-ask problem as location, just one screen later. **This is a timing refinement within an already-settled requirement, not a reopening of it — flagging so Frontend doesn't read "at join" as literally the join API call.**

**Elements (priming screen/sheet, shown once per experience, before the OS dialog fires):**
- Same illustration/icon family as 2.5a/2.5b, extending the trio visually
- Headline: "Stay in the loop for this event"
- One line, naming the exact scope per spec — no vaguer than the actual behavior: "We'll let you know if the organizer sends an update, or adds something new to find. Nothing else, and only for this experience."
- Button: "Continue" → triggers OS permission dialog
- Secondary: "Not now" — dismisses, no repeat prompt for this experience

**States:**
- *Granted:* confirmation is implicit (no extra screen) — just proceed
- *Denied:* no separate denial screen needed — per acceptance criteria, opting out must not degrade any other functionality, so there's nothing to explain or recover from. A quiet return to the map is correct; don't manufacture a "you'll miss out" message, since that contradicts "opting out does not degrade any other functionality."
- *Experience ends/expires:* push for that experience stops silently per spec — no user-facing screen needed here, this is a backend/OS-level cessation, not a UI state.

**Copy notes — the scope sentence is load-bearing.** It must map exactly to the two permitted triggers (organizer broadcast, new drop added) and explicitly exclude anything sounding like location-based ("near a drop") — the spec calls proximity push "the single most permission-expensive thing you could add" and a name for it appearing in *permission-priming copy*, even accidentally, would misrepresent what's being asked for. Don't let a well-meaning copy pass introduce "we'll let you know when you're close to something" — that's not what this permission does, and promising it in copy while not building it is worse than not mentioning it.

**Relationship to 2.5a/2.5b:** this is the third and last of the priming trio. All three should feel like the same voice making three specific, honest, narrowly-scoped asks — never one generic "allow permissions to continue" screen bundling multiple asks together. Bundling would violate the spirit of "ask for what you need, when you need it" even if technically each permission is still requested separately at the OS level.

---

## Part 3 — Screens intentionally not designed yet

- **Organizer authoring flow** (place a drop, set radius, preview) — separate spec, Organizer-facing not Explorer-facing, will follow once this Explorer-side spec is confirmed
- **Organizer analytics** — v1 done-state is a founder-produced report, not an in-app dashboard (per `geodrop-product-spec-v1.md` §4.10), so there's no UI to wireframe here yet

---

## Summary of open items blocking full confidence in this spec

| # | Item | Affects | Status |
|---|---|---|---|
| 13 (merged into #3) | Guest access branch | All of Part 1 | Both branches sketched here; will collapse to one once decided |
| — | Phone vs. email as signup identifier | 1.1, 1.4 field design | Needs Product/Backend answer |
| — | Soft-upgrade prompt timing (after 1st vs 2nd unlock) | 1.4 | My assumption, needs founder sign-off |
| 16 | Collect distinct from unlock? | 2.6, event flow between 2.4→2.6 | Assumed automatic; non-blocking but should close before Phase 4.2 |
| 15 | Unlock radius default | 2.5 copy tightness | Blocked on migration 3.3 field data |
| — | Backend: does unlock work with no network at venue? | 2.5 failure copy #3 | Blocking question per user-stories-v1.md §4 |
| — | Approximate-location priming timing (banner-triggered vs. immediate post-join) | 2.5a | My recommendation, needs sign-off; not a blocker |
| — | Push priming timing (right after map renders vs. literal join-moment) | 2.5c | My recommendation within an already-settled requirement (§4.11); not a blocker |

Nothing above stops implementation of the **shared core-loop screens (2.1–2.8)** — those don't depend on #13. Only Part 1 (onboarding) is genuinely forked pending that decision.
