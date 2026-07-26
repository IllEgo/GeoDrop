# GeoDrop Moderation Operations — Pilot Draft

Status: **Backend workflow draft; staffing, Legal approval, and operational
rehearsal are required before launch**

The backend creates one private moderation case for each user report, captures
the reported drop's text/media metadata as evidence, records server timestamps,
and writes a separate status document readable only by the reporter. Moderator
callables require an `admin` or `moderator` Firebase custom claim. Every queue
view, triage, decision, and appeal decision also creates an append-only moderator
audit event. A scheduled SLA monitor emits a structured `MODERATION_SLA_BREACH`
error for every overdue open case so production alerting can route it.

## Severity and response targets

| Severity | Examples | First triage target | Required response |
|---|---|---:|---|
| Critical | Credible imminent harm, child sexual abuse material, active violent threat, or a valid emergency/legal escalation | Under 1 hour while staffed | Preserve evidence, remove exposure when warranted, notify the incident lead immediately, and use the approved emergency/legal channel. |
| High | Violent content or a credible serious-safety report without confirmed imminence | Under 4 hours | Prioritized review; escalate to Critical when context indicates imminent harm. |
| Medium | Harassment, non-consensual sexual content, or repeated targeted abuse | Under 24 hours | Review content and account history; remove or suspend when policy requires. |
| Low | Spam, misleading promotion, duplicates, or low-risk policy issues | Under 24 hours | Standard queue review. |

Automated intake assigns `violence` High, `nsfw`/`harassment` Medium, and other
categories Low. A human moderator must promote imminent-harm cases to Critical.

## Case workflow

1. A signed-in user reports a drop and receives a `RECEIVED` status.
2. The report trigger creates a severity-ranked private case with evidence and a
   server receipt timestamp.
3. A moderator claims/triages the case. The first triage timestamp is immutable
   evidence for the SLA calculation.
4. The moderator selects `REMOVE_CONTENT`, `DISMISS`, or `ESCALATE` and records a
   rationale. Removal sets the drop's canonical deletion fields. Suspension sets
   a custom claim, revokes refresh tokens, and records the case on the profile.
5. The reporter receives `ACTION_TAKEN`, `CLOSED`, or `ESCALATED`; private
   rationale and enforcement details are not exposed.
6. The affected account may submit one documented appeal. A different moderator
   should decide it whenever staffing allows.

## Illegal or imminent-harm escalation

- Do not promise emergency response outside staffed pilot hours.
- Preserve original evidence and audit timestamps; do not circulate media in
  general chat or issue trackers.
- Escalate Critical cases to the named incident lead and Legal using the private
  channels recorded below.
- Contact emergency services only under the approved regional policy and only
  with the minimum necessary data.
- File required provider or statutory reports through the approved process.

## Coverage

The queue captures `contentType`, Storage path, MIME type, text/description,
drop type, visibility, and the reporter's source context. This covers text,
photo, audio, and video drops at intake. The callable-backed operator console
lists, triages, decides, and resolves appeals without granting direct database
access. It accepts moderator ID and App Check tokens only through environment
variables and relies on server audit events for access logging. Its production
endpoint, claims, and alert routing must still be verified before it is
operational.

Run the console from `functions/` with `npm run moderation:console -- --help`.
Run the isolated end-to-end emulator rehearsal with:

```text
firebase emulators:exec --only auth,firestore,storage,functions --project geodrop-ci "npm --prefix functions run moderation:rehearse"
```

The 2026-07-21 rehearsal passed report ingestion, authenticated queue access,
Critical triage, video removal, suspension, appeal by the affected user,
independent overturn, content restoration, suspension reversal, reporter status,
and moderator audit events.

## Launch roster and evidence

| Requirement | Named owner/channel | Status |
|---|---|---|
| Trust & Safety lead | Unassigned | Blocking |
| Pilot staffed hours and timezone | Unassigned | Blocking |
| Critical after-hours contact | Unassigned | Blocking |
| Legal escalation channel | Unassigned | Blocking |
| Moderator custom-claim provisioning and removal | Provisioning script and emulator claim flow passed; production roster not assigned | Blocking |
| Secure reviewer UI/CLI | Callable-backed CLI and server audit events implemented; production access not verified | Blocking |
| Appeal reviewer | Unassigned | Blocking |
| SLA dashboard and alerting | Scheduled breach detector implemented; production log-based alert and owner not connected | Blocking |

No external beta may begin until every blocking row has an owner, the moderator
workflow has been rehearsed with text/photo/audio/video fixtures, reporter
status is verified on both apps, and Legal approves the escalation and evidence
retention policy.
