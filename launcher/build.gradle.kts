plugins {
   alias(libs.plugins.android.application)
   alias(libs.plugins.kotlin.android)
   alias(libs.plugins.kotlin.compose)
   alias(libs.plugins.protobuf)
}

    android {
    namespace = "io.engst.launcher"
    compileSdk = 36

    defaultConfig {
      applicationId = "io.engst.launcher"
    minSdk = 31
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
       release {
           isMinifyEnabled = false
           proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
       }
    }
       compileOptions {
          sourceCompatibility = JavaVersion.VERSION_11
          targetCompatibility = JavaVersion.VERSION_11
       }
       kotlinOptions {
          jvmTarget = "11"
       }
       buildFeatures {
          compose = true
       }
       testOptions {
          unitTests {
             isIncludeAndroidResources = true
          }
       }
    }

protobuf {
   protoc {
      artifact = libs.protobuf.protoc.get().toString()
   }
   generateProtoTasks {
      all().forEach { task ->
         task.builtins {
            register("java") {
               option("lite")
            }
         }
      }
   }
}

  dependencies {
     implementation(project(":core"))

     implementation(libs.androidx.core.ktx)
     implementation(libs.androidx.lifecycle.runtime.ktx)
     implementation(libs.androidx.activity.compose)
     implementation(platform(libs.androidx.compose.bom))
     implementation(libs.androidx.compose.ui)
     implementation(libs.androidx.compose.ui.graphics)
     implementation(libs.androidx.ui.tooling.preview)
     implementation(libs.androidx.compose.material3)
     implementation(libs.androidx.compose.adaptive)
     implementation(libs.androidx.compose.foundation)
     implementation(libs.androidx.compose.material.iconsExtended)
     implementation(libs.koin.android)
     implementation(libs.koin.androidx.compose)
     implementation(libs.androidx.lifecycle.viewmodel.ktx)
     implementation(libs.androidx.datastore)
     implementation(libs.protobuf.javalite)

     testImplementation(libs.junit)
     testImplementation(libs.robolectric)
     testImplementation(libs.kotlinx.coroutines.test)
     testImplementation(libs.koin.test.junit4)
     testImplementation(libs.androidx.test.core)
     testImplementation(platform(libs.androidx.compose.bom))
     testImplementation(libs.androidx.compose.ui.test.junit4)
     debugImplementation(libs.androidx.ui.tooling)
     debugImplementation(libs.androidx.compose.ui.test.manifest)
  }