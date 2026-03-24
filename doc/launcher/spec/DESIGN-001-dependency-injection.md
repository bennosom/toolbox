# Dependency Injection

Use **Koin** exclusively for dependency injection.

- All modules are declared with the Koin DSL (`module { }`, `single { }`, `factory { }`, `viewModel { }`).
- No `object`-level singletons or manual service locators outside Koin.
