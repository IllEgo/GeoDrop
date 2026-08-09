# GeoDrop — Feature Inventory (Task 0.1)

Companion to `docs/product-direction.md` (target state) and `docs/migration-plan.md`
(ordered work). This is the **Phase 0 ground-truth map**: every feature in the codebase,
classified against the direction doc's launch-scope and deferred lists, with the files,
Firestore collections, Storage paths, and Cloud Functions each one touches. It is the
input that downstream deletion (Phase 2) and rules-hardening (Phase 1/3) tasks depend on.

Produced read-only. No code was changed. Status: **classification confirmed with the
owner — zero unclassified rows remain** (see Resolved seam decisions).

## Legend

| Tag | Meaning |
|---|---|
| **L** | Launch scope — build/keep for v1 |
| **L (partial)** | Launch scope, only partly implemented |
| **D** | Deferred and **present in code** → removal / gating target |
| **D (future)** | Deferred ambition, not built; recorded for sequencing |
| **D·absent** | Deferred and not implemented → nothing to remove |
| **Infra / supporting** | Not a headline feature; enabling machinery |

Platform abbreviations: **A** = Android, **i** = iOS.

---

## 1. Launch-scope items

From `docs/product-direction.md` §"Launch scope (v1)".

| Feature | Class | Where implemented | Data / Functions | Status & notes |
|---|---|---|---|---|
| Account creation + controlled guest access | **L** | A `ui/DropHereScreen.kt`, `MainActivity.kt`; i `Services/AuthService.swift`, `ViewModels/AppViewModel.swift` | `users`, `usernames`; `claimExplorerUsername` | Built. Email/pw + Google on both; guest = browse-only. **No anonymous auth** anywhere (good — matches safety constraint). |
| Map view + nearby-drop list | **L** | A `ui/DropHereScreen.kt` (GoogleMap); i `Views/DropFeedView.swift` | `drops` via `FirestoreRepo.getVisibleDropsForUser` | Built. |
| Text drops | **L** | A `data/Drop.kt`, `ui/DropHereScreen.kt`; i `Views/CreateDropView.swift` | `drops` | Built. |
| Photo drops | **L** | A `data/MediaStorageRepo.kt`; i `Services/StorageService.swift` | Storage `drops/photos/…` (A) / `drops/{uid}/…` (i) | Built. ⚠ iOS photo path bypasses the server Vision trigger — see §4. |
| Audio drops (conditional) | **L** | A `ui/AudioRecorderActivity.kt`; i `Views/CreateDropView.swift` (`AudioRecorderSheet`) | Storage `drops/audio/…` | Built. Direction doc permits for tours/storytelling; must never block the pilot. |
| Proximity unlocking | **L** | A `geo/GeofenceManager.kt`, `NearbyDropRegistrar.kt`, `GeofenceReceiver.kt`, `DropDecisionReceiver.kt`; i `ViewModels/AppViewModel.swift` | `drops` | Built on A (10 m geofence, 30 m pickup radius). **iOS is foreground-preview only** — no region monitoring. Android impl depends on background location — see the tension row in §2. |
| Drop expiration | **L** | A `data/Drop.kt` (`decayDays`/`isExpired`); i `Models/Drop.swift:146` | `drops` | Built client-side. **No server-side purge of expired drops** — expiration is filter-only. |
| Collect / claim | **L** | A `geo/DropDecisionReceiver.kt`, `FirestoreRepo.markDropCollected`; i `Services/NoteInventoryService.swift` | `drops.collectedBy`, `users/{uid}/inventory`; `notifyDropCreatorOnCollection`, `cleanupCollectedNotesOnDropDelete` | Built. Local-first inventory synced via `data/UserDataSyncRepository.kt`. |
| Basic creator profile | **L (partial)** | A `data/UserProfile.kt`, `ui/DropHereScreen.kt` (self); i `Models/UserProfile.swift`, `Views/ProfileView.swift` | `users` | Self-profile + `@username` handles only; no per-creator public profile page. Likely sufficient for "basic". |
| Report and block | **L** | A `ui/DropReportingUi.kt`, `FirestoreRepo` report/block; i `Services/FirestoreService.swift` | `reports`, `users/{uid}/blockedCreators`; `ingestUserReport` + moderation suite | Built **ahead of Phase 5**. |
| Simple redemption code (business rewards) | **L** | A `FirestoreRepo.redeemDrop`; i `Services/FirestoreService.swift:238` | `drops` (`redemptionCode`/`Limit`/`Count`/`redeemedBy`) | Built for `RESTAURANT_COUPON`. Codes are client-supplied drop fields; no server-side code generation. |
| Organizer analytics | **L (partial)** | i `Views/ProfileView.swift:231-518` (business dashboard) | — | **iOS client dashboard only.** No backend aggregation function and no Android equivalent. Launch-scope item still to complete (Phase 4.4). |
| Push notifications for joined experiences only | **L** | A `messaging/GeoDropMessagingService.kt`, `FirestoreRepo.registerMessagingToken`; i `Services/MessagingService.swift`; backend `notifyDropCreatorOnCollection` | `users/{uid}/notificationTokens`; FCM `sendEachForMulticast` | Built & **scoped** — per-user tokens, ownership/collection-triggered; geofence alerts scoped to the user's visible/joined drops. **No topic broadcast** exists. |

---

## 2. Deferred items

From `docs/product-direction.md` §"Explicitly deferred". Present-in-code rows are
removal/gating targets for the phase noted.

| Feature | Class | Where (if present) | Data / Functions | Disposition |
|---|---|---|---|---|
| Public anonymous posting | **D** | Anonymous *auth* **absent** (good). "Post anonymously" display toggle present: A `Drop.isAnonymous` (`DropHereScreen.kt:2367`); i `CreateDropView.swift:176` | `drops.isAnonymous`, `dropperUsername` | **Decision C.** Remove the toggle so every drop shows a display name. Account traceability already existed; the public-anonymous surface is cut for the pilot. |
| NSFW content + detection | **D** | A `util/GoogleVisionSafeSearchEvaluator.kt`, `DropSafetyEvaluator.kt`, `DropSafetyClassifier.kt`; i `Services/SafeSearchService.swift`; backend `safeSearch`, `analyzeOnUpload` | Cloud Vision; `dropModerationQueue`; `drops.isNsfw` | Present but pilot force-disables via `PilotFeatureFlags.nsfwEnabled` + rules force `isNsfw==false`. Phase 2.2 removal. |
| Open direct messaging | **D·absent** | — | — | Not implemented on either client. Nothing to remove. |
| Groups — invite-only (join primitive) | **L** | A `data/GroupMembership.kt`, `FirestoreRepo` group ops; i `Views/GroupManagementView.swift`; backend `manageGroup` | `groups`, `users/{uid}/groups`; `drops` GROUP visibility | **Decision A → Launch.** Invite-code / private (rules deny `list`/enumeration) = the "experiences the user explicitly joined" mechanism. Group drops are text-only. **Follow-up:** gate creation to organizer/business accounts. |
| Groups — public / discoverable | **D (future)** | Not built (would loosen the `groups` `list` deny) | — | **Decision A → Deferred future.** Records the ambition; requires the full Phase 5 safety stack (rate limits, reputation, group-level report/block + moderation, discovery/location controls, store-policy work) before any public exposure. |
| Video uploads | **D** | A `ui/DropVideoPlayer.kt`, capture in `DropHereScreen.kt`; i `CreateDropView.swift` video capture | Storage `drops/videos/…`; `Drop.contentType=VIDEO` | Present, no transcoding. Phase 2.3 removal. |
| Broad / background location tracking | **D** | A `ACCESS_BACKGROUND_LOCATION` (`AndroidManifest.xml:16`) for geofencing; `NearbyDropRegistrar`, `GeofenceReceiver` | — | Present via background geofencing. **Tension:** launch-scope proximity-unlock currently *depends* on it. Phase 3 reworks to one-time precise location; do not simply delete. |
| Likes (simple) | **L** | A `FirestoreRepo.setDropLike`; i `AppViewModel.swift:677` | `drops.likeCount`, `drops.likedBy` | **Decision B → Launch.** Simple likes stay as a supporting feature. |
| Dislikes / downvotes | **D — REMOVED 2.6** | — (was A `FirestoreRepo.setDropLike` dislike path; i `AppViewModel.swift`) | — (was `drops.dislikeCount`, `drops.dislikedBy`, `inventory.isDisliked`) | **Decision B → Deferred. Executed at 2.6 (2026-07-26)** on Android, iOS, and the rules layer. The fields are no longer allowed keys in `firestore.rules`, so any write naming one is denied. The like half was kept intact. |
| Complex voting systems | **REMOVED 2.6** | — | — | Direction-doc line resolved by Decision B: keep simple likes, drop dislikes/weighting. The concrete weighting was the `MOST_POPULAR` sort's `likeCount - dislikeCount` net score; it now ranks on likes alone. |
| Algorithmic recommendations | **D·absent** | — | — | Not implemented. |
| Multiple account types w/ extensive permission matrices | **L (scoped) — ENFORCED 2.7** | A `UserRole{EXPLORER,BUSINESS}`; group `GroupRole{OWNER,SUBSCRIBER}`; Auth claims `moderator`/`admin`/`suspended` | `users.role` | Two user types (explorer + business) are launch-needed. Moderator/admin/suspended are **operational claims**, not a user-facing matrix. **Confirmed at 2.7 (2026-07-29): nothing to remove — but the model was not enforced.** `role` is now server-authored end to end (business metadata included), parsed exactly on both clients, and off-model values fail closed. iOS's unused `GroupRole.EDITOR` removed. See `docs/account-model.md`. |
| National / global discovery | **D·absent** | — | — | Not implemented; discovery is proximity/group-scoped. |
| Consumer subscriptions / ads / data sale / crypto | **D·absent** | — | — | Not implemented. |

---

## 3. Features present but on neither direction-doc list

Classified inline; none left unclassified.

| Feature | Class | Where | Data / Functions | Note |
|---|---|---|---|---|
| Scavenger-hunt chains | **L** | A only: `data/HuntChain.kt`, `HuntBuilderState.kt`, `FirestoreRepo` hunt ops | `huntChains`, `users/{uid}/huntProgress` | **Decision D → Launch.** This is how the pilot's "main trail" / challenge works. **iOS has none — parity gap** to resolve in the pilot platform decision. Flag-gated (`PilotFeatureFlags.huntsEnabled`). |
| Explorer usernames (unique handles) | Supporting (L) | A `data/ExplorerUsername.kt`; backend `claimExplorerUsername` | `usernames` | Supports profiles and display-name handling. |
| Account export + deletion (GDPR-style) | **L** (Phase 5.4) | A `data/AccountLifecycleRepo.kt`, `ui/AccountDataDialog.kt`; i `Services/AccountLifecycleService.swift`; backend `requestAccountExport`, `deleteAccount`, purges | `accountDeletionReceipts`; Storage `account-exports/` | Built ahead of plan. Requires reauth + policy-version match. |
| Legal consent / terms gating | **L** (Phase 7) | A `data/LegalConsentRepo.kt`, `util/TermsPreferences.kt`; backend `legalConsent.ts` | `users/{uid}/legalAcceptances` | Built. **Policy URLs are DRAFT placeholders** — see `project_legal_policies` memory. |
| Moderation queue / cases / appeals / audit | **L** (Phase 5.2) | backend `moderationOperations.ts` | `moderationCases`, `moderationAppeals`, `moderationAuditEvents`, `users/{uid}/reportStatuses` | Built ahead of plan. **Backend-only — no client moderator console yet** (a `functions/scripts` CLI exists). |
| Business drop templates + `TOUR_STOP` type | Supporting (L) | A `data/BusinessDropTemplates.kt`, `BusinessCategory.kt`; i `Models/BusinessDropTemplate.swift` | `drops` | Supports business + tourism verticals. |
| Remote-config feature flags (fail-closed) | Infra | A/i `PilotFeatureFlags` | Firebase Remote Config + BuildConfig | Gating infra: creation, notifications, coupons, media, nsfw, hunts. |
| Guest → account continuity | Infra | A `data/GuestAccountUpgrade.kt`; i `Services/AuthService.swift`; backend `mergeGuestAccount` | `users`, `drops`, `usernames`, `accountMergeReceipts` | **Rewritten at 4.6.** Sign-in links the anonymous account in place (uid preserved); `mergeGuestAccount` covers only the case where the credential already belongs to an account. Replaces `migrateExplorerAccount`, which pointed the wrong way and was refused by rules. |
| Rate limiting / account reputation | **D·absent** (Phase 5.3) | — | — | Not implemented; direction doc requires it before public exposure (esp. before public groups). |

---

## 4. Cross-platform / integrity flags

Not blockers for this task, but they change later phases and are recorded here.

- **iOS photo Storage path bypasses server moderation.** Android writes `drops/photos/…`
  which triggers the `analyzeOnUpload` Cloud Function; **iOS writes `drops/{uid}/…`,
  which does not** — iOS relies solely on its client-side `SafeSearchService`. Relevant
  to Phase 2.2 (and to any decision to keep server-side moderation).
- **iOS lacks two Android features:** background geofencing/region monitoring (iOS
  proximity is a foreground preview only) and the entire scavenger-hunt-chain system.
- **No server-side expired-drop purge** — expiration is client-filtered only; expired
  drops persist in `drops`.
- **Backend-only collections have no explicit rules blocks** (`dropModerationQueue`,
  `moderationCases`, `moderationAppeals`, `moderationAuditEvents`,
  `accountDeletionReceipts`, `users/*/legalAcceptances`) — they rely on default-deny and
  Admin SDK writes. Worth an explicit deny rule when the Phase 1 harness lands.

---

## 5. Data-model index

### Firestore — top-level collections

| Collection | Referenced by |
|---|---|
| `drops` | A `FirestoreRepo.kt:34`, `GeofenceReceiver.kt:60`, `DropDetailActivity.kt:342`; i `FirestoreService.swift:21`; Fn `index.ts:637`, `accountLifecycle.ts`, `moderationOperations.ts`; rules `firestore.rules:140` |
| `users` | A `FirestoreRepo.kt:35`, `NearbyDropRegistrar.kt:145`; i `FirestoreService.swift:22`; Fn `index.ts`, `accountLifecycle.ts`, `legalConsent.ts`, `moderationOperations.ts`; rules `:18` |
| `usernames` | A `FirestoreRepo.kt:36`; i `FirestoreService.swift:23`; Fn `index.ts:318`, `accountLifecycle.ts`; rules `:69` |
| `reports` | A `FirestoreRepo.kt:37`; i `FirestoreService.swift:24`; Fn `moderationOperations.ts:428`, `accountLifecycle.ts`; rules `:85` |
| `groups` | A `FirestoreRepo.kt:44`; i `FirestoreService.swift:502`; Fn `index.ts:439`; rules `:120` |
| `huntChains` | **A only** `FirestoreRepo.kt:41`; rules `:581` |
| `accountDeletionReceipts` | Fn `accountLifecycle.ts:322,411` (backend-only) |
| `accountMergeReceipts` | Fn `accountLifecycle.ts` `mergeGuestAccount` (backend-only); rules deny client read and write |
| `dropModerationQueue` | Fn `index.ts:9,20,641,668` (backend-only) |
| `moderationCases` / `moderationAppeals` / `moderationAuditEvents` | Fn `moderationOperations.ts` (backend-only) |

### Firestore — subcollections (`users/{uid}/…` unless noted)

| Subcollection | Referenced by |
|---|---|
| `notificationTokens` | A `FirestoreRepo.kt:53,308`; i `FirestoreService.swift:681`; Fn `index.ts:75`; rules `:617` |
| `inventory` | A `FirestoreRepo.kt:47`; Fn `index.ts:801` + collectionGroup `accountLifecycle.ts:130`; rules `:504` |
| `blockedCreators` | A `FirestoreRepo.kt:50`; i `FirestoreService.swift:325`; rules `:480` |
| `huntProgress` | **A only** `FirestoreRepo.kt:56`; rules `:603` |
| `groups` | Fn `index.ts:441`; rules `:475` (callable-only) |
| `reportStatuses` | Fn `moderationOperations.ts:98`; rules `:639` (backend-only) |
| `legalAcceptances` | Fn `legalConsent.ts:102` (backend-only) |

### Firebase Storage

| Path | Referenced by |
|---|---|
| `drops/{photos\|audio\|videos}/…` | **A** `MediaStorageRepo.kt:26-38` |
| `drops/{uid}/…` | **i** `StorageService.swift:25` (⚠ bypasses `analyzeOnUpload`, which only watches `drops/photos/`) |
| `account-exports/{uid}/…` | Fn `accountLifecycle.ts:250` (backend + signed URLs only; rules deny direct access) |

### Cloud Functions (all v1, `us-central1`)

`safeSearch`, `claimExplorerUsername`, `updateBusinessProfile`, `manageGroup`,
`synchronizeDropMediaAccess`, `analyzeOnUpload`, `applyPendingModeration`,
`notifyDropCreatorOnCollection`, `cleanupCollectedNotesOnDropDelete` (`index.ts`);
`requestAccountExport`, `deleteAccount`, `purgeExpiredAccountExports`,
`purgeExpiredDeletionReceipts` (`accountLifecycle.ts`); `getLegalPolicyManifest`,
`recordLegalAcceptance` (`legalConsent.ts`); `ingestUserReport`, `listModerationQueue`,
`triageModerationCase`, `decideModerationCase`, `submitModerationAppeal`,
`decideModerationAppeal`, `alertOverdueModerationCases` (`moderationOperations.ts`).

---

## 6. Resolved seam decisions

Four features sat on the launch/deferred seam and were confirmed with the owner:

- **A — Groups.** Invite-only groups → **Launch** (the explicitly-joined-experience
  primitive; enumeration already denied). Public/discoverable groups → **Deferred
  future** behind the full Phase 5 safety stack. Follow-up: restrict group creation to
  organizer/business accounts.
- **B — Likes vs dislikes.** Likes → **Launch**. Dislikes/downvote maps → **Deferred**,
  removed in Phase 2.6.
- **C — "Post anonymously" toggle.** → **Deferred**; remove so every drop shows a
  display name.
- **D — Scavenger-hunt chains.** → **Launch**, as the pilot's "main trail" / challenge.
  iOS parity gap flagged.

---

## 7. Coverage check

Every §"Launch scope" item (13, including conditional audio) is mapped in §1. Every
§"Explicitly deferred" item (11) is mapped in §2:

- **Present → removal/gating target:** NSFW, video uploads, dislikes, anonymous-posting
  toggle, public/discoverable groups, broad/background location.
- **Absent → nothing to remove:** open DMs, algorithmic recommendations, national/global
  discovery, subscriptions/ads/data-sale/crypto.
- **Reclassified to Launch after review:** invite-only groups, simple likes, two account
  types.

Features on neither list are enumerated in §3. **No feature appears twice; zero rows
remain unclassified.**
