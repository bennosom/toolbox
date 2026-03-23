# Technical Design Constraints

These constraints apply to every story in this backlog. Each story's acceptance criteria are
written on top of these; reviewers and implementers must treat any violation as a blocking defect.

---

## Dependency injection

- Use **Koin** exclusively for dependency injection.
- All modules are declared with the Koin DSL (`module { }`, `single { }`, `factory { }`, `viewModel { }`).
- No `object`-level singletons or manual service locators outside Koin.

---

## Architecture

- Follow the **MVI (Model–View–Intent)** pattern in every feature.
  - `State` — immutable data class representing the full UI state.
  - `Intent` — sealed class / interface enumerating every user action or system event.
  - `Effect` — sealed class for one-shot side-effects (navigation, toasts, etc.).
- Enforce **unidirectional data flow**: `View → Intent → ViewModel → State/Effect → View`. No
  direct ViewModel → View callbacks; effects are emitted through a `SharedFlow`.
- Split ViewModels by **feature with high cohesion**: one ViewModel per tightly scoped feature
  screen or component. Do not aggregate unrelated concerns into a single ViewModel.

---

## File size

- **No file may exceed 300 lines.** If a file approaches the limit, split it along natural
  boundaries (e.g. separate state definitions, intent definitions, extension functions, previews).

---

## Compose

- Every composable must have a `@Preview` function at the **bottom of the same file**,
  covering representative states (empty, loaded, error where applicable).
- Extract **reusable composables and `Modifier` extensions** whenever the same UI pattern appears
  in more than one place. Name them after their purpose, not their location.

---

## Code quality

- Apply the **SOLID principles**:
  - *Single Responsibility* — each class/function has exactly one reason to change.
  - *Open/Closed* — extend behaviour through new types or parameters, not by editing existing logic.
  - *Liskov Substitution* — subtypes are substitutable for their base type without behavioural surprises.
  - *Interface Segregation* — keep interfaces narrow; callers must not depend on methods they do not use.
  - *Dependency Inversion* — high-level modules depend on abstractions, not concrete implementations.
- Use **meaningful, fully spelled-out names** — no abbreviations, no single-letter variables outside
  trivial loop indices. Names must reveal intent.
- Do **not** treat the existing implementation as the source of truth. Refactor structure and quality
  whenever a cleaner design is available; incremental improvement is expected, not optional.

---

## Error handling

- Use a **central error-handling strategy**: all `Result`/`Either` types are handled at the
  ViewModel boundary; raw exceptions must not propagate into the UI layer.
- Every recoverable error produces a user-visible `Effect` (e.g. a snackbar message) and a log entry.
- Non-recoverable errors are caught at the top-level coroutine scope and reported via the logger
  before rethrowing or triggering a safe fallback.

---

## Logging

- Use a **single, central `Logger`** (thin wrapper; no direct `android.util.Log` calls in feature
  code).
- Log entries must include a consistent tag derived from the calling class name.
- Add trace-level log calls at every key event: drag start/end, drop accepted/rejected, state
  transition, repository read/write, and any error path.

---

## Testing

- Write **unit tests for every acceptance criterion** in the story.
- Enforce **100 % branch coverage** on ViewModel, domain, and `Grid` extension logic; the CI gate
  must fail if coverage drops below 100 % on these layers.
- Use **Robolectric** for all Compose UI tests (`@RunWith(RobolectricTestRunner::class)` +
  `createComposeRule()`).
- **No instrumented Android tests** — the build must not require a connected device or emulator to
  reach full coverage.
