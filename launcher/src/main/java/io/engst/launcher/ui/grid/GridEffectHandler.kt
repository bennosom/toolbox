package io.engst.launcher.ui.grid

import android.content.Context
import android.content.pm.ShortcutInfo
import io.engst.core.Logging
import io.engst.core.apps.launchActivity
import io.engst.core.scopedLogger
import io.engst.launcher.core.launchAppDetails
import io.engst.launcher.core.launchAppRemovalRequest
import io.engst.launcher.core.launchShortcut
import io.engst.launcher.model.App

interface GridEffectHandler {
    fun launchApp(app: App)
    fun openAppDetails(app: App)
    fun removeApp(app: App)
    fun launchShortcut(shortcut: ShortcutInfo)
    fun openDefaultLauncherSettings()
    fun openWallpaperSettings()
    fun showResetDefaultsSnackbar()
    fun expandNotificationsPanel()
    fun openSearch()
}

class GridEffectHandlerImpl(
    private val context: Context,
) : GridEffectHandler, Logging by scopedLogger("GridEffectHandler") {

    var onSetDefaultLauncher: () -> Unit = {}
    var onOpenWallpaperSettings: () -> Unit = {}
    var onShowResetDefaultsSnackbar: () -> Unit = {}

    override fun launchApp(app: App) {
        context.launchActivity(app.componentName)
    }

    override fun openAppDetails(app: App) {
        context.launchAppDetails(app.componentName.packageName)
    }

    override fun removeApp(app: App) {
        context.launchAppRemovalRequest(app.componentName.packageName)
    }

    override fun launchShortcut(shortcut: ShortcutInfo) {
        context.launchShortcut(shortcut)
    }

    override fun openDefaultLauncherSettings() {
        onSetDefaultLauncher()
    }

    override fun openWallpaperSettings() {
        onOpenWallpaperSettings()
    }

    override fun showResetDefaultsSnackbar() {
        onShowResetDefaultsSnackbar()
    }

    @Suppress("DEPRECATION")
    override fun expandNotificationsPanel() {
        try {
            val statusBarService = context.getSystemService("statusbar")
            statusBarService?.javaClass
                ?.getMethod("expandNotificationsPanel")
                ?.invoke(statusBarService)
        } catch (e: Exception) {
            logWarn { "expandNotificationsPanel failed: ${e.message}" }
        }
    }

    override fun openSearch() {
        // stub — wired to EPIC-008 search when implemented
    }
}
