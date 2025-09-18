package io.engst.launcher.ui.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App
import io.engst.launcher.ui.shared.AppIcon
import java.text.DateFormat
import java.util.Date

@Composable
fun AppListRow(
   app: App,
   searchQuery: String,
   onLaunch: () -> Unit,
   onDetails: () -> Unit,
   onRemove: () -> Unit,
) {
   val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
   Surface(
      onClick = onLaunch,
      shape = MaterialTheme.shapes.medium,
      color = Color.Companion.Transparent
   ) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
         AppIcon(app, size = 48.dp)
         Column(Modifier.Companion.weight(1f)) {
            val q = searchQuery.trim()
            val nameAnnotated =
               if (q.isEmpty()) {
                  buildAnnotatedString { append(app.label) }
               } else {
                  buildAnnotatedString {
                     append(app.label)
                     val ranges = fuzzyMatchRanges(q, app.label)
                     ranges.forEach { r ->
                        addStyle(
                           SpanStyle(background = Color.Companion.Black, color = Color(0xFFFFFF00)),
                           r.first,
                           r.last + 1,
                        )
                     }
                  }
               }
            val detailsText = buildString {
               appendLine(if (app.isSystemApp) "Pre-installed system app" else "Installed by user")
               appendLine(app.componentName.packageName)
               appendLine("Version ${app.versionName} (${app.versionCode})")
               appendLine("Android SDK ${app.targetSdk} (${app.minSdk})")
               appendLine("Installed ${df.format(Date(app.installedTimeMillis))}")
               append("Last updated ${df.format(Date(app.lastUpdatedTimeMillis))}")
            }
            val detailsAnnotated =
               if (q.isEmpty()) {
                  buildAnnotatedString { append(detailsText) }
               } else {
                  buildAnnotatedString {
                     append(detailsText)
                     val ranges = fuzzyMatchRanges(q, detailsText)
                     ranges.forEach { r ->
                        addStyle(
                           SpanStyle(background = Color.Companion.Black, color = Color(0xFFFFFF00)),
                           r.first,
                           r.last + 1,
                        )
                     }
                  }
               }
            Text(nameAnnotated, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.Companion.size(6.dp))
            Text(detailsAnnotated)
            Spacer(Modifier.Companion.size(6.dp))
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

private fun fuzzyMatchRanges(query: String, target: String): List<IntRange> {
   if (query.isEmpty() || target.isEmpty()) return emptyList()
   val ranges = mutableListOf<IntRange>()
   var start = 0
   while (start < target.length) {
      val idx = target.indexOf(query, startIndex = start, ignoreCase = true)
      if (idx < 0) break
      val end = idx + query.length - 1
      ranges.add(IntRange(idx, end))
      // Advance past this match to avoid overlapping highlights
      start = idx + query.length
   }
   return ranges
}