# Progress & Cancellation

Operations that may take more than ~300 ms must never silently block the user.

| Principle | Requirement |
|-----------|-------------|
| Indicate progress | Show a `CircularProgressIndicator` (or inline skeleton) for any operation that may exceed 300 ms |
| Provide cancellation | Any sheet, dialog, or overlay that shows a progress indicator must have a visible cancel / dismiss action |
| Cancel propagates | Cancelling in the UI must cancel the underlying coroutine (use structured concurrency — pass `CoroutineScope` or cancel via `Job`) |
| No orphaned work | Navigating away from a screen must cancel all work that was started for that screen |

**Affected operations (non-exhaustive):**
- Grid spec change + layout migration (EPIC-005)
- Wallpaper decode and apply (EPIC-005)
- Drag-and-drop state commit if it triggers a persistence flush (EPIC-002, EPIC-007)
