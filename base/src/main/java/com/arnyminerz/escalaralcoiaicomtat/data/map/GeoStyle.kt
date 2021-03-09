package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.graphics.Color
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.utils.ColorUtils.colorToRgbaString
import java.io.Serializable

data class GeoStyle(
    val fillColor: String?,
    val strokeColor: String?,
    val lineWidth: Float?,
    @Property.LINE_JOIN
    val lineJoin: String?
) : Serializable {
    private fun colorIsNull(color: String?) =
        color == null || !color.startsWith("#") || color.contains("null")

    fun fillColor(): Int? = if (!colorIsNull(fillColor)) try {
        Color.parseColor(fillColor)
    } catch (ex: NumberFormatException) {
        null
    } else null

    fun strokeColor(): Int? = if (!colorIsNull(strokeColor)) try {
        Color.parseColor(strokeColor)
    } catch (ex: NumberFormatException) {
        null
    } else null
}

fun FillOptions.apply(geoStyle: GeoStyle): FillOptions {
    var modOptions = this

    if (geoStyle.strokeColor != null)
        modOptions = modOptions.withFillOutlineColor(colorToRgbaString(geoStyle.strokeColor()!!))
    if (geoStyle.fillColor != null)
        modOptions = modOptions.withFillColor(colorToRgbaString(geoStyle.fillColor()!!))

    return modOptions
}

fun LineOptions.apply(geoStyle: GeoStyle): LineOptions {
    var modOptions = this

    if (geoStyle.strokeColor != null)
        modOptions = modOptions.withLineColor(colorToRgbaString(geoStyle.strokeColor()!!))
    if (geoStyle.lineJoin != null)
        modOptions = modOptions.withLineJoin(geoStyle.lineJoin)
    if (geoStyle.lineWidth != null)
        modOptions = modOptions.withLineWidth(geoStyle.lineWidth)

    return modOptions
}
