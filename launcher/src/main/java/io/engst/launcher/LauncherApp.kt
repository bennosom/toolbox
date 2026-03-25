package io.engst.launcher

import android.app.Application
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import io.engst.core.Logging
import io.engst.core.scopedLogger
import io.engst.launcher.data.AppsRepository
import io.engst.launcher.data.AppsRepositoryImpl
import io.engst.launcher.data.proto.GridDataProto
import io.engst.launcher.data.store.GridDataProtoSerializer
import io.engst.launcher.ui.grid.GridEffectHandler
import io.engst.launcher.ui.grid.GridEffectHandlerImpl
import io.engst.launcher.ui.grid.GridViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File

val rootModule = module {
    single<DataStore<GridDataProto>> {
        DataStoreFactory.create(
            serializer = GridDataProtoSerializer,
            produceFile = { File(androidContext().filesDir, "grid_data.pb") },
        )
    }
    single<AppsRepository> { AppsRepositoryImpl(androidContext(), get()) }
    single<GridEffectHandler> { GridEffectHandlerImpl(androidContext()) }
    viewModel { GridViewModel(get(), get()) }
}

class LauncherApp : Application(), Logging by scopedLogger("LauncherApp") {

  private var lastConfiguration: String = ""

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
