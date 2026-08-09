# Redemption Tracking Flow — Organizer-Side

Session date: 2026-07-29
Status: drafted, not yet reviewed against Backend feasibility.
Updated 2026-07-29: added dispute-resolution capability (§4, "Copy summary") plus per-code timestamp and history log (§2) — resolves the "does marking a code redeemed require business confirmation" open item by adding an audit path rather than a confirmation requirement.
Depends on: `geodrop-product-spec-v1.md` §4.9 (redemption acceptance criteria, v1 done-state, non-goals), `experience-creation-flow.md` (this screen lives inside an experience, same as Drops).

## Design principle

Per §4.9's explicit v1 done-state, **the business validates a code manually against a founder-supplied list — no scanner, no merchant account, no business dashboard.** The moment at the counter (Explorer shows their code, business employee checks it against a paper or spreadsheet list) has no app or screen at all. This document is not that moment. It covers the *other* moment: after the fact, someone — the Organizer or founder — has to tell the system a code was actually used, so issued/redeemed counts stay accurate and queryable per business (a real acceptance criterion).

**Decided (2026-07-29): Organizer-only, no business-employee access.** A lightweight employee-facing tap-to-redeem view was considered and explicitly rejected — even a minimal version means a new access surface (who's allowed to mark codes for which business, how that access is granted/revoked, what stops misuse) that the spec's non-goals were written to avoid entirely, not just avoid the heavier scanner/dashboard version of. Revisit trigger is the same one already named in the spec: manual validation breaking down somewhere around 3–5 concurrent businesses.

## Primary action

Organizer marks a specific issued code as redeemed, based on information the business gave them out-of-band (a call, a text, a periodic check-in, an end-of-day tally) — and can see issued-vs-redeemed counts per business at a glance.

---

## Flow

### 1. Entry — Rewards / Redemption section

**Purpose:** a home for anything reward-drop-related within an experience, parallel to Drops.

- Reached from Experience Detail (alongside Drops and Settings, per the IA already established in `experience-creation-flow.md` §4)
- Only appears if the experience has at least one reward/perk drop authored — an experience with no business rewards shouldn't show an empty "Rewards" section cluttering the nav for the common case (per the same "don't add UI weight for the case that doesn't apply" principle used for the bounty disclosure row)

**Elements:**
- List of businesses with reward drops in this experience, each row showing: business name, issued count, redeemed count ("12 issued · 7 redeemed")
- Tapping a business row opens that business's code list (step 2)

**States:**
- *Empty (no reward drops at all):* this section doesn't render — see above, not a screen state so much as a navigation-visibility rule
- *Loading:* skeleton rows while counts load
- *Offline:* cached counts still viewable; marking a code redeemed while offline queues and syncs (see step 2's offline state)

### 2. Business Code List

**Purpose:** see every code issued for a specific business, and mark ones redeemed.

**Primary action:** mark a code as redeemed.

**Elements:**
- Business name as the screen header
- List of issued codes, each row showing: the code itself (needs to be visible and matchable against whatever the business reads off their own list — legible, not truncated), issue date, and status (Issued / Redeemed)
- **Each row also carries a redemption timestamp once marked** (not just a static "Redeemed" label) — date and time of the mark-redeemed action. Cheap to show since the system already has to record this to support the undo/correction action in §3; this just surfaces it rather than collapsing it to a bare status.
- **Full history per code, available on tap** (added 2026-07-29, resolving the dispute-handling open item below): every status change for a code — issued, marked redeemed, any later correction — shown as a simple timestamped log, e.g. "Issued 7/15 2:03pm → Marked redeemed 7/15 6:47pm → Corrected to not-redeemed 7/16 9:12am → Marked redeemed 7/16 9:15am." This costs nothing new to store (the system already tracks these events to support undo) — the addition is purely showing the trail instead of only the current state. This is what actually lets an Organizer investigate a specific disputed code rather than just restating "it says redeemed" back at a business that disagrees.
- Each unredeemed row has a clear action: "Mark redeemed"
- Sort/filter: unredeemed-first is the sensible default, since that's what the organizer is actually scanning for when reconciling with a business's report
- Search or filter by code — becomes genuinely useful once a business has more than a handful of codes, worth including even at pilot scale since the organizer is often working from a business's messy verbal or texted list and needs to find one specific code quickly, not browse

**Copy note — the confirmation matters here.** Marking a code redeemed is: "Mark this code as redeemed? This can't be undone." Single tap to confirm, no elaborate flow — but not a zero-friction single-tap-and-done action either, since redemption counts feed directly into "would this business pay again" (the entire reason this feature exists per §4.9's user story). An accidental mis-tap here quietly corrupts the one number the business actually cares about.

**States:**
- *Loading:* skeleton rows while codes load
- *Empty (no codes issued yet for this business):* "No codes issued yet — codes appear here once an Explorer unlocks a reward drop for this business." No dead end, just accurate expectation-setting.
- *Error (mark-as-redeemed fails):* "Couldn't update this code — try again." Standard retry pattern, consistent with every other save-failure copy across these authoring flows.
- *Offline:* the organizer can still mark codes redeemed while offline (this is very plausibly happening away from wifi, at a venue, mid-event) — the action queues locally and syncs once back online. Don't block this behind a connectivity requirement; per §4.9, "redemption marked used server-side" is the actual requirement, but the organizer's *tap* shouldn't have to wait for a live connection to register.

### 3. Undo / correction

**Primary action:** fix a code that was marked redeemed by mistake.

- A redeemed code isn't a dead end — tapping it offers "Mark as not redeemed" as a distinct, deliberate action, separate from the original one-tap confirm
- No time limit on this correction (unlike the drop-content edit-freeze mechanic elsewhere in this project) — a redemption-status correction doesn't have the same "someone already saw something different" risk a drop's content does; the only person affected by a status flip is the Organizer's own bookkeeping accuracy
- Copy: "Mark this code as not redeemed? Only do this if it was marked by mistake."

---

### 4. Dispute resolution — "Copy summary" (added 2026-07-29)

**Purpose:** give the Organizer a fast way to hand a business accurate numbers when their tallies don't match — resolved as a lightweight, out-of-band tool, not a business-facing feature. No new account type, no business login — the business still only ever interacts through a phone call, text, or in-person conversation with the Organizer, exactly as the rest of this flow assumes.

**Primary action:** copy a formatted, shareable summary of a business's codes to the clipboard.

**Why this shape, not a file export:** the realistic dispute is a business texting or calling the Organizer saying "our count doesn't match yours" — not a formal reporting relationship. A generated CSV or PDF is more polish than this actually needs; a copy-to-clipboard summary the Organizer can paste straight into a text message or email reply is faster and matches how the conversation is already happening. Considered and set aside: a full file export (real production cost for an audience of one business owner glancing at their phone), and a structured two-way comparison tool where the business submits their own list for automated diffing (genuine scope creep — a reconciliation feature, not a manual-validation feature, and not what §4.9 asked for).

**Elements:**
- Button on the Business Code List screen: "Copy summary"
- Tapping it copies plain text to the clipboard, formatted roughly as:
  > Aloha Shave Ice: 12 issued, 9 redeemed, 3 outstanding.
  > Redeemed: CODE1 (7/15, 2:03pm), CODE2 (7/15, 6:47pm), CODE3 (7/16, 9:15am)...
- Toast confirms the copy succeeded: "Summary copied — paste it into a text or email."

**States:**
- *Loading:* negligible — this is formatting already-loaded data, no separate fetch
- *Error:* if clipboard access somehow fails (rare, platform-dependent), fall back to just displaying the same text in a selectable text block the Organizer can manually copy: "Couldn't copy automatically — select and copy the text below."

---

## Accessibility

- Code text needs to be genuinely legible and copyable/selectable, not an image or icon-styled representation — the organizer may need to read it aloud on a phone call with a business owner reconciling a list
- "Mark redeemed" / "Mark as not redeemed" need clearly distinct labels from each other, not a single ambiguous toggle icon that requires interpreting current state to understand what tapping it will do

## Known scale ceiling (same pattern as the other authoring docs)

This flow assumes an Organizer manually reconciling a modest number of codes against a business's own report, occasionally, for one or a handful of businesses. This is explicitly not built to survive many concurrent businesses or high code volume — that's the same ceiling already named in `geodrop-product-spec-v1.md` §4.9 itself ("manual validation breaks somewhere around 3–5 concurrent businesses"), not a new one this document is introducing. Worth Product tracking alongside the other manual-process ceilings already flagged in `organizer-access-request-flow.md`.

## Open items needing sign-off before this is final

- **How the Organizer actually receives the business's report** (phone call, text, in-person, a shared spreadsheet) is genuinely out of scope for this app to design — flagging only so nobody assumes a missing "business reports codes" feature is an oversight; it's deliberately outside the product per the non-goals in §4.9
