# Update docs first, then tests, then production code

Add tests to verify each code path. Use robolectric to verify Compose Gesture detection.

Layout:
- the grid should fill the available space (both directions)
- the quickbar cell size should match the grid cell size

Update grid's touch gesture detection (high to low priority):
- press and release of non-empty cell (release within same cell bounds)  should launch the app
- longpress of non-empty cell should show the app context menu
- when the context menu has been appeared and the pointer is still down and starts to move then close the app context menu again and start app drag
- longpress on screen root should show the settings menu
- when a app is pressed show a ripple effect

Update app drag:
- when app drag starts, then hide the original app in the grid and show a identical looking drag shadow
- whenever the drag shadow is hovering an empty cell then show a drop-indication (white pusling dot)
- when the user drops the drag shadow on a empty cell then move the app to the the dropped cell. 

