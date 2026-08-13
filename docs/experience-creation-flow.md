# Experience Creation Flow

Session date: 2026-07-29
Status: drafted, not yet reviewed against Backend feasibility.
Depends on: `organizer-access-request-flow.md` (this is the destination of that flow's "Get started" — first thing an approved Organizer does), `drop-authoring-flow.md` and `bounty-organizer-authoring-flow.md` (both assume an experience already exists), `geodrop-product-spec-v1.md` §4.1 (entry/QR), §2 (funnel definitions).

> **R0 alignment override (approved 2026-08-09; see
> `redesign-alignment-proposal.md`):** strict organizer approval and the lack
> of an in-app guest list remain accepted. Keep the already-built private aggregate Results
> view inside organizer tooling; the statement in §4 that Results must be founder-report-
> only is superseded. Generate an owned-domain App Link plus human event code and qualify
> the cold-install path through Play. Scheduled drops remain deferred.

## Design principle

This is the first thing an Organizer does after approval, and it's the screen that produces the one artifact everything else depends on: the QR + event code pair that Explorers actually use to get in. Get this wrong and nothing downstream works, no matter how good the drop authoring or bounty flows are.

**Decided (2026-07-29): no in-app guest list for v1.** Guest lists stay organizer-side bookkeeping (a spreadsheet, a headcount written down before the event) — not a product feature. This was weighed directly: an in-app guest list would mean storing real PII (names/emails) with no v1 functional destination (no targeted push, no per-guest anything exists), and it would quietly reopen an already-settled decision — the product spec explicitly defines "Invited" as *organizer-supplied, offline, not instrumented*. Revisit trigger: if push ever gains targeted/per-guest sending, that's the moment an in-app guest list would have a real job to do. Not now.

## Primary action

Create one experience: a name, a date range (with timezone), and get back a shareable QR + event code. Drops are added afterward via the separate authoring flow — this screen's job ends at "experience exists and is joinable," not at "experience is fully seeded."

---

## Flow

### 1. Entry — "Create an experience"

**Primary action:** start creating a new experience.

- Reached from the Organizer's top-level "Experiences" list
- Single button: "Create an experience" — same "don't make them choose from a list of one" principle as drop authoring's entry point, since there's only one experience type in v1

**States:**
- *Empty (organizer's very first experience, reached right after approval):* the experiences list itself is empty and says so plainly — "No experiences yet. Create your first one to get started." This is the landing state referenced in `organizer-access-request-flow.md`'s reconciliation note; this doc is where that "Create your first one" button actually leads.

### 2. Basics

**Primary action:** name the experience and set when it runs.

- **Name** — single line, required. This is what Explorers see on the entry/preview screen before joining ("[Organizer]'s [Name]"), so it should read like an event name, not an internal label — inline placeholder text can hint at this: "e.g. Sarah & Tom's Wedding" rather than "Experience 1."
- **Date range** — start and end, both required. This is the experience-level end time that individual drops can inherit as their expiry default (already referenced in `drop-authoring-flow.md` §4's "Use the experience's end time" shortcut) — this is the field that shortcut points at.
- **Timezone** — defaults to device timezone, editable. Matters because an organizer might be setting this up from home for a venue in a different timezone (per the pilot criteria's "founder has physical site access beforehand" — the setup and the walkthrough may not happen in the same place or timezone).
- **Description** — optional, one short line. Shown on the same Explorer-facing entry/preview screen, per the entry-flow IA ("one line of what to expect"). Not required because a good default experience (name + dates alone) should still be joinable without forcing extra copywriting before the organizer can move on to drops.

**States:**
- *Loading:* none needed — this is local form entry, nothing to fetch yet
- *Error (end date before start date):* inline, same pattern as other date-validation messages already established: "This needs to end after it starts — check the dates."
- *Offline:* form stays interactive, save queues per the same offline pattern used throughout the authoring flows

### 3. Create → QR & Event Code Generated

**Primary action:** none — this is the payoff screen, shown immediately after basics are saved.

- Once the experience is created, the server generates the join mechanism: a QR code (Android App Link) and a human-readable event code (e.g. `ORCHID-42` — short, speakable, unambiguous, no `0/O` or `1/I/l` confusion, per the entry-flow spec).
- **Both shown together, not just the QR.** The event code is the fallback layer for anyone whose deep link doesn't resolve (no app installed and referrer stripped, sideloaded install, borrowed phone, someone photographing the sign and installing later) — per the entry-flow spec, this is not a minor fallback, it's a load-bearing layer of the entry mechanism. This screen should treat the code as equally prominent to the QR, not buried below it as fine print.
- **Primary action on this screen: "Download / Share"** — produces a shareable image (QR + code + experience name, styled for printing on a physical sign or sharing digitally) rather than making the organizer screenshot the app UI themselves.
- Copy: "Share this QR code and event code with your guests — either one gets them in." Sets the expectation directly that the code isn't a backup only for emergencies, it's a normal, equally valid way in.

**States:**
- *Loading:* brief spinner while the server generates the code/QR pair
- *Error (generation fails):* "Couldn't generate your join code — your experience is saved, try again." (same reassurance pattern as drop authoring's save-failure copy — the experience itself isn't lost, just this one artifact)

### 4. Experience Detail (ongoing home base)

**Primary action:** navigate to drops, or revisit/edit experience settings.

This is where the Organizer lands on every subsequent visit — not the creation flow itself, which only runs once.

- **Drops** — the drop list from `drop-authoring-flow.md` (Published/Drafts split), reached from here
- **Settings** — name, dates, timezone, description (all editable after creation — none of this is a one-way door), plus **re-access to the QR/event code** from step 3 (an organizer needs to regenerate a printable copy, or just look up the code again, well after initial creation — this shouldn't be a one-time-only screen)
- No separate "Rewards" or "Results" section designed here — redemption code settings are covered under the reward/perk drop type (out of this doc's scope), and Results/analytics stays the founder-produced report per the standing decision, not an in-app section

**States:**
- *Loading:* skeleton while experience data loads
- *Error:* standard retry pattern, consistent with the rest of the authoring surfaces
- *Offline:* cached experience data and drop list still viewable; anything requiring a fresh QR/code fetch (e.g., regenerating a lost printed sign) needs a connection, communicated plainly: "Need a connection to fetch your join code again."

### 5. Editing experience-level settings after drops exist

**Primary action:** change name, dates, timezone, or description on a live experience.

- Allowed at any time — none of these fields lock once drops or Explorers exist, consistent with the ongoing-authoring model established in `drop-authoring-flow.md`
- **One real interaction worth naming:** changing the experience end date affects any drop that inherited its expiry from "Use the experience's end time" (per that shortcut in `drop-authoring-flow.md` §4). Recommend those drops track the experience end time *live* (not a one-time copy at the moment the organizer tapped the shortcut) — so extending an event automatically extends any drop that deliberately opted into "match the event," without requiring the organizer to go edit each drop individually. A drop that set an explicit custom expiry, rather than using the shortcut, is unaffected by experience-level date changes.
- **Needs Backend sign-off:** confirms whether "inherit experience end time" is implemented as a live reference or a copied value — the recommendation above assumes live reference, but this is a real data-modeling choice, not just a UI behavior.

---

## Accessibility

- QR code image needs the event code presented as real, selectable/readable text alongside it — never QR-only, since a QR isn't independently accessible to a screen reader and some guests may need to type the code manually regardless of vision
- Date/timezone pickers need standard accessible date-input patterns, not a custom control that traps screen reader focus
- The "Download / Share" action needs a clear, distinct label from any other share actions elsewhere in the app (redemption code sharing, drop content sharing) so screen reader users aren't left guessing which "share" does what

## Open items needing sign-off before this is final

- **Whether "inherit experience end time" is a live reference or copied value (§5)** — recommendation given above, needs Backend confirmation
- **Event code generation rules** (word list, collision handling across concurrent experiences) — this doc assumes the code is unique and human-friendly per the entry-flow spec's existing guidance, but the actual generation logic is Backend's to build
- **Whether an experience can be created before Organizer approval fully completes**, or whether experience creation is strictly gated behind the access-request flow's "Approved" state — I've assumed the latter (strict gate, consistent with the access-request doc's "pure gate, zero organizer capability until approved" principle), but worth confirming this doc doesn't need its own separate check

## Known scale ceiling

None specific to this flow at pilot scale — one organizer creating one or a handful of experiences doesn't strain a single-form creation screen the way batch drop authoring does. Worth revisiting only if "one organizer running many concurrent experiences" becomes a real use case, which isn't part of the current pilot shape.
