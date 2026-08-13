# GeoDrop R1 — Backend, Security, Migration, and Analytics Contracts

Status: **Approved — R1 gate complete; R2 local implementation authorized**  
Date: 2026-08-09  
Authority: `product-direction.md`, migration decision P8, and
`redesign-alignment-proposal.md`  
Implementation state: **approved contract; R2 is implementing the additive boundary
locally, but none of the target behavior is production-live unless explicitly described
as current**

Approval record: on 2026-08-09 the owner directed the work to continue after the complete
R1 contract and checklist were presented. That continuation approves every checklist item
below and authorizes R2 implementation and verification. It does not authorize a production
deployment, Remote Config enablement, destructive migration, or the M4 legacy cutover.

## 1. Purpose and boundary

R1 defines the contracts R2 and later redesign tasks must build against. It makes no app,
Firestore Rules, Storage Rules, function, schema, deployment, Remote Config, or production
data change.

The contract covers:

- the Experience facade over the current `groups` collection;
- the Trail facade and server-owned progress;
- discovery metadata, private payload versions, and immutable unlock receipts;
- organizer access and authoring authority;
- reward issuance, confirmed use, correction history, and Results rollups;
- callable request/response/error behavior;
- the Pilot 1 event ledger and event ownership;
- migration, compatibility, rollback, retention, and deletion behavior.

Terminology in this document follows `voice-and-glossary-v1.md`. Existing collection and
field names are written literally when they remain implementation details.

## 2. Current-state findings that drive the migration

These are facts about the checked-in implementation, not proposed behavior:

1. `drops/{dropId}` currently combines location/discovery fields with locked text, photo
   references, reward metadata, `collectedBy`, `likedBy`, `reportedBy`, and
   `redeemedBy`. A participant who may browse the document receives its payload and its
   per-user maps.
2. Proximity is checked in the clients. Firestore accepts a collection transition without
   server knowledge of the location used for that decision.
3. `users/{uid}/inventory/{dropId}` is client-writable. It is useful as a cache, but it
   cannot be promoted to authoritative proof of a valid unlock.
4. `huntChains` and `users/{uid}/huntProgress` are client-writable. The latter cannot be
   treated as authoritative Trail completion.
5. `manageGroup` lets any non-suspended authenticated session create a group. It does not
   require `BUSINESS`, organizer approval, or owner-reviewed Experience fields.
6. `updateBusinessProfile` promotes any verified-email account to `BUSINESS`. It is not a
   human approval gate.
7. `redeemDrop` assigns a code and immediately writes `redeemedBy`/`redemptionCount`.
   Those fields mean issued codes, not confirmed business use.
8. `groups/{code}/analytics/summary` is correctly owner-readable and server-writable, but
   its `collects` and `redemptions` inputs come from the legacy maps above.
9. `users/{uid}` is readable by every signed-in account and mixes public profile data with
   role, moderation, and legal/account fields.
10. Payload media can become directly readable through Storage Rules when attached to a
    public drop. That is incompatible with locked photo payloads.
11. Android already includes Firebase Analytics and emits a few client events. There is no
    complete cross-client/server implementation of the required Pilot 1 event contract.

R2 must preserve these statements as migration starting-state evidence until its cutover
gate closes. Documentation must never imply that writing this contract fixed them.

## 3. Non-negotiable invariants

1. A browse-authorized client can learn where a drop is and its locked state, but cannot
   retrieve its title, body, photo asset, reward instructions, or reward code before a
   successful server-authoritative unlock.
2. A guest may preview, join, and browse an Experience through anonymous Firebase Auth, but
   cannot unlock, create a Collection receipt, advance a Trail, receive a reward code,
   author content, or read Results.
3. A successful unlock is one server transaction: validate, write an immutable receipt,
   advance Trail progress when applicable, issue at most one reward code when applicable,
   and enqueue canonical server events.
4. Submitted latitude, longitude, and accuracy exist only in function memory for the
   request. They are not written to Firestore, Storage, analytics, error details, traces,
   or application logs.
5. Drop coordinates are venue content, not attendee-location history. They may exist in a
   discovery record; an attendee fix may not.
6. Participant identifiers do not live on readable discovery or aggregate Results
   documents. Organizers receive aggregate counts and code state, never attendee identity
   or paths.
7. Only an approved `BUSINESS` account that owns an Experience may publish or mutate that
   Experience and its drops. `BUSINESS` remains the only elevated account role.
8. Reward **issued** and confirmed **used** are distinct states. Issuance reserves
   inventory; use and correction are owner-authorized and audited.
9. Earned payload snapshots never change. Editing a live drop creates a new payload version
   for future unlocks. Removal is limited to the governed account-deletion, legal, or safety
   takedown paths; expiry and ordinary organizer edit/delete do not rewrite the snapshot.
10. All value-bearing writes are Admin-SDK writes behind App Check-enforced callables.
    Remote Config is a kill switch, not an authorization boundary.
11. Old clients fail explicitly at cutover; they are never kept functional through an
    insecure legacy write/read path.
12. No destructive migration runs without an exact dry run, backup, integrity manifest,
    project-id confirmation, and a separately approved point-of-no-return gate.

## 4. Identifier and time conventions

- `experienceCode`: normalized human invitation code and Pilot 1 `groups` document id.
  Uppercase ASCII, 4–32 characters, not logged in analytics. The backend generates new
  codes from an unambiguous word/number scheme and checks collisions transactionally.
- `dropId`, `trailId`, `receiptId`, `rewardCodeId`, `eventId`: random Firestore ids. No
  semantic or user information is encoded in an id.
- `entrySessionId`: random 128-bit value for one QR/link/code entry attempt. The landing
  page creates/preserves it through Install Referrer, preview, anonymous Auth, account gate,
  and resumed unlock; it expires after 24 hours and is not an advertising/device id.
- `installKey`: app-generated random installation value used only to dedupe first-open. It
  resets on reinstall and is never sourced from an advertising identifier.
- Firestore domain time fields use server `Timestamp`, never device milliseconds. Callable
  responses serialize them as RFC 3339 strings.
- Schema fields are integers starting at `1`. Callable requests carry `apiVersion: 1` and
  responses carry `schemaVersion: 1`.
- A request never supplies a user id for an operation on the caller. The backend resolves
  it from the verified Auth context.
- User-facing code says Experience, Trail, Nearby, Collection, host, found, issued, and
  used. Legacy group/hunt/collect/redeem names stay internal until migrated.

## 5. Target Firestore and Storage schemas

### 5.1 Private account and public creator profile

`users/{uid}` becomes owner-readable, Admin-SDK-writable for protected fields, and no
longer globally readable to signed-in clients.

Target additions/invariants:

| Field | Type | Contract |
| --- | --- | --- |
| `role` | `EXPLORER` \| `BUSINESS` | Server-owned. `BUSINESS` iff organizer access is approved. |
| `organizerAccessStatus` | `NONE` \| `PENDING` \| `APPROVED` \| `DENIED` | Server-owned workflow state, not a role. |
| `organizerAccessSubmittedAt` | timestamp/null | Server-owned, sourced from the external application process. |
| `organizerAccessReviewedAt` | timestamp/null | Server-owned. |
| existing legal/moderation fields | existing types | Remain private and server-preserved. |

Invariant: `role == BUSINESS` if and only if `organizerAccessStatus == APPROVED`. R2 must
repair or refuse any off-model combination.

`creatorProfiles/{uid}` is the safe projection used by creator/host UI:

| Field | Type | Contract |
| --- | --- | --- |
| `schemaVersion` | int | `1` |
| `hostLabel` | string | Public professional/display label, 1–100 chars. |
| `username` | string/null | Existing public username if present. |
| `organizationName` | string/null | Approved organizer label. |
| `updatedAt` | timestamp | Server time. |

Clients do not write this projection directly. A trusted function maintains it from safe
profile fields. It contains no email, role, application status, categories, legal state,
moderation state, or account timestamps.

`organizerApplications/{uid}` is readable/writable only by admin/operator tooling. It may
contain organization name, contact details, description, submitted terms version, status,
and review timestamps. The participant app receives only status and timestamps through a
callable/profile read, never the application body.

`organizerApplicationTokens/{tokenDigest}` is direct-client-denied, single-use, and expires
after 30 minutes. It binds the external form submission to the authenticated account without
putting uid/email in the form URL. Raw tokens are never stored.

### 5.2 Experience facade over `groups`

Pilot 1 retains `groups/{experienceCode}` to avoid a high-risk collection rename. Target
documents use:

| Field | Type | Contract |
| --- | --- | --- |
| `schemaVersion` | int | `2` distinguishes an Experience-ready record. |
| `code` | string | Must equal the document id. |
| `ownerId` | string | Approved organizer uid; immutable without a later ownership-transfer design. |
| `name` | string | Required, 1–100 chars. |
| `description` | string/null | At most 240 chars. |
| `hostLabel` | string | Safe preview label, denormalized from creator profile. |
| `startsAt` / `endsAt` | timestamp | Required; end after start. |
| `timeZone` | string | Required IANA identifier, at most 64 chars. |
| `defaultRadiusM` | int | 15–100; default 25 for newly created drops. |
| `state` | `PUBLISHED` \| `CANCELLED` | No scheduled/draft Experience state in Pilot 1. |
| `createdAt` / `publishedAt` / `updatedAt` | timestamp | Server-owned. |

The code remains an invitation capability. Direct `get` is allowed only to the owner or an
existing member; collection `list` remains denied. Pre-membership preview is returned by
`resolveExperience`, which exposes only the preview response in section 6.

Membership remains `users/{uid}/groups/{experienceCode}`:

| Field | Type | Contract |
| --- | --- | --- |
| `schemaVersion` | int | `2` |
| `code` | string | Experience code. |
| `ownerId` | string | Denormalized owner. |
| `role` | `OWNER` \| `SUBSCRIBER` | Experience-scoped, server-owned. |
| `joinedAt` / `updatedAt` | timestamp | Server-owned. |

Only the member reads their membership. Membership enumeration remains backend-only. No
guest list, email list, name list, or live attendee state is added to the Experience.

### 5.3 Discovery record

R2 introduces `experienceDrops/{dropId}` rather than stripping the legacy `drops` document
in place. The parallel collection permits a rehearsed cutover and makes legacy payload
exposure easy to deny as one explicit step.

Allowed fields:

| Field | Type | Contract |
| --- | --- | --- |
| `schemaVersion` | int | `1` |
| `experienceCode` | string | Required membership scope. |
| `ownerId` | string | Host traceability and owner authorization. |
| `hostLabel` | string | Safe participant-facing label. |
| `state` | `PUBLISHED` \| `DELETED` | Draft/scheduled not accepted. |
| `moderationState` | `PENDING` \| `SAFE` \| `BLOCKED` | Participants receive only `SAFE`. |
| `lat` / `lng` | number | Drop coordinates, valid geographic bounds. |
| `radiusM` | int | 15–100. New default 25; migrated missing values use 30. |
| `contentKind` | `TEXT` \| `PHOTO` | Audio/video rejected. |
| `dropKind` | `STANDARD` \| `REWARD` | Reveals mechanic, not reward payload/code. |
| `payloadVersion` | int | Current immutable payload version, positive. |
| `trailId` | string/null | Ordered Trail membership. |
| `trailStepIndex` / `trailTotalSteps` | int/null | Zero-based step and total, validated together. |
| `likeCount` | non-negative int | Server-maintained aggregate only; no user map. |
| `createdAt` / `publishedAt` / `updatedAt` | timestamp | Server-owned. |
| `editedAt` | timestamp/null | Drives the quiet Edited marker. |
| `expiryMode` | `NONE` \| `CUSTOM` \| `EXPERIENCE_END` | Explicit source of expiry. |
| `expiresAt` | timestamp/null | Required only for `CUSTOM`; `EXPERIENCE_END` reads the live Experience end. |

No other keys are accepted. In particular the document never contains title, body,
description, media URL/path/data, alt text, reward terms, reward code, inventory limit,
issued/used count, user maps, report details, or exact attendee location. `likeCount` is the
only participant-engagement aggregate on discovery.

Participants query exactly one joined, currently active Experience with equality filters
for published/safe records. Before `startsAt` and after `endsAt`, only preview/Collection
remain available. Owners may query their Experience records including moderation state.
Expiry is enforced again by `unlockDrop`; a stale cached discovery record never authorizes
value.

Pilot 1 may display all Trail pins as locked. Later-step coordinates are therefore not
treated as secret; `unlockDrop` still enforces sequence. If product later requires hidden
future coordinates, that is a new server-filtered discovery design, not a client filter.

### 5.4 Private payload versions

`dropPayloads/{dropId}` (server/organizer-callable access only):

| Field | Type |
| --- | --- |
| `schemaVersion` | `1` |
| `dropId`, `experienceCode`, `ownerId` | string |
| `currentVersion` | positive int |
| `createdAt`, `updatedAt` | timestamp |

`dropPayloads/{dropId}/versions/{version}` is immutable after creation:

| Field | Type | Contract |
| --- | --- | --- |
| `schemaVersion` | int | `1` |
| `title` | string | Required, 1–80 chars. |
| `body` | string/null | At most 2,000 chars; required when no photo. |
| `contentKind` | `TEXT` \| `PHOTO` | Must match discovery. |
| `mediaAssetId` | string/null | Server asset reference, never a public URL. |
| `mediaMimeType` | string/null | Pilot allows JPEG/PNG/WebP after server validation. |
| `mediaAltText` | string/null | Required for photo, at most 240 chars. |
| `rewardPresentation` | map/null | Reward label, business label, instructions, terms; never a code. |
| `createdAt` | timestamp | Version creation time. |

An edit adds the next integer version and atomically updates the private parent plus the
discovery pointer/Edited timestamp. Existing unlock receipts are untouched.

### 5.5 Private payload media

- Client upload staging: `drop-upload-staging/{uid}/{uploadId}`. Only the authenticated,
  non-anonymous owner may create/read/delete their object. Image only, at most 10 MiB,
  metadata allowlisted, no overwrite, no download token.
- Published private asset: `drop-payloads/{dropId}/{version}/{assetId}`. No Firebase client
  read/write path. A trusted function validates/copies the staged image and deletes staging.
- `getCollectionMedia` returns a short-lived (10 minute), read-only signed URL only after
  verifying the caller's unlock receipt and matching payload version. The URL is never
  stored in Firestore or analytics.
- Clients cache an unlocked photo for offline Collection use under platform-private app
  storage. Cache loss requires a new online signed URL; it never changes entitlement.
- Organizer preview uses the same callable pattern with owner authorization.

### 5.6 Immutable unlock receipt and Collection

`users/{uid}/unlocks/{dropId}` is Admin-SDK-write-only and caller-read-only:

| Field | Type | Contract |
| --- | --- | --- |
| `schemaVersion` | int | `1` |
| `receiptId` | string | Random id, also used for deidentified reward linkage. |
| `dropId`, `experienceCode` | string | Required. |
| `unlockedAt` | timestamp | Server time. |
| `payloadVersion` | int | Version actually earned. |
| `source` | `SERVER_PROXIMITY_V1` | No legacy/unverified record may claim this value. |
| `snapshot` | map | Title, body, content kind, host label, media asset id/mime/alt, reward presentation, Edited timestamp. |
| `trail` | map/null | Trail id, step index, total steps, completion-at-unlock bool. |
| `hasRewardReceipt` | bool | Indicates a separate user reward receipt exists. |

The receipt contains no latitude, longitude, accuracy, distance, user display name, reward
code, mutable like state, or live pointer to payload text. Firestore Rules deny every client
create/update/delete. Only governed account-deletion/legal/safety operations may remove it.

The redesigned Collection reads this subcollection. Legacy local `CollectedNote` and
`inventory` remain caches/migration input only and never authorize payload or reward value.

### 5.7 Trail and server-owned progress

`groups/{experienceCode}/trails/{trailId}`:

| Field | Type | Contract |
| --- | --- | --- |
| `schemaVersion` | int | `1` |
| `title` | string | Required, 1–100 chars. |
| `dropIds` | list<string> | Ordered, unique, 1–20 for Pilot 1. |
| `isMain` | bool | Exactly one designated main Trail in the pilot Experience. |
| `state` | `ACTIVE` \| `RETIRED` | Server/owner callable only. |
| `version` | int | Incremented on an allowed reorder before participants start. |
| `createdAt`, `updatedAt` | timestamp | Server-owned. |

`users/{uid}/trailProgress/{trailId}` is server-write-only and caller-read-only:

| Field | Type |
| --- | --- |
| `schemaVersion` | `1` |
| `experienceCode`, `trailId` | string |
| `trailVersion` | int |
| `currentStepIndex` | int |
| `completedDropIds` | ordered list<string> |
| `startedAt`, `updatedAt` | timestamp |
| `completedAt` | timestamp/null |

Only `unlockDrop` advances progress. It accepts the first incomplete step, is idempotent for
an already-earned step, rejects a future step, and emits `trail_completed` once. R1 does not
approve changing Trail order after any participant starts; that requires a later policy.

### 5.8 Rewards: definition, code pool, user receipt, and audit

`rewards/{dropId}` is server/owner-callable access only:

| Field | Type |
| --- | --- |
| `schemaVersion` | `1` |
| `dropId`, `experienceCode`, `ownerId` | string |
| `state` | `ACTIVE` \| `PAUSED` \| `CLOSED` |
| `inventoryLimit` | positive int |
| `issuedCount`, `usedCount` | non-negative int; `usedCount <= issuedCount <= inventoryLimit` |
| `createdAt`, `updatedAt` | timestamp |

`rewards/{dropId}/codes/{rewardCodeId}` is direct-client-denied:

| Field | Type | Contract |
| --- | --- | --- |
| `code` | string | Pre-generated unambiguous display code. |
| `codeHash` | string | Normalized lookup/dedup key. |
| `state` | `AVAILABLE` \| `ISSUED` \| `USED` | Correction moves `USED` back to `ISSUED`. |
| `receiptId` | string/null | Random unlock receipt linkage, never uid. |
| `issuedAt`, `usedAt` | timestamp/null | Server time. |
| `lastChangedBy` | string/null | Organizer/operator uid, server-only audit field. |
| `version` | int | Optimistic/audit version. |

`users/{uid}/rewardReceipts/{dropId}` is server-write-only and caller-read-only:

| Field | Type |
| --- | --- |
| `schemaVersion` | `1` |
| `receiptId`, `dropId`, `experienceCode`, `rewardCodeId` | string |
| `code` | string |
| `state` | `ISSUED` \| `USED` |
| `issuedAt`, `usedAt`, `updatedAt` | timestamp/null |

Each status transition appends
`rewards/{dropId}/codes/{rewardCodeId}/events/{eventId}` with from/to state, timestamp,
actor, and reason. Clients cannot edit or delete history. Organizer list/search callables
return code, state, and timestamps but never uid, receipt owner, email, or path.

Issuance happens inside `unlockDrop`, not a separate participant action. The transaction
assigns one AVAILABLE code, writes both receipts, increments issued count, and returns the
same code on every retry. If no code is available, content unlock still succeeds and the
response reports reward state `UNAVAILABLE`; no count changes.

### 5.9 Private aggregate Results

Keep `groups/{experienceCode}/analytics/{docId}`, owner-readable and Admin-SDK-write-only.

`summary` fields:

- `schemaVersion: 2`
- `experienceCode`
- `joinedParticipants`
- `publishedDrops`
- `uniqueUnlockers`
- `unlocks`
- `mainTrailCompletions`
- `codesIssued`
- `codesUsed`
- `updatedAt`, `reconciledAt`

Per-drop documents use id `drop_{dropId}` and contain `dropId`, `unlocks`, `codesIssued`,
`codesUsed`, and timestamps. They contain no participant keys.

Incremental triggers use membership, unlock receipt, Trail progress, and reward-code state
as sources. The daily reconcile recomputes from those source collections. It does not read
legacy `collectedBy`/`redeemedBy` after cutover.

### 5.10 Simple likes without participant maps

Migration decision 0.3 kept simple likes; P8 did not reverse it. Preserve the capability
without carrying `likedBy` on discovery:

- `users/{uid}/likes/{dropId}` contains `dropId`, `experienceCode`, and server timestamps;
  caller-read, Admin-SDK-write/delete only.
- `setDropLike` requires a non-anonymous account, Experience membership, and an unlock
  receipt for the drop. It idempotently creates/removes the private like record and adjusts
  discovery `likeCount` transactionally.
- Likes are not an organizer Result or pilot success threshold. Later screen work may omit
  the control without deleting this signed migration capability.
- Account export/merge/delete explicitly classifies the likes subcollection. No organizer
  or participant can list who liked a drop.

## 6. Callable contracts

All value-bearing callables use region `us-central1`, enforce App Check, reject suspended
accounts, validate `apiVersion`, allowlist request keys, impose per-account/device/IP abuse
limits appropriate to the entry point, and return stable machine reasons in `HttpsError`
details. User-facing copy belongs to the client.

### 6.1 Entry and membership

`resolveExperience`

- Request: `{apiVersion: 1, code: string, entrySessionId?: string}`.
- Auth: optional for the owned web landing; anonymous Auth required in the app. App/web abuse
  controls still apply.
- Response: `{schemaVersion, experience: {code, name, description, hostLabel, startsAt,
  endsAt, timeZone, state, availability: UPCOMING|ACTIVE|ENDED|CANCELLED,
  availableDropCount}, membership: NONE|MEMBER|OWNER}`.
- A valid preview response idempotently emits `invite_link_opened` for the entry session;
  channel distinguishes QR/link/manual code. The web landing uses the same resolver so a
  direct Android App Link and a cold-install landing cannot double-count.
- Never returns owner uid, members, exact drop coordinates, Results, or payload.
- Reasons: `INVALID_CODE`, `EXPERIENCE_NOT_FOUND`, `EXPERIENCE_CANCELLED`, `RATE_LIMITED`.

`joinExperience`

- Request: `{apiVersion: 1, code: string, entrySessionId?: string}`.
- Auth: any non-suspended Firebase user, including anonymous.
- Behavior: idempotently creates/repairs the caller's membership, never accepts a role or
  owner id from the request, and emits one `experience_joined` event.
- Response: preview plus caller membership.
- Reasons: resolve reasons plus `EXPERIENCE_ENDED`.

`leaveExperience`

- Request: `{apiVersion: 1, code: string}`.
- Subscribers only. Owners cannot leave. Historical unlock/reward receipts remain in
  Collection; membership-scoped notifications stop.

### 6.2 Organizer approval and profile

`createOrganizerApplicationLink`

- Auth: non-anonymous account; request contains only `apiVersion`.
- Returns the approved external-form URL with a short-lived, single-use opaque application
  token. The token resolves to the caller only on the backend; raw uid/email is not placed
  in the URL.
- Valid form ingestion changes `NONE` to `PENDING`. Calling this function alone never
  claims submission and never changes role.

`setOrganizerAccessDecision` is operator/admin-only (custom claim, not merely `BUSINESS`).

- Request: `{apiVersion: 1, targetUid, decision: PENDING|APPROVE|DENY,
  applicationSubmittedAt?, termsVersion?}`.
- Atomically maintains the user status/role invariant and the private application record.
- APPROVE writes `BUSINESS` + `APPROVED`; DENY writes `EXPLORER` + `DENIED`; PENDING does
  not elevate.
- Every decision writes a server-only audit event.

`updateBusinessProfile` changes semantics in R2: it may update labels/categories only when
the caller is already `BUSINESS`/`APPROVED`. Verified email alone can never elevate a role.

### 6.3 Experience and authoring

`createExperience`

- Auth: non-anonymous approved organizer.
- Request: name, description, starts/ends, time zone, default radius. No caller-selected
  owner or code.
- Behavior: generates code, creates Experience plus OWNER membership atomically, emits
  `experience_published` once.

`updateExperience`

- Owner only. Allows name, description, dates, time zone, default radius, and cancellation.
- Owner/code/createdAt are immutable. Changing Experience end immediately affects only
  drops with `expiryMode == EXPERIENCE_END`; CUSTOM/NONE drops are unchanged. The organizer
  UI must disclose this consequence before saving the date change.

`saveDrop`

- Owner only. Creates or edits an immediately published text/photo drop.
- Request allowlists location, radius, expiry mode/custom time, text/photo payload, optional
  staging upload id, drop kind, and optional Trail placement. No scheduled/draft state.
- Creates immutable payload version plus discovery record atomically after media validation.
- Edit increments version; earlier unlock receipts remain unchanged.
- Emits `drop_created` only for first publish.

`deleteDrop`

- Owner only, soft-deletes discovery and closes reward issuance. Earned receipts and used
  code history remain. It never deletes a participant's Collection item.

`getOrganizerDrop` and organizer-media access return private payload only after owner
authorization. Organizers do not get attendee receipts.

### 6.4 Server-authoritative unlock

`unlockDrop`

Request:

```json
{
  "apiVersion": 1,
  "dropId": "random-document-id",
  "entrySessionId": "optional-random-session-id",
  "location": {
    "lat": 19.0,
    "lng": -155.0,
    "accuracyM": 12.5,
    "capturedAt": "RFC-3339 timestamp"
  }
}
```

Authorization and validation order:

1. App Check, authenticated non-anonymous account, not suspended.
2. Strict request shape/numeric bounds; no uid or Experience code trusted from request.
3. Caller membership in the drop's Experience.
4. Experience published/not cancelled and current time within its start/end window; drop
   published/safe/not deleted/not expired under its expiry mode.
5. Location age at most 30 seconds; positive reported accuracy no worse than
   `min(radiusM, 30 m)`.
6. Haversine distance. Accept when `distanceM <= radiusM + accuracyM`; otherwise return only
   the distance-to-boundary bucket `0_25`, `25_50`, or `50_PLUS`.
7. Trail step is the current allowed step.
8. Transactionally create or read the immutable receipt, advance Trail, and issue at most
   one reward code.

The accuracy allowance preserves the existing accuracy-aware shape while bounding it: a
new 25 m drop can pass no farther than 50 m when the fix reports the worst accepted
accuracy. Outdoor R6 testing may tighten the constants, but changing the formula requires a
contract revision and test evidence rather than a client-only tweak.

Success response:

```json
{
  "schemaVersion": 1,
  "receipt": {"dropId": "...", "payloadVersion": 1, "unlockedAt": "...", "snapshot": {}},
  "trail": {"state": "NOT_IN_TRAIL|ADVANCED|COMPLETED", "nextDropId": null},
  "reward": {"state": "NONE|ISSUED|ALREADY_ISSUED|UNAVAILABLE", "code": null}
}
```

An idempotent retry returns the same receipt/snapshot/code without re-emitting canonical
events or consuming inventory. The function never returns the server-computed exact
distance, submitted fix, other users, reward pool, or private next-step payload.

Stable reasons: `ACCOUNT_REQUIRED`, `EXPERIENCE_NOT_JOINED`, `DROP_NOT_AVAILABLE`,
`DROP_EXPIRED`, `LOCATION_INVALID`, `LOCATION_STALE`, `ACCURACY_INSUFFICIENT`, `TOO_FAR`,
`TRAIL_STEP_LOCKED`, `RATE_LIMITED`, `CONTRACT_VERSION_UNSUPPORTED`.

### 6.5 Collection media

`getCollectionMedia`

- Request: `{apiVersion: 1, dropId, payloadVersion}`.
- Verifies the caller's immutable receipt matches both values.
- Returns mime type, alt text, and a 10-minute signed URL. No URL is returned for a
  different payload version, organizer-owned-but-not-earned participant request, or guest.

### 6.6 Reward operations

`provisionRewardCodes` is admin/operator-only for Pilot 1. It validates uniqueness, code
format, pool size, and an owner-approved reward before writing AVAILABLE codes. The
founder-supplied validation list comes from this exact pool.

`listRewardCodes`

- Owner only. Request has drop id, state filter, search code, and bounded cursor/limit.
- Returns code, state, issued/used timestamps, and history summaries; never participant
  identity or receipt owner.

`markRewardCodeUsed`

- Owner only. Request `{apiVersion, dropId, code}`.
- `ISSUED -> USED`, updates the user's private reward receipt through its random receipt
  linkage, appends audit history, increments `codesUsed`, and emits the canonical used event.
- AVAILABLE, wrong Experience owner, unknown code, or already-used states get distinct
  stable reasons. An already-used retry is idempotent success.

`correctRewardCodeUse`

- Owner only. Request includes drop id, code, and fixed reason enum `MARKED_BY_MISTAKE` or
  `BUSINESS_CORRECTION`.
- `USED -> ISSUED`, clears current `usedAt`, appends history, decrements `codesUsed`, and
  updates the user's private receipt. History is never erased.

### 6.7 Analytics ingestion

`recordClientEvent`

- Auth: anonymous or non-anonymous app user; App Check required.
- Request contains a random event id, one client-owned event name, occurrence time, entry
  session id, app version/platform, optional resource context
  (`experienceCode`/`dropId`/`trailId`), and the allowlisted params in section 8.
- The function computes actor/Experience/drop keys; raw uid, code, coordinates, payload,
  reward code, and arbitrary resource values inside params are rejected. It verifies any
  supplied context exists and is visible to the caller before hashing it.
- Writes once by deterministic event id. Clients cannot read the ledger.

The owned web entry service writes `invite_link_opened` directly using the same ledger
schema and entry-session dedupe. It must not depend on an Android install already existing.

`recordAuthCompletion` is called after anonymous guest-session creation and after a later
credential link or merge. It derives `GUEST_SESSION` versus `ACCOUNT` from the verified
token rather than trusting a requested role, accepts the entry session/upgrade path only,
records whether a pending unlock is being resumed, and emits at most once per auth stage
per entry session. A client cannot use it to claim another account's completion.

### 6.8 Account lifecycle compatibility

Existing `requestAccountExport`, `deleteAccount`, and `mergeGuestAccount` retain their
recent-auth, policy-version, App Check, destination-wins, and Auth-delete-last guarantees.
R2 must extend their classified path lists before target documents can be written:

- Export: private profile, creator profile, application belonging to the caller,
  memberships, unlocks, reward receipts, Trail progress, blocks, owned Experiences/drops,
  submitted reports, and actor-scoped raw pilot events.
- Participant delete: remove the user's tree, unlock/media entitlement, reward receipts,
  Trail progress, likes, memberships, and raw actor events; anonymize reports and reward
  linkage; preserve only deidentified organizer aggregate Results.
- Organizer delete: additionally cancel and remove owned Experiences/discovery/private
  payload/media/Results, and remove participant unlock/reward receipts whose retained
  snapshots contain that organizer's authored content, matching the existing governed
  behavior that deletes collected copies of an account's owned drops. Deidentified reward
  audit may remain only for the approved retention window.
- Guest merge: move membership and blocks. Target guests cannot own unlock/reward/Trail
  value, but the implementation must explicitly classify those paths and refuse unexpected
  data rather than silently discard it. Link-in-place remains the normal path.
- Analytics identity: link-in-place keeps the actor. Merge-to-existing writes a server-only
  alias from guest actor key to destination actor key; it never exposes the uid pair.

### 6.9 Likes, reporting, and blocking

`setDropLike` follows section 5.10 and accepts only `{apiVersion, dropId, liked: bool}`.
The caller cannot supply counts or identity.

`submitReport` accepts an anonymous or account member, drop id, fixed reason enum, and
bounded optional narrative. The backend verifies visibility, resolves the host, writes the
private moderation record, and emits `report_submitted` without narrative or identity in
analytics. A report is idempotent per caller/drop/reason window.

`blockHost` accepts a visible drop id, resolves the host server-side, and writes the caller's
private block record. It emits `block_created` once and never adds blocker/host ids to
discovery or Results. Guest blocks follow the existing safe merge behavior.

## 7. Error contract

All callable failures use a Firebase `HttpsError` code plus:

```json
{
  "reason": "STABLE_MACHINE_REASON",
  "retryable": false,
  "field": null,
  "retryAfterSeconds": null,
  "distanceBucket": null,
  "contractVersion": 1
}
```

Rules:

- Clients branch on `reason`, never English message text.
- Details never contain uid, email, Experience code, reward code, location, exact distance,
  payload text/path, or existence information the caller is not authorized to know.
- `not-found` intentionally collapses missing/deleted/blocked private resources where a
  distinction would leak information.
- `resource-exhausted` carries bounded retry time for abuse limits.
- Network unavailable/timeouts are client transport states, not invented server reasons.
- Unexpected errors return a correlation id. Server logs use that id plus safe resource
  digests, not request bodies.

## 8. Pilot analytics contract

### 8.1 Canonical destination and privacy shape

The canonical Pilot 1 source is an append-only, Admin-SDK-only Firestore ledger at
`analyticsEvents/{eventId}`. Firebase Analytics may continue as supplemental Android
observability, but thresholds and founder reports use the ledger so server and client
events share one dedupable dataset. This closes the provider decision for Pilot 1 without
adding a third-party dependency.

Event fields:

- `schemaVersion`, `eventName`, `eventVersion`
- `origin: ENTRY|CLIENT|SERVER`
- `occurredAt`, `receivedAt`
- `actorKey` (HMAC of uid with a managed secret) or null before Auth
- `entrySessionId` (random, non-advertising id) or null
- `experienceKey`, `dropKey`, `trailKey` (HMAC/digests, never invitation/resource ids)
- `platform`, `appVersion`
- allowlisted `params`
- `dedupeKey`
- `expiresAt` for 180-day raw-event TTL

No event contains coordinates, exact distance, accuracy, email, display name, raw uid,
Experience code, payload text/media, reward code, application data, report narrative, or
notification body.

### 8.2 Required 21 events and single owner

All ledger documents are server-written. “Origin” below means where the fact originates.

| Event | Origin | Dedupe/source contract |
| --- | --- | --- |
| `invite_link_opened` | ENTRY | Once per entry session + Experience; fires when valid preview renders. Params: channel. |
| `app_first_open` | CLIENT | Once per install key. No advertising id. |
| `auth_completed` | SERVER | Once per auth stage/entry session. Params: guest/account, link/merge when relevant, pending unlock resumed bool. |
| `experience_joined` | SERVER | Membership create only; repair/retry emits none. |
| `location_permission_result` | CLIENT | Params: approximate/precise, granted/denied/dont_ask_again, context. |
| `map_loaded_with_drops` | CLIENT | Once per Experience/session when at least one drop renders. |
| `drop_viewed_locked` | CLIENT | Once per drop/session. |
| `unlock_attempted` | CLIENT | One per explicit tap. Params include account state and whether account gate appeared. |
| `unlock_failed_distance` | SERVER | `unlockDrop` rejection only. Param: boundary bucket; no exact distance. This corrects the draft table's client ownership because the server owns the rejection. |
| `unlock_succeeded` | SERVER | Receipt creation only; idempotent read emits none. |
| `drop_collected` | SERVER | Same transaction as receipt creation. Retained as Collection-integrity event, not a separate conversion step. |
| `trail_completed` | SERVER | First completion transition only. |
| `redemption_code_issued` | SERVER | Reward receipt/code assignment only. Event name retained for metrics compatibility; semantics are issued. |
| `redemption_code_marked_used` | SERVER | First ISSUED→USED transition. Correction and later re-use each have distinct audit versions but do not double-count current Results. |
| `push_sent` | SERVER | Per Experience broadcast outcome, with reached/failed buckets but no token/user ids. |
| `push_opened` | CLIENT | Notification id digest + Experience, once per open. |
| `report_submitted` | SERVER | Accepted report create only; no narrative in event. |
| `block_created` | SERVER | New block only; no target uid in event. |
| `feedback_submitted` | SERVER | Accepted response; analytics params contain rating categories, not free text. |
| `drop_created` | SERVER | First publish only, not edits. Params: text/photo, standard/reward. |
| `experience_published` | SERVER | Experience creation/publish transition once. |

Other unlock failures are observable through safe function outcome counters/log correlation,
but adding a new product event requires a versioned analytics-contract change. Do not emit
the same canonical event from both client and server.

### 8.3 Derived funnel definitions

- Invited/Eligible remain timestamped organizer-supplied offline numbers.
- Opened: unique entry sessions with `invite_link_opened`.
- Authenticated: guest/account `auth_completed` or an already-authenticated entry session;
  account-gate conversion uses the later ACCOUNT stage for a session that began as guest.
- Activated: same entry/account alias joined an Experience and produced
  `map_loaded_with_drops`, before first success.
- First value: at least one server `unlock_succeeded` in the Experience.
- Engaged: at least three unique successful drop receipts in one Experience.
- Completed: one `trail_completed` for the designated main Trail.
- Cross-Experience retention: one canonical actor key joins a second Experience within the
  approved 30/90/180-day windows.
- Reward conversion: unique codes used divided by unique codes issued. A correction changes
  current Results and preserves transition history.

Raw events expire after 180 days. Account deletion removes actor-scoped raw events and
identity aliases; deidentified aggregate Results remain. Retention values require Legal
confirmation before production launch, but they are the implementation default for R2.

## 9. Direct-access security matrix

Callables may return the specifically authorized response described above. Direct
Firestore/Storage access is narrower:

| Resource | Guest/non-member | Anonymous member | Account member | Experience owner | Admin SDK |
| --- | --- | --- | --- | --- | --- |
| Experience preview | callable only | callable/direct member get | same | direct get | full |
| Membership | none | own read | own read | own only; no guest list | full |
| Private `users` profile | none | own read | own read | own read | full |
| Creator profile | no enumeration | read when referenced | same | own read | write/full |
| Discovery drops | none | joined Experience published/safe | same | owned Experience incl. moderation | write/full |
| Payload/version | none | none | none; unlock callable only | organizer callable only | full |
| Private payload media | none | none | signed URL only with receipt | signed URL only as owner | full |
| Unlock receipt | none | none | own read | no attendee reads | write/full |
| Trail progress | none | none | own read | aggregate only | write/full |
| Reward receipt/code | none | none | own receipt read | code list via callable, no user | full |
| Results | none | none | none unless owner | owner read | write/full |
| Analytics ledger/identity links | none | none | none | none | full/operator tooling |
| Organizer application | none | status response only | own status response | own status response | full/operator tooling |

Required Rules tests in R2 include signed-out, anonymous non-member/member, account
member, wrong owner, correct owner, suspended, moderator, admin, and direct-write attempts
for every server-owned resource.

## 10. Required indexes and configuration for R2

R2 must declare and validate production indexes before any query ships:

- `groups`: owner query by `ownerId`; member collection-group `groups.code` index remains.
- `experienceDrops`: composite participant query on `experienceCode`, `state`,
  `moderationState`, and `publishedAt`; owner query on `experienceCode`/state.
- reward codes: collection-scope `codeHash` equality and state/issuedAt pagination.
- analytics events: actorKey + occurredAt and experienceKey + occurredAt for deletion/report
  tooling; TTL on `expiresAt`.
- target unlock/reward collection-group indexes required by reconcile or account lifecycle
  scripts must be statically validated, extending the existing index validator.

R2 adds `pilot_redesign_backend_enabled` and a minimum supported data-contract version to
Remote Config, both default false/unsupported. Rules/callable authorization holds
regardless of the flag.

## 11. Migration, backfill, compatibility, and rollback

### M0 — inventory and backup (read-only first)

Produce an exact report for production and emulator fixtures:

- groups, memberships, owner/role consistency, and missing Experience fields;
- drops by visibility/type/content/media/expiry/group/owner and all payload-bearing fields;
- `collectedBy`, inventories, hunt chains/progress, reward receipts/maps, analytics rollups;
- BUSINESS accounts and how each was approved;
- Storage objects, metadata, download tokens, and orphaned paths;
- all root/subcollections, indexes, functions, ruleset ids, and Remote Config version.

Classify every document/object as migrate, archive, preserve, or flag. Back up before any
write and produce hashes/counts in an integrity manifest. A nonzero unclassified count
blocks M1.

### M1 — additive backend

Deploy target collections, deny-by-default Rules, callables, indexes, event ledger, and
tests behind a false routing flag. Do not change legacy reads/writes yet. New private media
paths deny all clients by default; callables use Admin SDK.

Gate: adversarial tests prove no target payload/receipt/code/Results exposure while legacy
clients still operate in the internal environment.

### M2 — deterministic backfill rehearsal

Run dry run, emulator rehearsal from backup, then separately approved production apply:

- enrich each `groups` record to schema 2. Name may default to code only in staging; dates,
  timezone, host label, approval, and state require explicit owner input before publish;
- copy each eligible group drop to `experienceDrops`; write payload version 1 from legacy
  text/body/photo; copy media into private paths and strip download tokens;
- interpret missing legacy radius as 30 m; new authoring defaults to 25 m;
- convert resolvable hunt chains to Experience Trails and retain `legacyHuntId` in a
  server-only migration map;
- do **not** promote client-writable inventory, collected maps, or hunt progress to
  `SERVER_PROXIMITY_V1`. Archive/reset test data by default. Any discovered real participant
  requires a human disposition list and may only receive a visibly `LEGACY_UNVERIFIED`
  archive outside authoritative Collection;
- migrate valid legacy `likedBy.<uid> == true` entries to private user likes and reconcile
  only the aggregate `likeCount`; never copy the identity map to target discovery;
- import each existing server-issued code as reward state ISSUED, never USED; create its
  private user reward receipt/code record and set new Results `codesIssued`; `codesUsed=0`;
- rebuild creator projections and audit every current BUSINESS account. No unreviewed
  BUSINESS account is silently grandfathered as approved;
- seed new Results from target records and compare expected counts before clients read them.

Every writer is idempotent and stamps migration version/source. A retry produces zero
duplicate payload versions, codes, receipts, or events.

### M3 — target-client rehearsal

Android internal builds use only target entry/discovery/unlock/Collection paths. Exercise
all callable reasons and account/reward/Trail paths against migrated fixtures. iOS remains
on legacy paths during this parallel stage but gets an explicit minimum-contract gate ready
for cutover; it must not fail as unexplained empty content.

### M4 — cutover gate (point of no return)

In one reviewed release window:

1. Publish a current Android build with target contract support and an explicit update/
   unavailable screen for unsupported clients.
2. Deploy functions and target Rules; verify exact live source and indexes.
3. Turn off legacy creation/collect/redeem paths at the Rules layer.
4. Deny participant reads of legacy `drops` payload documents and legacy public media.
5. Route current Android to target; keep all feature flags fail-closed until smoke evidence.
6. Unsupported iOS/Android versions fail with an explicit contract-version screen. No
   insecure compatibility exception is permitted.

After step 4, rollback may not reopen legacy participant payload reads or client collection
writes. Recovery is roll-forward or restore to a private/admin-only staging project.

### M5 — reconcile and observe

Recompute Results from target receipts/reward states, compare migration manifests, verify
no precise-location fields/logs, run account export/delete/guest merge, validate signed URL
expiry, and inspect error/abuse counters. Only then enable a controlled device cohort.

### M6 — archive and eventual purge

Keep legacy collections/media read-denied and production-backup-protected for a documented
observation window. Purge only in a separate destructive task with a new dry run and owner
approval. Never make the target depend on legacy payload or maps during that window.

### Rollback boundaries

- Before M4 step 4: disable target routing and fix/re-run additive migration; legacy internal
  clients remain available.
- After M4 step 4: never restore legacy participant reads/writes. Roll forward target code,
  restore target data from the migration backup into private paths if necessary, or disable
  the pilot.
- New USED reward history cannot be collapsed back into the old “redeemed at issuance”
  meaning. Preserve and roll forward it.
- Backups and manifests are not deleted as part of rollback or code cleanup.

## 12. Retention and lifecycle defaults

These defaults require Legal confirmation before production exposure:

- submitted location fix: request-memory only; no retention;
- signed media URL: 10 minutes;
- upload staging: purge after 24 hours;
- unlock and reward receipts: account lifetime, deleted with account;
- reward pool/use audit: 180 days after Experience end, with user linkage removed on
  account deletion;
- raw pilot analytics and identity aliases: 180 days, actor data removed on account
  deletion;
- aggregate Results: retained after participant deletion, but deleted with the owning
  organizer/Experience;
- organizer application link token: 30 minutes or first successful form ingestion;
- declined organizer application: 90 days unless Legal requires shorter;
- account export object: existing 24-hour policy; signed link: existing 15-minute policy.

R2 must update the account lifecycle policy version before any new path becomes live.

## 13. R1 gate review

### Security proof by construction

- **Locked payload:** absent from the only listable drop document; private versions and
  media have no direct participant read.
- **Exact attendee location:** callable input only, explicitly excluded from every stored,
  returned, logged, analytics, and error schema.
- **Participant identity:** absent from discovery, reward owner responses, and Results;
  receipt linkage uses random ids and raw analytics uses protected HMAC actor keys.
- **Organizer-only metrics:** existing owner-read/Admin-write pattern retained and expanded;
  no client-writable source counter is authoritative.
- **Authorization:** organizer role/status invariant, Experience ownership, non-anonymous
  unlock, App Check, and server-only value writes are independent of UI/Remote Config.

### Approval checklist

- [x] Accept the target paths and field allowlists in section 5.
- [x] Accept the callable/error contracts in sections 6–7.
- [x] Accept Firestore ledger as canonical Pilot 1 analytics and the corrected server owner
  for distance failure in section 8.
- [x] Accept the 25 m new / 30 m migrated radius rule and bounded accuracy formula.
- [x] Accept that legacy client-writable Collection/Trail data is not promoted to an
  authoritative receipt without human disposition.
- [x] Accept the M4 point of no return and explicit unsupported-client behavior.
- [x] Accept the proposed retention defaults as implementation defaults pending Legal.
- [x] Confirm R2 may implement the server boundary in the order defined here.

The active task is now **R2 only**. R2 must produce its implementation plan, tests,
migration rehearsal, deployment plan, and gate evidence; approval of R1 does not authorize
an unreviewed production migration.
