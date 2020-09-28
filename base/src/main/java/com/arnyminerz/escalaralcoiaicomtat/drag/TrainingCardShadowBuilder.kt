package com.arnyminerz.escalaralcoiaicomtat.drag

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.view.View

class TrainingCardShadowBuilder(private val v: View) : View.DragShadowBuilder(v) {
    private val shadow = ColorDrawable(Color.LTGRAY)

    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        val width = view.width
        val height = view.height

        shadow.setBounds(0, 0, width, height)

        outShadowSize.set(width, height)

        outShadowTouchPoint.set(width / 2, height / 2)
    }

    override fun onDrawShadow(canvas: Canvas) {
        v.draw(canvas)
        //shadow.draw(canvas)
    }
}