# Resource Efficiency

The launcher runs continuously in the background. Every milliwatt and megabyte it consumes is taken
away from the user's actual apps.

| Requirement | Constraint |
|-------------|-----------|
| Background CPU | No background work unless triggered by a system event (package change, wallpaper change) |
| Memory | No retained bitmaps or caches larger than necessary for the current screen |
| Wake locks | None — the launcher must not hold wake locks |
| Coroutine scopes | All long-lived work must be scoped to `viewModelScope` or `lifecycleScope`; no leaks |
| Flows | All hot flows must be cancelled when the UI is not visible |
