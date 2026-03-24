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
| Apps list | Opens App Manager as a bottom sheet (EPIC-004) |
| Widgets | Opens Widget Gallery as a bottom sheet (EPIC-010) |
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

A proper wallpaper browsing and selection UI accessible from the long-press customisation menu.

### Sources

| Source | Mechanism |
|--------|-----------|
| Device photo / image | Storage Access Framework — `ActivityResultContracts.PickVisualMedia` (Android 13+) with `PickVisualMedia.ImageOnly` MIME filter; falls back to `GetContent("image/*")` on older API levels |
| Built-in wallpapers | Bundled drawable assets; shown in a horizontal scroll strip |
| Solid / gradient colour | Replaces the current dev shortcut colours; colour picker or preset swatches |

### Flow

1. User opens the customisation context menu → taps **Wallpaper**.
2. A bottom sheet shows three tabs: **Photos**, **Built-in**, **Colour**.
3. Selecting an image from any tab renders a **live preview** (full-screen, non-destructive) before
   the user confirms.
4. On confirm: `WallpaperManager.setStream()` (or `setBitmap()`) applies the wallpaper system-wide.
5. On cancel: no changes are made; the sheet dismisses.

### Permissions

- No `READ_EXTERNAL_STORAGE` required when using `PickVisualMedia` (photo picker grants temporary
  URI access without a persistent permission).
- `SET_WALLPAPER` permission must be declared in the manifest.

---

## Decisions

- [x] **Grid size input** — Fully custom col/row steppers. No fixed preset list. Replaces the
  current four-preset chip group.
- [x] **Grid spec change** — Preview before apply + evolution-based placement. See spec above.
- [x] **Dark mode** — In-app override toggle: system / light / dark. The current redirect to system
  Display Settings is replaced by a tri-state toggle in the customisation menu.
- [x] **Reset defaults** — No confirmation dialog. Instead a `Snackbar` with an **Undo** action is
  shown immediately after reset fires, giving the user a brief window to reverse. Snackbar duration
  is **4 seconds** (Material 3 default). The layout reset to disk is deferred until the snackbar
  times out or is explicitly dismissed; an Undo tap within that window reverts the state without
  writing to DataStore.
- [x] **Bar capacity** — Always equal to `spec.cols`. Not a user-configurable setting. When the
  grid column count changes, bar capacity changes automatically to match.
- [x] **Settings surface** — Options stay in the long-press context menu dropdown. No dedicated
  Settings screen is planned. The menu may grow but stays in place.

---

## Related

- EPIC-001 App Grid (grid spec)
- EPIC-004 App Manager (opened as bottom sheet from context menu)
- EPIC-007 Data Persistence (persisting custom spec)
- EPIC-009 App Folders
- EPIC-010 App Widgets (widget gallery opened from context menu)
- NFR Performance & Responsiveness (wallpaper decode off UI thread)
