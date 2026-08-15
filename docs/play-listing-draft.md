# Kithe Google Play listing draft

Status: **review-ready copy; do not submit**  
Prepared: 2026-08-14  
Scope: English (United States), participant-first Pilot 1 listing

This draft follows `product-direction.md`, `design-system-v1.md`, and
`voice-and-glossary-v1.md`. It does not imply E3HI ownership, public discovery, a social
feed, audio/video drops, background location, ads, or subscriptions.

## Store metadata

| Play field | Draft value | Status |
| --- | --- | --- |
| App name | `Kithe` | Ready; 5/30 characters |
| Default language | English (United States) | Recommended |
| App or game | App | Ready |
| Category | Events | Recommended for the event-first Pilot 1 wedge |
| Category alternative | Travel & Local | Use later only if local trails/tourism become the primary listing promise |
| Price | Free | Ready; no in-app purchases or subscriptions in v1 |
| Contains ads | No | Ready, subject to removing or explicitly resolving the inherited advertising-ID permissions |
| Developer name | Kithe | Ready; must not imply E3HI ownership |
| Contact email | `support@kitheapp.com` | Ready |
| Website | `https://kitheapp.com` | Live |
| Privacy policy | `https://kitheapp.com/privacy` | **Blocking: approved policy is not published** |
| Account deletion | `https://kitheapp.com/account-deletion` | **Blocking: external deletion pathway is not published** |

Google's current limits are 30 characters for the name, 80 for the short description,
and 4,000 for the full description. The listing must describe the real release artifact,
not remotely disabled or future functionality.

## Short description

> Follow an invitation and unlock digital drops when you reach real-world places

Character count: **78/80**.

## Full description

> Kithe connects digital experiences to real places.
>
> Join an invite-only Experience with a host's link or code. See the drops available to
> you, use the map or nearby list to choose where to go, and check your location only when
> you decide to unlock. Reach a drop to reveal text or a photo selected by the host.
>
> With Kithe, you can:
>
> - Preview an Experience before signing in
> - Browse as a guest, then sign in when you are ready to unlock
> - Use approximate location to see what is nearby
> - Request precise location only for a one-time unlock check
> - Save unlocked drops in your Collection
> - Follow Trails and receive a unique reward code when an Experience includes one
> - Report a drop or block a host
>
> Approved organizers can create invite-only Experiences, place text and photo drops,
> set when drops are available, and review aggregate results.
>
> Kithe does not use background location, show your live location to other people, or
> offer a public social feed. Experiences are invitation-only, so you will need a link or
> code from a host to participate.

Character count: **1,051/4,000**, including line breaks and Markdown list markers. Remove
the quote markers when copying this text into Play Console.

## Play review access

Kithe is restricted by an Experience code and account gate, so the Play Console **App
access** section must contain working reviewer instructions. Do not place passwords or
production support credentials in this repository.

Create a dedicated, non-privileged review account and a production-safe review
Experience. Store its secret only in the Play Console access field. Proposed instructions:

1. Open Kithe and enter `[PLAY_REVIEW_CODE]`.
2. Select **Join Experience**. Guest preview is available without an account.
3. Select a locked drop and choose **Unlock**.
4. Sign in with the dedicated credentials supplied below; Kithe resumes the same drop.
5. For the proximity check, set the review device's simulated location to the coordinates
   supplied below, then select **Unlock** again.
6. Open **Collection** to confirm the unlocked text/photo receipt.
7. Open **Account** to review report, block, export, and deletion controls.

Required fixture fields, created only after production-data authorization:

| Field | Requirement |
| --- | --- |
| Experience code | Stable, unique, not reused by an event |
| Review account | Dedicated Explorer; no organizer or operator privileges |
| Password | Stored in Play Console only |
| Test coordinate | Public, safe, and included in the Play instructions |
| Drops | One text and one non-sensitive photo drop, no reward of monetary value |
| Availability | Long enough for review, with an operator-owned expiry/retirement plan |

This uses the normal server-authoritative proximity path. Do not add a production bypass
or privileged reviewer backdoor. If Play cannot use simulated location, wait for a direct
review request before proposing a narrowly scoped alternative.

## Visual asset package

### App icon

- 512 by 512 pixel 32-bit PNG with alpha, maximum 1,024 KB.
- Use the approved Kithe mark without a store badge, ranking language, or promotional
  text.
- Verify the icon at small sizes and against light/dark Play surfaces.
- Owner-approved local asset: `../play-assets/store-listing/icon/kithe-play-icon.png` with editable
  SVG source beside it. It represents a `K` as a three-point trail with an amber nearby
  destination. It replaced the local Android launcher on 2026-08-14 and passed physical-device
  mask verification; Play upload remains unauthorized.

### Feature graphic

- 1,024 by 500 pixel JPEG or 24-bit PNG without alpha.
- Use `surface/light` (`#FDFCFA`), `brand/primary` (`#0B5D5D`), `state/near`
  (`#E07B24`), and `ink/primary` (`#14171A`).
- No gradient or glass treatment. Keep the visual focus and any copy in the center-safe
  area because Play crops the graphic in some placements.
- Recommended line: **Find what a place is holding**.
- Show one restrained place/drop relationship, not a busy public-social map.
- Owner-approved local asset: `../play-assets/store-listing/feature/kithe-feature-graphic.png`
  with editable SVG source beside it. Approval is not Play upload authorization.

### Phone screenshots

Prepare six portrait screenshots from the exact signed candidate. Archive device-native
originals, then export 9:16 derivatives at 1,080 by 1,920 without stretching. Use a
coherent, fictional Experience and no personal data, real attendee names,
email addresses, support tokens, or precise home locations. The detailed capture and QA
sequence is in `../play-assets/store-listing/screenshots/capture-manifest.md`.

| # | Screen | Optional caption | Alt text (140 characters maximum) |
| --- | --- | --- | --- |
| 1 | Invitation/code entry | Start with an invitation | Kithe entry screen with a field for an Experience code and a button to preview the invitation. |
| 2 | Pre-account Experience preview | See where the invitation leads | Experience preview showing the host, date, place, and a Join Experience action before sign-in. |
| 3 | Joined map with list alternative | Find drops around the Experience | One active Experience map with accessible markers and the nearby text equivalent reachable in one tap. |
| 4 | Locked/near drop detail | Know what to look for | Drop detail with distance band, state label, place guidance, and a bottom-third action. |
| 5 | One-shot precise-location primer | Your location is checked only when you unlock | Contextual prompt explaining the one-time precise check and limited retention before the Android permission dialog. |
| 6 | Successful text or photo unlock | Reveal the drop, then keep exploring | Found state revealing a safe sample drop, confirming it was saved, and offering the next participant action. |

At least two phone screenshots are required; four or more high-resolution phone
screenshots are recommended for broader Play placements. The six-screen set above is the
target. Do not show audio, video, public feeds, global discovery, scheduled publishing,
subscriptions, or unshipped organizer features.

## Remaining Play Console decisions

These are intentionally not guessed:

- target age group and whether any Pilot 1 event will intentionally include children;
- content-rating questionnaire answers based on the actual seeded and user-authored
  content policy;
- country/region availability for the fail-closed production listing;
- predefined Play tags available in the console;
- support phone publication (optional and not recommended for the initial listing);
- whether organizer creation is described in the participant listing screenshots;
- final review code, credentials, coordinate, and retirement owner.

Recommended Pilot 1 posture: adults-only target audience, United States availability,
no child-directed marketing, participant-first screenshots, no public phone number, and
organizer capabilities described only in the full description. This recommendation needs
owner/legal approval before it becomes a Play answer.

## Official references

- [Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en-EN)
- [Store listing best practices](https://support.google.com/googleplay/android-developer/answer/13393723?hl=en)
- [Preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)
- [Choose a category and tags](https://support.google.com/googleplay/android-developer/answer/9859673?hl=en-EN)
