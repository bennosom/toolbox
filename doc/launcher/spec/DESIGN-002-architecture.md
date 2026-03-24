# Architecture

Follow the **MVI (Model–View–Intent)** pattern in every feature.

- `State` — immutable data class representing the full UI state.
- `Intent` — sealed class / interface enumerating every user action or system event.
- `Effect` — sealed class for one-shot side-effects (navigation, toasts, etc.).

Enforce **unidirectional data flow**: `View → Intent → ViewModel → State/Effect → View`. No
direct ViewModel → View callbacks; effects are emitted through a `SharedFlow`.

Split ViewModels by **feature with high cohesion**: one ViewModel per tightly scoped feature
screen or component. Do not aggregate unrelated concerns into a single ViewModel.
