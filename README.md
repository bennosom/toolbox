# toolbox (Android multi‑module)

An Android multi‑module project using Kotlin, Jetpack Compose, and the Android Gradle Plugin. It comprises:
- launcher: a Compose-based launcher/home-screen style app showcasing adaptive layouts.
- core: a shared Android library with utilities used by apps (e.g., logging, app launching helpers).
- devicetool: an internal/debug app that surfaces device information and basic UI demos.

This README documents the stack, requirements, setup, common commands, tests, structure, and licensing notes for contributors and CI.

## Stack and Tooling
- Language: Kotlin
- Build system: Gradle (Kotlin DSL) via the provided Gradle Wrapper
- Android Gradle Plugin (AGP): 8.11.1 (version catalog)
- Kotlin: 2.0.21 (version catalog)
- Jetpack Compose:
  - org.jetbrains.kotlin.plugin.compose plugin
  - AndroidX Compose BOM: 2024.09.00
  - Adaptive layouts: androidx.compose.material3.adaptive
- Min/Target/Compile SDKs:
  - minSdk = 31 (launcher, core, devicetool)
  - targetSdk = 36 (launcher, devicetool)
  - compileSdk = 36 (launcher, core, devicetool)
- Java toolchain: Java 11 (source/targetCompatibility = 11; jvmTarget = 11)
- Repositories: google, mavenCentral (configured in settings.gradle.kts)
- Version catalog: gradle/libs.versions.toml controls dependency and plugin versions

## Modules and Entry Points
- :launcher (Android application)
  - Namespace: io.engst.launcher
  - Entry point Activity: io.engst.launcher.ui.LauncherActivity
  - Depends on :core
  - Compose UI grid (AppGrid) with screen/adaptive helpers (ScreenInfo)
- :core (Android library)
  - Namespace: io.engst.core
  - Utilities such as Logger.kt (wrapping android.util.Log) and app launching helpers
- :devicetool (Android application; internal/debug)
  - Namespace: io.engst.devicetool
  - Entry point Activity: io.engst.devicetool.DeviceToolInternalActivity
  - Depends on :core and uses Compose for UI

## Requirements
- Android Studio (Giraffe+ recommended; latest Arctic/Koala compatible with AGP 8.11)
- Java 11 toolchain (Android Studio installs one; CLI builds must use JDK 11)
- Android SDK Platform 36 and latest Platform Tools
- Internet access for Gradle dependency resolution (google and mavenCentral)

Environment prerequisites:
- ANDROID_HOME or ANDROID_SDK_ROOT environment variable, or a local.properties with sdk.dir=/path/to/Android/sdk
- For CLI builds without Android Studio, ensure the JDK 11 is on PATH or set org.gradle.java.home in gradle.properties

## Setup
1. Clone the repository.
2. Ensure Android SDK 36 is installed (via Android Studio SDK Manager or sdkmanager).
3. Provide SDK path either via environment variable or a local.properties file at repo root:
   sdk.dir=/absolute/path/to/Android/sdk
4. Open the project in Android Studio and let Gradle sync, or use the Gradle Wrapper from the terminal.

## Build and Run
Common Gradle wrapper commands:
- Build all modules (no tests):
  ./gradlew assemble
- Assemble a specific app:
  ./gradlew :launcher:assembleDebug
  ./gradlew :devicetool:assembleDebug
- Install to a connected device/emulator:
  ./gradlew :launcher:installDebug
  ./gradlew :devicetool:installDebug
- Clean builds:
  ./gradlew clean

Notes:
- compileSdk/targetSdk 36 requires a recent local Android SDK; install via SDK Manager if build fails early on missing platforms.
- The project uses the version catalog; upgrade dependencies in gradle/libs.versions.toml rather than in module files.

## Scripts/Tasks
There are no custom shell scripts in the repo; use Gradle tasks via the wrapper.
Examples:
- List tasks: ./gradlew tasks
- Lint/format tooling is not included by default. TODO: Add ktlint/detekt if desired and document here.

## Tests
This project includes host-side JVM tests and instrumented Android tests.
- Run all JVM unit tests across all modules:
  ./gradlew test
- Module-scoped JVM tests:
  ./gradlew :core:testDebugUnitTest
  ./gradlew :launcher:testDebugUnitTest
- Run a single test class or method (examples that exist in repo):
  ./gradlew :core:test --tests 'io.engst.core.ExampleUnitTest'
  ./gradlew :launcher:test --tests 'io.engst.launcher.ReorderTest'
- Instrumented tests (require emulator/device):
  ./gradlew connectedAndroidTest

Testing guidance:
- Keep unit tests pure JVM where possible. Avoid using android.* APIs in src/test; those require Robolectric or instrumentation.
- core/src/main/java/io/engst/core/Logger.kt wraps android.util.Log and is not available in plain JVM tests. If you need to test logging logic, either:
  - Move logic behind a pure interface and mock in JVM tests, or
  - Use Robolectric (add dependency and configuration), or
  - Test call sites indirectly without touching android.util.* in the JVM layer.
- For Compose/adaptive layout logic (e.g., ScreenInfo), prefer extracting pure functions for unit
  tests.

## Environment Variables
- ANDROID_HOME or ANDROID_SDK_ROOT: path to the Android SDK (if local.properties is not present)
- Optional: org.gradle.jvmargs, org.gradle.java.home can be set in gradle.properties for CI tuning

## Project Structure
Root
├─ settings.gradle.kts (includes :devicetool, :core, :launcher; configures repositories)
├─ build.gradle.kts (root plugin aliases via version catalog)
├─ gradle/libs.versions.toml (plugin and dependency versions)
├─ launcher/
│  ├─ build.gradle.kts
│ ├─ src/main/java/io/engst/launcher/ui/LauncherActivity.kt (entry point)
│ ├─ src/main/java/io/engst/launcher/ui/shared/ScreenInfo.kt
│ ├─ src/main/java/io/engst/launcher/ui/grid/ (AppGrid, AppTile, Reorder, etc.)
│ └─ src/test/java/io/engst/launcher/ReorderTest.kt
├─ core/
│  ├─ build.gradle.kts
│  ├─ src/main/java/io/engst/core/Logger.kt
│ ├─ src/main/java/io/engst/core/apps/LaunchActivity.kt
│  └─ src/test/java/io/engst/core/ExampleUnitTest.kt
├─ devicetool/
│  ├─ build.gradle.kts
│ ├─ src/main/java/io/engst/devicetool/DeviceToolInternalActivity.kt (entry point)
│ └─ src/main/java/io/engst/devicetool/DeviceInfo.kt
├─ gradlew / gradlew.bat (Gradle Wrapper)
├─ gradle/wrapper/
└─ gradle.properties

## Known Pitfalls and Tips
- If you see NoSuchMethodError/NoClassDefFoundError in JVM tests, ensure no android.* APIs are used in src/test; move such tests to androidTest or use Robolectric.
- Adaptive UI logic should be unit-tested via extracted pure functions where feasible.
- minSdk 31 implies Android 12+ behavior for runtime permissions and APIs; avoid deprecated Display metrics; prefer WindowManager/WindowMetrics.

## License
TODO: Add a LICENSE file and specify the project's license here.

## Maintainers / Contributing
- Contributions are welcome. Please open an issue or PR.
- TODO: Add CODE_OF_CONDUCT.md and CONTRIBUTING.md if needed and link them here.
