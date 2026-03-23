# EPIC-002 — App Reordering

## Summary

Users can rearrange apps on the home screen by long-pressing an app tile and dragging it to a new
position. Dragging is supported within a page, across pages (via the quick bar or future page-edge
scroll), and between the grid and the quick access bar.

---

## Goals

- Give the user full control over app placement without a dedicated "edit mode" modal
- Support the four drag scenarios: grid→grid, grid→bar, bar→grid, bar→bar
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

### Swap behaviour
- Grid → Grid: **swap** — source and destination cells exchange apps (neither is lost)
- Grid → Bar: displaced bar app moves to the source grid cell
- Bar → Grid: displaced grid app inserts into bar at origin index
- Bar → Bar: **shift** — apps slide to make room (no swap)

---

## Known issues / in-progress

- [ ] Inter-page drag not yet working — there is no edge-scroll or page-hop during drag
- [ ] The dragged tile remains visible at origin until `onEnded` fires in some edge cases
- [ ] `suppressGridMenu` / `showGridMenu` state management during drag has some race conditions
- [ ] Long-press → context menu → drag: two separate gesture paths need careful sequencing
- [ ] Bar drag uses duplicate gesture recognition code — should be extracted with grid drag

---

## Open questions / gaps

- [ ] Should there be a visual "drag mode" indicator beyond the narrowed page width?
- [ ] What is the expected behaviour when dropping on an already-dragging-over cell mid-animation?
- [ ] Should users be able to drag to an empty page (page creation via drag)?

---

## Related

- EPIC-001 App Grid
- EPIC-003 Quick Access Bar
