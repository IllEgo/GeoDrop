# GeoDrop — Voice, Tone & Terminology Glossary v1

> **Owner:** Product Designer. **Status:** draft v0.1, 2026-07-26.
> **This is the canonical wording reference for every role.** Support articles, store copy,
> and marketing all write against §3. If the UI says one word and a help article says
> another, the support ticket is ours.
> **Upload to:** every role project's knowledge base.
> ⚠️ `APP-BRIEF.md` §9 (tone) is `NOT YET ANSWERED` and the §11 seed list wasn't in the
> PROJECT-STATE version I have. §1 below is a **proposal to be corrected**, not the founder's
> own voice. §3 is a decision I'm making and will hold to unless overruled.

> **R0 alignment decision (2026-08-09; see `redesign-alignment-proposal.md`):** accepted as
> the canonical Pilot 1 terminology
> and voice reference. Trail replaces Hunt in user-facing copy; Nearby is the tab label;
> participant UI does not call someone an Explorer; guest-facing creator copy says host.
> Organizer remains valid in professional/account tooling. Any older design-file glossary
> or role label is superseded by this document.

---

## 1. Voice — proposed

> **A quiet, trustworthy local guide. Never the entertainment.**

Reasoning, so it can be argued with rather than accepted by default:

- **The app is not the event.** Someone is at a wedding, a reunion, a festival. GeoDrop's job is to point at something and get out of the way. An app with a big personality competes with the occasion it was hired to serve.
- **Privacy is a product principle**, which means the voice has to be able to make a serious promise about location and be believed. Jokey copy can't carry that sentence.
- **One voice has to work at a wedding, a corporate offsite, and a cultural trail** without recalibration. Anything gamey ("Quest complete! 🎉") is fine at a school event and wrong at a memorial reunion. Restraint travels; enthusiasm doesn't.

**Three rules:**
1. **Never manufacture excitement the user hasn't felt.** The unlock is exciting; the copy doesn't have to say so. Show the thing, don't cheer about it.
2. **No urgency, no FOMO, no streaks.** Nothing that makes a guest feel behind. This is a party, not a retention funnel.
3. **Errors don't apologize and are never vague.** Say what happened and what to do. No "Oops!", no "Something went wrong", no error codes.

**Register:** second person, active voice, sentence case, contractions fine, plain verbs. Short sentences — this is read while walking.

**Emoji:** none in system copy. Organizers can use them in their own drop content; that's their voice, not ours.

| Say | Not |
|---|---|
| "Not there yet — you're about 40 m away." | "Oops! You're too far away! 😅" |
| "This one closed when the event ended." | "Sorry, this drop has expired." |
| "12 drops hidden around the gardens." | "Discover amazing experiences near you!" |
| "We use your exact location for a second, then forget it." | "We value your privacy." |
| "No one's arrived yet. Results appear as guests start finding drops." | "No data available." |

---

## 2. The scavenger-hunt mechanic is called a **Trail**

The handoff asks for a name for the chained mechanic where finding one drop leads to the next. Decision: **Trail**, with the chained-unlock behaviour described as **"one at a time"**, not given a second noun.

Why:
- **It's already the project's word.** `metrics-spec-v1.md` fires `trail_completed`; `product-direction.md` sets "users completing the main trail ≥15%"; the Hilo vertical is "discovery trails"; `GeoDrop_idea_validation_chatgpt.md` says "featured trails." Naming it anything else now means renaming an event, a threshold, and a whole second vertical.
- **It carries the physical-movement meaning for free.** A trail is something you walk. That's the product.
- **It stays dignified across every event type.** Works at a wedding, a corporate offsite, and a cultural site.

Rejected, with reasons worth keeping on record:
- **Hunt** — energetic and clear, but reads juvenile at a wedding or a memorial, and "scavenger hunt" is more useful as the *sales* phrase organizers already say than as an in-app noun.
- **Quest / Journey** — gaming register; clashes with the corporate and cultural verticals.
- **Chain / Sequence / Path** — accurate, cold, and describes the data structure rather than the experience.

**Trail vs Experience — the distinction the docs currently blur, and Backend needs it clean:**

- **Experience** = everything one host made for one occasion. The joinable container. What a QR resolves to. (Open Decision #9's object.)
- **Trail** = an *ordered* set of drops inside an experience, unlocked one at a time.
- An experience can have a trail, several trails, or none — a wedding might be 12 unordered drops with no trail at all.
- `trail_completed` therefore fires on a trail, not on an experience. Worth confirming with Product/Backend that the metric assumes the same.

---

## 3. Glossary — canonical terms

**User-facing** — these exact words appear in the UI, help articles, and store copy:

| Term | Means | Notes |
|---|---|---|
| **drop** | One piece of content placed at one spot | The brand noun. Lowercase in body copy. Gloss on first use: *"a drop is something hidden at a spot here."* |
| **experience** | Everything a host made for one occasion | Say "this event" when talking to a guest about a specific one — "experience" is what it's called, "event" is what it is. |
| **trail** | An ordered set of drops, unlocked one at a time | §2 |
| **unlock** (v.) | Being at the spot and getting the content | The action, everywhere, always. The button says Unlock and the result says Found. |
| **found** | State of a drop you've unlocked | Past tense of the user's experience, not the system's. |
| **Collection** | Where your found drops live | The place. Not a second action — see below. |
| **host** | The person or business who made the experience | Guest-facing word. Warmer than "organizer" and true at a wedding. |
| **guest** | A person taking part | Used when a host talks about their people. |
| **reward** / **code** | A redeemable thing and the code that proves it | Never "coupon" — coupon implies a discount, and rewards won't all be discounts. |
| **nearby** | The map/list surface | Tab label. |

**Internal only — never render these in the UI:**

| Term | Why it stays internal |
|---|---|
| **Explorer** | Role name. To a guest, they're just "you". Putting "Explorer" on screen invents an identity nobody asked for. |
| **Organizer** | Correct in business, commercial, and product contexts. Guest-facing UI says **host**. This is the one deliberate audience split in the glossary — documented on purpose so it isn't "fixed" later by someone tidying up. |
| **Operator** | Internal moderation role. |
| **activation, engaged participant, north star** | Metrics vocabulary. Never leaks to organizers either — the Results screen says "found at least one drop", not "activated". |
| **proximity check, geofence, receipt** | Implementation. The user experiences "checking you're here". |

**Deliberately retired — don't reintroduce:**
*collect / claim* (one action, and it's **unlock**; Collection is the place things land — see `user-stories-v1.md` §6 and `metrics-spec-v1.md` §8 Q1, both of which resolve the same way) · *pin* (a pin is the map marker, not the content) · *post* (social-feed vocabulary; the direction doc is explicit that GeoDrop isn't that) · *user* (in any user-facing string) · *check in* (implies a different, passive mechanic).

---

## 4. Copy patterns

- **Buttons:** verb + object, sentence case, and the same verb survives the whole flow. `Unlock` → `Checking…` → `Found`. Never `Submit`, `OK`, `Continue` where a real verb exists.
- **Errors:** what happened, then what to do, in that order, one sentence each maximum. No apology, no exclamation mark, no code.
- **Empty states:** why it's empty + the next action. Never a bare "No drops."
- **Permissions:** what we take, what for, how long we keep it, in that order. The privacy promise is a differentiator (`APP-BRIEF.md` §7) — spend words on it.
- **Numbers:** metric distances with one unit, no decimals under 100 m ("about 40 m"). `ASSUMPTION:` metric — confirm, since a US pilot audience may read feet more naturally. This is a one-line change now and a find-and-replace across every string later.
