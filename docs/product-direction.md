# GeoDrop — Product Direction

Status: pre-pilot. Decisions below were made after a formal idea validation (Jul 2026).
This file defines **what we are building and what we are deliberately not building.**
When a request conflicts with this document, say so before implementing.

---

## What GeoDrop is

> A platform for creating location-unlocked digital experiences, rewards, stories,
> and challenges — for events, businesses, tourism, and communities.

**What GeoDrop is not:** "social media on a map." We are not competing with Snapchat,
Instagram, Nextdoor, or Google Maps for open-ended user-generated feeds. Do not propose
features whose main justification is parity with a general social network.

The core innovation is **proximity-gated content**: something placed at a real location
that you must physically visit to discover, unlock, collect, or redeem. Likes, comments,
profiles, groups, and feeds are supporting features, not the product.

## Primary market (in order)

1. **Events** — weddings, reunions, festivals, school and corporate events, concerts.
   This is the wedge. Events solve our hardest problem (local content density) by
   supplying a bounded geography, a concentrated audience, seeded drops, and a paying
   organizer. Distribution comes through the existing E3HI business, not the app store.
2. **Local business / Hilo trails** — second vertical.
3. **Tourism and hospitality** — third. Longer sales cycles, content approval, cultural
   sensitivity review.

Growth unit is a **drop experience**, not an individual user. GeoDrop must deliver full
value with exactly one creator and zero other nearby users. Any feature that only works
at scale is out of scope for now.

---

## Launch scope (v1)

Build only these. Treat the list as closed.

- Account creation, plus controlled guest access
- Map view and nearby-drop list
- Text and photo drops
- Proximity unlocking
- Drop expiration
- Collect / claim
- Basic creator profile
- Report and block
- Simple redemption code for business rewards
- Organizer analytics
- Push notifications **only** for experiences the user explicitly joined

Audio drops are permitted where they serve tours or storytelling, but must never block
the pilot. If audio work threatens the timeline, cut it.

## Explicitly deferred — do not build

Do not implement, scaffold, or add dependencies for these without an explicit decision
to reverse this section:

- **Public anonymous posting** (see Safety below)
- **NSFW content and NSFW detection**
- Open direct messaging
- Public group creation
- Video uploads
- Broad or background location tracking
- Complex voting systems
- Algorithmic recommendations
- Multiple account types with extensive permission matrices
- National or global discovery
- Consumer subscriptions, display ads, location-data sale, crypto/token rewards

If you notice unused code or config for a deferred feature, flag it rather than
extending it.

---

## Safety constraints (non-negotiable)

Anonymous **viewing** is fine. Anonymous **public creation** is not, and does not ship
in v1. An anonymous location-based UGC product produces predictable harm: harassment
tied to identifiable places, bullying at schools and workplaces, sexual content near
public locations, stalking, content on private property, and child-safety exposure.
Both Apple and Google hold UGC apps to moderation, reporting, and blocking requirements,
and Google removes apps with a reputation for misuse.

Requirements:

- Creators must be authenticated. Public display names are fine; internal account
  traceability is mandatory.
- Sexual content is prohibited by policy and by implementation.
- Reporting and blocking must exist before any public exposure.
- A documented moderation queue must exist before any public exposure.
- Organizers control the drops within their event.
- Rate limits and account reputation apply to creation.

## Location privacy (part of the product, not compliance overhead)

GeoDrop must never continuously broadcast a user's position. The required design:

1. Show nearby content using **approximate** location.
2. Request **precise** location only at the moment the user attempts to unlock a drop.
3. Check proximity.
4. Record the successful unlock — not a continuous location history.
5. Stop requesting precise location afterward.
6. Never show a user's live position to other users by default.

Android 17 provides a one-time precise-location mechanism intended for exactly this
pattern (nearby stores, location-tagged posts); prefer it. Assume precise location and
background location are sensitive, disclosed, and purpose-limited on both platforms.

Reject implementations that request precise location at app launch, hold it after an
unlock completes, or persist a location trail.

---

## Monetization context

Revenue is **B2B first**. This affects what we build: organizer-facing tooling,
analytics, redemption tracking, and reusable experience templates are load-bearing;
consumer paywalls are not. Pricing under test (hypotheses, not established rates):
self-service event $49–99, customized small event $200–500, branded activation
$750–2,500+, local business subscription tiers around $29/$49/$79 per month.

## What the moat actually is

Not the technology — proximity checks, maps, geofences, and notifications are all
replicable. The defensible assets are dense local content, organizer and venue
relationships, creator tooling quality, a catalog of reusable experience templates,
redemption and engagement data, trusted moderation, and local brand recognition.
Prioritize work that compounds those.

---

## What we are measuring

Pilot 1 is a single real E3HI event: 10–20 drops, one prize, one trail, 2–3 multimedia
drops, a QR code entry point, and very low-friction onboarding, for roughly 50–150
attendees. The loop under test is:

> see invitation → open GeoDrop → discover drop → walk to location → unlock → get value → unlock another

Downloads are not the success metric. Instrument the funnel so these are answerable:

| Metric | Continue threshold |
| --- | --- |
| Invite → activation | ≥ 30% |
| Activated users unlocking ≥1 drop | ≥ 60% |
| Users unlocking ≥3 drops | ≥ 25% |
| Users completing the main trail | ≥ 15% |
| Would use again | ≥ 50% |
| Serious safety or usability incidents | ~0 |
| Credible paid organizer commitment | ≥ 1 |

These are internal targets, not industry benchmarks. Also worth capturing: how much
explanation users needed, whether GPS accuracy caused frustration, which drop types
produced excitement, and whether discovery was enjoyable independent of the prize.

## 90-day go/no-go

Keep investing only if all four hold: users complete experiences unassisted; a
meaningful share voluntarily return; organizers or businesses pay real money; and
moderation, privacy, deletion, reporting, and support are sustainable to operate.

Reposition if: users participate only because they know the founder, GPS unlocking
frustrates consistently, organizers like it but won't pay, users vanish once the prize
is removed, quality experiences require too much manual authoring, or moderation load
outruns revenue.
