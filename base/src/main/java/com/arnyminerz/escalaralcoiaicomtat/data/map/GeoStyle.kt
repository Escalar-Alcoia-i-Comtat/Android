package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.utils.ColorUtils.colorToRgbaString

data class GeoStyle(
    val fillColor: String?,
    val strokeColor: String?,
    val lineWidth: Float?,
    @Property.LINE_JOIN
    val lineJoin: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readFloat(),
        parcel.readString()
    )

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

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(fillColor)
        dest.writeString(strokeColor)
        lineWidth?.let { dest.writeFloat(it) }
        dest.writeString(lineJoin)
    }

    companion object CREATOR : Parcelable.Creator<GeoStyle> {
        override fun createFromParcel(parcel: Parcel): GeoStyle {
            return GeoStyle(parcel)
        }

        override fun newArray(size: Int): Array<GeoStyle?> {
            return arrayOfNulls(size)
        }
    }
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
        modOptions = modOptions.withLineWidth(geoStyle.lineWidth * LINE_WIDTH_MULTIPLIER)

    return modOptions
}
