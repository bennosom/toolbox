# STORY-002-2 — Intra-page grid reordering

**Epic:** EPIC-002 App Reordering

## User story

As a user, I can drag an app to a new position on the same page.

## Acceptance criteria

- Dropping onto an empty cell moves the app there; the source cell becomes empty (`moveGridToGrid`)
- Dropping onto an occupied cell has no effect — the dragged app snaps back to its origin (no swap, no displacement)
- Long-press uses a single `PointerInput` that decides at a threshold whether to open the context menu or start a drag; the two paths do not conflict
- Drag-phase state is managed by a single state machine; the `suppressGridMenu` / `showGridMenu` boolean pair is replaced by it

## Technical constraints

See [TECHNICAL-DESIGN-CONSTRAINTS.md](../TECHNICAL-DESIGN-CONSTRAINTS.md) for the full set of
constraints that apply to all stories. Key highlights for this story:

- The unified `PointerInput` and drag-phase state machine live in dedicated files (≤ 300 lines
  each); the old boolean pair is deleted, not kept alongside.
- `moveGridToGrid` is a pure function on an immutable `Grid` value; it is tested in isolation
  without any Android runtime dependency.
- Unit tests cover: empty-cell move, occupied-cell no-op, gesture-threshold branching (menu vs.
  drag), and every state machine transition; 100 % branch coverage required.
- The ViewModel for this feature follows MVI strictly; the grid `State` is the sole source of
  truth for what is rendered.
- The logger traces: long-press threshold decision, state machine transitions, and drop outcome.
