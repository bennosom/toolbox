# Compose Conventions

- Every composable that takes a ViewModel as parameter should use an internal dump composable (
  without ViewModel reference, no state ownership).
- Every dump composable must have a `@Preview` function at the **bottom of the same file**, covering
  all representative states.
- Extract **reusable composables and `Modifier` extensions** whenever the same UI pattern appears in
  more than one place. Name them after their purpose, not their location.
