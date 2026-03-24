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

## Technical constraints

See the [spec/](../../spec/) design documents for the full set of
constraints that apply to all stories. Key highlights for this story:

- `draggableAppSource` is a `Modifier` extension declared in its own file (≤ 300 lines); it is the
  single source of gesture recognition shared by grid and bar — no per-site copies.
- Drag state (drag mode, dragging identifier, working grid) is represented as an MVI `State` data
  class; mutations are handled exclusively inside the ViewModel.
- The logger must emit a trace entry on drag start, drag cancel, and every drop outcome.
- Unit tests cover the cancel path, the ghost-tile suppression, and each drag-state transition;
  100 % branch coverage is required on the state machine.
- Compose previews must show the modifier applied to an `AppTile` in both dragging and idle states.
