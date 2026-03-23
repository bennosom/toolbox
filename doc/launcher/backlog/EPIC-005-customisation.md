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

### Grid size presets (implemented)
Available presets: `4×3`, `5×3`, `3×4`, `4×5`

> **Gap:** Changing the grid spec currently calls `resetDefaults`, which discards the existing
> layout. A merge strategy that preserves app order as much as possible is a planned improvement
> (TODO in `setGridSpec`).

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

## Open questions / gaps

- [ ] Should grid size presets be user-configurable (custom cols/rows input) or always a fixed list?
- [ ] Grid spec change destroys layout — merge strategy needs design: compact-fill? keep-and-trim?
- [ ] "Dark mode" redirects to system settings — is an in-app override desired?
- [ ] Should there be a confirmation dialog for "Reset defaults"?
- [ ] Should customisation options grow into a dedicated Settings screen rather than a dropdown?

---

## Related

- EPIC-001 App Grid (grid spec)
- EPIC-007 Data Persistence (persisting custom spec)
