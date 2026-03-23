# EPIC-001 — App Grid

## Summary

The app grid is the primary surface of the launcher. It presents installed apps as tiles arranged
in a fixed-size grid that spans one or more horizontal pages. The user navigates between pages by
swiping. Each page holds `cols × rows` cells; each cell may be empty or contain one app.

---

## Goals

- Give the user a stable, predictable home for their apps
- Support multi-page layouts without automatic re-sorting
- Load fast and reflect app install/removal events in real time
- Work correctly across the supported grid size presets
- Page swipes feel instant — no jitter, no dropped frames, no perceptible hang
- Swipe down anywhere on the home screen surface opens the system notification drawer
- Swipe up anywhere on the home screen surface opens the quick-search bottom sheet (see EPIC-008)

---

## Current implementation

| Aspect | Detail |
|--------|--------|
| Component | `AppGrid` composable (`ui/grid/AppGrid.kt`) |
| Layout | `HorizontalPager` → one `LazyVerticalGrid` per page |
| Grid model | `Grid(spec: GridSpec, grid: List<Map<Cell, App?>>, bar: List<App>)` |
| Cell model | `Cell(col, row)` — zero-indexed |
| Default spec | `GridSpec(cols=4, rows=4)` |
| App tile | `AppTile` composable — icon + label, size 60 dp, spacing 12 dp |
| Page indicator | Dot strip below the pager (`PageIndicator`) |
| Loading state | Full-screen `CircularProgressIndicator` while `savedState` is null |
| App source | `AppsRepository.grid` — a combined Flow of installed apps + stored layout |
| Live updates | `LauncherApps.Callback` reacts to package add/remove/change events |

### Grid construction (first run / reset)
- Apps are sorted by `ComponentName` and paginated to fill pages column-by-column.
- Newly installed apps not yet in the stored layout are appended after the last occupied cell,
  creating new pages as needed.

---

## Gesture map

| Gesture | Result |
|---------|--------|
| Swipe left / right | Navigate to adjacent grid page |
| Swipe down | Delegate to system notification drawer (`StatusBarManager` / `expandNotificationsPanel`) |
| Swipe up | Open quick-search bottom sheet (EPIC-008) |
| Long-press on empty space | Open customisation context menu (EPIC-005) |
| Long-press on app tile | Start drag / open tile context menu (EPIC-002) |

---

## Decisions

- [x] **Empty cell indicator** — No permanent affordance. During drag only, the hovered cell shows a
  white glowing dot that fades in on pointer-enter and fades out on pointer-leave. All other empty
  cells remain invisible.
- [x] **Edge-scroll during drag** — Hovering within the edge zone of a page during drag auto-scrolls
  to the adjacent page. See EPIC-002 for the drag specification.
- [x] **Page count** — Unlimited. Pages are added automatically when new apps overflow the last page
  or when the user drags past the last page.
- [x] **Empty page collapse** — When all apps on a page are removed or uninstalled the page
  auto-collapses. The final remaining page is never removed (minimum one page at all times).

---

## Related

- EPIC-002 App Reordering (drag-and-drop within grid)
- EPIC-003 Quick Access Bar (bar at bottom)
- EPIC-005 Customisation (grid size presets)
- EPIC-007 Data Persistence (saving layout to disk)
- EPIC-008 Search / Quick Find (swipe-up bottom sheet)
- NFR Performance & Responsiveness
