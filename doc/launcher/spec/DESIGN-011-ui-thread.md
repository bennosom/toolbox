# UI Thread

The main / UI thread must be reserved exclusively for drawing and event dispatch.

| Rule | Detail |
|------|--------|
| No blocking I/O on main | All file, database, and `SharedPreferences` access on `Dispatchers.IO` |
| No heavy computation on main | Layout migration, fuzzy search indexing, bitmap decode — all off main thread |
| Frame budget | Every frame must complete within 16 ms (60 fps) / 11 ms (90 fps) where the display supports it |
| Strict mode | `StrictMode.ThreadPolicy` with `detectAll()` enabled in debug builds |
