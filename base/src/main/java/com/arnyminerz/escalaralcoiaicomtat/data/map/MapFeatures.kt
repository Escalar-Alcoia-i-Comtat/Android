package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.os.Parcel
import android.os.Parcelable

class MapFeatures(
    markers: ArrayList<GeoMarker>? = null,
    polylines: ArrayList<GeoGeometry>? = null,
    polygons: ArrayList<GeoGeometry>? = null
) : Parcelable {
    val markers: ArrayList<GeoMarker> = markers ?: arrayListOf()
    val polylines: ArrayList<GeoGeometry> = polylines ?: arrayListOf()
    val polygons: ArrayList<GeoGeometry> = polygons ?: arrayListOf()

    constructor(parcel: Parcel) : this() {
        val markerCount = parcel.readInt()
        val polylineCount = parcel.readInt()
        val polygonCount = parcel.readInt()

        repeat(markerCount) {
            parcel.readParcelable<GeoMarker>(GeoMarker::class.java.classLoader)?.let {
                markers.add(it)
            }
        }

        repeat(polylineCount) {
            parcel.readParcelable<GeoGeometry>(GeoGeometry::class.java.classLoader)?.let {
                polylines.add(it)
            }
        }

        repeat(polygonCount) {
            parcel.readParcelable<GeoGeometry>(GeoGeometry::class.java.classLoader)?.let {
                polygons.add(it)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) = with(parcel) {
        writeInt(markers.size)
        writeInt(polylines.size)
        writeInt(polygons.size)
        for (marker in markers)
            writeParcelable(marker, 0)
        for (polyline in polylines)
            writeParcelable(polyline, 0)
        for (polygon in polygons)
            writeParcelable(polygon, 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MapFeatures> {
        override fun createFromParcel(parcel: Parcel): MapFeatures = MapFeatures(parcel)

        override fun newArray(size: Int): Array<MapFeatures?> = arrayOfNulls(size)
    }


}
