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

## Current implementation (in-memory only)

| Component | Detail |
|-----------|--------|
| Store | `MutableStateFlow<GridData>` inside `AppsRepositoryImpl` |
| Schema | `GridData(cols, rows, grid: List<Map<Cell, AppId?>>?, bar: List<AppId>?)` |
| Lifetime | Process lifetime only — reset to `defaultGridData` on every cold start |
| Null semantics | `grid == null` or `bar == null` means "use auto-populated defaults" |

The in-memory model is already serialisation-friendly: it uses only primitive types and
`ComponentName.flattenToString()` as stable app identifiers.

---

## Planned: Jetpack DataStore (Protobuf)

### Rationale over SharedPreferences
- Type-safe schema via Protocol Buffers
- Coroutine/Flow-native API — fits the existing reactive architecture
- Handles concurrent writes safely
- Schema evolution support (field additions without migration pain)

### Proposed schema (draft)

```proto
message GridData {
  int32 cols = 1;
  int32 rows = 2;
  repeated GridPage grid = 3;  // absent = auto-populate
  repeated string bar = 4;     // absent = auto-populate; values = ComponentName strings
}

message GridPage {
  repeated GridCell cells = 1;
}

message GridCell {
  int32 col = 1;
  int32 row = 2;
  string app_id = 3;  // ComponentName.flattenToString(); empty = vacant cell
}
```

### Migration path
1. Add `datastore-proto` dependency and generate `GridData` Protobuf class
2. Replace `MutableStateFlow<GridData>` with `DataStore<GridData>` in `AppsRepositoryImpl`
3. Map existing `io.engst.launcher.data.GridData` Kotlin class to/from the Protobuf type
4. Provide a `resetDefaults()` that writes the null-grid sentinel to the store

---

## Decisions

- [x] **Proto schema location** — `:launcher` module. The schema is specific to launcher layout
  and has no current reason to be shared with other modules.
- [x] **DataStore DI** — Exposed as a **Koin singleton**. `AppsRepositoryImpl` receives the
  `DataStore<GridData>` via constructor injection. This makes the store testable and mockable
  independently of the repository.
- [x] **Cloud backup** — Android Auto Backup (passive). The DataStore file is included in Auto
  Backup by default. No active Drive sync or user-visible backup management is in scope.
- [x] **Migration story** — Not applicable pre-launch (no persisted state exists yet). Post-launch
  migrations will use DataStore's built-in `DataMigration` API. No bespoke migration tooling needed.
- [x] **Export/import** — Not in scope.

---

## Related

- EPIC-001 App Grid
- EPIC-003 Quick Access Bar
- EPIC-005 Customisation (spec changes must also be persisted)
