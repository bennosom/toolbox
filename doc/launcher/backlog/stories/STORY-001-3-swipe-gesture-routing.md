# STORY-001-3 — Swipe gesture routing

**Epic:** EPIC-001 App Grid

## User story

As a user, swiping down opens notifications and swiping up opens search, so the launcher stays out of my way.

## Acceptance criteria

- Swipe down anywhere on the grid surface calls `StatusBarManager.expandNotificationsPanel()` (or the appropriate API for the target SDK)
- Swipe up anywhere on the grid surface opens the quick-search bottom sheet (stub/no-op placeholder acceptable until EPIC-008 is built)
- Horizontal swipe is handled natively by `HorizontalPager`; no gesture conflict with vertical swipes
- Gestures do not interfere with app tile taps or long-presses
