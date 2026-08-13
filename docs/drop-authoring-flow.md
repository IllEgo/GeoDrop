# Drop Authoring Flow — Base (Text / Photo Drop)

Session date: 2026-07-29
Status: drafted, not yet reviewed against Backend feasibility or founder walkthrough-timing.
Updated 2026-07-29: corrected drop-count assumption (was 10–40, now 8–20 per original vision); confirmed authoring model is ongoing/ add-anytime, not all-at-once — see new §7 (Editing Live Drops) and §8 (Scheduled Drops).
Depends on: `geodrop-product-spec-v1.md` §4.3–4.5, `bounty-organizer-authoring-flow.md` (this is the screen that flow's bounty disclosure row attaches to).

> **R0 alignment override (approved 2026-08-09; see
> `redesign-alignment-proposal.md`):** Pilot 1 keeps immediate publish,
> configurable radius, expiration, text/single-photo authoring, edit, and delete. Scheduled
> publishing (§8) is deferred. The 30-minute retroactive edit model (§7) is superseded by
> an immutable snapshot at unlock; later edits affect future unlocks only and carry an
> Edited marker. Basic cloud drafts are conditional on the timed venue walkthrough proving
> interruption is a blocker. Every claim below that an authoritative write queues offline
> is superseded for Pilot 1: cached reads may work offline, but publish/edit/delete/reward
> mutations fail clearly and retry online.

## Design principle

This is the highest-frequency screen an Organizer touches, and it's timed — under 3 minutes end to end is a go/no-go input for the whole pilot, not a nice-to-have. Every design decision here is in service of that budget. If a choice makes the screen more complete but slower, it's the wrong choice for v1.

The known risk this screen sits on top of: authoring 8–20 drops on a phone is the largest unpriced cost in the current plan, and it's one of the six named reasons the whole product could need repositioning. This spec can't solve that on its own — it can only avoid making it worse, and flag where the ceiling is.

**Authoring model: ongoing, not all-at-once.** An experience is not required to be fully built before it goes live — an Organizer can publish an experience with a handful of drops and add more later, including after Explorers have already joined and started exploring. This has downstream effects: it's why push trigger (b) "new drop added to a joined experience" exists at all (product spec §3.2), it's why the bounty flow's mid-event-edit behavior matters, and it's why editing an already-collected drop (§7 below) is a real scenario rather than an edge case.

## Primary action

Author one drop: a location, a radius, a title, body text and/or one image, and an optional expiry. One drop, one screen, one save. Bounty attachment is a collapsed add-on (already spec'd separately) — it does not add a step to this flow for organizers not using it.

---

## Flow

### 1. Entry — "Add a drop"

**Primary action:** start authoring a new drop.

- Reached from the experience's drop list (organizer-side list view, not the Explorer map)
- Single button: "Add a drop" — no submenu, no "choose a type" step. Text/photo is the only v1 type, so don't make the organizer pick from a list of one.

### 2. Placement

**Primary action:** set the drop's location.

Two methods, both first-class — not one primary and one buried fallback:
- **"Drop a pin"** — map view, tap to place, draggable after placement to fine-tune
- **"Use my location"** — one tap, uses device GPS directly. This is the one that'll actually get used on a venue walkthrough (per acceptance criteria) — an organizer standing at the spot shouldn't have to find themselves on a map first.

Below the map/location control:
- **Radius** — numeric stepper or slider, default 25m, editable per drop (never hardcoded, never hidden as an "advanced" setting — this is a real per-drop decision given GPS drift varies by venue). Label reads "How close someone needs to be" rather than "radius" — organizers aren't GPS engineers.
- Small inline note under the radius control: "Most venues work well around 25m. Tighter spacing (under 30m between drops) can cause overlap." — this is the ≥30m-apart requirement from the pilot-site criteria, surfaced at the moment it's actionable, not buried in a separate help doc.

**States:**
- *Loading:* map tiles/skeleton while location resolves
- *Permission-denied (device location, for "use my location"):* falls back to manual pin-drop, doesn't dead-end — "Turn on location to drop a pin at your exact spot, or place it on the map instead."
- *Offline:* map tiles may not load; pin-drop by coordinates still works if a cached map tile is available, otherwise: "Map needs a connection to load — try again once you're online, or use your location once you have signal."

### 3. Content

**Primary action:** write the title and body, and/or attach one image.

- **Title** — single line, required
- **Body text** — required if no image attached; optional if an image is attached (a photo drop can be image-only with just a title)
- **One image** — upload or camera capture directly from this screen (organizers are often standing at the spot with their phone already out — capture-in-flow beats "go to camera app, come back, attach")
  - Compression happens automatically on upload, no organizer-facing quality slider — venue wifi is assumed bad, LTE at best, so this can't be a manual step, it has to just happen
  - Size cap enforced with a clear message if exceeded, not a silent failure: "That photo's a bit large — we've compressed it to keep things fast for guests on the day."
  - No multiple images in v1 — if an organizer tries to add a second, the message should name it as a v1 limit, not sound like a bug: "One photo per drop for now — you can always add a second drop nearby if you want to show more."

**States:**
- *Loading:* image upload progress, inline, not a blocking modal — organizer can keep typing the title/body while it uploads
- *Error (upload fails):* "That photo didn't upload — your text is saved either way. [Retry]" — never lose the text content because an image failed
- *Offline:* text fields still work and save locally as a draft; image upload queues and completes once back online, communicated plainly: "Saved — the photo will finish uploading once you're back online."

### 4. Expiry (optional)

**Primary action:** decide whether this drop expires, and when.

- Collapsed by default, same disclosure pattern as the bounty row: **"Set an expiration"**
- If expanded: date/time picker, plus a shortcut to inherit the experience-level default if one's already set ("Use the experience's end time" as a single tap, rather than re-entering the same date per drop)
- Copy note if a drop has already been collected by anyone before expiry hits: not shown here, but worth the Organizer knowing expiry doesn't claw back what's already earned — a one-line note under the control covers it: "Guests who already found this keep it in their collection even after expiry."

### 5. Review / Publish

**Primary action:** publish the drop, or save it as a draft.

**Design note — draft state reinstated.** The realistic failure mode isn't hesitation about one drop, it's interruption during a batch: an organizer walking a venue authoring 15 drops in a row gets pulled away mid-way and shouldn't lose partial work or feel pressured to fully finish each one before moving on. A draft here costs nothing against the 3-minute budget — it's not an extra decision organizers are forced to make, it's a safety net for the session getting cut short, which is the actual shape of "authoring by phone, standing outdoors."

- Single summary view: location (small map thumbnail), radius, title, content preview, expiry if set, bounty status if attached (from the separate flow)
- Two buttons, visually distinct (publish is primary, draft is secondary — not equal weight, since publish is still the goal state):
  - **"Publish"** — drop goes live immediately, visible to Explorers
  - **"Save as draft"** — drop is saved but not visible to Explorers, stays in the drop list under a clearly separate "Drafts" section/filter
- A drop only needs a location to be saved as a draft — title/body/image can be genuinely incomplete. This matters for the walkthrough use case: an organizer might drop 15 pins in a row (location-only) while walking the venue, then come back later at a desk to write titles and content for each.
- Publish validates for completeness (location + radius + title, and either body or image) before going live — a draft has no such requirement. If an organizer taps "Publish" on an incomplete drop, don't block silently: "This needs a title before it can go live — want to save it as a draft instead?" turns a dead end into a redirect.
- Edit is always available afterward from the drop list — publishing isn't a one-way door, and neither is drafting; a draft can be published later once completed, no re-entry of already-saved fields.

**States:**
- *Loading:* publish or draft-save in progress, relevant button shows spinner, both disabled to prevent double-submit
- *Error:* "Couldn't save — your content is still here, try again." (works for both publish and draft failures; matches the reassurance pattern already established in the bounty flow's error state — organizers should never wonder if they lost their work)
- *Success (publish):* returns to the drop list, new drop visible immediately under the live/published section, lightweight confirmation (toast, not a full-screen "Congratulations")
- *Success (draft):* returns to the drop list, new drop visible under Drafts, toast confirms: "Saved as a draft — you can finish this anytime."
- *Offline:* both publish and draft-save queue locally and sync once back online — this is the same interrupted-session scenario, just caused by connectivity instead of attention. Don't force a draft to stay pending only in memory; treat it as saved the moment the organizer taps the button, sync silently after.

### 6. Edit / Delete / Drafts (post-save)

**Primary action:** modify, remove, or complete a drop already saved (published or draft).

- The organizer's drop list is split into two clearly labeled sections: **Published** and **Drafts** — not visually merged, since they're different states an organizer needs to scan for different reasons ("what's live for guests" vs. "what do I still need to finish")
- Tapping any drop, published or draft, reopens this same authoring screen, pre-filled
- A draft row shows what's missing at a glance, not just a generic "Draft" label — e.g. "Draft — needs a title" or "Draft — needs content" — so an organizer batch-finishing drops later can prioritize without opening each one to check
- Delete is a separate, deliberate action for either state (not just "clear all fields and save empty") — confirmation required. Wording differs slightly by state:
  - Published: "Delete this drop? Guests who've already found it keep it in their collection — this only removes it from the map going forward."
  - Draft: "Delete this draft? Nothing was published, so this can't be undone." (lower stakes copy — no guest has seen it, so the warning is simpler)
- If the drop is a bounty anchor or member, deletion here should surface the same warning as the bounty flow's anchor-deletion constraint (blocking or confirming, per whatever Product/Backend lands on for that) rather than silently orphaning the relationship

---

## 7. Editing Live Drops (added 2026-07-29)

This section exists because the authoring model is ongoing, not all-at-once (see Design principle above) — an Organizer can and will edit a drop after it's already been published, and possibly after someone's already collected it. Three distinct edit scenarios, each with a different answer:

**A. Editing a draft.** No restriction — nothing's been seen by anyone, edit freely, same as originally spec'd in §6.

**B. Editing a published drop nobody has collected yet.** No restriction — same as A in practice, since there's no collector experience to protect.

**C. Editing a drop at least one Explorer has already collected.** This is the real design question, and it resolves as a **30-minute grace window**:

- **Within 30 minutes of publish (or of the drop's last edit):** the Organizer can edit freely — title, body, image, anything. Changes propagate immediately to anyone who already collected it, updating their Collection view too. This covers the realistic case of catching a typo or mistake shortly after publishing.
- **Once 30 minutes have passed:** the drop's content freezes for everyone who has already collected it. That collection entry never changes again, regardless of what the Organizer does afterward. Matches the existing expiry principle already established in §4: "you don't take back what someone earned" — the same logic extends to "you don't rewrite what someone earned," once enough time has passed that it's no longer a same-minute correction.
- **An edit made after the 30-minute window creates a new version going forward only.** Anyone who collects the drop *after* that edit sees the new version. Anyone who already collected it before the edit keeps what they originally got. The Organizer is not blocked from editing after the window — they just can't retroactively change what past collectors received.

**Why 30 minutes, and why this matters more than it might look:** collecting a drop is the core reward mechanic, not a passing comment — it's the thing someone physically walked somewhere to earn, and people may screenshot or share it as proof of what they found. An unrestricted, always-live edit model creates a bait-and-switch risk directly analogous to the "edit a viral post into something offensive after it's been reposted" problem seen on platforms that allow silent full-content edits. A short grace window preserves the ability to fix an honest mistake without leaving open a window to swap content once a drop has had time to spread.

**Visible edit indicator — always shown, even within the grace window.** Unlike Reddit's fully-silent-then-asterisk pattern (acceptable for a low-stakes comment), a drop always carries a quiet "edited" marker the moment it's changed post-publish, even inside the 30-minute window. No diff view, no "originally said," just a small honest signal. This is a deliberate departure from the Reddit precedent, chosen because collecting a drop is higher-stakes than reading a comment — the goal is that nobody's later confused about why their memory of a drop doesn't match what someone else has, even for an edit made in good faith one minute after publishing.

**Implementation note, not a UI change:** this requires the Collection view (2.6 in `geodrop-wireframe-spec-v1.md`) to store a snapshot of the drop's content at the moment of collection, not a live reference to the current drop record, once the 30-minute window has closed. Flagging for Backend — this is a real data-modeling requirement, not just a display rule.

**Needs sign-off:** the 30-minute figure itself is a judgment call (recommended by Design, agreed in review), not derived from any existing spec number — Backend/Product should confirm it's workable given whatever infrastructure handles drop versioning.

---

## 8. Scheduled Drops (added 2026-07-29)

Newly in v1 scope — previously listed as a non-goal in `geodrop-product-spec-v1.md` §4.5 ("scheduled future publish"), reopened now that the ongoing authoring model makes it a natural fit rather than an extra.

**Primary action:** set a drop to become visible/unlockable at a future time, rather than immediately on publish.

- Added as an option alongside expiry in §4 (Expiry), not a separate flow — both are "when is this drop live" controls and belong together on the same screen rather than splitting time-related settings across two places.
- Collapsed disclosure, same pattern as expiry and bounty: **"Schedule this for later"**
- If expanded: date/time picker for a **start time**, distinct from the existing expiry (**end time**) control. An Organizer can set either, both, or neither.
- A scheduled-but-not-yet-live drop behaves like a draft from the Explorer's side (invisible, not on the map) but like a published drop from the Organizer's side (appears under Published, not Drafts, with a clear "Scheduled — goes live [date/time]" status) — this distinction matters because the Organizer has already finished and committed the content; it's just not time yet. Folding it into "Drafts" would misrepresent it as incomplete.
- Publish validation (§5) still applies at the moment of scheduling — a scheduled drop must be complete (title, and body or image) before it can be scheduled, same bar as immediate publish. No scheduling an incomplete drop "to finish later" — that's what drafts are for.
- Going live at the scheduled time is a server-side transition, not something the Organizer needs to be present for or manually trigger.

**Use case this directly supports (per discussion):** an Organizer wanting to introduce a new element mid-event — a surprise drop that appears partway through, timed to add a moment of excitement rather than being available from the start. This is now a first-class supported pattern, not a workaround.

**States:**
- *Loading:* same as expiry's date/time picker
- *Error (start time after end time):* "This needs to go live before it expires — check the times." Inline, not a dead-end save failure.
- *Offline:* same as other authoring fields — saves locally, syncs when back online; the scheduled time itself is unaffected by connectivity since it's evaluated server-side.

**Interaction with push (product spec §3.2, trigger b):** a drop going live on schedule should fire the same "new drop added to a joined experience" push as a drop published manually — no special-cased exemption for scheduled drops. From an Explorer's perspective, a drop appearing is a drop appearing, regardless of what caused it to appear.

**Needs sign-off:** Backend feasibility of server-side scheduled transitions and whether this interacts with the existing expiry enforcement mechanism, since both are now time-gated server-side checks on the same drop.

---

- Map pin-drop needs a non-map fallback for placing/adjusting location precisely (coordinate entry or "use my location" satisfies this — never make precise placement map-only)
- Radius stepper needs to be operable via screen reader with clear value announcements ("25 meters," not just a slider position)
- Image upload button and camera capture need clear labels distinct from each other, not one ambiguous "add photo" icon
- Photo preview needs alt-text entry — a body-text-only drop doesn't need this, but an image-bearing drop should let the Organizer add a short description for Explorer-side screen readers

## Known scale ceiling (flagged, same note as the bounty doc)

This flow is designed for authoring one drop at a time, carefully, in under 3 minutes. It is not designed for bulk authoring, templates, or duplicating a drop with minor edits. Drafts (§5–6) solve the *interruption* problem — an organizer can drop 15 pins on a walkthrough and fill in content later without losing work — but they don't solve the *total time* problem. Fifteen drops still means fifteen passes through titles, content, and images eventually, whether that happens in one sitting or several. That's fine at pilot scale (8-20 drops) but is exactly the cost the product spec names as the biggest unpriced risk — if a founder or organizer is seeding 15+ drops by hand and it's taking hours in aggregate, that's the signal, not a UI polish gap. Worth timing honestly during the first real walkthrough, per the spec's explicit instruction to measure this rather than estimate it.

## Open items needing sign-off before this is final

- Whether "use my location" requires the Organizer to be standing within the radius they're setting, or just drops a pin at their current GPS position regardless of radius — I've assumed the latter (radius is independent of how the pin was placed), but worth confirming with Backend since it affects the placement UI copy
- **30-minute grace window for editing collected drops (§7)** — figure is a judgment call, needs Backend/Product confirmation it's workable
- **Drop-content snapshotting for the Collection view (§7)** — real data-modeling requirement (freeze content at collection time once the grace window closes), not just a display rule; needs Backend design
- **Scheduled drops (§8)** — needs Backend feasibility confirmation, and confirmation that it doesn't conflict with the existing server-side expiry enforcement mechanism
