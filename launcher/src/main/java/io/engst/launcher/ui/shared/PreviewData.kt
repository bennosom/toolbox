package io.engst.launcher.ui.shared

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import io.engst.launcher.model.App
import io.engst.launcher.model.Cell
import io.engst.launcher.model.GridSpec
import androidx.core.graphics.drawable.toDrawable

fun previewApp(id: String, label: String = id) = App(
    id = id,
    label = label,
    icon = android.graphics.Color.MAGENTA.toDrawable(),
    componentName = ComponentName("pkg.$id", "cls.$id"),
    launchIntent = Intent(),
    shortcuts = emptyList(),
    versionName = "1.0",
    versionCode = 1L,
    minSdk = 31,
    targetSdk = 36,
    lastUpdatedTimeMillis = 0L,
    installedTimeMillis = 0L,
    isSystemApp = false,
)

val previewSpec = GridSpec(cols = 4, rows = 4)

fun previewPage(spec: GridSpec = previewSpec, apps: List<App> = emptyList()): Map<Cell, App?> {
    val page = mutableMapOf<Cell, App?>()
    var appIndex = 0
    for (row in 0 until spec.rows) {
        for (col in 0 until spec.cols) {
            page[Cell(col, row)] = apps.getOrNull(appIndex)
            appIndex++
        }
    }
    return page
}

val previewApps = listOf(
    previewApp("chrome", "Chrome"),
    previewApp("maps", "Maps"),
    previewApp("photos", "Photos"),
    previewApp("camera", "Camera"),
    previewApp("phone", "Phone"),
    previewApp("messages", "Messages"),
    previewApp("settings", "Settings"),
    previewApp("calendar", "Calendar"),
    previewApp("clock", "Clock"),
    previewApp("weather", "Weather"),
    previewApp("music", "Music"),
    previewApp("files", "Files"),
    previewApp("contacts", "Contacts"),
    previewApp("notes", "Notes"),
    previewApp("calculator", "Calculator"),
    previewApp("podcasts", "Podcasts"),
    previewApp("translate", "Translate"),
    previewApp("recorder", "Recorder"),
    previewApp("tasks", "Tasks"),
    previewApp("news", "News"),
    previewApp("books", "Books"),
    previewApp("wallet", "Wallet"),
    previewApp("health", "Health"),
    previewApp("mail", "Mail"),
)
