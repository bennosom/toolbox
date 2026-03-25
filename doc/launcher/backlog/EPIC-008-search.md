# EPIC-008 — Search / Quick Find

## Summary

A quick-access search experience triggered by swiping up on the home screen. A bottom sheet slides
up containing a search input field; results are highlighted directly in the grid in real time as the
user types. Previous / next buttons let the user step through multiple matches without leaving the
home screen.

---

## Goals

- Give the user instant access to any installed app by name without leaving the home screen
- Keep the user on the home screen — results appear in place inside the grid, not a separate list
- Provide prev/next navigation to step through matches when there are more than one
- Dismiss with a downward swipe or back gesture, leaving the grid in its normal state

---

## UX Specification

### Trigger

- **Swipe up** anywhere on the home screen surface opens the bottom sheet (see EPIC-001 Gesture map).
- Tapping the search field in any other future entry point (e.g. quick-settings tile) should also
  open this sheet.

### Bottom Sheet

| Element | Behaviour |
|---------|-----------|
| Search input | Auto-focused on open; keyboard raises immediately |
| Clear button (×) | Visible when input is non-empty; clears text and resets highlights |
| Prev (‹) button | Navigates to the previous matching tile; wraps around |
| Next (›) button | Navigates to the next matching tile; wraps around |
| Match count badge | "2 / 7" style indicator between prev/next buttons; hidden when no query |
| Dismiss | Swipe down on sheet, back gesture, or tapping outside the sheet |

### Grid Highlighting

- All tiles whose label **fuzzy-matches** the current query are highlighted (e.g. accent border or
  tinted overlay — exact visual TBD).
- The **active match** (the one prev/next is positioned on) is additionally emphasised
  (e.g. pulsing ring or scale pop).
- The pager automatically scrolls to the page containing the active match **when the user
  navigates via Prev / Next**. Manual paging (swipe) is never overridden by auto-scroll.
- When the query is cleared or the sheet is dismissed, all highlights are removed and the grid
  returns to its normal appearance.

### Matching algorithm

- Case-insensitive substring match on the app label is the baseline.
- Fuzzy matching (same `fuzzyMatchRanges` approach used in `AppRow`) may be applied to tolerate
  minor typos; this is a quality-of-life improvement and not required for v1.
- Results are ordered: **page order → row → column** (reading order).

---

## Decisions

- [x] **Highlight style** — **scale pop + border ring**. All matched tiles receive an accent border.
  The active match (the one prev/next is positioned on) additionally scales to 1.1× with a brief
  spring animation. Non-matching tiles dim slightly. This clearly separates "has a match" from
  "is the current match" without a separate overlay layer.
- [x] **Sheet height** — 28 % of screen height as the default peek height. This leaves roughly
  70 % of the grid visible so the user can see highlights while typing.
- [x] **Keyboard behaviour on dismiss** — The software keyboard closes together with the sheet in a
  single dismiss action (swipe-down or back). No two-step dismiss.
- [x] **Active match cursor on re-filter** — When the user types a new character and the match list
  changes, the active match index is preserved if the previously active app is still in the new
  result set. If it is no longer a match, the cursor resets to match 1.
- [x] **Pager auto-scroll scope** — Auto-scroll to the active match's page fires **only** when
  triggered by the Prev / Next buttons. If the user manually swipes to a different page while search
  is open, the active match stays highlighted but the pager does not jump back. Auto-scroll resumes
  on the next Prev / Next press.

---

## Related

- EPIC-001 App Grid (gesture trigger; grid highlight rendering)
- NFR Performance & Responsiveness (search must filter on every keystroke without UI lag)
