# STORY-007-4 — App install / uninstall reactions update persisted state

**Epic:** EPIC-007 Data Persistence

## User story

As a user, installing a new app adds it to the grid and uninstalling one removes it cleanly — without corrupting my layout.

## Acceptance criteria

- On app install: new app is appended after the last occupied cell; a new page is created if the last page is full
- On app uninstall: the cell becomes vacant (empty `app_id`); no other cell shifts position
- Both reactions update DataStore so the new state survives the next reboot
- `has_user_grid` remains `true` if the user had previously customised the layout
