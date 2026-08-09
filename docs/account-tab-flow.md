# Account Tab

Session date: 2026-07-29
Status: drafted, not yet reviewed against Legal (data deletion/export) or Backend feasibility.
Resolves the open item flagged in `top-level-navigation.md`: "Account tab itself has no other detailed spec beyond permission status and organizer entry point."
Depends on: `top-level-navigation.md` (this is that doc's third tab), `organizer-access-request-flow.md` (surfaces its states here), `geodrop-wireframe-spec-v1.md` §2.5a/b/c (the permissions this screen lets someone revisit).

## Design principle

This is the least-visited tab in the app, and that's fine — it doesn't need to compete with Map or Collection for attention or polish. Its job is to be the reliable, boring place where account-level things live: permissions, the Organizer door, and the handful of account-lifecycle actions (sign out, delete account) every app needs somewhere. Nothing here should try to be engaging: no achievements, no stats celebration, no personality-forward copy. Matches the same "organizer-facing tooling is neutral/professional" register already established for B2B surfaces in this project, extended here because account settings is a utility, not a moment.

---

## Screen contents

### 1. Identity block (top)
- Whatever minimal identity exists for the account (per Branch A/B in `geodrop-wireframe-spec-v1.md` — an email/phone if account-created, or a plain "Guest" label with an upgrade prompt if still on a device-bound guest session per Branch B's soft-upgrade flow)
- If guest: this is a natural second surface for the upgrade prompt (in addition to the post-unlock soft prompt in 1.4) — someone who dismissed the in-the-moment prompt might come here later deliberately wanting to upgrade. Same non-urgent framing as that screen: no guilt-trip copy, just an available action.

### 2. Permissions
- Location and push status, each shown plainly (Granted / Not granted) with a direct link to the OS settings screen for that permission if denied — per `geodrop-wireframe-spec-v1.md`'s existing note ("You can turn this on anytime in Settings if you change your mind"), this is where that mention actually resolves to something tappable, not just a promise made in copy elsewhere with no way to act on it.
- No re-priming screens shown here — someone visiting Account already knows what they're turning on; the priming copy (2.5a/b/c) belongs to the in-context moment, not repeated here redundantly.

### 3. Organizer entry point
- If not yet an Organizer: "Become an Organizer" or similar, leading into `organizer-access-request-flow.md` Screen 1
- If application is under review or not approved: shows that flow's Screen 2 or 3b state directly, per `top-level-navigation.md`'s existing note
- If approved: "Organizer tools" entry point leading to the Experiences list

### 4. Joined experiences (added 2026-07-29)
- A plain list of every experience the account has ever joined, past and present — this is the natural home for reviewing "what have I been part of" outside the Map tab's switcher (which is about *active* experiences, not history)
- Each row: experience name, organizer name, date range, and whether it's still active or has ended
- Not required for the Map tab's switcher to function — that's a separate, lighter-weight control (per `top-level-navigation.md`). This is the fuller, browsable version for someone who wants to look back, not switch context mid-task.

### 5. Account actions
- **Sign out** — standard, no special design needed
- **Delete account** — flagged below as needing real thought, not skipped

---

## Account deletion (needs real attention, not a placeholder)

Nothing in this project has settled data deletion or export commitments yet — this is genuinely open territory, not something already decided elsewhere that this screen just needs to reflect. Rather than silently picking a shape for it, here's what's actually at stake:

- **Baseline expectation, regardless of specific legal requirements:** most app store review guidelines (and general good practice) expect some self-serve account deletion path to exist, not just "contact us to delete your account." This isn't a GeoDrop-specific legal question so much as a baseline most published apps meet.
- **What deletion actually needs to handle here, specifically:** an Explorer's Collection contains snapshotted drop content (per the edit-freeze mechanic in `drop-authoring-flow.md` §7) and possibly redemption codes tied to real-world rewards a business may still owe them. Deleting an account mid-experience, with an unredeemed code sitting in Collection, is a genuinely different situation than deleting an account with nothing outstanding — this doc doesn't resolve that tension, just names it.
- **Recommended minimum for v1, pending Legal:** a "Delete account" action reachable from here, with a confirmation step that plainly states what's lost ("This removes your account and everything in your Collection. This can't be undone.") — not a silent, instant action, but also not requiring a support ticket or email round-trip for something this baseline.

**This is flagged as a real open item below, not quietly designed around** — the specific mechanics (grace period before permanent deletion? immediate? what happens to an outstanding redemption code?) are Legal/Product territory this document can't close alone.

---

## What this resolves

- Closes the "Account tab has no detailed spec" item from `top-level-navigation.md`.
- Gives the permission-revisit promise made in `geodrop-wireframe-spec-v1.md` an actual destination.
- Gives the organizer-access-request flow's ongoing states (pending/approved/not-approved) a consistent, discoverable home, as already decided in the nav doc.

## Open items needing sign-off before this is final

- **Account deletion mechanics** (grace period vs. immediate, handling of outstanding redemption codes/unfinished bounties at time of deletion) — genuinely unresolved, needs Legal and Product input, not a Design-only call.
- **Data export** — not designed here at all. Flagging that this is a real gap (common baseline expectation, e.g. "download your data") rather than an oversight being silently skipped; needs a decision on whether it's in scope for v1 or explicitly deferred with a named reason, same pattern as other parked items in this project.
- **Language/locale settings** — out of scope for this pass; `SKILL.md`'s localization-readiness note applies to layout/text-expansion concerns, but an actual in-app language switcher (if the app ever supports more than one language) isn't designed here.

## Known scale ceiling

None specific to this screen — it's low-traffic by nature and doesn't carry the same "gets unwieldy past N items" risk the authoring/redemption flows do. The "Joined experiences" list (§4) could theoretically grow long for a very active Explorer over time, but that's a distant-future concern, not a pilot-scale one.
