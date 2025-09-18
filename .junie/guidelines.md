Project: toolbox (Android multi-module)

This document captures project-specific build, test, and development guidelines to accelerate onboarding and reduce friction for future changes. It focuses on the reality of this repo (Gradle config, modules, SDK levels, dependencies) and verified commands.

Modules
- launcher: Android app (Jetpack Compose, Adaptive layouts). Depends on :core.
- core: Android library with shared utilities.
- devicetool: Internal/debug Android app for device info.

Build and Configuration
- JDK: Java 11
  - Configured via compileOptions/kotlinOptions in each module (sourceCompatibility/targetCompatibility = 11; jvmTarget = 11).
- Kotlin: 2.0.21 (version catalog)
- Android Gradle Plugin (AGP): 8.11.1 (version catalog)
- Gradle: Wrapper provided (8.13 per output)
- SDK Levels
  - compileSdk = 36 (launcher, core)
  - targetSdk = 36 (launcher)
  - minSdk = 31 (launcher, core)
- Compose
  - Uses org.jetbrains.kotlin.plugin.compose plugin and the AndroidX Compose BOM (2024.09.00).
  - Adaptive layouts via androidx.compose.material3.adaptive.
- Repositories: google, mavenCentral (declared in settings.gradle.kts)
- Version catalog: gradle/libs.versions.toml controls dependency/plugin versions. Prefer updating here, not inline.
- Namespaces: io.engst.core, io.engst.launcher
- Local Android SDK path: local.properties (sdk.dir) is expected but should not be committed. CI should set ANDROID_HOME/ANDROID_SDK_ROOT or generate local.properties.

Verified Build Commands
- Build all modules (no tests):
  ./gradlew assemble
- Run JVM unit tests across all modules:
  ./gradlew test
  Note: This runs host-side unit tests only (src/test). Instrumented tests (src/androidTest) require an emulator/device and are out of scope for typical CI here.
- Module-scoped tests:
  ./gradlew :core:testDebugUnitTest
  ./gradlew :launcher:testDebugUnitTest
- Run a single test class or method (examples that exist in repo):
  ./gradlew :core:test --tests 'io.engst.core.ExampleUnitTest'
  ./gradlew :launcher:test --tests 'io.engst.launcher.ExampleUnitTest.addition_isCorrect'

Testing Guidance
- Keep unit tests pure JVM when possible. Avoid android.* APIs in src/test; they require Robolectric or instrumentation. In this repo:
  - core/src/main/java/io/engst/core/Logger.kt wraps android.util.Log, which is not available in plain JVM tests. If you need to test logging logic, either:
    - Move logic behind a pure interface and mock in JVM tests, or
    - Use Robolectric (add dependency and configure), or
    - Test call sites indirectly without touching android.util.* in the JVM layer.
- Compose UI and Window Adaptive logic should be tested via unit tests that do not instantiate Android runtime components. For logic embedded in Composables or adaptive computations, extract pure functions where feasible.
- Instrumented tests exist under src/androidTest in core and launcher but are not exercised in default CI. To run locally:
  ./gradlew connectedAndroidTest
  Requires a running emulator or USB device. Ensure SDK platform 36 and latest platform-tools are installed.

How to Add a New Unit Test (Demonstrated and Verified)
- Create a new Kotlin file under the module’s src/test/java path, e.g. launcher/src/test/java/io/engst/launcher/MyFeatureTest.kt.
- Example content:
  package io.engst.launcher
  import org.junit.Assert.assertEquals
  import org.junit.Test
  class MyFeatureTest {
    @Test fun addition_demo() { assertEquals(4, 2 + 2) }
  }
- Run just that test:
  ./gradlew :launcher:test --tests 'io.engst.launcher.MyFeatureTest'
- Or run all launcher JVM tests:
  ./gradlew :launcher:testDebugUnitTest

Notes From a Live Test Run
- Existing unit tests pass:
  - io.engst.core.ExampleUnitTest::addition_isCorrect
  - io.engst.launcher.ExampleUnitTest::addition_isCorrect
- A temporary demonstration test was created, executed, and then removed to keep the repo clean. Use the example above to reproduce as needed.

Development Practices and Code Style
- Language: Kotlin (Android)
- Style: Follow standard Kotlin style (Kotlin official). Prefer idiomatic constructs (extension functions, top-level functions where appropriate). Keep Android-specific APIs out of core business logic to enable JVM testing.
- Compose:
  - Use stable previews only for local tooling; avoid Preview-only code in production sources.
  - For adaptive UX (ScreenConfig), consider extracting calculation logic into pure functions to unit-test breakpoints/orientation rules without Compose dependencies.
- Logging (core/Logger.kt):
  - Wraps android.util.Log with lightweight helpers. Consider introducing an abstraction if you need to unit test logging behavior.
- API/ABI considerations:
  - minSdk 31 implies RuntimePermission/API behavior at or after Android 12; avoid using deprecated Display width/height properties (seen in devicetool) for future work—prefer WindowManager/WindowMetrics.

Common Pitfalls and Tips
- compileSdk/targetSdk 36 requires the latest Android SDK; ensure it’s installed locally or in CI, or Gradle sync/build will fail early.
- If you see NoSuchMethodError/NoClassDefFoundError in JVM tests, verify no android.* dependencies are leaking into src/test. Move tests to androidTest or add Robolectric.
- When adding dependencies, prefer updating gradle/libs.versions.toml and referencing via the version catalog in module build.gradle.kts files (libs.xyz names already defined).
- For Compose BOM upgrades, update composeBom in libs.versions.toml; do not set versions on compose artifacts individually when using BOM.

Quick Reference: Key Files
- settings.gradle.kts: module includes, repositories.
- build.gradle.kts (root): plugin aliases via version catalog.
- gradle/libs.versions.toml: versions for plugins and libraries.
- launcher/build.gradle.kts and core/build.gradle.kts: module android{} config.
- launcher/src/main/java/io/engst/launcher/ScreenConfig.kt: Adaptive layout logic (candidates for pure function extraction/testing).
- core/src/main/java/io/engst/core/Logger.kt: Android logging helpers.

CI Suggestions (if/when set up)
- Cache Gradle and Android SDK.
- Stages:
  - ./gradlew ktlintFormat detekt (if added later)
  - ./gradlew assemble
  - ./gradlew test
  - Optionally run connectedAndroidTest on an emulator matrix.

FAQ
- Q: How do I run only one module’s tests? A: ./gradlew :moduleName:testDebugUnitTest
- Q: How do I run a single test method? A: ./gradlew :moduleName:test --tests 'fully.qualified.ClassName.methodName'
- Q: Why do my JVM tests fail when using android.util.*? A: Those APIs aren’t on the plain JVM. Use Robolectric or move logic behind interfaces and test the pure parts.
