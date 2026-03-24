# STORY-007-3 — Migrate write path: layout mutations persist to disk

**Epic:** EPIC-007 Data Persistence

## User story

As a user, changes I make to the grid or bar (reorders, additions, removals) are durable across reboots.

## Acceptance criteria

- Every mutation that previously updated `MutableStateFlow<GridData>` now also writes to DataStore
- `populated` is set to `true` on the first committed custom change (grid or bar); grid and bar are always populated together
- `resetDefaults()` sets `populated` to `false`; does not clear `repeated` fields (the flag is sufficient)
- Writes are fire-and-forget from the UI (non-blocking); coroutine scope is tied to the repository, not the composable, so navigating away does not orphan a write
