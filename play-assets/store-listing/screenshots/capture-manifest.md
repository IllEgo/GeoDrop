# Kithe Play screenshot capture manifest

Status: **capture plan ready; final images require the release-candidate build and review fixture**

Use real participant UI from the signed release candidate. Do not use a Compose preview,
debug-only catalog, mock map, prototype, or a screen containing personal/test credentials.
Capture in the light theme because outdoor legibility is the primary Kithe context.

## Device and output

- Primary device: connected Samsung `SM-S938U`; current physical output is 1,080 by 2,340
  at 450 dpi with a 420 dpi density override.
- Portrait only for this set. Archive the 1,080-by-2,340 device-native originals, then
  export Play derivatives at 1,080 by 1,920 (9:16) without stretching. Prefer fitting the
  full capture proportionally on an approved flat-color canvas rather than cropping UI;
  if cropped, remove only safe system/background areas.
- Export lossless PNG, sRGB, without a device frame. Crop only safe system/background
  areas or place the full capture on an approved flat-color caption canvas; never crop
  required UI, map attribution, or safety/privacy copy.
- Disable notification previews, system overlays, TalkBack focus rectangles, and developer UI.
- Use a dedicated Play-review Experience with reviewed fictional content and stable inventory.
- Show Hawai'i place context without exposing a participant's exact live position.
- Create captioned derivatives only after the uncropped originals are archived.

## Six required states

| # | Participant promise | UI state to capture | Caption candidate | Acceptance check |
|---:|---|---|---|---|
| 1 | Invitation-first entry | Experience-code entry before sign-in | **Start with an invitation** | Kithe branding, no organizer CTA competing with entry, no real code visible |
| 2 | Clear preview | Valid Experience preview | **See where the invitation leads** | Experience name, host, date/context, and Join action are readable |
| 3 | Nearby discovery | Joined Experience map with list alternative reachable | **Find drops around the Experience** | One active Experience only; map and text equivalent; no exact participant marker in store image |
| 4 | Useful place detail | Locked/near drop detail | **Know what to look for** | Distance band, state label, place guidance, and bottom-third action are visible |
| 5 | One-shot location | Precise-location primer immediately before Unlock | **Your location is checked only when you unlock** | Says purpose and limited duration; does not imply background tracking |
| 6 | Reward and continuation | Successful text or photo unlock | **Reveal the drop, then keep exploring** | Payload plus next step; no audio/video; no third-party private data |

## Capture order

1. Install the exact signed candidate intended for the closed test.
2. Record its version name/code, APK or AAB hash, Firebase project, and feature flags.
3. Clear app data and rehearse the reviewer instructions from a clean install.
4. Seed/reset the dedicated review fixture and verify every shot state.
5. Capture all six originals in one session.
6. Inspect at 100% and thumbnail size for clipping, contrast, outdated copy, stale identity,
   status-bar leaks, debug labels, and map attribution.
7. Add restrained caption bands only if the UI alone does not communicate the promise.
8. Have Product, Privacy, and Accessibility approve the exact exported files.

## Current blockers

- The owner-approved Kithe mark is installed and visually verified on the physical Samsung.
  The exact signed release candidate must reuse the same resources.
- Google Maps is still intentionally disabled in the Kithe project pending Maps terms and
  approved billing. The current app correctly fails closed to List, but final map screenshots
  cannot be captured until that pre-pilot dependency is authorized and verified.
- The Play-review code/account and stable production-like fixture are not yet approved.
- Google identity/device/contact-phone verification currently blocks app creation in Play.
- Final screenshot capture must follow the signed release build, production policy URLs, and
  the exact Data safety audit—not the current local debug APK.
