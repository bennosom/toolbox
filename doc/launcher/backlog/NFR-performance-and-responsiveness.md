# NFR — Performance & Responsiveness

Non-functional requirements that apply across the whole launcher. These are not features; they are
quality constraints that every epic must respect.

---

## Cold Start

The launcher is the first screen the user sees after boot or after pressing the home button from
another app. A slow cold start destroys the perception of device quality.

| Requirement | Target |
|-------------|--------|
| Time-to-interactive (grid visible and tappable) | ≤ 300 ms on reference hardware |
| Splash / loading screen | Must not exist. Show the grid immediately; stream data into it. |
| Deferred work | Anything not needed to render the first frame (analytics, background sync) must be deferred |

**Implementation guidance:**
- `AppsRepository` must emit its first value from the persisted store (EPIC-007) synchronously or
  within one coroutine hop — never wait for `LauncherApps.getActivityList()` before showing the UI.
- Avoid `GlobalScope`, `runBlocking`, or any blocking I/O on the main thread during startup.

---

## Resource Efficiency

The launcher runs continuously in the background. Every milliwatt and megabyte it consumes is taken
away from the user's actual apps.

| Requirement | Constraint |
|-------------|-----------|
| Background CPU | No background work unless triggered by a system event (package change, wallpaper change) |
| Memory | No retained bitmaps or caches larger than necessary for the current screen |
| Wake locks | None — the launcher must not hold wake locks |
| Coroutine scopes | All long-lived work must be scoped to `viewModelScope` or `lifecycleScope`; no leaks |
| Flows | All hot flows must be cancelled when the UI is not visible |

---

## UI Thread

The main / UI thread must be reserved exclusively for drawing and event dispatch.

| Rule | Detail |
|------|--------|
| No blocking I/O on main | All file, database, and `SharedPreferences` access on `Dispatchers.IO` |
| No heavy computation on main | Layout migration, fuzzy search indexing, bitmap decode — all off main thread |
| Frame budget | Every frame must complete within 16 ms (60 fps) / 11 ms (90 fps) where the display supports it |
| Strict mode | `StrictMode.ThreadPolicy` with `detectAll()` enabled in debug builds |

---

## Progress & Cancellation

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
- App list load in App Manager (EPIC-004)
- Drag-and-drop state commit if it triggers a persistence flush (EPIC-002, EPIC-007)

---

## Related

- EPIC-001 App Grid (page-swipe frame rate)
- EPIC-002 App Reordering (drag must never block UI thread)
- EPIC-004 App Manager (app list loading)
- EPIC-005 Customisation (wallpaper decode, layout migration)
- EPIC-007 Data Persistence (async read/write)
- EPIC-008 Search (per-keystroke filtering latency)
