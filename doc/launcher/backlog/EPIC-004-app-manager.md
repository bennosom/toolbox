# EPIC-004 — App Manager

## Summary

A full-screen diagnostic view for power users and developers. It lists all installed apps with
technical metadata (target SDK, package name, last updated) and allows launching, inspecting, and
uninstalling apps.

> **Important context:** The launcher grid always shows *all* installed and launchable apps — there
> is no hidden app drawer distinction. App Manager is therefore not needed for app discovery; it is
> a developer-oriented tool surfacing technical detail.

---

## Goals

- Provide a technical inventory of all installed apps for dev/debug purposes
- Allow launching, inspecting, and uninstalling apps from one place
- Surface metadata not shown on the grid (target SDK, package name, last updated)

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

## Decisions

- [x] **Presentation** — Stays full-screen modal (replaces the grid). This is appropriate for a
  developer/power-user tool that is not part of normal daily flow.
- [x] **Add to grid/bar from here** — Not in scope. The grid already shows all installed apps;
  there is no need to "add" from App Manager. The feature is view/search/launch/uninstall only.
- [x] **Sort state persistence** — Sort state resets on each open. Acceptable given the dev-tool
  nature of the screen; sort is cheap to re-apply.
- [x] **App count in header** — Out of scope for now; the sticky header shows search and filter
  controls. A count badge may be added later without a design decision.
- [ ] **`AppRow` metadata audit** — `AppRow` implementation not yet reviewed. Needs a code-review
  pass to document exactly which fields are shown per row.

---

## Related

- EPIC-001 App Grid (entry point)
- EPIC-003 Quick Access Bar (add to bar from here?)
