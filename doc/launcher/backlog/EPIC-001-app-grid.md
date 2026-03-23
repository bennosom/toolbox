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

## Open questions / gaps

- [ ] Empty cells have no visual affordance — should they be indicated (dashed border, dim slot)?
- [ ] Page auto-scroll during drag: should hovering near the edge of a page scroll to the next one?
- [ ] Maximum page count — is there a cap, or unlimited pages?
- [ ] What happens when all apps are uninstalled from a page — does the page collapse?

---

## Related

- EPIC-002 App Reordering (drag-and-drop within grid)
- EPIC-003 Quick Access Bar (bar at bottom)
- EPIC-005 Customisation (grid size presets)
- EPIC-007 Data Persistence (saving layout to disk)
