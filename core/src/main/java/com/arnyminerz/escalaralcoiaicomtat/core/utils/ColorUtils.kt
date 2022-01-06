package com.arnyminerz.escalaralcoiaicomtat.core.utils

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces

/**
 * Creates a new `Color` instance from an ARGB color int.
 * The resulting color is in the [sRGB][ColorSpace.Named.SRGB]
 * color space.
 *
 * @author Arnau Mora
 * @since 20220106
 * @return A non-null instance of [Color]
 * @see <a href="https://stackoverflow.com/a/48596185">StackOverflow</a>
 */
fun @receiver:ColorInt Int.color(): Color {
    val r = ((this shr 16) and 0xff) / 255f
    val g = ((this shr 8) and 0xff) / 255f
    val b = ((this) and 0xff) / 255f
    val a = ((this shr 24) and 0xff) / 255f
    return Color(r, g, b, a, ColorSpaces.Srgb)
}
