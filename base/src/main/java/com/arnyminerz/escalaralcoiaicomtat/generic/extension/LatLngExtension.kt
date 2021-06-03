package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import android.location.Location
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.GeoPoint

fun LatLng.toUri(showMarker: Boolean = false, markerTitle: String? = null): Uri {
    return Uri.parse(
        if (showMarker)
            "geo:0,0?q=$latitude,$longitude${
                if (markerTitle != null) "(${markerTitle.replace(" ", "+")})"
                else ""
            }"
        else "geo:$latitude,$longitude"
    )
}

fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)

fun LatLngBounds.Builder.includeAll(points: Collection<LatLng>): LatLngBounds.Builder {
    for (point in points)
        include(point)
    return this
}

fun LatLng.distanceTo(other: LatLng): Float {
    val results = floatArrayOf(0f, 0f, 0f)
    Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, results)
    return results.first()
}

fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)
