# EPIC-003 — Quick Access Bar

## Summary

A fixed horizontal bar pinned to the bottom of the home screen that gives instant access to a
small set of frequently used apps. It is always visible regardless of which grid page is active.

---

## Goals

- Provide one-tap access to the user's most important apps without paging
- Auto-populate with sensible defaults on first run (no manual setup required)
- Allow the user to rearrange bar slots and move apps between bar and grid via drag

---

## Current implementation

| Aspect | Detail |
|--------|--------|
| Component | `Row` inside `AppGrid` (lower section of the composable) |
| Height | 96 dp |
| Capacity | Always equal to `spec.cols` (e.g. 4 for a 4×4 grid) |
| Default population | System default phone, messenger, and browser apps (resolved via `ResolveDefaultApps`) |
| Item layout | `AppIcon` only (no label) — 60 dp icons centred in the row |
| Interaction | Tap to launch; long-press → context menu or drag |
| Drag target | Full `DragAndDropTarget` — accepts drops from grid |
| Drop positioning | `determineQuickBarTargetIndex` — inserts at the midpoint of the nearest neighbour |
| Drag source | Same `dragAndDropSource` gesture as grid tiles |

### Default app resolution (`resolveDefaultApps`)
Queries the system for:
1. Default phone/dialler app
2. Default SMS/messaging app
3. Default browser app

Only apps actually installed are included; missing defaults are silently skipped.

---

## Decisions

- [x] **Bar capacity** — Always bound to `spec.cols` (column count) so the bar visually aligns with
  the grid above it. Not a user-configurable setting. When the grid spec changes, bar capacity
  changes automatically.
- [x] **Empty slot placeholders** — Not supported. Removing an app closes the gap immediately;
  remaining icons are horizontally centred. No placeholder concept exists in the bar.
- [x] **Max bar slots** — No hard cap. When icons exceed the bar width the bar scrolls horizontally;
  no icon is hidden or clipped. There is no minimum-size enforcement — scroll is the overflow strategy.
- [x] **Bar labels** — No labels, intentional. The minimal icon-only aesthetic is by design.
- [x] **Bar visibility** — The bar can be hidden via a toggle in the customisation settings.

---

## Related

- EPIC-001 App Grid
- EPIC-002 App Reordering
- EPIC-005 Customisation
