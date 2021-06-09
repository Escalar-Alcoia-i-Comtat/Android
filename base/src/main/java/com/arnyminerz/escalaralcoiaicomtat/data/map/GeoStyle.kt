package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import idroid.android.mapskit.model.CommonCap
import idroid.android.mapskit.model.CommonJointType
import idroid.android.mapskit.model.CommonPolygonOptions
import idroid.android.mapskit.model.CommonPolylineOptions

data class GeoStyle(
    val fillColor: String?,
    val strokeColor: String?,
    val lineWidth: Float?,
    val jointType: CommonJointType?,
    val startEndCap: Pair<CommonCap?, CommonCap?> = Pair(null, null)
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readFloat(),
        when (parcel.readInt()) {
            0 -> CommonJointType.ROUND
            1 -> CommonJointType.BEVEL
            else -> CommonJointType.DEFAULT
        },
        Pair(
            when (parcel.readInt()) {
                0 -> CommonCap.BUTT
                1 -> CommonCap.ROUND
                2 -> CommonCap.SQUARE
                else -> null
            },
            when (parcel.readInt()) {
                0 -> CommonCap.BUTT
                1 -> CommonCap.ROUND
                2 -> CommonCap.SQUARE
                else -> null
            },
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
        dest.writeInt(
            when (jointType) {
                CommonJointType.BEVEL -> 0
                CommonJointType.ROUND -> 1
                CommonJointType.DEFAULT -> 2
                else -> 2
            }
        )
        dest.writeInt(
            when (startEndCap.first) {
                CommonCap.BUTT -> 0
                CommonCap.ROUND -> 1
                CommonCap.SQUARE -> 2
                else -> -1
            }
        )
        dest.writeInt(
            when (startEndCap.second) {
                CommonCap.BUTT -> 0
                CommonCap.ROUND -> 1
                CommonCap.SQUARE -> 2
                else -> -1
            }
        )
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

fun CommonPolygonOptions.apply(geoStyle: GeoStyle): CommonPolygonOptions {
    val strokeColor = geoStyle.strokeColor()
    val strokeWidth = geoStyle.lineWidth
    val jointType = geoStyle.jointType
    return apply {
        if (strokeColor != null)
            this.strokeColor = strokeColor
        this.strokeJointType = jointType
        if (strokeWidth != null)
            this.strokeWidth = strokeWidth
    }
}

fun CommonPolylineOptions.apply(geoStyle: GeoStyle): CommonPolylineOptions {
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
