package io.engst.launcher.ui.grid

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App

@Composable
fun AppMenu(
   app: App,
   visible: Boolean,
   onDismissRequest: () -> Unit = {},
   onShortcutLaunch: (ShortcutInfo) -> Unit = {},
   onAppDetails: (App) -> Unit = {},
   onAppRemove: (App) -> Unit = {},
) {
   DropdownMenu(
      expanded = visible,
      onDismissRequest = onDismissRequest,
      shape = MaterialTheme.shapes.large,
   ) {
      Column(
         Modifier.requiredSizeIn(minWidth = 180.dp),
         horizontalAlignment = Alignment.CenterHorizontally,
      ) {
         // app related actions
         Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
         ) {
            IconButton(
               onClick = {
                  onAppDetails(app)
                  onDismissRequest()
               }
            ) {
               Icon(Icons.Outlined.Info, contentDescription = "Info")
            }
            IconButton(
               enabled = !app.isSystemApp,
               onClick = {
                  onAppRemove(app)
                  onDismissRequest()
               },
            ) {
               Icon(Icons.Outlined.Delete, contentDescription = "Remove")
            }
         }

         // list of available shortcuts
         if (app.shortcuts.isNotEmpty()) {
            HorizontalDivider()
         }

         app.shortcuts.forEach { shortcut ->
            val label =
               shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: shortcut.id
            Box(
               modifier =
                  Modifier
                     .fillMaxWidth()
                     .clickable {
                        onShortcutLaunch(shortcut)
                        onDismissRequest()
                     }
                     .padding(12.dp)
            ) {
               Text(label, style = MaterialTheme.typography.bodyLarge)
            }
         }
      }
   }
}
