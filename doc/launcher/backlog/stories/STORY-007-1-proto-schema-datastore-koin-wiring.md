# STORY-007-1 — Proto schema & DataStore Koin wiring

**Epic:** EPIC-007 Data Persistence

## User story

As a developer, I need a type-safe, DI-friendly DataStore instance available to the repository so all future persistence work has a foundation to build on.

## Acceptance criteria

- `grid_data.proto` is defined in `:launcher` with `GridData`, `GridPage`, `GridCell` messages and the `populated`, `bar_slots` fields
- `datastore-proto` dependency added; Protobuf class is generated at build time
- `DataStore<GridData>` exposed as a Koin singleton; `AppsRepositoryImpl` receives it via constructor injection
- No behaviour change — existing in-memory flow still drives the UI
