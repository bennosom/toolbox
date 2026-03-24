# Testing

- Write **unit tests for every acceptance criterion** in the story.
- Enforce **100 % branch coverage** on ViewModel, domain, and `Grid` extension logic; the CI gate
  must fail if coverage drops below 100 % on these layers.
- Use **Robolectric** for all Compose UI tests (`@RunWith(RobolectricTestRunner::class)` +
  `createComposeRule()`).
- **No instrumented Android tests** — the build must not require a connected device or emulator to
  reach full coverage.
