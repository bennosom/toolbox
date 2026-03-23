# STORY-003-3 — Bar layout & scrollable overflow

**Epic:** EPIC-003 Quick Access Bar

## User story

As a user, my bar icons are always neatly centred and I can scroll to reach any app when I have more than fit on screen.

## Acceptance criteria

- Bar icons are laid out in a `LazyRow` with `Arrangement.Center` — no placeholders, no gaps
- When total icon width exceeds the bar width the row scrolls horizontally; no icon is hidden or clipped
- Removing an app immediately closes the gap and re-centres the remaining icons
- Bar slot count is read from `bar_slots` in DataStore, not derived from `spec.rows`
- `bar_slots = 0` falls back to `spec.cols` until the user sets an explicit value (EPIC-005)
