# Repository Guidelines

## Project Structure & Module Organization

- `launcher/`: Main Android launcher app using Jetpack Compose; UI logic in `ui/`, data layer in
  `data/`, shared models in `model/`.
- `core/`: Shared Android library exposing logging and activity-launch helpers consumed by all apps.
- `devicetool/`: Internal diagnostics app showcasing inset handling, wallpaper helpers, and device
  info readouts.
- Tests live beside their modules (`src/test/` for JVM, `src/androidTest/` for instrumented). Assets
  and Compose resources live under each module’s `src/main/res/`.
- Build and dependency metadata is centralized in `gradle/libs.versions.toml`, with module
  registration in `settings.gradle.kts`.

## Build, Test, and Development Commands

- `./gradlew assemble`: Build all modules without running tests.
- `./gradlew :launcher:assembleDebug` / `:devicetool:assembleDebug`: Produce installable APKs for
  the respective apps.
- `./gradlew :launcher:installDebug`: Build and deploy the launcher debug APK to a connected device
  or emulator.
- `adb shell am start -n io.engst.launcher/.ui.LauncherActivity`: Launch the installed launcher
  activity on a connected device or emulator.
- `./gradlew test`: Execute JVM unit tests across every module.
- `./gradlew :launcher:testDebugUnitTest`: Run launcher-specific unit tests (e.g., grid reordering
  logic).
- `./gradlew connectedAndroidTest`: Launch instrumented tests on a connected device or emulator.
- `./gradlew :launcher:test --tests 'io.engst.launcher.ReorderTest'`: Run a single test class or
  method.

## Coding Style & Naming Conventions

- Kotlin + Compose code uses four-space indentation and trailing commas only where Kotlin style
  allows.
- Follow descriptive naming: camelCase for functions/variables, PascalCase for classes, UPPER_CASE
  for constants.
- Keep composables focused, avoid needless comments, and document intent for non-obvious logic only.
- Prefer pure helpers in `core/` or `launcher/data/` for testability; avoid Android APIs in
  `src/test/` JVM sources.

## Testing Guidelines

- JVM unit tests use JUnit 4 (`test` source set). Instrumented tests run under AndroidJUnitRunner (
  `androidTest`).
- Name tests descriptively (e.g., `SomeFeatureTest`). Group behaviour assertions per method using
  `@Test`.

## Commit & Pull Request Guidelines

- Write commits in present tense (“Add drag-and-drop helper”) with focused scopes; group unrelated
  changes into separate commits.
- PRs should describe intent, affected modules, and testing performed. Link issues or tasks when
  available, and attach UI screenshots/GIFs for visual changes.
- Ensure Gradle builds succeed and new tests accompany behaviour or contract changes.
- Run `./gradlew test` before submitting; add instrumented coverage when code depends on Android
  framework types.

## Agent Notes

- Respect the existing module boundaries. Shared utilities belong in `core/`; launcher-only logic
  stays under `launcher/`.
- Before large refactors, sync with maintainers and capture architecture decisions in `README.md` or
  follow-up docs.
