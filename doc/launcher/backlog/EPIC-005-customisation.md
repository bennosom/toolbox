# EPIC-005 — Customisation

## Summary

Users can adjust the look and behaviour of the launcher through a context menu accessible by
long-pressing on empty home screen space. Customisation is intentionally limited to keep the
product focused and the UX clean.

---

## Goals

- Let the user tune the grid density to their preference
- Support a wallpaper that feels personal without requiring a separate app
- Integrate gracefully with the system's dark/light mode
- Make it easy to (re-)set the launcher as the system default

---

## Current implementation

### Grid context menu (long-press on empty space)

| Menu item | Action |
|-----------|--------|
| Apps list | Opens App Manager |
| Set as default launcher | Opens system Default Apps settings |
| Reset defaults | Clears layout and re-seeds from installed apps |
| Grid size presets | 4×3, 5×3, 3×4, 4×5 — calls `setGridSpec` → `resetDefaults(spec)` |
| Dark mode | Opens system Display settings |
| Color (Cyan) | Sets a solid cyan wallpaper via `setColorWallpaper` |
| Color (Dark Gray) | Sets a solid dark gray wallpaper via `setColorWallpaper` |

> **Note:** The solid-color wallpaper options are placeholder/dev tooling.
> A full wallpaper picker is planned (see below).

### Grid size (planned replacement)
The fixed-preset approach is superseded. Grid size will be configured via **numeric col/row
steppers** (fully custom input). The menu item currently labelled "Grid size presets" will open
a control with independent column and row steppers.

#### Spec-change layout migration
When the user changes the grid spec, the flow is:
1. A **preview sheet** is shown with the proposed new layout rendered at reduced scale.
2. An **evolution-based placement algorithm** fills the new grid by trying to preserve relative
   app positions as closely as possible (fitness = proximity to original cell, no overlaps).
3. The user confirms or cancels. On confirm, the new spec and recomputed layout are committed.

> The current `setGridSpec` → `resetDefaults` path is the temporary implementation and will be
> replaced by the preview + migration flow above.

### Default launcher prompt
On every `onResume`, `LauncherActivity` checks `isDefaultLauncher()`. If not set as default,
a `Snackbar` is shown with an action to open Default App Settings.

### Theming
- Dynamic Material You colours — `dynamicDarkColorScheme` / `dynamicLightColorScheme`
- Wallpaper-aware system bar colours via `SyncWallpaperToSystemBars`
- Edge-to-edge enabled; transparent window background shows system wallpaper

---

## Planned: Wallpaper Picker

A proper wallpaper browsing and selection UI. Details TBD, but expected to cover:
- Browsing a set of built-in wallpapers
- Picking a photo from the system media picker
- Solid/gradient color options (formal replacement for the dev shortcuts above)
- Preview before applying

---

## Decisions

- [x] **Grid size input** — Fully custom col/row steppers. No fixed preset list. Replaces the
  current four-preset chip group.
- [x] **Grid spec change** — Preview before apply + evolution-based placement. See spec above.
- [x] **Dark mode** — In-app override toggle: system / light / dark. The current redirect to system
  Display Settings is replaced by a tri-state toggle in the customisation menu.
- [x] **Reset defaults** — No confirmation dialog. Instead a `Snackbar` with an **Undo** action is
  shown immediately after reset fires, giving the user a brief window to reverse.
- [x] **Settings surface** — Options stay in the long-press context menu dropdown. No dedicated
  Settings screen is planned. The menu may grow but stays in place.

---

## Related

- EPIC-001 App Grid (grid spec)
- EPIC-007 Data Persistence (persisting custom spec)
