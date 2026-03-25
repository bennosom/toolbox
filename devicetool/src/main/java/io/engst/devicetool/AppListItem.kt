package io.engst.devicetool

import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.DateFormat
import java.util.Date

@Composable
fun AppListItem(
    app: InstalledApp,
    onLaunch: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
  val detailsText =
      remember(app, dateFormat) {
        buildString {
          appendLine(if (app.isSystemApp) "Pre-installed system app" else "Installed by user")
          appendLine(app.packageName)
          appendLine("Version ${app.versionName} (${app.versionCode})")
          appendLine("Android SDK ${app.targetSdk} (${app.minSdk})")
          appendLine("Installed ${dateFormat.format(Date(app.installedTimeMillis))}")
          append("Last updated ${dateFormat.format(Date(app.lastUpdatedTimeMillis))}")
        }
      }

  Surface(
      modifier = modifier,
      onClick = onLaunch,
      shape = MaterialTheme.shapes.medium,
      tonalElevation = 1.dp,
  ) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AndroidView(
          factory = {
            ImageView(context).apply {
              adjustViewBounds = true
              scaleType = ImageView.ScaleType.CENTER_CROP
            }
          },
          modifier = Modifier.size(48.dp),
          update = { imageView -> imageView.setImageDrawable(app.icon) },
      )
      Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
        Text(app.label, style = MaterialTheme.typography.titleMedium)
        Text(detailsText, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Button(onClick = onDetails) { Text("Settings") }
          if (!app.isSystemApp) {
            Button(
                onClick = onRemove,
                colors =
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
              Text("Uninstall")
            }
          }
        }
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun AppListItemPreview() {
  val sample =
      InstalledApp(
          packageName = "io.engst.sample",
          label = "Sample App",
          versionName = "1.0",
          versionCode = 1,
          targetSdk = 36,
          minSdk = 31,
          installedTimeMillis = 0L,
          lastUpdatedTimeMillis = 0L,
          isSystemApp = false,
          icon = null,
      )

  MaterialTheme {
    AppListItem(
        app = sample,
        onLaunch = {},
        onDetails = {},
        onRemove = {},
    )
  }
}
