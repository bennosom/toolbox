# EPIC-007 — Data Persistence

## Summary

The launcher's grid layout — app positions, bar contents, and grid spec — must survive process
death, device reboots, and app updates. Currently the state lives only in a `MutableStateFlow` in
memory and is lost on every restart.

---

## Goals

- Persist grid layout and quick bar state across reboots and process restarts
- Survive app installs and removals gracefully (missing apps become empty cells; new apps are appended)
- Provide a clean reset path (back to auto-populated defaults)
- Keep the persistence layer decoupled from the UI and the `AppsRepository` interface

---

## Current implementation

| Component | Detail |
|-----------|--------|
| Store | Jetpack DataStore (Protobuf) via Koin singleton |
| Schema | `GridDataProto` — cols, rows, grid pages, bar entries, populated flag, preferences |
| Read path | `AppsRepository.grid` combines DataStore with installed apps Flow |
| Write path | Fire-and-forget coroutine on `Dispatchers.Default` scope |

---

## Population lifecycle

### First launch (`populated = false`)
1. DataStore returns default instance (`populated = false`, empty grid/bar)
2. Repository auto-populates bar from system default apps (phone, messenger, browser)
3. Repository builds grid from all installed apps sorted by `ComponentName`, column-by-column
4. Resulting layout is persisted with `populated = true`

### Subsequent launches (`populated = true`)
1. DataStore returns persisted grid and bar layout
2. Repository resolves stored `ComponentName` strings against current installed apps
3. Uninstalled apps become vacant cells (no shifting)
4. Newly installed apps are appended after the last occupied cell

### App changes during runtime
- `LauncherApps.Callback` fires on package add/remove/change
- Repository immediately recomputes the grid and persists the updated layout
- UI recomposes via the `grid` Flow

### App changes between launches
- On cold start the repository combines persisted layout with current installed apps
- This naturally handles apps installed/uninstalled while the launcher process was dead
- The reconciliation runs asynchronously on `Dispatchers.Default`

### Reset defaults
- `resetDefaults()` writes `populated = false` to DataStore
- Next emission from `grid` Flow triggers re-population from scratch

---

## Proto schema

```proto
message GridDataProto {
  int32 cols = 1;
  int32 rows = 2;
  repeated GridPageProto grid = 3;
  repeated string bar = 4;
  bool populated = 5;
  int32 dark_mode_preference = 6;
  bool bar_hidden = 7;
}

message GridPageProto {
  repeated GridCellProto cells = 1;
}

message GridCellProto {
  int32 col = 1;
  int32 row = 2;
  string app_id = 3;
}
```

> **`populated` flag:** Proto3 `repeated` fields cannot distinguish "absent" from "empty list".
> The single `populated` boolean solves this: when `false`, the `grid` and `bar` fields are
> ignored and defaults are auto-populated. When `true`, the persisted layout is used as-is.

---

## Decisions

- [x] **Proto schema location** — `:launcher` module. The schema is specific to launcher layout
  and has no current reason to be shared with other modules.
- [x] **DataStore DI** — Exposed as a **Koin singleton**. `AppsRepositoryImpl` receives the
  `DataStore<GridData>` via constructor injection. This makes the store testable and mockable
  independently of the repository.
- [x] **Cloud backup** — Android Auto Backup (passive). The DataStore file is included in Auto
  Backup by default. No active Drive sync or user-visible backup management is in scope.
- [x] **Single populated flag** — Replaces the earlier `has_user_grid` / `has_user_bar` pair.
  Grid and bar are always populated together — there is no state where one is customised and the
  other is not.
- [x] **Export/import** — Not in scope.

---

## Related

- EPIC-001 App Grid
- EPIC-003 Quick Access Bar
- EPIC-005 Customisation (spec changes must also be persisted)
