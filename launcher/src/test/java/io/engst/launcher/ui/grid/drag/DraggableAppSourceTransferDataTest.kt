package io.engst.launcher.ui.grid.drag

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.view.View
import io.engst.launcher.model.App
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DraggableAppSourceTransferDataTest {

    @Test
    fun buildDragTransferData_exposes_global_intent_payload_for_external_targets() {
        val app = App(
            id = "maps",
            label = "Maps",
            icon = ColorDrawable(),
            componentName = ComponentName("com.example.maps", "MapsActivity"),
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

        val transferData = buildDragTransferData(app)
        val intent = transferData.clipData.getItemAt(0).intent

        assertEquals(APP_DRAG_MIME_TYPE, transferData.clipData.description.getMimeType(0))
        assertTrue((transferData.flags and View.DRAG_FLAG_GLOBAL) != 0)
        assertTrue((transferData.flags and View.DRAG_FLAG_OPAQUE) != 0)
        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertEquals(app.componentName, intent.component)
        assertTrue(intent.hasCategory(Intent.CATEGORY_LAUNCHER))
    }
}
