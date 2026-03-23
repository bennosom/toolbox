# EPIC-006 — Adaptive Layouts

## Summary

The launcher runs on a wide range of Android form factors: phones in portrait and landscape,
tablets, and foldable devices in book and tabletop postures. Each form factor benefits from a
layout tuned to its screen geometry and interaction model.

---

## Goals

- Make the launcher feel at home on every supported form factor
- Exploit the extra screen area on tablets (larger grid, different proportions)
- Handle foldable postures gracefully — especially tabletop (split across hinge)
- Never break or look wrong on an unsupported configuration

---

## Form factor classification (`ScreenInfo`)

`ScreenInfo` is derived from `WindowAdaptiveInfo` (Jetpack Compose adaptive library).
It classifies the current window into one of:

| Class | Condition |
|-------|-----------|
| `MOBILE_PORTRAIT` | Small width, medium or large height |
| `MOBILE_LANDSCAPE` | Medium or large width, small height |
| `TABLET_PORTRAIT` | Medium width, large height |
| `TABLET_LANDSCAPE` | Large width, medium height |
| `DESKTOP` | All other large-window cases |
| `FOLDABLE_BOOK` | Vertical separating hinge |
| `FOLDABLE_TABLETOP` | Horizontal separating hinge |
| `FOLDABLE_FLAT` | Hinge present but not separating |

> **Known issue (`TODO` in `ScreenInfo.kt`):** The current logic determines portrait/landscape
> from absolute width/height breakpoints rather than orientation, which can mis-classify some
> configurations. Should be fixed to: detect orientation first, then derive form factor from
> the smallest edge.

---

## Current status

`ScreenInfo` is computed and passed to `AppTile` via `rememberScreenInfo()`, but **form-factor-
specific layout adaptations have not yet been implemented**. The grid currently renders identically
across all form factors.

---

## Planned adaptations (design TBD)

| Form factor | Expected behaviour |
|-------------|-------------------|
| `MOBILE_PORTRAIT` | Default layout — current behaviour |
| `MOBILE_LANDSCAPE` | Reduced row height; wider page; bar may move to side |
| `TABLET_PORTRAIT` | Larger default grid spec; wider tiles or multi-panel |
| `TABLET_LANDSCAPE` | Side-by-side panels (e.g. grid + app manager) |
| `FOLDABLE_BOOK` | Two distinct half-screen areas, one per panel |
| `FOLDABLE_TABLETOP` | Grid in upper half; quick bar and controls in lower half |
| `FOLDABLE_FLAT` | Treat as tablet |
| `DESKTOP` | Large grid, pointer-friendly interaction (hover states, right-click menu) |

---

## Open questions / gaps

- [ ] Should grid spec defaults differ per form factor (e.g. 6×5 on tablet vs 4×4 on phone)?
- [ ] Foldable tabletop — what belongs in the lower panel beyond the quick bar?
- [ ] Foldable book — should each panel have its own independent grid page, or show one continuous grid?
- [ ] Desktop — right-click / pointer support needed? Is this a target at all?
- [ ] Should `ScreenInfo` be exposed to the grid to drive column count automatically, or remain a layout hint?

---

## Related

- EPIC-001 App Grid
- EPIC-003 Quick Access Bar
- EPIC-005 Customisation (grid spec defaults per form factor)
