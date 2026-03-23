# STORY-002-2 — Intra-page grid reordering

**Epic:** EPIC-002 App Reordering

## User story

As a user, I can drag an app to a new position on the same page.

## Acceptance criteria

- Dropping onto an empty cell moves the app there; the source cell becomes empty (`moveGridToGrid`)
- Dropping onto an occupied cell has no effect — the dragged app snaps back to its origin (no swap, no displacement)
- Long-press uses a single `PointerInput` that decides at a threshold whether to open the context menu or start a drag; the two paths do not conflict
- Drag-phase state is managed by a single state machine; the `suppressGridMenu` / `showGridMenu` boolean pair is replaced by it
