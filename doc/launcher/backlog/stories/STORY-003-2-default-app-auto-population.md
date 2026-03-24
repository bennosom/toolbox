# STORY-003-2 — Default app auto-population

**Epic:** EPIC-003 Quick Access Bar

## User story

As a user, the bar is pre-filled with my default phone, messaging, and browser apps on first launch so I don't have to set it up manually.

## Acceptance criteria

- On first run (`populated = false`), `resolveDefaultApps` queries the system for the default dialler, SMS, and browser apps
- Any default that is not installed is silently skipped — no placeholder, no crash
- Resolved apps are written to DataStore together with the full grid+bar layout and `populated` is set to `true` so auto-population never re-runs
- Resolution runs off the main thread
