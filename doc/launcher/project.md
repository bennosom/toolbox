# Launcher — Product Overview

## Vision

A minimal, opinionated Android launcher that gets out of the way.

The Launcher does not try to be everything. It provides a clean home screen with deliberate
constraints: a paginated app grid, a quick access bar for the apps you use most, and just enough
customisation to make it yours. No widgets, no feeds, no notifications surface — just apps.

The guiding principle is **less, but better**: fewer moving parts, cleaner interactions, and a
visual layer that respects the system wallpaper and Material You theming rather than competing
with it.

---

## User Goals

| Goal | How the Launcher supports it |
|------|------------------------------|
| Find and launch apps quickly | Paginated grid with consistent placement; quick bar for daily drivers |
| Organise apps on my terms | Drag-and-drop reordering across pages and into/from the quick bar |
| Keep the home screen uncluttered | Fixed grid, no widgets, no automatic re-sorting |
| Use the same launcher across my devices | Adaptive layout for phones, tablets, and foldables |
| It should feel native | Material You dynamic theming, edge-to-edge, wallpaper-aware colours |

---

## Scope

### In scope

- **App grid** — multi-page horizontal pager, configurable columns × rows, per-cell app placement
- **Quick access bar** — pinned apps row, auto-seeded from system default apps (phone, messaging, browser)
- **Drag-and-drop reordering** — move apps within the grid, across pages, and between grid and quick bar
- **App context menu** — shortcuts, info, uninstall from a long-press menu
- **App manager** — full installed-app list with search, filter (user/system), and sort
- **Customisation** — grid size presets, wallpaper picker, dark/light mode
- **Adaptive layouts** — phone portrait & landscape, tablet portrait & landscape, foldable book & tabletop
- **Persistent layout** — grid and bar state stored via Jetpack DataStore (Protobuf)

### Out of scope (intentionally)

- Home screen widgets
- Notification badges
- Gesture navigation overrides
- Icon packs / theming engine
- Search / web search bar

---

## Key Constraints

- **minSdk 31** (Android 12+) — enables Material You dynamic colour and modern window APIs
- **Compose-only UI** — no XML layouts
- **Single activity** — `LauncherActivity` is the sole entry point
- **No cloud sync** — layout state is local to the device

---

## Documentation Map

```
doc/launcher/
├── project.md          ← this file — vision and goals
├── backlog/            ← epics and stories
└── spec/               ← ADRs and technical design documents
```

---

## Status

| Area | Status |
|------|--------|
| App grid (display & launch) | Implemented |
| Quick access bar | Implemented |
| Drag-and-drop reordering | In progress (WIP) |
| App manager | Implemented |
| Grid size customisation | Implemented |
| Wallpaper picker | Planned |
| Adaptive layouts | Scaffolded — form factor detection in place, layout adaptation in progress |
| Data persistence | Planned (DataStore / Protobuf) |
