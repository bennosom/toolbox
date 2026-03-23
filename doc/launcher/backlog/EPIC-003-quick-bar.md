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
| Capacity | Defaults to `spec.rows` slots (e.g. 4 for a 4×4 grid) |
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

- [x] **Bar capacity** — Decoupled from `spec.rows`. Bar slot count is an independent user-configurable
  value exposed in the customisation settings. The current coupling to `spec.rows` is a temporary
  default and should be replaced with a dedicated `barSlots` setting (see EPIC-005).
- [x] **Empty slot placeholders** — Supported. Users may leave intentional gaps between bar icons.
  An empty slot is a first-class bar item that occupies space and can be dragged around or removed.
- [x] **Max bar slots** — No hard cap. Overflow is naturally constrained by the physical bar width;
  icons below the minimum touch-target size are not shown. The drag system prevents adding more slots
  than fit comfortably at the minimum icon size.
- [x] **Bar labels** — No labels, intentional. The minimal icon-only aesthetic is by design.
- [x] **Bar visibility** — The bar can be hidden via a toggle in the customisation settings.

---

## Related

- EPIC-001 App Grid
- EPIC-002 App Reordering
- EPIC-005 Customisation (bar slot count)
