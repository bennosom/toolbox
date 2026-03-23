# STORY-002-1 — Drag infrastructure & shared gesture modifier

**Epic:** EPIC-002 App Reordering

## User story

As a developer, drag-and-drop shares a single well-defined gesture implementation so there is no duplicated code between the grid and bar.

## Acceptance criteria

- A shared `draggableAppSource` modifier encapsulates long-press → drag start, `ClipData` construction (launcher `Intent`, MIME `text/vnd.android.intent`), and drag shadow (60 dp icon bitmap)
- `AppTile` (grid) and bar icons both use `draggableAppSource`; no duplicated gesture logic remains
- On drag start `draggingId` is set immediately so the source tile is hidden without delay (no ghost tile visible at origin)
- `dragMode = true` triggers the narrowed-page-width animation; no additional overlay or banner is added
- On cancel (`onEnded` without a preceding `onDrop`), `dragMode` and `draggingId` are reset and `workingState` is discarded — committed grid state is unchanged
