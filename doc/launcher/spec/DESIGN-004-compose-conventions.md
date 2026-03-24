# Compose Conventions

- Every composable must have a `@Preview` function at the **bottom of the same file**,
  covering representative states (empty, loaded, error where applicable).
- Extract **reusable composables and `Modifier` extensions** whenever the same UI pattern appears
  in more than one place. Name them after their purpose, not their location.
