# Logging

- Use a **single, central `Logger`** (thin wrapper; no direct `android.util.Log` calls in feature
  code).
- Log entries must include a consistent tag derived from the calling class name.
- Add trace-level log calls at every key event: drag start/end, drop accepted/rejected, state
  transition, repository read/write, and any error path.
