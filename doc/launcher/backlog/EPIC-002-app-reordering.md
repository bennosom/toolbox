# EPIC-002 — App Reordering

## Summary

Users can rearrange apps on the home screen by long-pressing an app tile and dragging it to a new
position. Dragging is supported within a page, across pages (via the quick bar or future page-edge
scroll), and between the grid and the quick access bar.

---

## Goals

- Give the user full control over app placement without a dedicated "edit mode" modal
- Support drag within a page and across pages (edge-scroll or page-hop triggers navigation)
- Support grid ↔ quick-access bar drag in both directions (grid→bar, bar→grid)
- Support drag to AAOS system bar slots (status bar / navigation bar drop targets exposed by SystemUI)
- Keep the grid state consistent at all times — no lost apps, no duplicates
- Provide clear visual feedback during the drag (ghost image, animated reflow)

---

## Current implementation (WIP — commit `a86c885`)

| Aspect | Detail |
|--------|--------|
| Trigger | Long-press on any app tile in grid or bar |
| Mechanism | Android `DragAndDrop` API (`dragAndDropSource` / `dragAndDropTarget`) |
| Transfer data | `ClipData` wrapping a launcher `Intent` (MIME `text/vnd.android.intent`) |
| Drag shadow | App icon bitmap drawn at 60 dp |
| Drag state | `dragMode: Boolean` — when true, an extra empty page is appended to the pager |
| Working state | `workingState: Grid` — mutated live during drag, committed on `onDrop` |
| Drop commit | `onGridUpdate(workingState)` → `AppsRepository.update(grid)` |
| Cancel | `onEnded` resets `dragMode` and `draggingId` without committing |

### Supported moves (implemented in `Grid` extension functions)
| From | To | Function |
|------|----|----------|
| Grid cell | Grid cell (same or other page) | `moveGridToGrid` |
| Grid cell | Quick bar slot | `moveGridToBar` |
| Quick bar slot | Grid cell | `moveBarToGrid` |
| Quick bar slot | Quick bar slot | `moveBarToBar` |

### Drop behaviour
- Grid → Grid (empty cell): app moves to the target cell; source cell becomes empty.
- Grid → Grid (occupied cell): **rejected** — no effect, app snaps back to its origin cell. The
  user must free the target cell first (remove or relocate the occupying item) before the drop
  is accepted. This applies to any occupant: app, folder, or widget span.
- Grid → Bar: displaced bar app moves to the source grid cell.
- Bar → Grid (empty cell): app moves to the target cell; bar slot becomes empty.
- Bar → Grid (occupied cell): **rejected** — same no-op rule as Grid → Grid above.
- Bar → Bar: **shift** — apps slide to make room (no swap).

---

## Known issues / implementation tasks

| # | Issue | Notes |
|---|-------|-------|
| BUG-1 | **Inter-page drag missing** — no edge-scroll or page-hop during drag | Core blocker; see Decisions → page creation via drag for the desired behaviour |
| BUG-2 | **Ghost tile lingers at origin** — dragged tile stays visible at source cell until `onEnded` fires in some edge cases | Likely caused by `draggingId` not being cleared eagerly enough on `onStarted` |
| BUG-3 | **Menu state race** — `suppressGridMenu` / `showGridMenu` has race conditions during drag | Needs a unified drag-phase state machine rather than two independent booleans |
| BUG-4 | **Long-press gesture sequencing** — long-press → context menu and long-press → drag are two separate paths that need careful ordering to avoid conflict | Consider a single `PointerInput` that decides at a threshold whether to open the menu or start a drag |
| BUG-5 | **Duplicate gesture code** — bar drag uses its own copy of the grid drag recognition logic | Extract shared `draggableAppSource` modifier; use in both `AppTile` (grid) and bar icon |

---

## Decisions

- [x] **Drag mode indicator** — The existing narrowed-page-width animation is sufficient. No
  additional overlay or banner is needed.
- [x] **Drop on mid-animation cell** — Last-writer-wins: whatever cell the pointer is over when
  `onDrop` fires is the target. In-flight animations complete visually after the state update.
- [x] **Page creation via drag** — Dragging past the last page auto-creates a new empty page (already
  reflected in the `dragMode` logic that appends a trailing empty page). On drop into that page the
  extra placeholder is committed as a real page; on cancel it is discarded.
- [x] **No-swap on occupied cells** — Dropping an app (from grid or bar) onto an occupied cell has
  no effect; the dragged app snaps back to its origin. There is no automatic swap or displacement.
  The user must manually clear the target cell before the drop is accepted. This keeps moves
  explicit and avoids accidental overwriting.
- [x] **Bar shift vs. grid no-op asymmetry** — Bar → Bar reorders by shifting neighbours because
  the bar is a 1-D list where shifting is natural, cheap, and expected by users (mirrors standard
  Android dock behaviour). The grid is 2-D: cascading a shift across rows/pages would be complex,
  non-obvious, and easy to trigger accidentally. The grid therefore uses no-op-on-occupied as the
  simpler, safer model.

---

## Related

- EPIC-001 App Grid
- EPIC-003 Quick Access Bar
- NFR Performance & Responsiveness (drag must never block the UI thread)
