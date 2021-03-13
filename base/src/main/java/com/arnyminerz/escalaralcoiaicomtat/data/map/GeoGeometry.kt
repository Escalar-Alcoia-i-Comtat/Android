package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.os.Parcel
import android.os.Parcelable
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.plugins.annotation.*
import timber.log.Timber

data class GeoGeometry(
    val style: GeoStyle,
    val points: List<LatLng>,
    val windowData: MapObjectWindowData?,
    val closedShape: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable<GeoStyle>(GeoStyle::class.java.classLoader)!!,
        with(parcel.readParcelableArray(LatLng::class.java.classLoader)!!.toList()) {
            val list = arrayListOf<LatLng>()
            for (i in this)
                if (i is LatLng)
                    list.add(i)
            list
        },
        parcel.readParcelable(MapObjectWindowData::class.java.classLoader),
        parcel.readInt() == 1
    )

    fun addToMap(fillManager: FillManager, lineManager: LineManager): Pair<Line, Fill?> {
        Timber.v("Creating new polygon with ${points.size} points...")
        if (closedShape) { // Polygon
            val fillOptions = FillOptions()
                .withLatLngs(listOf(points))
                .apply(style)
            val lineOptions = LineOptions()
                .withLatLngs(points)
                .apply(style)

            val fill = fillManager.create(fillOptions)
            val line = lineManager.create(lineOptions)
            return Pair(line, fill)
        } else { // Polyline
            val lineOptions = LineOptions()
                .withLatLngs(points)
                .apply(style)
            val line = lineManager.create(lineOptions)
            return Pair(line, null)
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(style, 0)
        dest.writeParcelableArray(points.toTypedArray(), 0)
        dest.writeParcelable(windowData, 0)
        dest.writeInt(if (closedShape) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<GeoGeometry> {
        override fun createFromParcel(parcel: Parcel): GeoGeometry {
            return GeoGeometry(parcel)
        }

        override fun newArray(size: Int): Array<GeoGeometry?> {
            return arrayOfNulls(size)
        }
    }
}

fun Collection<GeoGeometry>.addToMap(
    fillManager: FillManager,
    lineManager: LineManager
): List<Pair<Line, Fill?>> {
    val list = arrayListOf<Pair<Line, Fill?>>()
    for (marker in this)
        list.add(marker.addToMap(fillManager, lineManager))
    return list.toList()
}

fun LatLngBounds.Builder.include(points: ArrayList<LatLng>) {
    for (point in points)
        include(point)
}
