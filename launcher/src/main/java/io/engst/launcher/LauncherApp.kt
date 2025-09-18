package io.engst.launcher

import android.app.Application
import android.content.res.Configuration
import io.engst.core.Logging
import io.engst.core.scopedLogger
import io.engst.launcher.data.AppsRepository
import io.engst.launcher.data.AppsRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

val rootModule = module { single<AppsRepository> { AppsRepositoryImpl(androidContext()) } }

class LauncherApp : Application(), Logging by scopedLogger("LauncherApp") {

  companion object {
    lateinit var lastConfiguration: String
  }

  override fun onCreate() {
    super.onCreate()
    lastConfiguration = resources.configuration.toString()
    startKoin {
      androidContext(this@LauncherApp)
      modules(rootModule)
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    logDebug {
      """onConfigurationChanged:
        $lastConfiguration
        $newConfig
      """
    }
    lastConfiguration = newConfig.toString()
    super.onConfigurationChanged(newConfig)
  }
}
