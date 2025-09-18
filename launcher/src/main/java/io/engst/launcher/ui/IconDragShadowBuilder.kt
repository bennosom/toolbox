package io.engst.launcher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.view.View

class IconDragShadowBuilder(private val icon: Bitmap) : View.DragShadowBuilder() {

  override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
    outShadowSize.set(icon.width, icon.height)
    outShadowTouchPoint.set(icon.width / 2, icon.height / 2)
  }

  override fun onDrawShadow(canvas: Canvas) {
    canvas.drawBitmap(icon, 0f, 0f, null)
  }
}