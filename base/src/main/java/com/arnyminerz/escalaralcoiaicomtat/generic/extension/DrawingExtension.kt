package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import android.graphics.Canvas
import android.text.StaticLayout
import androidx.core.graphics.withTranslation

fun StaticLayout.draw(canvas: Canvas, x: Float, y: Float) {
    canvas.withTranslation(x, y) {
        draw(this)
    }
}