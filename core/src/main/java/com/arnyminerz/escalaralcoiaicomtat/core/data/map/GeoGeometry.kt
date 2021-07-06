package com.arnyminerz.escalaralcoiaicomtat.core.data.map

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import timber.log.Timber

data class GeoGeometry(
    val style: GeoStyle,
    val points: Collection<LatLng>,
    val windowData: MapObjectWindowData?,
    val closedShape: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable<GeoStyle>(GeoStyle::class.java.classLoader)!!,
        with(parcel.readInt()) {
            val points = arrayListOf<LatLng>()
            for (i in 0 until this)
                points.add(LatLng(parcel.readDouble(), parcel.readDouble()))
            points
        },
        parcel.readParcelable(MapObjectWindowData::class.java.classLoader),
        parcel.readInt() == 1
    )

    fun addToMap(map: GoogleMap): Pair<Polyline?, Polygon?> {
        Timber.v("Creating new polygon with ${points.size} points...")
        return if (closedShape) { // Polygon
            val options = PolygonOptions()
                .addAll(points)
                .apply(style)

            val fill = map.addPolygon(options)
            Pair(null, fill)
        } else { // Polyline
            val options = PolylineOptions()
                .addAll(points)
                .apply(style)
            val line = map.addPolyline(options)
            Pair(line, null)
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(style, 0)
        dest.writeInt(points.size)
        for (point in points) {
            dest.writeDouble(point.latitude)
            dest.writeDouble(point.longitude)
        }
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
    map: GoogleMap,
): List<Pair<Polyline?, Polygon?>> {
    val list = arrayListOf<Pair<Polyline?, Polygon?>>()
    for (geometry in this)
        list.add(geometry.addToMap(map))
    return list.toList()
}
