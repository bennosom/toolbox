# STORY-007-3 — Migrate write path: layout mutations persist to disk

**Epic:** EPIC-007 Data Persistence

## User story

As a user, changes I make to the grid or bar (reorders, additions, removals) are durable across reboots.

## Acceptance criteria

- Every mutation that previously updated `MutableStateFlow<GridData>` now also writes to DataStore
- `has_user_grid` is set to `true` on the first committed custom grid change; same for `has_user_bar`
- `resetDefaults()` sets both sentinel flags to `false`; does not clear `repeated` fields (sentinel is sufficient)
- Writes are fire-and-forget from the UI (non-blocking); coroutine scope is tied to the repository, not the composable, so navigating away does not orphan a write
