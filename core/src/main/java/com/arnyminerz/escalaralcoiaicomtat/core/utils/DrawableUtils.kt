package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

/**
 * Tints the Drawable with the set [color].
 * @author Arnau Mora
 * @since 20220106
 * @param color The tint color to set.
 * @return The same drawable but tinted with [color].
 */
fun Drawable.tint(@ColorInt color: Int?): Drawable =
    if (color != null)
        DrawableCompat.wrap(this).apply {
            DrawableCompat.setTint(this, color)
        }
    else this
