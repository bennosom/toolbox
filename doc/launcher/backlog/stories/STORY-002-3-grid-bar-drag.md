# STORY-002-3 — Grid ↔ bar drag

**Epic:** EPIC-002 App Reordering

## User story

As a user, I can drag apps between the grid and the quick access bar in both directions.

## Acceptance criteria

- Grid → Bar: the dragged app moves to the bar slot; the displaced bar app (if any) moves to the vacated grid cell (`moveGridToBar`)
- Bar → Grid (empty cell): the app moves to the target cell; the bar slot is removed and remaining bar icons close the gap (`moveBarToGrid`)
- Bar → Grid (occupied cell): rejected — the app snaps back to its bar origin
- Bar → Bar: shift — neighbouring bar icons slide to make room; no swap (`moveBarToBar`)
