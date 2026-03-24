# Error Handling

- Use a **central error-handling strategy**: all `Result`/`Either` types are handled at the
  ViewModel boundary; raw exceptions must not propagate into the UI layer.
- Every recoverable error produces a user-visible `Effect` (e.g. a snackbar message) and a log entry.
- Non-recoverable errors are caught at the top-level coroutine scope and reported via the logger
  before rethrowing or triggering a safe fallback.
