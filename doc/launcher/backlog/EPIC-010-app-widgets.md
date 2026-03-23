# EPIC-010 — App Widgets

## Summary

Users can place Android app widgets directly on the home screen grid. Widgets span one or more grid
cells, are resizable, and are discovered through a **Widget Gallery** bottom sheet reachable from
the long-press context menu. Widgets are added by dragging them from the gallery onto the grid.

---

## Goals

- Allow users to place rich, live content (clocks, weather, media controls, etc.) on the home screen
- Respect the Android widget hosting contract (`AppWidgetHost` / `AppWidgetManager`)
- Keep widget placement consistent with the existing cell-based grid model
- Make widget discovery effortless — a single gallery entry point, drag-to-place

---

## Widget Gallery

### Entry point

Long-press on empty grid space → context menu → **Widgets** opens the Widget Gallery as a
`ModalBottomSheet` (same pattern as App Manager).

### Gallery layout

- A vertically scrolling list grouped by **provider app** (header per app with icon + name).
- Each widget preview card shows:
  - The widget preview image (from `AppWidgetProviderInfo.previewImage`, fallback to app icon)
  - Widget name (`AppWidgetProviderInfo.loadLabel`)
  - Default size expressed in cells (e.g. "4 × 2")
- The sheet is **searchable** — a search field at the top filters by widget name or provider app.

### Drag-to-place

The user initiates a drag directly from a widget card in the gallery. As the finger crosses the
grid, a ghost preview sized to the widget's default cell span tracks the pointer. Releasing over a
valid (unoccupied, in-bounds) target area places the widget there. Releasing outside a valid area
or back over the sheet cancels the operation.

---

## Widget Placement

### Cell span

Each widget occupies a rectangular region of grid cells defined by `(col, row, colSpan, rowSpan)`.
The minimum span is the widget's `minWidth` / `minHeight` expressed in cells (rounded up). The
maximum span is the widget's `maxResizeWidth` / `maxResizeHeight` where set by the provider.

### Overlap prevention

A widget may only be placed on a region where all cells are empty. Occupied cells (apps, folders,
other widgets) block the drop. The ghost preview turns red / shows a visual error state when the
target region is unavailable.

### Configuration activity

If `AppWidgetProviderInfo.configure` is non-null the widget's configuration `Activity` is launched
immediately after placement (standard `ACTION_APPWIDGET_CONFIGURE` flow). If the user cancels
configuration the widget is removed.

---

## Widget Resize

After placement the user can resize a widget by long-pressing it to reveal **resize handles** on its
edges. Dragging a handle expands or shrinks the widget by whole cell increments. The resize
respects `minResizeWidth`, `minResizeHeight`, `maxResizeWidth`, `maxResizeHeight` from
`AppWidgetProviderInfo`. Apps displaced by an expanding widget are not automatically moved — the
resize is blocked if it would overlap occupied cells.

---

## Widget Hosting

| Aspect | Detail |
|--------|--------|
| Host | `AppWidgetHost` instance owned by the launcher; `hostId` is a stable constant |
| View | `AppWidgetHostView` embedded inside an `AndroidView` composable wrapper |
| Lifecycle | `startListening()` on `onResume`, `stopListening()` on `onPause` |
| ID allocation | `AppWidgetHost.allocateAppWidgetId()` before binding; released on removal |
| Binding | `AppWidgetManager.bindAppWidgetIdIfAllowed()` → prompt for `BIND_APPWIDGET` if denied |
| Persistence | Widget IDs + spans stored alongside the grid layout in EPIC-007 |

---

## Decisions

- [x] **Widget removal** — **long-press the widget → context menu → "Remove"**. Consistent with the
  app tile long-press pattern; no separate trash zone needed. The menu also exposes "Resize" as a
  shortcut to enter resize mode.
- [x] **Widget in bar** — not supported. Quick-access bar slots (EPIC-003) hold single app icons
  only.
- [x] **Partially occupied cell** — **round up**. A widget whose `minWidth` / `minHeight` does not
  align to a whole cell boundary always claims the next full cell. Standard Android launcher
  practice; avoids sub-cell clipping artefacts.
- [x] **Widget update frequency** — no launcher-side rate-limiting. `AppWidgetManager` enforces its
  own update intervals per provider; the launcher does not add further throttling. Reassess if
  battery impact is reported in testing.

---

## Related

- EPIC-001 App Grid (cell model; widget cells are first-class items alongside apps and folders)
- EPIC-005 Customisation (Widget Gallery entry point in context menu)
- EPIC-007 Data Persistence (widget ID + span serialisation)
- EPIC-009 App Folders (folders and widgets share the same cell space — overlap rules apply)
- NFR Performance & Responsiveness (widget host view must not block the UI thread; `AppWidgetHostView` rendering is provider-controlled but embedding overhead must be minimal)
