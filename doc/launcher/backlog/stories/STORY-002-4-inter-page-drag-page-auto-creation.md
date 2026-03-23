# STORY-002-4 — Inter-page drag & page auto-creation

**Epic:** EPIC-002 App Reordering

## User story

As a user, I can drag an app to a different page or create a new page by dragging past the last one.

## Acceptance criteria

- Holding the dragged item at a page edge triggers navigation to the adjacent page (edge-scroll or page-hop)
- Dragging past the last page reveals a trailing empty page (tracked by `dragMode` appending a placeholder)
- Dropping into the trailing page commits it as a real page in the grid
- Cancelling while hovering over the trailing page discards it — the page count returns to its pre-drag value
