# Cold Start

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
