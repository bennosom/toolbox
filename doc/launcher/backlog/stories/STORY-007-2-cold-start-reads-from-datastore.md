# STORY-007-2 — Migrate read path: cold-start loads from DataStore

**Epic:** EPIC-007 Data Persistence

## User story

As a user, my grid layout survives a process restart so I don't have to re-arrange apps after every reboot.

## Acceptance criteria

- On cold start, `AppsRepositoryImpl` reads `DataStore<GridData>` instead of constructing `defaultGridData` from scratch
- When `has_user_grid = false` (first launch / after reset), auto-populated defaults are used regardless of `grid` field contents; same for `has_user_bar`
- Grid is visible without blocking the main thread; DataStore read happens on IO dispatcher (StrictMode clean)
- If the read takes >300 ms a `CircularProgressIndicator` is shown until data is ready
