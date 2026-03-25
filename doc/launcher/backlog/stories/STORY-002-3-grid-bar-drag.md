# STORY-002-3 — Grid ↔ bar drag

**Epic:** EPIC-002 App Reordering

## User story

As a user, I can drag apps between the grid and the quick access bar in both directions.

## Acceptance criteria

- Grid → Bar: the dragged app moves to the bar slot; the displaced bar app (if any) moves to the vacated grid cell (`moveGridToBar`)
- Bar → Grid (empty cell): the app moves to the target cell; the bar slot is removed and remaining bar icons close the gap (`moveBarToGrid`)
- Bar → Grid (occupied cell): rejected — the app snaps back to its bar origin
- Bar → Bar: shift — neighbouring bar icons slide to make room; no swap (`moveBarToBar`)

## Technical constraints

See the [spec/](../../spec/) design documents for the full set of
constraints that apply to all stories. Key highlights for this story:

- `moveGridToBar`, `moveBarToGrid`, and `moveBarToBar` are pure functions on immutable value
  types; each lives in its own file or a logically grouped extension file (≤ 300 lines).
- Grid and bar drag state are handled through a single MVI source of truth (`GridScreenState`)
  in `GridViewModel`; grid/bar composables communicate changes via intents.
- Unit tests cover all four move directions plus the occupied-cell rejection path; 100 % branch
  coverage required.
- Robolectric Compose tests verify that bar and grid composables render the correct state after
  each move type and that previews are present in the same file.
- The logger traces: move type selected, displacement target, rejection reason (occupied cell).
