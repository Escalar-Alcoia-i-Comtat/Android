package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import android.location.Location
import android.net.Uri
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.parse.ParseGeoPoint

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

fun LatLngBounds.Builder.includeAll(points: Collection<LatLng>) {
    for (point in points)
        include(point)
}

fun ParseGeoPoint?.toLatLng() =
    if (this != null)
        LatLng(latitude, longitude)
    else null
