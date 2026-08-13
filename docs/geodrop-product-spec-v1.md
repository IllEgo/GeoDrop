# GeoDrop — Product Spec v1

**Owner:** Product Manager role project
**Date:** 2026-07-26
**Scope of this doc:** user stories + acceptance criteria for the closed launch list, metrics spec, monetization validation plan, Now/Next/Later roadmap, capacity check, pilot 1 selection criteria.
**Not in scope:** relitigating launch scope or platform sequencing. Both closed.

> **R0 alignment override (approved 2026-08-09; see
> `redesign-alignment-proposal.md`):** `product-direction.md` and the signed P8
> decision now govern conflicts. Locked payload release and receipt creation move behind a
> server-authoritative one-shot proximity check. Guest browsing gates an account at the
> first unlock. Pilot 1 creation is approved-organizer-only, using internal role `BUSINESS`
> without adding a merchant account type. Reward codes have separate issued and confirmed-
> used states. Keep the private aggregate in-app Results view and supplement it with a
> founder report. Scheduled publishing, audio, merchant tooling, and iOS visual parity are
> deferred for Pilot 1.

---

## 0. Read this first — three things I found

### 0.1 A definitional conflict in the pilot thresholds

`product-direction.md` lists both *"Invite → activation ≥ 30%"* and *"Activated users unlocking ≥1 drop ≥ 60%."*

If activation includes a first unlock, the second row is 100% by construction and measures nothing. **Activation must be defined as everything up to but not including the first unlock.** Definition in §2. This is a definition fix, not a threshold change.

### 0.2 "Controlled guest access" collides with Phase 1.2

Launch scope includes *"Account creation, plus controlled guest access."*
PROJECT-STATE §3 records *"Anonymous auth restricted to view-only (Phase 1.2)."*

These cannot both be true if a guest is expected to unlock a drop. This is the single highest-leverage unresolved item in this doc: it sits directly on the invite→activation number, which is pilot 1's top-of-funnel metric, and it changes acceptance criteria on six of the eleven features. Raised as new Open Decision #13. I have written the stories for **account-required unlock** as the default and marked the guest branches.

### 0.3 Pilot 1 at Android-only may not be numerically readable

Worked in §8.3. Short version: a 150-person guest list yields roughly 3–5 people at the bottom of the funnel once you filter for Android. You cannot distinguish a 15% completion rate from a 5% one at that sample size. This does not invalidate pilot 1 — it changes what pilot 1 is *for*. Recommendation in §8.3.

---

## 1. What I decided vs. what I need from you

| # | Decision | Made by me | Needs founder/role input |
|---|---|---|---|
| 1 | Activation = experience joined + map loaded, before first unlock | ✅ | |
| 2 | North star = Completed Experiences / month | ✅ | |
| 3 | Retention measured experience-to-experience, not D1/D7/D30 | ✅ | |
| 4 | Organizer analytics v1 = founder-produced post-event report, not in-app dashboard | ✅ (definition of done, not a scope cut) | |
| 5 | Redemption v1 = server-issued code, manually validated by the business | ✅ | |
| 6 | No proximity-triggered push in v1 (requires deferred background location) | ✅ | |
| 7 | Organizer payment stays **off-platform** (invoice, not IAP) | Recommended | Legal to confirm |
| 8 | Can a guest unlock, or is an account required first? | | **Backend + Founder — blocking** |
| 9 | Analytics provider (still `UNKNOWN:` in PROJECT-STATE) | | **Backend — blocks instrumentation** |
| 10 | Is "experience" a first-class entity (Open Decision #9)? | | **Backend — every metric below assumes yes** |

`ASSUMPTION:` E3HI is the founders' existing events business, and pilot 1 would be an event E3HI runs *for a client*. §6.2 depends on this being right.

---

## 2. Funnel and retention definitions

These are the canonical definitions. Everything downstream — thresholds, dashboards, post-event reports — uses these words with these meanings.

| Stage | Definition | Source of number |
|---|---|---|
| **Invited** | On the organizer's guest list, and reachable | Organizer-supplied, offline. Not instrumented. |
| **Eligible** | Invited **and** holding a device the app runs on | Organizer poll or founder estimate. Offline. |
| **Opened** | Resolved the QR/deep link and reached first app screen | Client |
| **Authenticated** | Completed account creation or guest session | Server |
| **Activated** | Joined the experience **and** rendered a map with ≥1 drop visible | Server (join) + client (render) |
| **First value** | ≥1 successful unlock | Server |
| **Engaged** | ≥3 successful unlocks in one experience | Server (derived) |
| **Completed** | Finished the designated trail | Server (derived) |
| **Returned (intra-event)** | Session resumed after a ≥2h gap, same experience | Client |
| **Retained (cross-experience)** | Joined a *second, different* experience within 90 days | Server (derived) |

**On retention:** standard D1/D7/D30 is the wrong instrument here. A wedding is one afternoon; there is nothing to come back to on day 7 and a flat retention curve would tell you nothing about the product. GeoDrop's real retention question is *"does a person who had a good time at one experience join another one?"* — which is also the question that determines whether this is a business or a novelty. Measure it at 30 / 90 / 180 days. Accept that it is unmeasurable until at least two experiences exist, and plan for pilot 2 to be the first read.

**Denominator warning:** *Invited* and *Eligible* are offline numbers supplied by a human. Write them down before the event, in a file, with a timestamp. Retro-fitting a denominator after you've seen the numerator is how pilots lie to founders.

---

## 3. Metrics spec

### 3.1 North star

> **Completed Experiences per month** — count of (user × experience) pairs where the user finished the designated trail.

Rationale:

- It only rises when experiences exist (supply), people join them (distribution), and the loop actually works end-to-end (quality). One number, three failure modes visible.
- It matches the stated growth unit: a drop experience, not a user.
- It is hard to inflate. "Drops unlocked" can be juiced by placing twenty drops in one courtyard. "Downloads" is explicitly rejected in `product-direction.md`. "DAU" is meaningless for an event product with no daily use case.

**Honest caveat:** at pilot scale this number will be somewhere between 3 and 20. It is not steerable and should not be reported as a percentage of anything until after pilot 2. Its job right now is to tell Engineering what to instrument, not to run the company.

**Guardrails** — the north star is invalid if any of these move the wrong way:

| Guardrail | Threshold |
|---|---|
| Serious safety incidents | 0 |
| Unlock attempts failing on distance | < 25% of attempts |
| Repeat-paying organizers | ≥1 by end of pilot 2 |
| Median unlocks per activated user | ≥2 (guards against one obsessive completionist carrying the number) |

### 3.2 Event spec

**Assignment rule:** anything that gates value, feeds a threshold, or could be spoofed is **server-side**. Anything that describes UI behavior or a failure the server never sees is **client-side**. Every event below is assigned to exactly one side. Do not fire the same concept from both.

| Event | Fires when | Side | Question it answers |
|---|---|---|---|
| `invite_link_opened` | QR/deep link resolves | Client | Did the entry point work? Which channel? |
| `app_first_open` | First launch, per install | Client | Install→open drop-off |
| `auth_completed` | Account or guest session created | Server | Where does onboarding leak? |
| `experience_joined` | User added to an experience | Server | **Activation numerator** |
| `location_permission_result` | Permission dialog resolves (approx / precise / denied) | Client | Does the privacy pattern cost us users? |
| `map_loaded_with_drops` | Map renders ≥1 drop | Client | Activation completion |
| `drop_viewed_locked` | Locked drop detail opened | Client | Is the tease working? |
| `unlock_attempted` | User taps unlock | Client | Intent vs. success |
| `unlock_failed_distance` | Server rejects on proximity; include distance bucket (0–25m, 25–50m, 50m+) | Client | **"Did GPS frustrate people?"** — the single most important diagnostic in pilot 1 |
| `unlock_succeeded` | Server validates proximity | Server | **First value / Engaged / all thresholds** |
| `drop_collected` | Item added to collection | Server | Does collecting matter, or is unlock enough? |
| `trail_completed` | Final trail drop unlocked | Server | **North star** |
| `redemption_code_issued` | Code generated for a user | Server | Reward funnel top |
| `redemption_code_marked_used` | Founder/business marks redeemed | Server | Reward funnel bottom → the number a business will actually care about |
| `push_sent` | Broadcast dispatched | Server | Delivery denominator |
| `push_opened` | Notification tapped | Client | Is push worth building on? |
| `report_submitted` | Report filed | Server | Safety load |
| `block_created` | Block applied | Server | Safety load |
| `feedback_submitted` | Post-event prompt answered | Server | "Would use again" |
| `drop_created` | Organizer publishes a drop | Server | Authoring cost — see §7 exit criteria |
| `experience_published` | Organizer publishes an experience | Server | Supply side |

**Blocking dependency:** none of this can be built until the analytics provider is chosen (PROJECT-STATE §3, `UNKNOWN:`). Server-side events can land in Firestore regardless, but client events need a destination. **Instrument before pilot 1, not after — there is no retrofit.**

**Dependency on Open Decision #9:** `experience_joined`, `trail_completed`, `experience_published` and the entire cross-experience retention definition all assume an experience/trail is a first-class entity with an ID you can join and query. If it stays an implicit grouping of drops, this metrics spec does not work as written. **That is a product argument for resolving #9 as "yes, first-class."** Feasibility and cost are Backend's call, not mine.

---

## 4. User stories and acceptance criteria

Format: story → acceptance criteria → v1 non-goals. "Done" additionally requires everything in §5.

Throughout: **A** = attendee/explorer, **O** = organizer, **B** = business.

---

### 4.1 Account creation + controlled guest access

> As **A**, I want to get into an experience in under 60 seconds from scanning a QR code, so that I don't give up before I've seen anything.

- [ ] Scanning the event QR resolves to the specific experience, not a generic home screen
- [ ] If the app isn't installed, the link routes to install and **preserves the experience target through install** (deferred deep link)
- [ ] Account creation requires no more than: one identifier + one credential. No profile setup wall.
- [ ] A newly authenticated user lands directly on the experience map, not on a tutorial or a feed
- [ ] Every creator account is internally traceable per the safety constraints, regardless of public display name
- [ ] **Guest branch (pending Open Decision #13):** either (a) guest may view but is prompted to create an account at first unlock attempt, or (b) guest may unlock with a device-bound session upgradeable to a full account. Pick one before Design finalizes onboarding.

**v1 non-goals:** social login beyond whatever is cheapest; profile completion prompts; email verification gates before first unlock.

---

### 4.2 Map view and nearby-drop list

> As **A**, I want to see what's findable near me without handing over my precise location, so that the app doesn't feel like surveillance on first open.

- [ ] Map browsing uses **approximate** location only — precise is never requested at launch or on map open
- [ ] Locked drops show location, a title/teaser, and distance; contents are not retrievable client-side before unlock
- [ ] A list view exists as an alternative to the map (accessibility, and it works when GPS is poor)
- [ ] Drops outside the joined experience do not appear during pilot 1
- [ ] Denied-location state is a usable screen, not a dead end — list view still renders the experience's drops
- [ ] No live position of any user is shown to any other user

**v1 non-goals:** global/national discovery; clustering at city scale; heatmaps; anything showing other users on the map.

**⚠ Payload security:** "contents not retrievable before unlock" means the locked payload is not shipped to the client. If the map query returns full drop documents, proximity gating is cosmetic. Backend owns the fix; I own the requirement.

---

### 4.3 Text and photo drops

> As **O**, I want to place a text or photo drop at a spot on a map, so that guests find something worth walking to.

- [ ] Drop = location + radius + title + body text and/or one image + optional expiry
- [ ] Placement by map pin-drop **and** by standing at the spot ("use my location") — the second is what you'll actually use on a venue walkthrough
- [ ] Image upload has a size cap and a compression step (venue wifi is bad; assume LTE at best)
- [ ] Authoring a single drop takes **under 3 minutes** end to end — time this, it's a go/no-go input (see §7)
- [ ] Editing and deleting a drop works after publish
- [ ] Photo drops render legibly on a phone in direct sunlight — outdoors is the default context, not the edge case

**v1 non-goals:** video; multiple images per drop; rich text; drop templates (that's Later, and it's part of the moat).

---

### 4.4 Proximity unlocking

> As **A**, I want to physically walk to a place and have the drop open, so that the trip felt like it did something.

- [ ] Precise location is requested **at the unlock attempt only**, per the location-privacy sequence
- [ ] Proximity is validated **server-side**. A client-side check is a suggestion, not a gate.
- [ ] After validation, the app stops requesting precise location
- [ ] Only the successful unlock is recorded. No location trail is persisted.
- [ ] Failure state names the actual problem and the actual distance: "You're about 60m away — head toward the banyan tree," not "Unlock failed"
- [ ] Weak-GPS state is distinguished from too-far state and says so
- [ ] Unlock radius is **configurable per drop**, default 25m. Do not hardcode.
- [ ] Repeated failed attempts at the same drop are rate-limited but do not lock the user out of the experience

**v1 non-goals:** anti-spoofing beyond server-side validation; indoor positioning; bluetooth beacons.

**Why the radius matters:** consumer GPS in tree cover or between buildings drifts 10–30m routinely. A 10m radius produces a frustrating pilot and a false negative on the whole concept. Start at 25m, tune on the venue walkthrough, and log the distance buckets so you learn the real distribution.

---

### 4.5 Drop expiration

> As **O**, I want drops to stop being findable after my event ends, so that guests aren't wandering the venue next Tuesday.

- [ ] Expiry is optional per drop and settable at the experience level
- [ ] Expired drops are not unlockable and are removed from the map
- [ ] **Already-collected drops remain in the user's collection after expiry** — you don't take back what someone earned
- [ ] Expiry is enforced server-side (client clock is not trusted)
- [ ] Expiring an experience does not delete its analytics

**v1 non-goals:** scheduled future publish; recurring windows; timed reveals.

---

### 4.6 Collect / claim

> As **A**, I want unlocked drops to stay somewhere I can look at later, so that the walk produced something I keep.

- [ ] Unlocked drops appear in a personal collection
- [ ] Collection persists across sessions and reinstalls (tied to account — another reason #13 matters)
- [ ] Collection shows progress within the experience: "7 of 12 found"
- [ ] Trail completion produces a visible, distinct completion state

**v1 non-goals:** trading; sharing collections; badges beyond a single completion marker; rarity.

---

### 4.7 Basic creator profile

> As **A**, I want to see who placed this, so that I know it's the organizer and not a stranger.

- [ ] Display name + optional avatar + optional one-line bio
- [ ] Attribution appears on each drop
- [ ] Internal account traceability maintained regardless of display name

**v1 non-goals:** following; creator feeds; verification badges; public creator directories.

---

### 4.8 Report and block

> As **A**, I want to report or block content that shouldn't be there, so that I have recourse — and so the app is store-compliant.

- [ ] Report is reachable from every piece of user-generated content in ≤2 taps
- [ ] Report writes to a **moderation queue that a human actually opens** — the queue must exist before any public exposure, per the safety constraints
- [ ] Block hides all of that creator's drops from the blocking user
- [ ] Blocked state persists across sessions
- [ ] Documented turnaround target for reports (`ASSUMPTION:` 24h — Legal/Compliance to set the real one)

**v1 non-goals:** appeals workflow; automated classification; NSFW detection (explicitly deferred).

**Straight talk:** in v1 there are no feeds, no DMs, and no public posting, so *block* has thin semantics — the realistic case is a guest blocking another guest's drop inside an event. Build it anyway. Both stores require it, and it must be in place before the day you turn on any broader creation.

---

### 4.9 Simple redemption code for business rewards

> As **B**, I want proof that a person actually showed up and claimed the offer, so that I'd consider paying for this again.

- [ ] Server issues a **unique, single-use** code on claim
- [ ] Code is visible in the user's collection and doesn't vanish on app restart
- [ ] Redemption is marked used server-side; a used code visibly reads as used
- [ ] Issued vs. redeemed counts are queryable per business

**v1 done-state:** the business validates the code **manually** against a founder-supplied list. No business account type, no scanner, no merchant app — "multiple account types with extensive permission matrices" is explicitly deferred and this doesn't need to break that.

**v1 non-goals:** POS integration; barcode/QR scanning by merchant; self-serve business dashboard.

**Trigger to revisit:** manual validation breaks somewhere around 3–5 concurrent businesses. That's a pilot-2 problem, not a pilot-1 problem.

---

### 4.10 Organizer analytics

> As **O**, I want to see what my guests actually did, so that I can judge whether this was worth the money.

**v1 done-state: a post-event report the founder produces from server data and delivers as a document.** Not an in-app dashboard.

- [ ] Server data supports, without manual reconstruction: invited (offline) → activated → unlocked ≥1 → unlocked ≥3 → completed
- [ ] Per-drop unlock counts, ranked
- [ ] Unlock-failure counts with distance buckets
- [ ] Redemption issued/used per business
- [ ] Report is deliverable within 48h of the event

To be explicit, since it matters: **this is not removing organizer analytics from launch scope.** The scope item ships; its v1 definition of done is a report rather than a UI. At n=1 organizer, building a dashboard costs builder-weeks to serve one person the founder is standing next to. The queries you write for the report are the same queries the dashboard would use later, so nothing is thrown away.

**Trigger to revisit:** 3+ organizers, or the first organizer who asks to see numbers *during* their event.

---

### 4.11 Push notifications (joined experiences only)

> As **A**, I want to hear from an experience I chose to join, and from nothing else.

- [ ] Push is scoped to experiences the user explicitly joined
- [ ] Permission is requested **at join**, with a reason string, not at first launch
- [ ] v1 triggers, and only these: (a) organizer broadcast to the experience, (b) new drop added to a joined experience
- [ ] Opting out of push does not degrade any other functionality
- [ ] All push stops when the experience ends or expires

**v1 non-goals — and one is load-bearing:** **no proximity- or geofence-triggered notifications.** "You're near a drop!" requires background location monitoring, which is explicitly deferred in `product-direction.md` and is the single most permission-expensive thing you could add. If this comes up during the pilot as an obvious idea, it is an obvious idea that costs you the privacy positioning. Say no.

---

### 4.12 Audio drops — conditional

Permitted where they serve tours or storytelling; must never block the pilot (Open Decision #8).

**My recommendation: out of pilot 1.** Reasoning: audio adds recording/upload/playback/offline-caching surface for a feature whose value is unproven at an event, at a venue where ambient noise is high and half the guests won't have earbuds. It is a strong fit for the *Hilo trail* use case (pilot 2), where people are walking alone, quiet, and the storytelling is the point.

Proposed rule, so it doesn't need relitigating: **audio ships when the first tour-style experience is scheduled, not before.**

---

## 5. Definition of done — applies to every feature

A feature is not done until all of these are true:

| Gate | Requirement |
|---|---|
| **Design states** | Default, loading, empty, error, permission-denied, and offline states all designed and built. Empty and permission-denied are the two that get skipped and the two most likely to appear at a real event. |
| **Offline/degraded** | Behavior on poor connectivity is defined and non-destructive. Venue wifi will be bad. |
| **Server authority** | Anything that gates value is validated server-side |
| **Instrumentation** | All §3.2 events for that feature fire, verified in the analytics destination — not just in code |
| **Privacy** | Conforms to the six-step location sequence in `product-direction.md` |
| **QA** | Tested on ≥2 real Android devices including one low-end, and **once outdoors at a real venue.** Emulator testing does not test GPS. |
| **Copy** | Real strings, no placeholder text, no debug affordances reachable in a release build |

---

## 6. Monetization

### 6.1 Pricing rationale

The hypotheses in `product-direction.md` stand — they are reasonable and I'm not moving them. What's missing is *how you'd know they're right* and *which one to lead with*.

| Package | Hypothesis | My read |
|---|---|---|
| Self-service event | $49–99 | **Don't sell this first.** It implies self-service authoring tooling that doesn't exist, and it anchors your whole price list at coffee-money. Later. |
| Customized small event | $200–500 | **Lead here.** Matches what you'll actually deliver in pilot 1: you build the experience by hand. It's a line item on an event invoice, not a software purchase decision. |
| Branded activation | $750–2,500+ | Right band for a sponsor-funded event. Needs one case study first. |
| Local business subscription | $29/49/79 mo | Pilot 2 question, not pilot 1. Depends on redemption data existing. |

Two things worth stating plainly:

1. **You are not selling software, you are selling an event upgrade.** The comparison in an organizer's head is a photo booth or a live musician, not an app subscription. Photo booth rental runs several hundred dollars for a few hours. $200–500 for a custom experience is unremarkable in that frame. $49 makes it look like a novelty.
2. **Charge for the build, not the app.** Your cost is your hours. Price the customization.

### 6.2 What "validated" means

Not validated: "the organizer loved it." Not validated: "they said they'd definitely pay next time."

**Validated = money moved before value was delivered, from someone who isn't doing you a favor.**

| Level | Evidence | Counts as validation? |
|---|---|---|
| 0 | Verbal enthusiasm | No |
| 1 | "Yes, I'd pay $X" in writing, no invoice | No |
| 2 | Invoice sent and paid at a stated pilot rate | **Partial** |
| 3 | Second event booked and paid at full rate after seeing results | **Yes** |
| 4 | An organizer with no personal relationship to either founder pays | **Yes, and this is the real one** |

**The E3HI problem.** If pilot 1 is an E3HI event and E3HI "pays" GeoDrop, that's an internal transfer and validates nothing. The go/no-go criterion — *organizers or businesses pay real money* — is only satisfiable if the payer is **E3HI's client**, as a line item on their event invoice. So:

> Pilot 1's paying customer must be the client whose event it is, not E3HI. Quote it as a line item. Let them decline. A declined upsell is real information; a comped one is not.

### 6.3 When to test

**Before pilot 1 is delivered, not after.** Delivering free and asking about price afterward sets that customer's reference price at zero permanently, and you'll never get a clean read from them again.

Sequence:
1. Event selected → quote GeoDrop as a line item at $200–500 with a "pilot rate" discount, in writing
2. Deliver
3. Within 7 days of delivery, while the organizer still has the feeling: ask for a second booking at **full** rate
4. Log the outcome against the ladder in §6.2

### 6.4 Payment mechanics — recommendation

**Keep organizer payment entirely off-platform** — invoice or existing E3HI payment rails, no in-app purchase.

- B2B services sold outside the app avoid the 15–30% platform cut entirely
- It sidesteps the store rules that would apply to selling digital content in-app
- It's how the transaction naturally happens anyway: it's a line on an event invoice

This becomes a live question the moment a *consumer* pays for anything in-app (premium trails, creator marketplace — both Later). At that point the cut is real and the pricing math changes. **Legal owns confirming this read.** It also closes Open Decision #4 if accepted.

---

## 7. Roadmap

### NOW — through pilot 1 (Android only)

Everything here is required. Nothing here is optional.

1. Finish migration Phases 2–3 per `migration-plan.md`
2. Choose analytics provider *(blocks 3)*
3. Instrument the §3.2 event spec
4. Resolve guest-access decision *(Open Decision #13)* and build whichever branch
5. QR/deep-link entry incl. deferred deep link through install
6. Complete launch-scope features to the §5 done bar
7. Moderation queue exists and a human is assigned to it
8. Pilot 1 distribution mechanism resolved *(Open Decision #3)*
9. Venue walkthrough + outdoor GPS testing at the actual site
10. Post-event report queries written and dry-run **before** the event

### NEXT — after pilot 1

Ordered. Note that item 1 consumes nearly all available builder capacity (§8).

1. **iOS port** from the finished Android build
2. Fixes from pilot 1 findings — reserve capacity for this; there will be findings
3. Pilot 2: Hilo discovery trail, 10–15 locations, 3–5 businesses, one theme, one reward
4. Audio drops *(pilot 2 is the right home for them — §4.12)*
5. Redemption workflow that survives 3–5 businesses
6. Cross-experience retention read — first time this is measurable

### LATER — post go/no-go only

- Experience templates catalog *(named as moat — highest-value item here)*
- Self-service organizer authoring *(prerequisite for the $49–99 tier)*
- In-app organizer dashboard
- Business self-serve subscription
- Tourism/hospitality vertical
- Creator marketplace *(first thing that triggers platform-cut math)*

### NEVER — until an explicit reversal

Everything in `product-direction.md`'s deferred list, plus: proximity-triggered push, live user positions, anonymous public creation.

### Roadmap exit criteria — pilot 1 → NEXT

Do not start iOS until:

- [ ] Post-event report delivered
- [ ] Authoring cost measured (minutes per drop, hours per experience)
- [ ] Pilot 1 defects triaged and the P0s fixed
- [ ] A decision recorded on whether pilot 2 is worth running

**On authoring cost specifically:** "creating a quality experience requires too much manual work" is one of the six named reposition triggers. It is the only one you can measure *during* pilot 1 rather than after. If seeding 15 drops takes 12 hours, that's a business-model finding, not an inconvenience — at $300/event against 12 hours of founder time, this is a job, not a product. Time it and write it down.

---

## 8. Capacity check

### 8.1 The model

`ASSUMPTION:` builder's 20 nominal hrs/week → **14–16 effective** hrs/week after context-switching, review, environment friction, and life. Correct me if your tracked history says otherwise.

`ASSUMPTION:` all sizes below are mine and need correction by Frontend/Backend. They are *relative* sizes, not a schedule.

| NOW item | Builder-hours |
|---|---|
| Migration Phases 2–3 remainder | **UNKNOWN** — gated on Phase 0 |
| Analytics setup + instrumentation (21 events, both sides) | 15–25 |
| QR / deferred deep link | 8–14 |
| Guest access branch | 0–20 (depends on #13) |
| Push (joined-experience broadcast only) | 12–20 |
| Redemption code issuance + marking | 8–12 |
| Expiration enforcement | 5–8 |
| Report/block + queue plumbing | 10–16 |
| Report queries + dry run | 8–12 |
| Pre-pilot QA incl. outdoor device testing | 20–30 |
| Store/distribution setup | 6–12 |
| **Subtotal, excluding migration** | **~92–169** |

At 14–16 effective hrs/week: **roughly 6 to 12 builder-weeks after migration completes**, plus the unknown migration remainder.

I am deliberately not producing a pilot 1 date. PROJECT-STATE says the estimate is withheld pending Phase 0 and I'm not going to launder an old number into a new one.

### 8.2 Three things the model says

1. **The Android-first call was correct** and the numbers support it rather than merely rationalizing it. There is no version of the above that fits alongside a parallel iOS build at 20 hrs/week.
2. **The iOS port is not a small follow-on.** `ASSUMPTION:` 60–70% of Android's total feature effort even with a working app as the spec — UI is per-platform, and Compose→SwiftUI doesn't translate mechanically. Call it 150–250 hours ≈ **10–17 builder-weeks.** That is essentially all of NEXT. Plan NEXT as "iOS, plus pilot-1 fixes, and nothing else."
3. **Partner hours don't relieve builder hours.** 20 + 20 ≠ 40. Design, organizer relationships, and event ops run genuinely in parallel and are not on the critical path — but no quantity of them ships a line of Kotlin. When something must be cut for time, it comes out of the builder queue or it doesn't come out.

### 8.3 The sample-size problem

`ASSUMPTION:` roughly half of US phones are iPhones; substitute the real guest-list number the moment you have one.

150 invited → ~67 Android-holding → apply the published thresholds:

| Stage | Threshold | Expected n |
|---|---|---|
| Eligible (Android) | — | ~67 |
| Activated | ≥30% | ~20 |
| Unlocked ≥1 | ≥60% | ~12 |
| Unlocked ≥3 | ≥25% | ~5 |
| Completed trail | ≥15% | ~3 |

**You cannot distinguish 15% from 5% with n=20.** One enthusiastic group of friends moves every number below the fold.

This doesn't kill pilot 1. It reframes it:

> **Pilot 1 is a qualitative usability and operations test that produces directional numbers. Pilot 2 is the quantitative test.**

Consequences to accept now:

- Add real qualitative instrumentation: **5 structured post-event interviews** (booked in advance, not improvised), founder observation notes taken *during* the event, and the in-app feedback prompt
- Report absolute counts, never percentages, for anything below "unlocked ≥1"
- Set a floor: if fewer than **40 eligible Android-holding invitees**, even the top of the funnel is noise — pick a bigger event
- The unlock-failure distance distribution is the highest-value data from pilot 1, and it's the one number that *is* readable at small n because it's per-attempt, not per-user

---

## 9. Pilot 1 event selection criteria

Founder makes the pick. Here's what I'd push on. Feeds Open Decision #6.

### Hard criteria — fail any one, pick another event

| # | Criterion | Why |
|---|---|---|
| 1 | Bounded, known guest list, organizer holds contact info | No denominator, no activation metric |
| 2 | ≥40 eligible Android-holding invitees | §8.3 — below this the pilot is unreadable |
| 3 | Predominantly outdoor or open-air venue | GPS through a concrete multi-story building doesn't work. This is physics, not a bug you can fix. |
| 4 | ≥3 hours with genuine unstructured time | A seated-dinner-only event gives nobody time to walk anywhere |
| 5 | Contiguous, walkable venue, drops placeable ≥30m apart | 25m radius + GPS drift → overlapping drops unlock each other |
| 6 | Founder has physical site access beforehand | You must walk it with the app before the guests do |
| 7 | Organizer is a **client**, not E3HI itself | §6.2 — otherwise willingness-to-pay is untestable |
| 8 | Predominantly adult (18+) guest list | Minors add consent, age-gating, and liability questions that are not resolved. See handoff to Legal. |

### Strong preferences

| Criterion | Why |
|---|---|
| Repeat or annual event | Gives you a natural second booking to sell |
| Organizer is competent and communicative | They're your first user for the authoring tools |
| Some daylight | Photo drops in the dark are worse |
| A sponsor or business already involved | Lets you test redemption codes in the same pilot |
| Not a wedding, for pilot 1 | Highest emotional stakes, least tolerance for anything glitching, and the guest of honor cannot be your QA. Corporate events, reunions, and festivals are more forgiving. |

### Scorecard

| Event | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | Prefs | Verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| | | | | | | | | | | |

### The device-mix question, concretely

Open Decision #6 says to check the guest list's actual iOS/Android split. Two things about that:

1. **Ask the organizer to poll.** For a corporate event or reunion, this is one email. Don't estimate what you can measure.
2. **Decide in advance what iPhone guests do**, because you will not improvise it well on the day. The honest options are: they don't participate (and you exclude them from the denominator, per §2), or you pick an event with an unusually high Android share. There is no good third option — a web fallback is out of launch scope, and shared loaner devices don't test the real loop.

If iPhone guests can't participate, **tell the organizer that before they sign**, in writing. Discovering it at the event is a relationship cost you can't undo, and E3HI relationships are named as part of the moat.

---

## 10. New open decisions — for Cross-Team to merge

| # | Decision needed | Owner role | Blocking? | Notes |
|---|---|---|---|---|
| 13 | Can a guest/anonymous user unlock and collect, or is an account required first? | Backend + Product | **Yes** | Conflict between launch scope's "controlled guest access" and Phase 1.2's view-only anonymous auth. Drives invite→activation and six features' acceptance criteria. |
| 14 | Analytics provider | Backend + DevOps | **Yes** | `UNKNOWN:` in PROJECT-STATE. Blocks all client-side instrumentation. No retrofit possible. |
| 15 | Pilot 1 reframed as qualitative-primary | Product + Founder | No | §8.3. Recommendation: accept, add 5 structured interviews, report absolute counts below the "≥1 unlock" line. |
| 16 | Audio deferred to pilot 2 | Product | No | Closes Open Decision #8. Recommendation: defer. |
| 17 | Organizer payment stays off-platform (invoice, not IAP) | Product + Legal | No | Closes Open Decision #4 if Legal confirms. |
| 18 | Minors at pilot 1 | Legal | No, unless a candidate event has minors | Simplest mitigation is criterion #8 — pick an adult event first. |

**Also relevant to existing #9 (experience as first-class entity):** from a product standpoint, yes. The metrics spec, cross-experience retention, push scoping, trail completion, and organizer reporting all require a joinable, queryable experience ID. Cost and feasibility are Backend's call; the product requirement is unambiguous.

---

## 11. Handoffs

```
### HANDOFF → Backend Engineer
**Context:** Launch scope closed; PM has produced user stories + a 21-event metrics
spec. Two items block instrumentation and six features' acceptance criteria.
**What I need:**
1. Can an anonymous/guest user unlock and collect drops under the Phase 1.2 rules, or
   must account creation precede first unlock? Launch scope says "controlled guest
   access"; Phase 1.2 says anonymous auth is view-only. Pick one.
2. Analytics provider decision — still UNKNOWN in PROJECT-STATE and it blocks all
   client-side events. Server-side events can land in Firestore meanwhile.
3. Confirm locked drop payloads are not shipped to the client on map query. If they
   are, proximity gating is cosmetic.
4. Sizing correction on the §8.1 table — my hours are assumptions, not estimates.
**Constraints from Product:** Unlock proximity must be validated server-side. Unlock
radius configurable per drop, default 25m, not hardcoded. Expiry enforced server-side.
**Blocking:** Yes — 1 and 2.
```

```
### HANDOFF → Product Designer
**Context:** User stories + acceptance criteria now exist for all eleven launch-scope
features (§4). Definition of done in §5 includes required design states.
**What I need:** Flows designed against these criteria. Priority order for pilot 1:
QR → onboarding → map → unlock attempt → unlock failure. The failure state is the one
that decides whether the pilot reads as broken; it needs distance and direction, not
an error toast.
**Constraints from Product:** Every feature needs default, loading, empty, error,
permission-denied, and offline states. Onboarding must survive a still-open decision on
guest access — design the account-required path first, keep the guest branch swappable.
Outdoor daylight legibility is a requirement, not a nice-to-have.
**Blocking:** No.
```

```
### HANDOFF → Legal / Compliance
**Context:** Monetization is B2B; organizer pays by invoice. Pilot 1 event not chosen.
**What I need:**
1. Confirm that selling event experiences to organizers off-platform (invoice, not IAP)
   is clean under both stores' rules, given the app itself is free to attendees.
2. What changes if a candidate pilot event has minors on the guest list? I've added an
   adults-only preference for pilot 1 as the cheap mitigation — is that sufficient, or
   is age-gating needed in v1 regardless?
3. Required moderation turnaround time — I've placeholdered 24h.
**Constraints from Product:** No consumer-facing payment in v1. No NSFW detection
(deferred). Reporting, blocking, and a documented moderation queue ship before any
public exposure.
**Blocking:** No.
```

```
### HANDOFF → Cross-Team (PROJECT-STATE.md)
**Context:** PM deliverables complete for scope, metrics, monetization, roadmap.
**What I need:** Merge into PROJECT-STATE.md —
- §5 Scope: add the definition-of-done gates (§5 of this doc) and the v1 done-states
  for organizer analytics (report, not dashboard) and redemption (manual validation).
  These are done-state definitions, not scope changes.
- §6 Open decisions: add #13–#18 (§10). Note #4 and #8 have recommended closures.
- §10 Metrics: replace with north star (Completed Experiences/month), the funnel
  definitions (§2), and the 21-event spec (§3.2).
- §12 Decision log: activation defined as pre-first-unlock; retention defined
  cross-experience; audio deferred to pilot 2.
**Constraints from Product:** Launch scope itself unchanged. Platform sequencing
untouched. No pilot date produced — deliberately.
**Blocking:** No.
```
