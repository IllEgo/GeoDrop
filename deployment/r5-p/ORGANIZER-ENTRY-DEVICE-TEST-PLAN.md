# Organizer-entry route — device test plan

Status: **plan only. Nothing in this document has been executed. Every step marked GATED
requires explicit owner approval at the moment of the action.**

Subject: the Organizer-access entry route added in commit `0fe4876`
("Add organizer access entry flow and optional release signing").

Goal: verify this chain on real hardware without replacing, uninstalling, or damaging the
signed release currently installed on the Samsung SM-S938U.

> Experience entry → Organizer access → sign in → approved Organizer tools →
> open/create Experience → QR displayed → Share QR / Save QR

---

## 1. Current facts established by inspection

### Worktree

- Branch `master`, working tree **clean**. Everything previously listed as modified or
  untracked is committed in `0fe4876`. No unrelated change is at risk.

### Installed app on the connected device

| Property | Value |
| --- | --- |
| Device | `R5CY114LNCE`, `SM_S938U` |
| Package | `com.kitheapp` |
| versionCode / versionName | `1` / `1.0` |
| Installed | 2026-08-25 15:57 |
| Installer | `null` — sideloaded, **not** Play-installed |
| Signing cert SHA-256 | `2B:7B:4D:86:B8:C4:70:A2:1D:18:1B:42:8D:98:F6:DB:A8:4D:6F:93:8A:69:0A:8B:42:63:47:30:94:5E:9C:B1` |

That fingerprint is the documented Kithe Play upload key
(`deployment/r5-p/UPLOAD-KEY-HANDLING.md`). **A locally built, upload-signed APK is
signature-compatible with what is installed**, so `adb install -r` is an in-place update
that preserves app data. No uninstall is required and none is proposed.

### Where the verification targets actually live

`R7OrganizerContent.kt` owns the event-code card, the QR bitmap, **Share QR**, and
**Save QR** (`ActivityResultContracts.CreateDocument("image/png")`). That composable is
reached only through this branch in `DropHereScreen.kt:4298`:

```
if (r7OrganizerApproved && showOrganizerTools) -> R7OrganizerContent      // has QR
else if (!r7TargetEnabled && isBusinessUser && showOrganizerTools) -> BusinessHomeDestination  // no QR
```

So the QR half of the objective is reachable **only on the R7 path**, and:

- `r7OrganizerApproved` requires `r7TargetEnabled`, which is
  `r7OrganizerGateway != null || PilotFeatureFlags.redesignBackendEnabled`.
- `redesignBackendEnabled` is fail-closed: it needs Remote Config
  `pilot_redesign_backend_enabled = true` **and**
  `pilot_redesign_min_contract_version` in `1..1`
  (`R6RolloutPolicy.SUPPORTED_CONTRACT_VERSION = 1`). Remote Config initialization is
  currently gated, so on any production build today this is `false`.
- The QR panel additionally requires `BuildConfig.APP_LINK_CONFIGURED`, i.e. the build must
  pass `KITHE_APP_LINK_HOST=join.kitheapp.com`. An unset host yields
  `r5-unconfigured.invalid` and the QR block renders nothing.
- `r7OrganizerGateway` is non-null only in a **debug** build
  (`MainActivity.kt:52-68` guard on `BuildConfig.DEBUG`) *and* only while
  `r5EntryStore.activeExperienceCode() == DebugDemoR5EntryGateway.DEVICE_DEMO_CODE`.

### Why the App Check story forces the build-variant choice

Every relevant callable is `runWith({enforceAppCheck: true})` — `redesign.ts:26` covers the
R5/R6/R7/R9 gateways, plus `index.ts`, `legalConsent.ts`, `accountLifecycle.ts`,
`moderationOperations.ts`.

`A5B-EVIDENCE.md` already recorded that the upload-signed **release** sideload returned
**HTTP 403** from Play Integrity App Check, exactly as expected for an APK that Play did not
install and re-sign. A release-variant sideload therefore cannot exercise sign-in, organizer
approval, or Experience creation — it will fail at the first protected call.

The `internal` build type is the intended vehicle: `initWith release`,
`USE_PLAY_INTEGRITY_APPCHECK = false`, and `internalImplementation firebase-appcheck-debug`.
`GeoDropApplication.kt:16` resolves `shouldUseDebugProvider = BuildConfig.DEBUG || !usePlayIntegrityAppCheck`,
so `internal` installs the **debug** App Check provider and bypasses Play Integrity.

**But `internal` is currently signed with the debug keystore** (`app/build.gradle`), which
does not match the installed release. Installing it would fail with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` and the only way through would be uninstalling the
signed release — which deletes its data and is explicitly out of bounds.

### Isolated test package is not viable as-is

`app/google-services.json` registers exactly one client, `com.kitheapp`. Adding an
`applicationIdSuffix` (e.g. `com.kitheapp.internal`) makes the Google Services plugin fail
at configuration time, and fixing it means **registering a second Android app in
`kithe-production`** — a Firebase mutation, plus a second App Check registration and a
second Maps/OAuth client. That is a larger and less reversible footprint than the in-place
signed update below, so an isolated package is **not** recommended.

---

## 2. Recommended approach — two tracks, cheapest first

### Track B (run first): emulator + `debug` variant + device-demo fixture

Zero risk to the physical device and zero Play/Firebase configuration change. An AVD is
already available locally (`Medium_Phone_API_36.0`, `android-36` system image).

This works because the debug-only fixtures are fully local:

- `DebugDemoR7OrganizerGateway.loadAccessState()` returns `APPROVED` unconditionally.
- `DebugDemoR7OrganizerGateway.createExperience()` writes to the in-memory
  `DebugDemoExperienceStore`.
- With the demo gateway installed, `r7TargetEnabled` is `true` **without Remote Config**.

Sequence:

1. Build `assembleDebug` with `-PKITHE_APP_LINK_HOST=join.kitheapp.com` (needed for the QR
   panel to render at all).
2. Install to the **emulator only** — every `adb` command in this track must carry
   `-s emulator-5554`. Never `-d`, never a bare `adb install`.
3. Enter the device-demo code on the Experience-entry screen and join. This sets
   `KEY_ACTIVE_EXPERIENCE`, which is what arms `r7GatewayOverride`.
4. Cold-restart the app. (`r7GatewayOverride` reads `activeExperienceCode()` as a plain
   value during composition, not as observable state, so a restart makes the arming
   deterministic.) `clearPendingEntry()` deliberately does **not** clear
   `KEY_ACTIVE_EXPERIENCE`, so the Organizer route keeps the demo gateway armed.
5. Walk the full chain and capture screenshots at each step.

What this proves: the whole UI route, the auto-open logic, QR rendering, the share chooser,
and the SAF save document flow.

What it does **not** prove: real App Check, real organizer approval, real
`createExperience` callable behaviour, or Play Integrity.

**GATED — one approval needed:** step 4's auto-open branch requires a **non-anonymous**
user (`DropHereScreen.kt:3080`, `currentUser?.isAnonymous != false`). Reaching approved
Organizer tools therefore needs one real Firebase sign-in with an existing
owner-controlled account. That is a production auth session and may write a user-profile
document. If the owner prefers zero production contact, Track B can stop at "the Account
tab opens and the sign-in dialog appears", which still verifies the new routing code, and
the remainder waits for Track A.

### Track A (only after Track B passes): device + upload-signed `internal` candidate

One code change is required first, and it is small:

> In `app/build.gradle`, make the `internal` build type use the `upload` signing config
> **when `uploadSigningConfigured` is true**, falling back to the debug keystore otherwise.
> This keeps the current behaviour for anyone without the keystore env vars, and makes an
> owner-run build produce an APK that updates the installed release in place.

Build (all three key env vars set together; the build fails closed on a partial set):

```
KITHE_UPLOAD_STORE_FILE / KITHE_UPLOAD_STORE_PASSWORD / KITHE_UPLOAD_KEY_PASSWORD
-PKITHE_APP_LINK_HOST=join.kitheapp.com
assembleInternal
```

Unset the three variables immediately after the build, per `UPLOAD-KEY-HANDLING.md`.

Install with `adb -s R5CY114LNCE install -r <internal.apk>` — same package, same signature,
`versionCode 1` over `versionCode 1`, so this is an update and app data survives.
Rollback is reinstalling the retained release APK the same way.

**GATED — approvals this track needs, each stated separately:**

| # | Action | Scope / reversibility |
| --- | --- | --- |
| A1 | Apply the `internal` signing change to `app/build.gradle` | Source-only; inert until the env vars are set |
| A2 | Use the upload keystore for a **local, non-Play** build | No Play upload; key never leaves the machine |
| A3 | `adb install -r` the internal APK over the installed release | In-place update, data preserved; reversible by reinstalling the release APK |
| A4 | Register the App Check **debug token** printed to logcat in the Firebase console | Firebase config mutation; reversible by deleting the token; must be deleted after the test |
| A5 | Publish Remote Config `pilot_redesign_backend_enabled=true` and `pilot_redesign_min_contract_version=1` | **This is the A5c-gated Remote Config initialization.** Production-wide; affects every client, not just this device |
| A6 | Ensure the signing-in account has R7 organizer status `APPROVED` | Production data; may already be satisfied by the R5PTEST2 test owner |

A5 is by far the heaviest item — it is a production-wide flag flip, not a device-local
setting. **Recommendation: do not request A5 for a routing smoke test.** Track B already
exercises the same UI on the R7 path via the demo gateway. Defer A5 until the R7 backend
rollout is being approved on its own merits.

Without A5 and A6, Track A still verifies, on real hardware: the entry screen's new
Organizer prompt, the Organizer-access bypass, the Account landing, the sign-in prompt, and
that App Check now succeeds (the 403 from A5b should be gone). It will land on
`BusinessHomeDestination` rather than `R7OrganizerContent`, so the QR steps stay on Track B.

Also note for Track A: the internal certificate must **never** be added to the production
`assetlinks.json` (`deployment/r5-p/README.md`). App Link auto-verification will not work
for this build — expect the `1024` verifier code A5b already recorded. The route under test
uses manual entry, not App Links, so this does not block anything.

---

## 3. Verification checklist

Run before either track, unchanged from the established command:

```
& 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' -classpath gradle\wrapper\gradle-wrapper.jar `
  org.gradle.wrapper.GradleWrapperMain testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
```

On-device / on-emulator steps to capture:

| # | Step | Expected |
| --- | --- | --- |
| 1 | Launch with no memberships | Experience-entry screen, with "Hosting an Experience? Sign in or continue to Organizer tools." and an **Organizer access** button |
| 2 | Tap **Organizer access** | Entry gate bypassed; participant shell opens on the **Account** tab |
| 3 | Guest / signed-out | Account sign-in dialog opens automatically, once (not repeatedly) |
| 4 | Sign in as an approved Organizer | Organizer tools open automatically, once |
| 5 | Sign in as an unapproved user | Stays on Account with application/status options; tools do **not** open |
| 6 | Open or create an Experience | Experience detail renders |
| 7 | QR | Event code card shows the code and the QR image |
| 8 | **Share QR** | System share sheet with the QR share card |
| 9 | **Save QR** | SAF create-document picker; PNG written |
| 10 | Incoming QR / App Link | Still routes through the normal participant entry path, not the Organizer route |

---

## 4. Findings to resolve, independent of the test

1. ~~**No automated coverage of the auto-open decision.**~~ **Resolved 2026-08-26.** The
   branch that decides *prompt sign-in* vs *auto-open Organizer tools* is now
   `R5OrganizerAccessPolicy.nextAction()` in `app/src/main/java/com/kitheapp/util/`,
   following the existing `R5UnlockResumePolicy` idiom, with 10 tests in
   `app/src/test/java/com/kitheapp/util/R5OrganizerAccessPolicyTest.kt`. The refactor is
   behaviour-preserving; `DropHereScreen.kt` now dispatches on the returned action.
   Suite: **144 tests, 0 failures, 0 errors** (was 134), `lintDebug` and `assembleDebug`
   pass.

   Checklist items 3, 4, and 5 are now automated, including the "once" semantics and two
   cases that are awkward to stage by hand: a null (signed-out) user is treated as a guest,
   and a stale `organizerToolsAvailable` cannot open tools while the approval state is
   still reloading. Steps 1, 2, and 6–10 still require a device or emulator.

2. **No route back to the Experience-entry screen.** `manualEntryRequested` is only set from
   inside the entry flow itself. Once `organizerAccessRequested` is `true`, `needsEntry` is
   permanently `false` for that saved-state lifetime, and a user with **no memberships** has
   no in-app way to return and enter a code — they must cold-restart the app. Worth
   confirming as intended during step 2, and worth an "Enter an Experience code" affordance
   on the Account tab if it is not.

3. **Maps is unconfigured** (`KITHE_MAPS_API_KEY_NOT_CONFIGURED`). This does not affect the
   Organizer route but the Nearby map will not render tiles in either track. Do not read
   that as a regression.

---

## 5. Explicitly not proposed here

- No new Play release, upload, or track change.
- No uninstall or replacement of the installed signed release.
- No `versionCode` bump.
- No production Firestore mutation; the R5PTEST2 fixture is touched read-only or not at all.
- No policy publication — the pending Google Play legal-name correction
  ("Robert Peralta" vs "Robert Micah Lee Peralta") still blocks that.
- No QR distribution.
