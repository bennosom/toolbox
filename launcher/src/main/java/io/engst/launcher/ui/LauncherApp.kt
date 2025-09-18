package io.engst.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.engst.launcher.App
import io.engst.launcher.LauncherStateHolder
import io.engst.launcher.LauncherStateHolderNoop

@OptIn(ExperimentalFoundationApi::class)
@Preview(name = "Mobile Portrait", widthDp = 280, heightDp = 560)
@Preview(name = "Mobile Landscape", widthDp = 560, heightDp = 280)
@Preview(name = "Tablet Portrait", widthDp = 560, heightDp = 780)
@Preview(name = "Tablet Landscape", widthDp = 780, heightDp = 560)
@Preview(name = "Desktop", widthDp = 1200, heightDp = 800)
@Composable
fun LauncherApp(
    stateHolder: LauncherStateHolder = LauncherStateHolderNoop(),
    displayId: Int = 0,
    onDragStart: (App) -> Unit = {}
) {
  val screenConfig = rememberScreenConfig()
  Text("$screenConfig", style = MaterialTheme.typography.labelSmall)
  var showDetails by remember { mutableStateOf(false) }
  if (showDetails) {
    AppManager(stateHolder = stateHolder, onBack = { showDetails = false })
  } else {
    Column(Modifier.fillMaxSize()) {
      Row(
          Modifier.fillMaxWidth().statusBarsPadding(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { showDetails = !showDetails }) { Icon(Icons.Default.Info, null) }
          }
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        if (stateHolder.isLoading.collectAsStateWithLifecycle().value) {
          CircularProgressIndicator()
        }

        val apps by stateHolder.apps.collectAsStateWithLifecycle()
        LazyVerticalGrid(
            columns =
                when (screenConfig) {
                  ScreenConfig.MOBILE_PORTRAIT,
                  ScreenConfig.MOBILE_LANDSCAPE -> GridCells.Fixed(4)

                  ScreenConfig.TABLET_PORTRAIT,
                  ScreenConfig.TABLET_LANDSCAPE -> GridCells.Fixed(6)

                  else -> GridCells.Adaptive(120.dp)
                },
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxSize()) {
              items(apps, key = { it.id }) { app ->
                Box(
                    modifier =
                        Modifier.clip(MaterialTheme.shapes.medium)
                            .animateItem()
                            .combinedClickable(
                                onClick = { stateHolder.launch(app, displayId) },
                                onLongClick = { onDragStart(app) }),
                    contentAlignment = Alignment.BottomCenter) {
                      AppIconWithLabel(app = app, screenConfig = screenConfig, iconSizeDp = 80.dp)
                    }
              }
            }
      }
    }
  }
}

@Composable
private fun AppIconWithLabel(
    app: App,
    screenConfig: ScreenConfig,
    iconSizeDp: Dp,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
      horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(app, iconSizeDp)
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(vertical = 6.dp, horizontal = 3.dp)) {
              Text(
                  text = app.name,
                  style =
                      when (screenConfig) {
                        ScreenConfig.MOBILE_PORTRAIT -> MaterialTheme.typography.bodySmall
                        else -> MaterialTheme.typography.headlineSmall
                      },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
            }
      }
}

@Composable
fun AppIcon(app: App, size: Dp) {
  val density = LocalDensity.current
  val size = with(density) { size.roundToPx() }
  Image(
      bitmap = app.icon.toBitmap(size, size).asImageBitmap(),
      contentDescription = app.name,
      modifier = Modifier.padding(3.dp))
}
