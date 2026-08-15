# Kithe Market-Ready Pilot Backlog

Status: Draft for cross-functional review  
Pilot recommendation: closed, single-geography launch  
Initial scope: Explorer discovery/collection, creator drops, business offers, groups, reporting/blocking, and contextual nearby notifications. Adult content and scavenger hunts remain off for the pilot.

## Product thesis and pilot assumptions

Kithe turns physical places into a discovery layer for local stories, community posts, and business rewards. The pilot should test one proposition: **people will repeatedly travel to, collect, and share useful or delightful content because it is tied to where they are.**

The pilot geography should be a dense, walkable campus, event district, or neighborhood with an identifiable operator and at least five committed creators/businesses. Before user invitations, it must contain at least 30 reviewed live drops, with at least three useful drops reachable by 80% of invitees. Kithe should expand geography only after it proves local liquidity, retention, operational safety, and merchant fulfillment in the first zone.

Primary users:

- Explorers seeking nearby novelty, rewards, recommendations, or community experiences.
- Local businesses and community organizers seeking measurable in-person engagement.

Core jobs:

- Find something worthwhile nearby.
- Leave a story, recommendation, clue, or reward at a place.
- Drive and measure local visits.
- Share a location layer with a community.

## Critical journeys

1. Install -> understand the promise -> grant foreground location contextually -> explore as guest -> view and collect a nearby drop.
2. Guest discovers value -> creates an Explorer account -> preserves state -> creates a first safe drop -> receives collection feedback.
3. Business registers -> completes profile -> publishes an offer -> staff validates redemption -> reviews results.
4. Explorer joins a group -> sees authorized group drops -> creates or collects within the group -> leaves the group.
5. User encounters harmful content -> hides/reports/blocks -> receives confirmation -> Operations resolves it within SLA.
6. Activated user enables nearby alerts -> enters a chosen radius -> opens a relevant notification -> changes radius or opts out.
7. Account holder requests export or deletion -> reauthenticates -> receives confirmation -> data is removed within the published window.

## Phased milestones

| Milestone | Outcome | Exit condition |
|---|---|---|
| M0: Safety baseline | Production data, identity, content, and permissions are safe enough for invited testing. | Every P0 epic is Done; zero open critical/high security defects; legal approval recorded. |
| M1: Internal dogfood | Core journeys work reliably with known testers and seeded content. | Core action success >=99%; crash-free sessions >=99.7%; all critical journey tests pass. |
| M2: Closed creator beta | Supply partners can create and operate trustworthy drops. | 30 reviewed drops, 5 active creators/businesses, moderation coverage, no unresolved P0 incidents. |
| M3: Geo pilot | Invited users repeatedly find value in one dense market. | Four stable weeks meeting liquidity, activation, D7 retention, reliability, and safety gates. |
| M4: Limited public launch | The operating model can expand one geography at a time. | Two independently healthy pilot zones and completed store-readiness review. |

## Prioritized epics

Status values: `Not started`, `In progress`, `Blocked`, `Done`.

### P0 — external-beta blockers

| Epic | User outcome | Owner(s) | Dependencies | Acceptance criteria | Launch metric | Status |
|---|---|---|---|---|---|---|
| Secure media and group access | Users can trust that private/group content and media are visible and mutable only by authorized people. | Engineering, QA | Firebase rules design; data migration | Only uploader/service account can delete media; non-members cannot read group documents or media by query, document ID, or direct URL; public/guest behavior is explicitly tested; emulator abuse suite passes. | 0 unauthorized-read/delete findings; 100% security tests pass | Blocked |
| Account deletion and export | Users can leave Kithe and control their data without contacting support. | Engineering, Product, Legal, QA | Retention policy; backend deletion workflow | In-app export/deletion on both platforms; reauthentication and explicit confirmation; covers Auth, profile, username, tokens, memberships, inventory, owned drops/media, and documented retention exceptions; completion receipt provided. | >=99% successful deletion; completed within published window | Blocked |
| Moderation operations | Harmful content is removed quickly and consistently, with an auditable process. | Operations, Engineering, Product, Legal, QA | Policy taxonomy; reviewer tooling; staffing | Reports enter a severity-ranked queue with evidence and timestamps; moderators can remove content and suspend users; illegal/imminent-harm escalation and appeal process documented; reporter receives status; text/photo/audio/video are covered. | 95% priority reports triaged <24h; critical reports <1h while staffed | Blocked |
| Contextual permissions | Users understand why location and notifications are requested and can still use Kithe after denial. | Product, Engineering, QA | Onboarding design; degraded browse mode | No runtime prompt before onboarding; foreground location requested at nearby discovery; notifications after demonstrated value; background location only after explicit alert enablement and rationale; denial and Settings recovery paths tested. | Foreground-location opt-in >=65%; 0 premature background prompts | Blocked |
| Legal and privacy consistency | Users receive accurate, consistent disclosures and consent. | Legal, Product, Engineering | Production policy URLs; data inventory | Terms, privacy, community guidelines, promotion terms, retention, processor list, minors policy, and support path are published and consistent across platforms; acceptance version/timestamp stored server-side; no unsupported promise remains. | 100% legal checklist approved; 0 broken policy links | Blocked |
| Adult-content launch guard | Pilot users cannot encounter adult content through UI, API, cache, direct read, or notification. | Engineering, Product, QA | Remote config/build flag; content migration | NSFW creation/view/settings/direct reads/notifications disabled in production pilot; regression suite covers existing flagged content. | 0 adult-content exposures in pilot | Blocked |
| Release and observability baseline | The team can ship, detect failures, and safely roll back both apps. | Engineering, QA | Signing; CI; crash/analytics projects | One authoritative Android build config; clean signed Android/iOS releases; secrets rotated and restricted; crash reporting active; feature kill switches for creation, notifications, coupons, and media; supported-device smoke matrix passes. | Crash-free sessions >=99.7%; core action success >=99% | Blocked |

### P1 — geo-pilot requirements

| Epic | User outcome | Owner(s) | Dependencies | Acceptance criteria | Launch metric | Status |
|---|---|---|---|---|---|---|
| Task-based activation | New users understand drops and reach value quickly. | Product, Engineering, QA | Event taxonomy; seeded sample/real drops | Onboarding demonstrates discovery, distance locking, collect, and safety; records step outcomes; guest-to-account upgrade preserves inventory/preferences; location-denied path remains useful. | Onboarding >=70%; first-session valid drop view >=50% | Not started |
| Seed-market operations | Invitees open Kithe to a useful local experience rather than an empty map. | Operations, Product | Pilot partner commitments; moderation review | Named pilot zone and operator; >=30 reviewed live drops; >=5 active supply partners; >=80% of invitees have 3 relevant reachable drops; weekly freshness plan exists. | Availability >=80%; median time to first value <5 min in-zone | Not started |
| Shared analytics taxonomy | Product decisions use comparable, privacy-safe data from Android and iOS. | Product, Engineering, QA | Consent decision; dashboards | Both platforms emit versioned events for onboarding, permissions, feed load/empty, view, preview, collect, create, redemption, report/block, notification delivery/open, signup, and errors; no precise coordinates or content in analytics; QA validates payloads. | >=98% expected event completeness; <1% schema errors | Not started |
| Support and incident readiness | Users and partners can resolve account, safety, and redemption problems. | Operations, Product, Legal | Support tooling; escalation roster | In-app Help/Contact/Report a problem; password/account recovery; published response targets; safety, privacy, merchant dispute, and outage playbooks; incident owner and communication channel assigned. | >=90% first response within target; 100% critical incidents acknowledged | Not started |
| Accessibility baseline | Core journeys work with assistive technology and large text. | QA, Engineering, Product | Device/accessibility matrix | Critical journeys pass TalkBack/VoiceOver, 200% text/Dynamic Type, contrast, touch targets, focus order, reduced motion, and error announcement; blockers remediated or formally waived. | 100% critical accessibility tests pass | Not started |
| Store submission readiness | Reviewers and customers receive a complete, policy-compliant listing. | Product, Legal, Engineering, QA | Final binaries; production URLs | SDK/policy requirements validated; privacy/data-safety declarations, age rating, permission declarations, reviewer access, listings, screenshots, support/privacy URLs, signing, and iOS privacy manifest complete. | 100% store checklist complete; no preventable rejection | Not started |
| Business-offer trust | Explorers know what an offer requires, and merchants can honor it reliably. | Product, Engineering, Operations, Legal, QA | Merchant terms; support workflow | Offer terms, eligibility, expiry, inventory, redemption instructions, and merchant identity visible before travel; one redemption per user enforced; merchant can pause offer; dispute flow tested. | Offer view-to-redemption >=10%; fulfillment complaints <2% | Not started |

### P2 — post-pilot growth

| Epic | User outcome | Owner(s) | Dependencies | Acceptance criteria | Launch metric | Status |
|---|---|---|---|---|---|---|
| Complete localization | English and Spanish users receive an equivalent app, legal, and support experience. | Product, Engineering, Legal, QA | String extraction; native review | 100% string coverage on both platforms; locale-correct dates/distance/plurals; legal and store text reviewed by native speakers. | <1% untranslated-string defects | Not started |
| Cross-platform scavenger hunts | Users can create and complete reliable hunts on Android and iOS. | Product, Engineering, QA | Core pilot stability; iOS parity | Equivalent creation, progression, recovery, moderation, and analytics; interrupted/offline progress tests pass. | Hunt completion >=35% among starters | Not started |
| Invites and referrals | Communities can grow through attributable, safe invitations. | Product, Engineering, QA | Deep links; abuse controls | Group/drop links open correct destination through install/sign-in; attribution and rate limits work; blocked/expired invite states are clear. | Invite-to-activated-user >=20% | Not started |
| Verified business and richer insights | Users trust merchants and businesses understand outcomes. | Product, Operations, Engineering, Legal | Verification process; analytics quality | Verification state visible; bot/test traffic excluded; dashboard defines each metric; export available. | Verified-business complaint rate <1% | Not started |
| Mature-content reconsideration | Eligible adults can opt into mature content only if it can be operated safely and legally. | Legal, Product, Operations, Engineering, QA | Age assurance; multimodal moderation; store approval | Written regional/legal approval; effective age assurance; full moderation and appeals; store ratings/declarations; kill switch and exposure audit. | No launch until every dependency is Done | Blocked |

## Rollout cohorts and exit gates

### Cohort A — internal dogfood (20–30)

- Employees, contractors, and trusted testers; synthetic and seeded content only.
- Exercise direct Firebase abuse cases as well as all seven critical journeys.
- Exit: every P0 Done, zero critical/high defects, >=99% successful core actions, crash-free sessions >=99.7%, account deletion verified end to end.

### Cohort B — closed creator beta (50–100)

- Invited creators, businesses, operators, and a small Explorer group in one zone.
- Operations reviews every published drop and staffs moderation during announced hours.
- Exit: 30 reviewed live drops, 5 active supply partners, >=70% activation, >=80% local availability, no unresolved P0 incident, D7 >=15%.

### Cohort C — geo pilot (500–1,500)

- Invitation-led users in the same geography; creation remains rate-limited.
- Run for at least four stable weeks.
- Exit: D7 >=18%; >=30% collect/redeem within seven days; availability >=80%; crash-free >=99.7%; core action success >=99%; safety and support SLAs met each week.

### Cohort D — limited public launch

- Add one pre-seeded geography at a time; no broad paid acquisition.
- Exit to broader growth only after two zones independently meet Cohort C gates and business fulfillment complaints stay below 2%.

Any confirmed unauthorized data access, uncontained harmful-content exposure, inability to delete accounts, critical merchant fraud, or crash-free rate below 99% triggers a rollout pause.

## KPI definitions and event needs

| KPI | Definition | Required events/properties |
|---|---|---|
| Activated user | New user who loads a valid drop and then previews, collects, or redeems within 7 days. | `onboarding_completed`, `feed_loaded`, `drop_viewed`, `drop_previewed`, `drop_collected`, `offer_redeemed`; anonymous cohort ID, platform, role, zone, app version |
| Local availability | Share of active users with >=3 eligible drops within the configured practical radius. | `feed_loaded`; eligible count bucket, zone, permission state; never precise coordinates |
| Time to first value | Time from first app open to first preview/collect/redeem. | `app_first_open`, value event; server/client timestamp and session ID |
| Guest conversion | Guests who register within 7 days divided by eligible guest starters. | `guest_started`, `signup_started`, `signup_completed`; preserved-state result |
| D1/D7/D30 retention | Activated users with a meaningful session 1/7/30 days after activation. | `session_started` plus a meaningful action; activation cohort date and zone |
| Creator activation | Signed-in users who successfully publish a policy-compliant first drop. | `drop_create_started`, `drop_created`, `moderation_result`; content type, role, error code |
| Offer conversion | Unique successful redeemers divided by unique offer viewers. | `offer_viewed`, `redemption_started`, `offer_redeemed`; offer ID, business ID, failure reason |
| Reliability | Successful completion rate and latency for feed/create/collect/redeem/delete. | `operation_result`; operation, duration bucket, offline state, error code |
| Safety exposure | Confirmed violating drops viewed divided by all viewed drops. | `drop_viewed`, `report_submitted`, `moderation_decision`; pseudonymous IDs, category, severity |
| Moderation SLA | Time from report receipt to first triage and final decision. | `report_received`, `report_triaged`, `moderation_decision`; severity, queue, timestamps |
| Notification value | Opens and meaningful actions attributable to delivered nearby alerts. | `notification_sent`, `notification_delivered`, `notification_opened`, value event; campaign/type, no precise location |

Analytics must exclude drop contents, redemption codes, emails, exact coordinates, raw usernames, and other direct identifiers. Product owns taxonomy; Engineering owns implementation; QA owns payload validation; Legal approves collection and retention.

## Decision log

| Decision | Rationale | Owner | Date/status |
|---|---|---|---|
| Launch one dense geography, not a broad network. | Local liquidity is the prerequisite for value and retention. | Product | Proposed |
| Lead with nearby discovery and rewards. | This is clearer than marketing the complete feature set. | Product | Proposed |
| Disable NSFW for the pilot. | Age controls and multimodal moderation are not yet launch-ready. | Product, Legal | Proposed |
| Defer hunts from pilot scope. | They add platform parity and recovery risk before the core loop is proven. | Product | Proposed |
| Require contextual permission prompts. | Improves trust, comprehension, and opt-in quality. | Product | Proposed |
| Expand only after cohort exit gates are met. | Prevents paid growth from masking local product-market and safety failures. | Product, Operations | Proposed |

## Open questions

1. What exact pilot geography, operator, dates, and staffed hours are committed?
2. Which five or more supply partners will seed content, and who approves each drop?
3. Is Kithe intended for users 13+, 16+, or 18+, and which regions are in scope?
4. Are group drops meant to be confidential, unlisted, or merely filtered in the UI?
5. What is the deletion/export retention policy for reports, fraud evidence, transactions, and backups?
6. Who is the accountable moderation lead and what after-hours coverage is available?
7. Who bears liability and handles disputes when a merchant does not honor an offer?
8. Which event/consent framework will be authoritative across Android and iOS?
9. What constitutes a practical discovery radius in the selected geography?
10. Which features must have remote kill switches before external testing?

## Explicit go/no-go checklist

All items require a named approver and evidence link. Any unchecked P0 item is an automatic **No-Go**.

### Product and market

- [ ] Pilot geography, dates, operator, and target cohort approved.
- [ ] At least 30 reviewed drops and 5 active supply partners are live.
- [ ] At least 80% of invitees can access 3 relevant drops.
- [ ] Guest, Explorer, Business, group, notification, safety, and account-lifecycle journeys pass.
- [ ] Feature flags disable NSFW and hunts for pilot users.

### Engineering and privacy

- [ ] Media ownership and group access rules pass adversarial tests.
- [ ] Account deletion/export passes end to end on Android and iOS.
- [ ] Secrets are rotated/restricted and release builds use production configuration.
- [ ] Crash reporting, analytics, alerting, rollback, and kill switches are verified.
- [ ] Backup, retention, and deletion behavior matches published policy.

### Trust, legal, and operations

- [ ] Terms, privacy, community guidelines, promotion terms, minors policy, and store declarations are approved.
- [ ] Moderation console, policy, staffing, escalation, appeal, and reporter feedback are operational.
- [ ] Support, merchant dispute, privacy request, and incident playbooks are rehearsed.
- [ ] Permission prompts and degraded states pass product/legal/QA review.
- [ ] No open critical/high security, safety, privacy, or accessibility defect.

### Quality and rollout

- [ ] Supported-device critical-journey matrix passes.
- [ ] Crash-free sessions are >=99.7% and core action success is >=99% in dogfood.
- [ ] Accessibility baseline passes for every critical journey.
- [ ] Store assets, URLs, reviewer instructions, signing, and policy forms are complete.
- [ ] Dashboard owners can measure every Cohort C exit gate before invitations are sent.
- [ ] Product, Engineering, Independent QA, Operations, and Legal each record Go approval.

Final authority: Product may schedule rollout only after Independent QA confirms evidence and Legal/Operations approve their gates. Engineering or Independent QA may call a No-Go for an unresolved P0 condition; Operations may pause rollout for an uncontained safety or merchant incident.
