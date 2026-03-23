# STORY-001-1 — Static grid layout composable

**Epic:** EPIC-001 App Grid

## User story

As a user, I see a paginated grid of app tiles when I open the launcher.

## Acceptance criteria

- `AppGrid` renders a `HorizontalPager` with one `LazyVerticalGrid` per page using the active `GridSpec` (default 4×4)
- `AppTile` shows icon + label (60 dp icon, 12 dp spacing)
- `PageIndicator` dot strip is shown below the pager; active page dot is visually distinct
- A full-screen `CircularProgressIndicator` is shown while `AppsRepository.grid` emits no value yet (null state)
- Layout is correct at 4×4, 5×5, and any spec within supported range
