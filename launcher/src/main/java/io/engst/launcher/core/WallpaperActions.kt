package io.engst.launcher.core

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.view.WindowManager
import androidx.annotation.ColorInt

private const val MAX_DECODE_SIDE = 4096

fun Context.setSolidColorWallpaper(@ColorInt color: Int): Result<Unit> = runCatching {
    val bounds = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
    val width = bounds.width().coerceAtLeast(1)
    val height = bounds.height().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawColor(color)
    WallpaperManager.getInstance(this).setBitmap(bitmap)
}

fun Context.setImageWallpaper(uri: Uri): Result<Unit> = runCatching {
    val source = ImageDecoder.createSource(contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val size = info.size
        val maxSide = maxOf(size.width, size.height)
        if (maxSide > MAX_DECODE_SIDE) {
            val scale = MAX_DECODE_SIDE.toFloat() / maxSide.toFloat()
            decoder.setTargetSize(
                (size.width * scale).toInt().coerceAtLeast(1),
                (size.height * scale).toInt().coerceAtLeast(1),
            )
        }
    }
    WallpaperManager.getInstance(this).setBitmap(bitmap)
}
