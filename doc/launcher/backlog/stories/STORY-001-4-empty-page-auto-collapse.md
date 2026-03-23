# STORY-001-4 — Empty page auto-collapse

**Epic:** EPIC-001 App Grid

## User story

As a user, pages that become completely empty disappear automatically so I'm not left swiping through blank screens.

## Acceptance criteria

- When all cells on a page become vacant (via uninstall or drag-away), the page is removed from the pager
- The pager snaps to the preceding page; if the collapsed page was page 0, it stays on the new page 0
- The last remaining page is never removed — a minimum of one page always exists
- Collapse is reflected in the persisted layout (DataStore updated)
