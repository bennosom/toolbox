package io.engst.launcher.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.engst.core.launchOnDisplay
import io.engst.launcher.App
import io.engst.launcher.LauncherStateHolder
import io.engst.launcher.LauncherStateHolderNoop
import java.text.DateFormat
import java.util.Date

fun Context.findActivity(): Activity? =
    when (this) {
      is Activity -> this
      is ContextWrapper -> baseContext.findActivity()
      else -> null
    }

enum class AppSortBy {
  PackageName,
  Label,
  TargetSdk,
  LastUpdated
}

data class AppDetails(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val minSdk: Int?,
    val targetSdk: Int?,
    val lastUpdated: Long,
    val installTime: Long,
    val isSystemApp: Boolean
)

@Composable
private fun rememberAppDetails(app: App): AppDetails {
  val context = LocalContext.current
  val pm = context.packageManager
  val pkg = app.componentName.packageName
  // compute synchronously once; acceptable for small lists
  val pi = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull()
  val ai = pi?.applicationInfo
  val activityInfo =
      runCatching { context.packageManager.getActivityInfo(app.componentName, 0) }.getOrNull()
  val minSdk = activityInfo?.applicationInfo?.minSdkVersion ?: ai?.minSdkVersion
  val targetSdk = ai?.targetSdkVersion
  val isSystem = (ai?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
  val isUpdatedSystem = (ai?.flags ?: 0) and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
  return remember(app.id) {
    AppDetails(
        packageName = pkg,
        versionName = pi?.versionName,
        versionCode = pi?.longVersionCode ?: 0L,
        minSdk = minSdk,
        targetSdk = targetSdk,
        lastUpdated = pi?.lastUpdateTime ?: 0L,
        installTime = pi?.firstInstallTime ?: 0L,
        isSystemApp = isSystem || isUpdatedSystem)
  }
}

@Preview
@Composable
fun AppManager(
    stateHolder: LauncherStateHolder = LauncherStateHolderNoop(),
    onBack: () -> Unit = {}
) {
  val context = LocalContext.current
  val apps by stateHolder.apps.collectAsStateWithLifecycle()
  val isLoading by stateHolder.isLoading.collectAsStateWithLifecycle()
  var sortBy by remember { mutableStateOf(AppSortBy.Label) }
  var sortExpanded by remember { mutableStateOf(false) }

  val sorted =
      remember(apps, sortBy) {
        val withDetails = apps.map { it to sortKey(context, it, sortBy) }
        when (sortBy) {
          AppSortBy.PackageName,
          AppSortBy.Label -> withDetails.sortedBy { it.second as String }
          AppSortBy.TargetSdk -> withDetails.sortedBy { it.second as Int? ?: Int.MIN_VALUE }
          AppSortBy.LastUpdated -> withDetails.sortedByDescending { it.second as Long }
        }.map { it.first }
      }

  Box(Modifier.fillMaxSize().statusBarsPadding()) {
    if (isLoading) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      LazyColumn(
          Modifier.padding(horizontal = 6.dp),
          contentPadding = WindowInsets.navigationBars.asPaddingValues(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        stickyHeader {
          Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, null) }
            Text("App Manager", modifier = Modifier.align(Alignment.Center))
          }
          Surface(
              Modifier.padding(start = 6.dp, top = 6.dp, end = 6.dp),
              shape = MaterialTheme.shapes.extraLarge,
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
              shadowElevation = 6.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Spacer(Modifier.size(6.dp))
                      Text("Sort by")
                      OutlinedButton(onClick = { sortExpanded = true }) { Text(sortBy.label()) }
                      DropdownMenu(
                          expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            AppSortBy.entries.forEach { option ->
                              DropdownMenuItem(
                                  text = { Text(option.label()) },
                                  onClick = {
                                    sortBy = option
                                    sortExpanded = false
                                  })
                            }
                          }
                    }
              }
        }
        items(sorted, key = { it.id }) { app ->
          val details = rememberAppDetails(app)
          val context = LocalContext.current
          val removalRequestLauncher =
              rememberLauncherForActivityResult(
                  contract = ActivityResultContracts.StartActivityForResult()) { result ->
                    val message =
                        when (result.resultCode) {
                          Activity.RESULT_OK ->
                              "${app.componentName.packageName} was successfully removed"
                          else -> "Uninstall of ${app.componentName.packageName} failed"
                        }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                  }

          AppListRow(
              app = app,
              details = details,
              onClick = { context.launchOnDisplay(app.componentName) },
              onSettings = {
                val uri = "package:${details.packageName}".toUri()
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri).apply {
                      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(intent)
              },
              onRemove = {
                removalRequestLauncher.launch(
                    Intent(Intent.ACTION_DELETE).apply {
                      data = "package:${app.componentName.packageName}".toUri()
                      putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    })
              })
        }
      }
    }
  }
}

@Composable
private fun AppListRow(
    app: App,
    details: AppDetails,
    onClick: () -> Unit,
    onSettings: () -> Unit,
    onRemove: () -> Unit
) {
  val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
  Surface(onClick = onClick, shape = MaterialTheme.shapes.medium) {
    Row(
        Modifier.padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AppIcon(app = app, size = 56.dp)
      Column(Modifier.weight(1f)) {
        Spacer(Modifier.size(12.dp))
        Text(app.name, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.size(6.dp))
        Text(if (details.isSystemApp) "Pre-installed system app" else "Installed by user")
        Text(details.packageName)
        Text("Version ${details.versionName} (${details.versionCode})")
        Text("Android SDK ${details.targetSdk} (${details.minSdk})")
        Text("Installed ${df.format(Date(details.installTime))}")
        Text("Last updated ${df.format(Date(details.lastUpdated))}")
        Spacer(Modifier.size(6.dp))
        Row(Modifier.fillMaxWidth()) {
          TextButton(onClick = onSettings) { Text("Settings") }
          TextButton(onClick = onRemove) {
            Text("Uninstall", color = MaterialTheme.colorScheme.error)
          }
        }
      }
    }
  }
}

private fun AppSortBy.label(): String =
    when (this) {
      AppSortBy.PackageName -> "Package"
      AppSortBy.Label -> "Label"
      AppSortBy.TargetSdk -> "Target SDK"
      AppSortBy.LastUpdated -> "Last Updated"
    }

private fun sortKey(context: Context, app: App, sortBy: AppSortBy): Any? {
  return when (sortBy) {
    AppSortBy.PackageName -> app.componentName.packageName
    AppSortBy.Label -> app.name
    AppSortBy.TargetSdk -> {
      val info =
          runCatching { context.packageManager.getActivityInfo(app.componentName, 0) }.getOrNull()
      info?.applicationInfo?.targetSdkVersion
    }
    AppSortBy.LastUpdated -> {
      val pi =
          runCatching { context.packageManager.getPackageInfo(app.componentName.packageName, 0) }
              .getOrNull()
      pi?.lastUpdateTime ?: 0L
    }
  }
}
