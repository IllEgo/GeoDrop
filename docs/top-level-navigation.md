# Top-Level App Navigation

Session date: 2026-07-29
Status: drafted, not yet reviewed against Backend/Frontend feasibility.
Resolves the open item flagged in `bounty-mechanic-design.md`: "Where the overall Collection screen sits in the app's top-level navigation."
Updated 2026-07-29 (second pass): added code-entry join path (1.0b in `geodrop-wireframe-spec-v1.md`) and confirmed multi-experience support — Map tab shows one active experience with an explicit switcher, Collection combined across all joined experiences by default.
Depends on: every Explorer-side and Organizer-side flow doc in this project — this is the structural skeleton they all connect to.

> **R0 alignment override (approved 2026-08-09; see
> `redesign-alignment-proposal.md`):** the participant tabs are **Nearby**,
> **Collection**, and **Account**; every reference below to a top-level Map tab means Nearby.
> One active Experience is shown at a time while Collection combines all joined
> Experiences. Approved organizer tools remain an Account entry, never a fourth tab.
> "Explorer" is internal terminology and must not label the participant in UI.

## Design principle

**One shared shell, additive by account type — not two separate apps, and not a full nav swap.** Explorer nav is the constant base every account has, since every account starts as an Explorer (per the access-request flow's own framing — Organizer is an upgrade granted to an existing account, not a separate signup). An approved Organizer gains one additional entry point into Organizer tools; they don't lose Explorer nav in exchange for it.

This matters concretely for the pilot: the founder is very likely both the Organizer running an experience *and* an Explorer testing their own event on the same device/account. A design that swapped Explorer nav away entirely the moment Organizer approval landed would make that basic testing workflow clunky — bouncing between two disconnected shells rather than one app with an extra door.

---

## Explorer nav (the constant base, every account)

**Bottom tab bar, three items:**

1. **Map** (default/home tab) — the core loop screen (2.1 in `geodrop-wireframe-spec-v1.md`). This is where the app opens to by default for a returning user with an active joined experience.
2. **Collection** — drops + stamps sub-section (per the IA already resolved in `bounty-mechanic-design.md`: Collection is the primary destination, Stamps nest inside it as a compact card, not a co-equal tab). Carries the "new since last visit" presence dot when applicable (already spec'd in that same doc).
3. **Account** — not previously spec'd anywhere in this project; covers device/account settings, permission status (location/push — lets someone revisit a permission they denied earlier without digging through OS settings blindly), and the entry point into Organizer access request if not yet an Organizer (see below).

**Report/Block does not get a tab** — correctly stays reachable from within a drop's detail view only, per its own already-spec'd "≤2 taps from any drop" requirement. Giving it top-level nav weight would overstate how often it's used and clutter the bar for the 99% of sessions that never need it.

**What happens if someone hasn't joined any experience yet:** Map tab shows a state prompting them to scan a QR or enter an event code (`geodrop-wireframe-spec-v1.md` screens 1.0 / 1.0b), rather than an empty map with nothing to look at.

**Joining and switching between multiple experiences (added 2026-07-29):** an Explorer can be joined to more than one experience at a time — confirmed, not an edge case to design around defensively. This shapes the Map tab as follows:

- **One active experience shown at a time, not an overlay of all joined experiences on one map.** Showing multiple experiences' drops simultaneously would be confusing (unclear whose drops are whose, why one might be towns away) and sits awkwardly against the location-privacy-conscious approach used everywhere else in this project — a combined multi-experience map is a meaningfully bigger, messier surface than anything else spec'd so far, for no real benefit over switching.
- **Switcher lives on the Map tab itself**, not as a separate nav item — a header showing the current experience's name, tappable to reveal a list of all joined experiences (plus "Join another" leading to 1.0b's code-entry screen, or a QR-scan shortcut). This keeps "switch" and "join new" in the same natural place, and avoids adding nav weight for something that, like Organizer tools, most sessions won't need (most people are probably only ever in one experience at a time in practice, even though more than one is supported).
- **Joining a new experience while already in one doesn't require leaving the first** — the newly joined experience becomes available in the switcher; the previously active one stays exactly as it was, nothing about it changes or gets displaced.
- **Collection is combined across all joined experiences by default**, not filtered to whichever experience is currently active on the Map tab. Each entry is labeled with which experience it came from (name + rough date), so someone's Collection reads as their whole Kithe history rather than a series of disconnected per-event lists that reset every time they switch context. The sort modes already spec'd in `bounty-mechanic-design.md` (Newest first / Oldest first / Bounties first) apply across this combined view — none of them needed redesigning to accommodate multiple experiences, they already sort by properties (date, bounty membership) that work the same way regardless of source experience.

---

## Organizer nav (additive, only for approved Organizer accounts)

**One additional entry point from the Account tab, not a fourth tab bar item:** "Organizer tools" (or similar), shown only once `organizer-access-request-flow.md`'s Approved state has been reached. Tapping it leads to the Experiences list (top of `experience-creation-flow.md`'s IA — Create an experience / existing Experiences list).

**Why an entry point and not a fourth tab:** at pilot scale, one person is the Organizer for effectively one or a handful of experiences — this isn't a mode someone lives in all day the way Map or Collection is for an Explorer. A permanent fourth tab would give Organizer tools equal visual weight to core Explorer functions for an audience where almost nobody has Organizer access at all. This mirrors the same "don't build UI weight for the case that doesn't usually apply" principle already used for the bounty disclosure row and the Rewards section in `redemption-tracking-flow.md`.

**Everything inside Organizer tools is already spec'd and nests correctly under this one entry point:**
- Experiences list → Experience Detail → Drops (`drop-authoring-flow.md`) / Rewards (`redemption-tracking-flow.md`) / Settings (`experience-creation-flow.md` §4)
- Bounty/anchor setup nests inside Drops, per `bounty-organizer-authoring-flow.md`
- No separate Organizer analytics destination — stays the founder-produced report, per the standing decision, not an in-app section

**Account with a pending or not-approved organizer application:** the Account tab shows the relevant state from `organizer-access-request-flow.md` (Screen 1, 2, or 3b) in place of live Organizer tools — this is really just that flow's existing states, surfaced from a consistent, discoverable location (Account tab) rather than a link buried in settings as the original doc's "low-key link in account settings" phrasing suggested. Slight refinement, not a contradiction: the entry point is still low-key (nested in Account, not a top-level tab), just consistently placed.

---

## What this resolves

- Closes the open item in `bounty-mechanic-design.md` — Collection is a top-level tab, Stamps nest inside it, confirmed.
- Gives Organizer tools a real, consistent home without inflating their importance in the nav relative to how rarely most accounts will use them.
- Confirms an Organizer never loses Explorer capability — both are always reachable from the same shell.

## New open items surfaced by this doc

- ~~No code-entry fallback screen exists for joining an experience without scanning a QR~~ — **resolved 2026-07-29**, see `geodrop-wireframe-spec-v1.md` screen 1.0b (Enter Event Code).
- ~~Multiple simultaneously joined experiences~~ — **resolved 2026-07-29:** confirmed possible. One active experience at a time on the Map tab with an explicit switcher; Collection combined across all joined experiences by default, each entry labeled with its source experience. See the "Joining and switching between multiple experiences" subsection above.
- ~~Account tab itself has no other detailed spec~~ — **resolved 2026-07-29**, see `account-tab-flow.md`.

## Needs sign-off

- Whether "Account" is the right label/icon for that third tab versus something like "You" or "Profile" — naming call, not decided here.
- Confirm with Product that Organizer tools genuinely shouldn't be a fourth tab even post-pilot, if the studio ever supports many concurrent Organizers on one device — the "additive entry point, not a tab" reasoning above is scoped to pilot-era usage patterns specifically.
