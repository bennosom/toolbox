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

## Planned adaptations

| Form factor | Expected behaviour |
|-------------|-------------------|
| `MOBILE_PORTRAIT` | Default layout — current behaviour |
| `MOBILE_LANDSCAPE` | Reduced row height; wider page; bar may move to side |
| `TABLET_PORTRAIT` | Auto-detected larger initial grid spec; wider tiles |
| `TABLET_LANDSCAPE` | Auto-detected larger initial grid spec; landscape proportions |
| `FOLDABLE_BOOK` | **Two independent grids** — each panel hosts its own grid with its own pages and layout |
| `FOLDABLE_TABLETOP` | Grid in upper half; quick bar + notification/widget tray in lower half |
| `FOLDABLE_FLAT` | Treat as tablet |
| `DESKTOP` | Layout adaptations in scope; pointer-specific interactions (hover, right-click) deferred |

---

## Decisions

- [x] **Per-form-factor defaults** — On first launch `ScreenInfo` is used to auto-select a sensible
  initial `GridSpec` (e.g. more columns on wider screens). After first run the spec is user-controlled
  and never overridden by layout changes.
- [x] **`ScreenInfo` → column count** — Auto-detect on first launch only. Thereafter `ScreenInfo`
  is a layout hint (padding, margins, panel splits) and does not override the user's chosen spec.
- [x] **Foldable book** — Two independent grids. Each panel maintains its own `Grid` model, page
  set, and bar. The two grids share no state.
  - **Book (unfolded):** both panels are active and rendered side-by-side.
  - **Flat/folded:** only the primary (left) panel grid is shown. The secondary panel grid is
    **suspended** — it remains fully persisted and is restored when the device unfolds back to
    book mode. No content is lost or merged.
  - **Storage model:** two separate `DataStore<GridData>` instances with distinct file names
    (e.g. `grid_primary.pb` and `grid_secondary.pb`). App installs/uninstalls while in flat mode
    update only the primary store; the secondary store is not touched until the device is back in
    book mode and `AppsRepositoryImpl` reattaches to it.
- [x] **Foldable tabletop lower panel** — Quick bar + a notification/widget tray area. Exact
  widget content is TBD, but the lower panel is not an empty strip.
- [x] **Desktop target** — In scope for layout (column count, spacing). Pointer-specific
  interactions (hover states, right-click context menu) are deferred to a later iteration.

---

## Related

- EPIC-001 App Grid
- EPIC-003 Quick Access Bar
- EPIC-005 Customisation (grid spec defaults per form factor)
