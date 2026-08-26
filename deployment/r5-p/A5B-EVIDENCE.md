# A5b signed internal-test candidate evidence

Status: **A5b complete. Local signed candidate, physical-device smoke, independent encrypted
recovery, exact Play upload, Play upload-certificate confirmation, approved tester enrollment,
and Internal-testing rollout all pass**.

The owner explicitly approved A5b's local upload-key and exact-candidate preparation, then
separately approved A5b item 3 on 2026-08-25. The exact signed bundle was uploaded, its Play
certificate was confirmed, two owner-approved testers were enrolled, and version `1 (1.0)`
was released only to Internal testing on 2026-08-26. No Firebase/Cloudflare mutation,
policy/setup-form edit, closed test, or production change was made.

## Upload-key control

- Dedicated private key location, outside the repository:
  `C:\Users\rober\KitheReleaseKeys\com.kitheapp-upload\kithe-upload.jks`.
- Alias: `kithe-play-upload`.
- Certificate subject: `CN=Kithe Play Upload, OU=Release Engineering, O=Kithe, L=Honolulu,
  ST=Hawaii, C=US`.
- Certificate SHA-256:
  `2B:7B:4D:86:B8:C4:70:A2:1D:18:1B:42:8D:98:F6:DB:A8:4D:6F:93:8A:69:0A:8B:42:63:47:30:94:5E:9C:B1`.
- Algorithm: 4096-bit RSA with SHA-256; certificate expiry: 2054-01-10.
- Public certificate:
  `C:\Users\rober\KitheReleaseKeys\com.kitheapp-upload\kithe-upload-certificate.pem`.
- The key directory has inherited access removed and is limited to the owner-controlled
  Windows identity and SYSTEM. Distinct random store/key passwords are saved only as
  Windows-user DPAPI secure strings and were used in memory without being printed.
- Gradle release signing is fail-closed: the three `KITHE_UPLOAD_*` environment variables
  must be supplied together and the store file must exist. Keystore extensions are ignored
  repository-wide.
- Ownership, backup, recovery, rotation, and incident controls are defined in
  `UPLOAD-KEY-HANDLING.md`.

The DPAPI files are not an independent recovery backup because they are tied to the current
Windows profile. The selected recovery design therefore uses separate encrypted key and
password archives plus offline custody of the password-archive recovery code.

The removable-copy half of that control was completed on 2026-08-25:

- destination volume label: `RECOVERY_BACKUP`;
- relative archive path: `Kithe/com.kitheapp-upload/kithe-upload-recovery.7z`;
- archive SHA-256:
  `B2F5E484B8016532BFE7669F352D8439B953A0D05194E0B400808B3E5B3C9CC9`;
- encryption: 7z AES with encrypted file names, using the existing store password;
- archive integrity test, restored JKS hash, restored certificate hash, and upload-certificate
  fingerprint: **passed**; and
- restored private-key signing plus signature verification using the distinct key password:
  **passed**.

The temporary restored private key was deleted after the test. No password, DPAPI file, or
plaintext private key was copied to the drive. Public recovery instructions and the public
certificate accompany the archive.

The owner initially selected 1Password but confirmed no existing account, so no new account
was created solely for recovery. The owner then approved an offline sealed-envelope escrow:

- password archive: `Kithe/com.kitheapp-upload/kithe-upload-password-escrow.7z` on the
  `RECOVERY_BACKUP` volume;
- archive SHA-256:
  `2B83A3A85EF5242630875828DEB92B9EC63FEACE231B825DA2F39B06599B141A`;
- encryption: 7z AES with encrypted file names;
- archive integrity and in-memory recovery of both distinct source passwords: **passed**;
- recovery code: 128 random bits, displayed only in a local window and never persisted in a
  file, chat, log, or clipboard; and
- designated second custodian: Micah. The owner acknowledged manually checking the complete
  code and sealing it for Micah, separately from the USB backup.

The recovery code is not recoverable from this repository or workstation evidence. Losing
both the sealed envelope and all authorized recollection of it makes the password archive
unrecoverable by design.

## Exact signed candidate

- File: `app/build/outputs/bundle/release/app-release.aab`.
- Size: `20,248,886` bytes.
- SHA-256: `BF7CF87B17F1FBF2EEE954D375060E0FEFBEA2DC77F6EA34A88F28E09214222D`.
- `jarsigner -verify -verbose -certs`: **verified and not unsigned**.
- `keytool -printcert -jarfile`: one signer matching the upload certificate above.
- Google bundletool 1.18.3 `validate`: **passed**. The verified bundletool JAR SHA-256 was
  `A099CFA1543F55593BC2ED16A70A7C67FE54B1747BB7301F37FDFD6D91028E29`.

The exact AAB was also converted locally into an installable universal APK set without
changing the AAB:

- APKS SHA-256: `A7FDF7F2DE8AC8405758BBAE13D602BE0ACCAF528AC608ADC3F2C9BD1944DE58`.
- Universal APK SHA-256:
  `0DA989370AD292C31EFAFCE355A1C9B1140D6E94350FF987330A082CF7E5D84F`.
- `apksigner` verifies APK Signature Schemes v2 and v3 with one 4096-bit RSA signer whose
  certificate SHA-256 is the same upload-certificate fingerprint.

## Play Internal testing draft

On 2026-08-25, after the separate approval for A5b item 3:

- the exact AAB above was re-hashed immediately before transfer and still matched
  `BF7CF87B17F1FBF2EEE954D375060E0FEFBEA2DC77F6EA34A88F28E09214222D`;
- Google Play accepted `app-release.aab` into only the Internal testing track for Kithe,
  package `com.kitheapp`, and identified it as version `1 (1.0)`, minimum API 26, and target
  SDK 36;
- the release was saved as draft release `1 (1.0)` and was not advanced to preview or
  rollout; and
- Play's App signing page reported upload-certificate SHA-256
  `2B:7B:4D:86:B8:C4:70:A2:1D:18:1B:42:8D:98:F6:DB:A8:4D:6F:93:8A:69:0A:8B:42:63:47:30:94:5E:9C:B1`,
  exactly matching the dedicated local upload certificate.

The owner created and explicitly confirmed the selected `Kithe's Internal Testing` email list
with two users. Their addresses are intentionally not copied into repository evidence. The
tester assignment was saved before publishing.

Play's final release review reported one warning: no deobfuscation file was associated with
the bundle. The release configuration contains no R8/ProGuard minification or resource
shrinking setting, so no mapping file is generated or required for this candidate. Play
accepted the release with that non-blocking warning.

Final Play state on 2026-08-26:

- Internal testing track: **Active**;
- latest release: `1 (1.0)`;
- release status: **Available to internal testers**;
- released: Aug 26, 2026 at 2:22 AM as reported by Play Console;
- review state: **Not reviewed**, with temporary app name `com.kitheapp (unreviewed)`; and
- tester opt-in URL: `https://play.google.com/apps/internaltest/4701473779914778401`.

## Candidate identity and fail-closed state

- Package: `com.kitheapp`.
- Version code: `1`; version name: `1.0`.
- Minimum SDK: 26; compile/target SDK: 36.
- Application: `com.kitheapp.GeoDropApplication`; label: `Kithe`.
- Firebase project: `kithe-production`.
- App Link host/path: `https://join.kitheapp.com/e/` with auto-verification.
- `APP_LINK_CONFIGURED=true`, `USE_PLAY_INTEGRITY_APPCHECK=true`, and
  `MAPS_CONFIGURED=false`.
- Coupons, creation, hunts, media, and notifications build-time feature upper bounds are all
  false. The Maps value remains the not-configured sentinel.

The packaged release manifest has no Advertising ID permission, AdServices declaration,
microphone permission, registered recorder activity, Robolectric `ComponentActivity`, or
`AudioRecorderActivity`. Its expected camera, location, notification, network, Firebase
Messaging, and install-referrer declarations remain present.

## Tests, lint, and dependency audit

- `testReleaseUnitTest`: **130 passed, 0 failed, 0 errors, 0 skipped**.
- `lintRelease`: **0 errors, 156 warnings, 14 hints**.
- `bundleRelease`, including `validateSigningRelease` and `signReleaseBundle`: **passed**.
- Generated bundle dependency report SHA-256:
  `CD1D31685CEFD06E3CFD2EBBAAE15FE636B8E44EAE6DE7640C54A11580131794`.
- Offline Gradle `releaseRuntimeClasspath` graph output SHA-256:
  `8EF071058E1931E5A3B1E852D916B21C67AAC5760608D87971A93E745E06B13E`.
- The resolved graph includes `firebase-appcheck-playintegrity` and contains no Firebase
  Analytics SDK, Google Play Services measurement SDK, Google ads SDK, or ads-identifier
  dependency.
- The lightweight `firebase-measurement-connector:20.0.1` interface remains transitively
  required by Crashlytics, Remote Config, and Messaging. It is a connector/interface, not the
  Analytics collection runtime, and is recorded here so the audit does not overstate absence
  of every dependency whose name contains “measurement.”

## Physical-device smoke — passed

The exact universal APK was tested on a Samsung SM-S938U running Android 16/API 36. The device
had a prior `com.kitheapp` debug build targeting API 34. Its Android Debug certificate did not
match the upload certificate, so Android could not update it in place. With the owner's
explicit approval, only that package was uninstalled, its local app data was removed, and the
candidate was installed cleanly. Firebase and Play were not changed.

Post-install verification established:

- the pulled installed APK SHA-256 remained
  `0DA989370AD292C31EFAFCE355A1C9B1140D6E94350FF987330A082CF7E5D84F`, exactly matching the
  candidate derivative;
- package `com.kitheapp`, version `1` / `1.0`, minimum SDK 26, and target SDK 36;
- one verified signer with upload-certificate SHA-256
  `2B:7B:4D:86:B8:C4:70:A2:1D:18:1B:42:8D:98:F6:DB:A8:4D:6F:93:8A:69:0A:8B:42:63:47:30:94:5E:9C:B1`;
- cold launcher start: `Status: ok`, `MainActivity` foregrounded, 257 ms total, and zero fatal
  crash matches after stabilization; and
- explicit `https://join.kitheapp.com/e/R5PTEST2` entry: `Status: ok`, `MainActivity`
  foregrounded, 137 ms total, followed by the expected fail-closed “We couldn't open this
  Experience right now” state and zero fatal crash matches.

The sideload's Play Integrity App Check attestation returned HTTP 403, as expected because the
APK was not installed and re-signed by Play. The app used no successful protected backend
path and surfaced its safe error UI instead of crashing.

Android reported custom domain-verifier code `1024` for the upload-key-signed sideload. The
public `https://join.kitheapp.com/.well-known/assetlinks.json` returned direct HTTP 200 JSON
and intentionally contains only the three approved Play App Signing certificates. Project
policy expressly excludes debug and upload certificates from the production association.
Therefore the explicit App Link entry passes this sideload smoke, while automatic verified
default routing remains correctly deferred to a later Play-installed build signed with the
Play App Signing certificate.

Local screenshots were captured under `app/build/reports/a5b-device/`. Launcher screenshot
SHA-256 is `12A01B46D48A6F0132086D7038A83C71DBF7FE0D4603211DB9B51563CDFA587F`; final App Link
fail-closed screenshot SHA-256 is
`F40B05D84A5DAFF22B23127F8C97DF3C45243D19848B59C38DE2E79B752EEE41`.

The earlier local emulator attempts remain host-only diagnostics: HAXM/Vanguard and software
emulation both terminated before ADB boot. They are superseded by the passing physical API 36
smoke and are not app failures.

## Gate

A5b is complete. The upload key, exact candidate, dependency/privacy audit, physical/API 36
smoke, removable recovery archive, restored private-key signing test, encrypted password
escrow, second-custodian designation, exact Play upload, upload-certificate match, approved
tester assignment, and Internal-only rollout all pass.

Stop here. A5c, closed testing, production access, production release, and all Play setup-form
changes remain closed and require their own later approval.
