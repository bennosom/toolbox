# STORY-002-4 — Inter-page drag & page auto-creation

**Epic:** EPIC-002 App Reordering

## User story

As a user, I can drag an app to a different page or create a new page by dragging past the last one.

## Acceptance criteria

- Holding the dragged item at a page edge triggers navigation to the adjacent page (edge-scroll or page-hop)
- Dragging past the last page reveals a trailing empty page (tracked by `dragMode` appending a placeholder)
- Dropping into the trailing page commits it as a real page in the grid
- Cancelling while hovering over the trailing page discards it — the page count returns to its pre-drag value

## Technical constraints

See the [spec/](../../spec/) design documents for the full set of
constraints that apply to all stories. Key highlights for this story:

- Edge-scroll / page-hop logic is a self-contained component (≤ 300 lines) that emits navigation
  intents into the MVI flow; it must not access the pager state directly.
- Page auto-creation is driven entirely by `dragMode` state in the ViewModel; the placeholder page
  is never written to the repository until `onDrop` commits it.
- Unit tests cover: edge trigger fires navigation intent, drop into placeholder commits page, cancel
  over placeholder discards page, and page count invariant across all paths; 100 % branch coverage
  required.
- Robolectric Compose test verifies the trailing placeholder page appears during drag mode and
  disappears on cancel.
- The logger traces: edge-scroll trigger, page-hop event, placeholder page commit, and placeholder
  discard.
