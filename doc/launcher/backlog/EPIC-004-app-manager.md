# EPIC-004 — App Manager

## Summary

A diagnostic view for power users and developers presented as a **bottom sheet** over the home
screen. It lists all installed apps with technical metadata (target SDK, package name, last updated)
and allows launching, inspecting, and uninstalling apps.

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
| Presentation | `ModalBottomSheet` over the home screen (grid remains visible and interactive behind the scrim) |
| Exit | Swipe down, back gesture, or tap outside the sheet |
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

- [x] **Presentation** — `ModalBottomSheet` over the home screen. The grid stays visible behind the
  scrim — the user can see their apps while consulting metadata. The sheet replaces the previous
  full-screen modal approach.
- [x] **Add to grid/bar from here** — Not in scope. The grid already shows all installed apps;
  there is no need to "add" from App Manager. The feature is view/search/launch/uninstall only.
- [x] **Sort state persistence** — Sort state resets on each open. Acceptable given the dev-tool
  nature of the screen; sort is cheap to re-apply.
- [x] **App count in header** — Out of scope for now; the sticky header shows search and filter
  controls. A count badge may be added later without a design decision.
- [x] **`AppRow` metadata audit** — `AppListRow` composable (`ui/manager/AppRow.kt:26`).
  Each row shows:
  - App icon (48 dp) + label
  - "Pre-installed system app" / "Installed by user" classification
  - Package name (component name string)
  - Version name + version code
  - Target SDK + minimum SDK
  - Install date and last-updated date
  - Inline fuzzy-match highlighting when a search query is active (yellow on black)

  Actions per row: **Launch** (tap row), **Settings** (opens system App Info), **Uninstall**
  (system dialog; hidden for system apps).

---

## Related

- EPIC-001 App Grid (entry point)
- EPIC-003 Quick Access Bar (add to bar from here?)
