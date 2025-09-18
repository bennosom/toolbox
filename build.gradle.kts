// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
  tasks.register("resolveAllDependencies") {
    group = "dependency"
    description = "Resolves and downloads all dependencies for all configurations in all projects."

    doLast {
      configurations
        .filter { it.isCanBeResolved }
        .forEach { config ->
          println("🔍 Resolving ${config.name} in ${project.name}")
          try {
            config.resolve()
          } catch (e: Exception) {
            println("❌ Failed to resolve ${config.name} in ${project.name}: ${e.message}")
          }
        }
    }
  }
}