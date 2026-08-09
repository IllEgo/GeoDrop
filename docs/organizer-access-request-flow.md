# Organizer Access Request Flow — Approval Gate

Session date: 2026-07-29
Status: drafted, not yet reviewed against Legal (ToS/liability language at request time) or Backend feasibility.
Updated 2026-07-29: reconciled with the draft state added to `drop-authoring-flow.md` — see new subsection under "Get started" behavior, below Screen 3a.

## Design principle

Open authoring (anyone, anywhere) is not in scope. Organizer accounts are gated behind a request → human review → approve/deny process. No ID verification — vetting is reputation-based (org/business name, contact info, brief description, ToS agreement), not identity-based, at current pilot scale. This is a **pure gate**: no sandboxed/trial organizer tier. Applicants get zero organizer capability until approved.

Review is manual (founder/partner), asynchronous, and expected to take a few business days. The flow is designed around that reality — no fake progress indicators, no promises the review process can't keep.

## Context this replaces

Two options were considered and rejected:
- **ID verification at request time** — rejected. Introduces biometric/identity-data regulatory surface (retention, deletion obligations) disproportionate to pilot-stage risk. Reputation-based vetting plus a moderation queue that already exists for approved-organizer content is judged sufficient at this scale.
- **Sandboxed/trial organizer account** (full creation tooling, unpublishable until approved) — rejected for now. Solves a "dead time during review" problem that doesn't exist yet, since v1 creation is largely founder-built-on-organizer's-behalf rather than true self-serve. Revisit if/when review times lengthen or creation tooling goes self-serve.

## Flow

**Entry point**
- In-app: low-key link in account settings — "Request Organizer Access." Not a growth CTA; this is B2B intake, not an Explorer-facing funnel.
- Leads to an in-app explanation screen, which hands off to an external form (Google Form for pilot 1 — no dedicated marketing website in scope for this flow).

**1. Request Organizer Access (in-app, pre-application)**
- Explains what an organizer account is for, states that every account is reviewed, sets expectation on timing ("a few business days," not a false-precision estimate), links out to the external application form.
- Primary action: Continue to application form.

**2. Application Pending / Under Review (in-app)**
- Shown after the applicant returns from the external form, and any time a pending applicant revisits this section.
- States: submission date, confirmation that email is the primary notification channel, a low-friction contact link for questions.
- No progress bar, no percentage, no ETA countdown.

**3a. Approved (in-app + email)**
- Email is the primary notification (don't assume the applicant is watching the app).
- In-app: short, restrained confirmation banner on next login — "You're approved as an Organizer" + entry into organizer tooling. Not a celebratory moment; see Voice note below.

### What "Get started" actually lands on (added 2026-07-29)

This flow originally stopped at the "Get started →" button without specifying its destination. Base authoring (`drop-authoring-flow.md`) now has a Published/Drafts split, which makes this a real gap rather than an implementation detail — a freshly approved Organizer with zero drops needs an honest first screen, not an assumed populated dashboard.

- **First landing is the drop list, correctly empty.** No experience, no drops, no drafts exist yet — the list should say so directly rather than showing a generic loading or blank state: "No experiences yet. Create your first one to start adding drops." Matches the same "explain, don't just show nothing" principle used throughout the Explorer-side empty states.
- **This does not require a tutorial or onboarding sequence layered on top.** Per the general working agreement against unnecessary interstitials — a newly-approved Organizer should be able to go straight from "Get started" to creating their first experience and first drop, with the empty state itself doing the explaining, not a separate walkthrough.
- **No seeded/example content.** Do not pre-populate a sample drop or demo experience to make the screen feel less empty — that risks the Organizer mistaking placeholder content for something real, and any confusion here is worse than a plainly-worded empty state.
- This reconciliation only touches the very first screen after approval — everything downstream (creating an experience, then a drop, then optionally a bounty) is already covered by `drop-authoring-flow.md` and `bounty-organizer-authoring-flow.md` and doesn't need restating here.

**3b. Not Approved (in-app + email)**
- Generic decline: no specific reason given.
- No automated "reapply" action. Path back in (if any) is "contact us" — reapplication, if ever allowed, is a deliberate human/policy decision, not a default retry loop.
- Email and in-app copy must match in tone/warmth — same institution either way.

## Copy (draft, not final — Legal review needed on request-form field language and ToS-agreement wording)

**Screen 1:**
> **Organizer accounts**
> Organizer accounts let you build and manage GeoDrop experiences for your event, business, or organization — placing drops, tracking engagement, and issuing rewards.
>
> Every organizer account is reviewed before approval. This helps keep GeoDrop's content trustworthy for everyone using it.
>
> Review typically takes a few business days. We'll follow up by email either way.
>
> [Continue to application →]

**Screen 2:**
> **Your application is under review**
> Submitted [date]. We'll email you at [email] once a decision is made — no need to check back here, but you're welcome to.
>
> Questions in the meantime? [Contact us]

**Screen 3a:**
> **You're approved as an Organizer**
> You can now create and manage experiences. [Get started →]

**Screen 3b:**
> **Application not approved at this time**
> Thanks for your interest in GeoDrop. We're not able to approve this application right now.
>
> If you have questions, you can [contact us].

## Voice note

This flow sits on the B2B/organizer side of the product. Per APP-BRIEF §8, organizer-facing tooling is a deliberately separate tonal register — neutral and professional, not the Explorer-facing voice (warm, low-key, dry). Approval/decline copy above follows that register: no personality performance, no over-celebration on approval, no false warmth softening a decline.

## States

- **Default (not yet applied):** Screen 1.
- **Pending:** Screen 2. Reachable directly (skip Screen 1) if the applicant already has a request in flight.
- **Approved:** Screen 3a, then normal entry into organizer tooling thereafter — this state doesn't persist as a distinct screen after first view.
- **Not approved:** Screen 3b, terminal unless/until a human-driven contact-us path reopens it.
- **Already an organizer:** entry point to this whole flow should be unreachable/hidden, not shown-and-disabled.

## Open items — need sign-off before build

- **Legal:** exact ToS/liability language collected at application time (org name, contact, description, agreement — no ID upload). Confirm this is sufficient without identity verification.
- **Legal/Product:** whether reapplication is ever permitted, and if so, under what constraints (cooldown period, changed circumstances, etc.) — not designed here; current flow assumes no automated reapply path.
- **Product:** confirm Google Form (vs. a dedicated website/landing page) is acceptable for pilot 1. No new website build is proposed as part of this flow.
- **Backend:** account/role model should support this as a status field (e.g. `pending` / `approved` / `not_approved`) rather than a hardcoded boolean, consistent with the earlier flag that org/account-type decisions are one-way doors.

## Known scale ceiling (flagged, not a v1 problem)

Manual review doesn't scale linearly with app growth the way drop moderation does (which has a defined 24h turnaround assumption). An applicant queue could outpace the founder's review capacity well before drop-moderation volume becomes a problem. Worth Product tracking application volume as a named metric alongside the existing manual-measures list (authoring cost, organizer support load) in PROJECT-STATE.md, rather than discovering the bottleneck live.

**Related ceiling, same underlying shape:** `drop-authoring-flow.md` and `bounty-organizer-authoring-flow.md` both flag their own manual-scale limits (authoring time per drop; flat multi-select lists past ~40 drops). None of these three ceilings are urgent at pilot scale, but they're the same risk wearing three outfits — manual, founder-mediated process that works fine at n=1 pilot and doesn't obviously survive n=10 organizers. Worth Product treating them as one line item ("manual-process scaling") rather than three separate footnotes, so the actual trigger condition (review queue backs up, or authoring hours balloon, or the flat drop-select list becomes unusable) gets noticed as a pattern rather than three unrelated one-off complaints.
