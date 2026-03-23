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

## Open questions / gaps

- [ ] Bar capacity is currently coupled to `spec.rows` — should it be an independent setting?
- [ ] Should the bar support an empty/unoccupied slot placeholder (to preserve positional intent)?
- [ ] What is the max number of bar slots? Is there a visual overflow treatment?
- [ ] Bar apps have no label — is this intentional for the minimal aesthetic?
- [ ] Should the bar be hideable (e.g. full-screen mode)?

---

## Related

- EPIC-001 App Grid
- EPIC-002 App Reordering
- EPIC-005 Customisation (bar slot count)
