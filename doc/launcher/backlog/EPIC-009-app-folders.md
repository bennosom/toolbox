# EPIC-009 — App Folders

## Summary

Users can group related apps into folders directly on the grid. A folder occupies a single grid
cell. Tapping it expands the folder in place to reveal its contents. Apps can be added to, removed
from, and moved between folders by drag and drop.

---

## Goals

- Let users reduce grid clutter by grouping related apps into a single cell
- Keep the creation gesture natural — drag one app onto another to form a folder
- Allow folders to be named for easy identification
- Make it equally easy to dissolve a folder and return apps to the grid

---

## UX Specification

### Folder creation

Drag any app tile on top of another app tile. When the ghost hovers over the target for long enough
(dwell threshold, ~600 ms) or the user releases, the two apps merge into a new unnamed folder in
that cell.

### Folder tile (collapsed state)

- Displays a 2×2 (or 3×3 if more than four apps) icon collage of the first apps in the folder.
- A name label below the tile (same position as a regular app label); empty until the user sets one.
- Tap → expand the folder.

### Folder expansion

- The folder expands **in place** as a floating overlay panel anchored to the cell, showing all
  contained apps in a small grid.
- A **folder name** text field sits at the top of the expanded panel; tapping it opens the keyboard
  for editing.
- Tapping any app inside launches it and collapses the folder.
- Tapping outside the panel collapses it without launching anything.

### Adding apps to a folder

Drag any grid app onto a collapsed folder tile — it is appended to the folder.

### Removing apps from a folder

Long-press an app inside the expanded panel to start a drag; drop it onto any grid cell outside the
folder to move it there. If the folder becomes empty it is automatically dissolved.

### Folder dissolution

When a folder reaches zero apps it is removed and the cell becomes empty. A one-app folder is
allowed (the user may create a folder by mistake and should be able to remove the second app without
a separate "dissolve" action).

### Drag and drop into a folder from the grid

Dropping a grid app onto any folder tile (collapsed or expanded) appends the app to that folder and
removes it from its original cell.

---

## Decisions

- [ ] **Expansion animation** — whether the folder expands with a scale+fade from the cell or slides
  up as a small bottom-anchored panel.
- [ ] **Max folder capacity** — whether a hard cap exists (e.g. 16 apps) or the panel scrolls
  without limit.
- [ ] **Folder in bar** — whether folders are allowed in the quick-access bar (EPIC-003); likely
  no for v1.
- [ ] **Folder persistence** — folder structure stored as part of the `Grid` model (EPIC-007);
  exact data shape TBD.
- [ ] **Folder on AAOS system bars** — out of scope; system bar slots hold single apps only.

---

## Related

- EPIC-001 App Grid (cell model, layout)
- EPIC-002 App Reordering (drag-and-drop gestures)
- EPIC-007 Data Persistence (folder model serialisation)
- NFR Performance & Responsiveness (folder open/close animation must hit frame budget)
