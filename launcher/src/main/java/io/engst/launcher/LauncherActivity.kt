package io.engst.launcher

import android.content.ClipData
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.engst.core.logDebug

class LauncherActivity : ComponentActivity() {

  private val stateHolder: LauncherStateHolder by lazy { LauncherStateHolderImpl(this) }

  @OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    logDebug {
      "onCreate: $this rootTask=$isTaskRoot task=$taskId intent=$intent display=${display.displayId}"
    }
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
    window.setBackgroundDrawable(TRANSPARENT.toDrawable())

    setContent {
      val view = LocalView.current
      val density = LocalDensity.current
      MaterialTheme {
        LauncherApp(
            stateHolder = stateHolder,
            displayId = display.displayId,
            onDragStart = { app ->
              val size = with(density) { 80.dp.roundToPx() }
              view.startDragAndDrop(
                  ClipData.newIntent(app.name, app.launchIntent),
                  IconDragShadowBuilder(app.icon.toBitmap(size, size)),
                  null,
                  View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE)
            })
      }
    }
  }
}

@Preview(name = "Mobile Portrait", widthDp = 280, heightDp = 560)
@Preview(name = "Mobile Landscape", widthDp = 560, heightDp = 280)
@Preview(name = "Tablet Portrait", widthDp = 560, heightDp = 780)
@Preview(name = "Tablet Landscape", widthDp = 780, heightDp = 560)
@Preview(name = "Desktop", widthDp = 1200, heightDp = 800)
@Composable
fun PreviewLauncherApp() {
  MaterialTheme { LauncherApp() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherApp(
    stateHolder: LauncherStateHolder = LauncherStateHolderNoop(),
    displayId: Int = 0,
    onDragStart: (App) -> Unit = {}
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val density = LocalDensity.current
    val apps by stateHolder.apps.collectAsStateWithLifecycle()
    val isLoading by stateHolder.isLoading.collectAsStateWithLifecycle()
    val screenConfig = rememberScreenConfig()

    if (isLoading) {
      CircularProgressIndicator()
    }
    Text("$screenConfig", color = MaterialTheme.colorScheme.surface)
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
        contentPadding = WindowInsets.safeContent.asPaddingValues(),
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
                  Column(
                      verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                      horizontalAlignment = Alignment.CenterHorizontally) {
                        val size = with(density) { 80.dp.roundToPx() }
                        Image(
                            bitmap = app.icon.toBitmap(size, size).asImageBitmap(),
                            contentDescription = app.name,
                            modifier = Modifier.padding(3.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .padding(vertical = 6.dp, horizontal = 3.dp)) {
                              Text(
                                  text = app.name,
                                  style =
                                      when (screenConfig) {
                                        ScreenConfig.MOBILE_PORTRAIT ->
                                            MaterialTheme.typography.bodySmall
                                        else -> MaterialTheme.typography.headlineSmall
                                      },
                                  maxLines = 1,
                                  overflow = TextOverflow.Ellipsis)
                            }
                      }
                }
          }
        }
  }
}
