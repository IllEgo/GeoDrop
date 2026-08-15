# Kithe Play visual assets

Status: **owner-approved local identity set; Play upload remains unauthorized**

These assets follow the accepted Kithe visual system: deep teal `#0B5D5D`,
near-state amber `#E07B24`, warm surface `#FDFCFA`, and ink `#14171A`.
They use flat color, high contrast, and no gradients, glass effects, store badges,
ranking language, or feature claims.

## Approved mark

The mark is a geometric `K` drawn as a three-point trail. The upper amber point
represents a nearby place that can be unlocked. It deliberately avoids the legacy
media-pin artwork, whose photo/video/audio symbols no longer match Kithe Pilot 1.

Files:

- `icon/kithe-play-icon.svg` — editable 512 by 512 source.
- `icon/kithe-play-icon.png` — Play-ready 512 by 512 RGBA PNG with a
  fully opaque artwork canvas.
- `icon/kithe-adaptive-foreground.svg` — transparent source for Android's masked
  adaptive launcher foreground.
- `feature/kithe-feature-graphic.svg` — editable 1024 by 500 source.
- `feature/kithe-feature-graphic.png` — Play-ready 1024 by 500, opaque PNG.
- `screenshots/capture-manifest.md` — the six-shot participant-first capture plan.

## Adoption status

The owner approved the mark and feature graphic on 2026-08-14. Android now uses the
geometry for its adaptive foreground, round icon, and every legacy density fallback. The
local `website/` source has matching SVG, 32-pixel favicon, and Apple touch icon assets,
but that site update has not been deployed. The Play files have not been uploaded. iOS
visual adoption remains deferred under the Android-first migration sequence.

Before Play upload, verify the exported pixel dimensions, color profile, alpha channel,
and appearance inside Play's square, circle, rounded-square, and squircle masks.
