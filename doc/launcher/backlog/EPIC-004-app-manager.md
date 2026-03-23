# EPIC-004 — App Manager

## Summary

A full-screen list of all installed apps that the user can browse, search, sort, and act on.
It serves as the escape hatch when an app cannot be found on the home screen grid.

---

## Goals

- Let the user find any installed app regardless of grid placement
- Provide enough metadata to be useful for power users (target SDK, last updated, package name)
- Allow launching, inspecting, and uninstalling apps from one place
- Be fast enough to use as an app drawer alternative

---

## Current implementation

| Aspect | Detail |
|--------|--------|
| Component | `AppManager` composable (`ui/manager/AppManager.kt`) |
| Entry point | Grid context menu → "Apps list" |
| Exit | Close button (top left) — returns to the app grid |
| Layout | `LazyVerticalGrid` with adaptive columns (min 300 dp) — scales on tablets |
| Header | Sticky — survives scrolling |

### Search
- Toggle via search icon in the header
- Matches app label or package name (case-insensitive, substring)
- Clear button and back-to-list button in the search bar

### Filter chips
| Chip | Effect |
|------|--------|
| User apps | Show only non-system apps (default: on) |
| System apps | Show only system apps (default: off) |
| Both on | Show all apps |
| Both off | Show all apps (fallback) |

### Sort options (filter chips, toggleable direction)
| Option | Sort key |
|--------|----------|
| Label | `app.label` (alphabetical) |
| Package | `app.componentName.packageName` |
| Last Updated | `app.lastUpdatedTimeMillis` |
| Target SDK | `app.targetSdk` |

Direction toggles between ↑ ascending and ↓ descending by tapping the active chip again.

### Per-app actions (via `AppRow`)
- Launch app
- Open system app details (Settings → App info)
- Request uninstall (system dialog); disabled for system apps

---

## Open questions / gaps

- [ ] App Manager is currently modal (replaces the grid) — should it be a bottom sheet or overlay instead?
- [ ] There is no way to add an app from App Manager directly to the grid or bar — is that in scope?
- [ ] `AppRow` implementation not reviewed — what metadata is shown per row?
- [ ] Sort state is not persisted — resets each time App Manager is opened
- [ ] Should the app count be shown in the header?

---

## Related

- EPIC-001 App Grid (entry point)
- EPIC-003 Quick Access Bar (add to bar from here?)
