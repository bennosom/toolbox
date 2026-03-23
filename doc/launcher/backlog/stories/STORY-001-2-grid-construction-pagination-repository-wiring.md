# STORY-001-2 — Grid construction, pagination, and repository wiring

**Epic:** EPIC-001 App Grid

## User story

As a user, my installed apps are laid out in a predictable order and the grid updates instantly when I install or remove an app.

## Acceptance criteria

- On first run / after reset, apps are sorted by `ComponentName` and filled column-by-column across pages
- Newly installed apps not yet in the stored layout are appended after the last occupied cell; a new page is created when the last page is full
- `AppGrid` is driven by `AppsRepository.grid` — a `Flow` combining installed apps with the stored layout (wired to EPIC-007 DataStore)
- `LauncherApps.Callback` triggers a recomposition on package add/remove/change; no manual refresh needed
- Grid read and layout construction do not run on the main thread
