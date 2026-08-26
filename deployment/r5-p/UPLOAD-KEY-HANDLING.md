# Kithe Play upload-key handling

Status: **independent removable-drive and sealed-envelope recovery controls complete; Micah
is the designated second custodian. Play upload remains separately gated**.

This document contains public metadata and operating controls only. It must never contain a
keystore, private key, plaintext password, recovery secret, or password-manager export.

## Public identity

- Purpose: dedicated Google Play upload key for package `com.kitheapp`.
- Alias: `kithe-play-upload`.
- Certificate subject: `CN=Kithe Play Upload, OU=Release Engineering, O=Kithe, L=Honolulu,
  ST=Hawaii, C=US`.
- Algorithm: 4096-bit RSA with SHA-256.
- Certificate SHA-256: `2B:7B:4D:86:B8:C4:70:A2:1D:18:1B:42:8D:98:F6:DB:A8:4D:6F:93:8A:69:0A:8B:42:63:47:30:94:5E:9C:B1`.
- Certificate expiry: 2054-01-10.
- Public certificate: `C:\Users\rober\KitheReleaseKeys\com.kitheapp-upload\kithe-upload-certificate.pem`.

The private keystore is outside the source repository at
`C:\Users\rober\KitheReleaseKeys\com.kitheapp-upload\kithe-upload.jks`. That directory has
inherited access removed and is limited to the current Windows identity and SYSTEM. The two
distinct random passwords are stored only as Windows-user DPAPI ciphertext in the same
restricted directory; builds decrypt them in memory and pass them through temporary
environment variables. No password is written to Gradle properties or this repository.

## Ownership and custody

- Accountable owner: the Kithe Google Play Console account owner.
- Current custodian: the owner-controlled Windows account that created the key.
- Second owner-approved custodian: Micah.
- Access is limited to an authorized release build or a documented recovery exercise.

## Backup and recovery gate

Before any Play upload, create two separately protected recovery components under owner
control:

1. one encrypted copy of the original JKS keystore; and
2. one separately protected password escrow containing the store password, key password,
   alias, package, certificate fingerprint, and recovery instructions. The preferred form is
   an owner-approved password manager. An owner-approved offline alternative may use a
   separately stored sealed envelope containing only a strong recovery code for an encrypted
   password-escrow archive.

DPAPI ciphertext alone is not an independent backup because it is tied to the current
Windows user profile. Keep the removable archive and password escrow in separate custody. Do
not use source control, chat, email, a shared project folder, or Play listing fields for
either item's secrets.

Verify recovery on an isolated authorized machine by opening the copied keystore with
`keytool`, exporting its public certificate, and confirming the SHA-256 fingerprint above.
Record only the date, verifier, storage-system identifiers, and pass/fail result in release
evidence. Do not record passwords or key material.

On 2026-08-25, the first part was completed on the removable NTFS volume labeled
`RECOVERY_BACKUP`. `Kithe/com.kitheapp-upload/kithe-upload-recovery.7z` uses 7z AES encryption
with encrypted file names; its password is the JKS store password. An isolated temporary
restore reproduced the JKS and certificate hashes, the public certificate fingerprint, and a
successful private-key signing operation. The temporary restored key was deleted. No DPAPI
or plaintext password file was copied. The archive SHA-256 is
`B2F5E484B8016532BFE7669F352D8439B953A0D05194E0B400808B3E5B3C9CC9`.

The owner selected the offline escrow alternative. A second 7zAES archive,
`kithe-upload-password-escrow.7z`, contains the two distinct passwords only in encrypted form.
Its SHA-256 is
`2B83A3A85EF5242630875828DEB92B9EC63FEACE231B825DA2F39B06599B141A`. Its independent 128-bit
recovery code was displayed once in a local window, was never written to a file, chat, log, or
clipboard, and was acknowledged as manually checked and sealed in an envelope for Micah. The
envelope must remain separate from the `RECOVERY_BACKUP` drive.

The encrypted password archive passed integrity and in-memory recovery checks against both
source passwords. Together with the already restore/signing-tested key archive and the sealed
envelope under Micah's custody, the independent recovery gate is complete.

## Rotation and incident response

- Review custody, backups, access, and certificate expiry at least annually and before every
  release-owner transition.
- Treat unexpected key access, loss of custody, or an unverified recovery copy as a release
  blocker. Stop uploads and notify the Play account owner.
- If the upload key is lost or compromised, use Google Play's upload-key reset process. Do
  not replace the app, package, or Google-managed app-signing key.
- For a planned reset, generate a new dedicated key under the same controls, export only its
  public certificate for Play, confirm Play's reported upload fingerprint, and retire the old
  private key from active release use after the reset is effective.

## Build interface

Release signing is fail-closed and accepts only these temporary environment variables:

- `KITHE_UPLOAD_STORE_FILE`
- `KITHE_UPLOAD_STORE_PASSWORD`
- `KITHE_UPLOAD_KEY_PASSWORD`

All three must be present together, and the store file must exist. Unset them immediately
after the build. The alias is fixed in Gradle as `kithe-play-upload`.
