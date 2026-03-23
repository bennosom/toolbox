# STORY-003-1 — Bar composable

**Epic:** EPIC-003 Quick Access Bar

## User story

As a user, I see a row of my most-used apps pinned to the bottom of the screen on every grid page.

## Acceptance criteria

- `QuickBar` is a fixed 96 dp `Row` pinned below the grid pager; it remains visible regardless of which page is active
- Slots render `AppIcon` only — no label, 60 dp icons centred in the row
- Tapping a slot launches the corresponding app
- When the bar visibility toggle is off (EPIC-005), `QuickBar` is not composed and the grid takes the full height
- Bar state is driven by `AppsRepository.bar` (the same Flow used for persistence in EPIC-007)
