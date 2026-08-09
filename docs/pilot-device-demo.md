# Pilot device demo checklist

Three approval gates are outstanding and **all three need the same device session**, so they
are collected here rather than chased separately:

| Gate | What it needs proved | Deferred since |
| --- | --- | --- |
| **2.1** — anonymous creation removed | Guest browse still works end to end | 2026-08-06 (prod had zero drops) |
| **4.5** — scoped push notifications | A membership-scoped push actually lands on a device | 2026-08-08 |
| **4.6 prerequisite** — guest→account continuity | Guest activity survives sign-in, both link and merge paths | 2026-08-09 |

This is an evidence checklist. Record what happened, including partial failures; a step you
skipped is not a step that passed.

---

## Preconditions (verified against production 2026-08-09)

**Data is already sufficient for most of this.** 6 live drops, of which 1 is `PUBLIC` and 4
belong to experience `EATZ`; groups `EATZ`, `EPIC`, `FANF` exist; `accountMergeReceipts` is
empty, confirming no merge has ever run.

### The build

Both notification switches are ANDed, and every feature flag defaults to false in the build,
so an ordinary release build cannot run most of this. Build the internal variant with:

```
GEODROP_FEATURE_NOTIFICATIONS_ENABLED=true
GEODROP_FEATURE_CREATION_ENABLED=true      # only for the authored-drop steps
GEODROP_FEATURE_HUNTS_ENABLED=true         # only for the trail step
```

**App Check is enforced on `mergeGuestAccount`** (as on `deleteAccount` and
`updateBusinessProfile`). A debug build uses the debug provider, so its debug token must be
registered in the Firebase console or the merge will be refused with an App Check error
rather than anything that looks like a merge failure. Confirm this before going outdoors.

### Remote Config — a production change, so plan it

All five keys are `false` and `conditions` is empty, so flipping one affects every client.
Other clients are protected only by their own build flags defaulting to false.

| Key | Needed for | Flip? |
| --- | --- | --- |
| `pilot_notifications_enabled` | 4.5 entirely | Yes, then flip back |
| `pilot_creation_enabled` | Guest creating a drop (4.6 step 2) | Only for the full version |
| `pilot_hunts_enabled` | Trail step (4.6 step 4) | Only for the full version |
| `pilot_media_enabled` | Photo/audio drops | Not required |
| `pilot_coupons_enabled` | Redemption | Not required |

**A minimal 4.6 run needs no flips.** Joining an experience and collecting an existing drop
already exercises claims, inventory, and membership migration — which is most of the risk.

### Accounts

- **A Google account with no GeoDrop account yet** — for the link path. This is the one most
  likely to be missing; check before you start.
- **An existing account** — for the merge path. 25 profiles exist; `EATZ` is owned by
  `HY1o48UI…`, so use something else as the destination to keep roles simple.

---

## Part A — Guest browse (gate 2.1)

1. Clean install. Accept terms, complete onboarding.
2. Choose guest access. **Do not sign in yet.**
3. Confirm the map loads and the nearby list shows drops.
4. Deny precise location when asked. Browsing must still work, showing an **accuracy area**
   and banded distances (*Nearby* / *A short walk* / *Farther out*), never an exact metre
   figure or a centre pin.
5. Confirm no drop is auto-selected before you are located.

**Record:** did a guest see content without an account, and did coarse-only browsing stay
usable?

---

## Part B — Guest activity to migrate (4.6 setup)

As the **same guest session**, without signing in:

1. **Join `EATZ`** by code. Confirm its group drops become visible.
2. **Collect a drop.** Tap **Unlock drop** → grant precise location → the content reveals →
   **Pick up drop**. Note the drop id.
3. *(needs `pilot_creation_enabled`)* **Create a drop.** Note its id.
4. *(needs `pilot_hunts_enabled`)* **Advance one trail step.**

**Record:** the guest's drop ids and how many drops you collected. You are about to check
that exact list survives.

> Do not sign out, and do not clear app data. The guest's ID token is what proves the
> session was yours, and it is only held in memory for that session.

---

## Part C — The link path (4.6, uid preserved)

Sign in with the **new** Google account.

**Expected:** `linkWithCredential` succeeds, so **the uid never changes** and nothing moves.

- [ ] Everything from Part B is still present — collected drops, created drop, trail
      position, `EATZ` membership.
- [ ] **No** status message about guest content moving. Silence here is correct: nothing
      moved because nothing needed to.
- [ ] `accountMergeReceipts` is **still empty** — the callable was not called.

**Record:** the signed-in uid, and whether it matches the guest uid. If a merge receipt
appeared, linking failed and fell back — note why.

---

## Part D — The merge path (4.6, the code under test)

Reinstall or clear app data, then repeat Part B as a **fresh guest**. Now sign in with the
**existing** account.

**Expected:** linking fails with `ERROR_CREDENTIAL_ALREADY_IN_USE`, so the app signs in
(new uid) and calls `mergeGuestAccount`.

- [ ] Status reads *"Your guest drops and collections moved to this account."*
- [ ] **My Drops** lists the drop the guest created, now owned by this account.
- [ ] The collected drop is still collected — and **not collectable again**.
- [ ] Trail position carried over.
- [ ] `EATZ` membership carried over, and its group drops are visible.
- [ ] The destination's **own** pre-existing collections are untouched.
- [ ] A display name/username you set as a guest appears **only if** this account had none.

**Then check the server side:**

```
node -e "…"   # or the Firebase console
```

- [ ] Exactly **one** `accountMergeReceipts` document, holding counts and uid **digests** —
      never raw uids.
- [ ] The guest's `users/{uid}` document is gone.
- [ ] The guest's Auth user is gone.

**The failure worth catching:** if the status instead reads *"couldn't be moved"*, the
sign-in succeeded and the merge did not. Check the `mergeGuestAccount` logs for the reason
— `GUEST_TOKEN_INVALID`, `GUEST_NOT_ANONYMOUS`, or an App Check rejection are the likely
three, and they mean different things.

---

## Part E — Scoped notifications (gate 4.5)

Needs `pilot_notifications_enabled` **on** and the build flag true.

1. On the device, enable nearby alerts. Confirm a token registers under
   `users/{uid}/notificationTokens`.
2. From a second account that owns an experience the device account has joined, create a
   drop in it — or run:

```
node functions/scripts/demo-experience-notification.js --code=EATZ --audit
node functions/scripts/demo-experience-notification.js --code=EATZ --owner=<uid> --apply
node functions/scripts/demo-experience-notification.js --code=EATZ --retire --apply
```

3. **Read the log line.** It is the tell for which release is live:

| Log line | Meaning |
| --- | --- |
| `reached 1/2 member(s)` | PR #53 is deployed; the count is trustworthy |
| `Notified 1 member(s)` | Stale release — redeploy functions before trusting any number |

- [ ] The push arrives on the device.
- [ ] The payload carries **no location** — it says a drop exists in an experience you
      joined, never where you are.
- [ ] Turn alerts off, repeat, and confirm **no** push arrives — and that the token is gone
      and `notificationSettings/preferences` says opted out.

**Known parity gap:** the opt-out is Android-only. iOS registers tokens but never writes the
preference and never deletes its token, so an iOS user who turns alerts off keeps being sent
to. Do not test that on iOS and conclude it works.

**Afterwards: flip `pilot_notifications_enabled` back to false.**

---

## Recording the result

Update the gate sections in `docs/migration-plan.md`, and log any production action
(Remote Config flips included) in its deployment table **in the same session**. If a step
could not run, write down why and who owns it — the plan's convention is deferrals with
reasons, not waivers.

**iOS:** everything above is Android. iOS is compile-verified by CI only; there is no macOS
or Xcode toolchain on this project today, so the iOS runs stay deferred with a stated reason
rather than being inferred from the Android result.
