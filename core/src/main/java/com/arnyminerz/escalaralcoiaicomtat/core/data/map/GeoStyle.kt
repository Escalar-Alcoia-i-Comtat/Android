package com.arnyminerz.escalaralcoiaicomtat.core.data.map

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.Cap
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions

data class GeoStyle(
    val fillColor: String?,
    val strokeColor: String?,
    val lineWidth: Float?,
    @com.arnyminerz.escalaralcoiaicomtat.core.annotations.JointType val jointType: Int?,
    val startEndCap: Pair<Cap?, Cap?> = Pair(null, null)
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readFloat(),
        parcel.readInt(),
        Pair(
            parcel.readParcelable(Cap::class.java.classLoader),
            parcel.readParcelable(Cap::class.java.classLoader),
        )
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
        jointType?.let { dest.writeInt(it) }
        dest.writeParcelable(startEndCap.first, 0)
        dest.writeParcelable(startEndCap.second, 0)
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

fun PolygonOptions.apply(geoStyle: GeoStyle): PolygonOptions {
    val strokeColor = geoStyle.strokeColor()
    val strokeWidth = geoStyle.lineWidth
    val jointType = geoStyle.jointType
    return apply {
        if (strokeColor != null)
            this.strokeColor(strokeColor)
        if (jointType != null)
            this.strokeJointType(jointType)
        if (strokeWidth != null)
            this.strokeWidth(strokeWidth)
    }
}

fun PolylineOptions.apply(geoStyle: GeoStyle): PolylineOptions {
    var modOptions = this

    val strokeColor = geoStyle.strokeColor()
    if (strokeColor != null)
        modOptions = modOptions.color(strokeColor)

    val jointType = geoStyle.jointType
    if (jointType != null)
        modOptions = modOptions.jointType(jointType)

    val caps = geoStyle.startEndCap
    val startCap = caps.first
    if (startCap != null)
        modOptions = modOptions.startCap(startCap)
    val endCap = caps.second
    if (endCap != null)
        modOptions = modOptions.startCap(endCap)

    val lineWidth = geoStyle.lineWidth
    if (lineWidth != null)
        modOptions = modOptions.width(lineWidth * LINE_WIDTH_MULTIPLIER)

    return modOptions
}
