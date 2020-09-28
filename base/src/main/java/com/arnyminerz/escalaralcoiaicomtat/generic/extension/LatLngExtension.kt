package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import android.location.Location
import android.net.Uri
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil

fun LatLng.toUri(showMarker: Boolean = false, markerTitle: String? = null): Uri {
    return Uri.parse(
        if (showMarker) "geo:0,0?q=$latitude,$longitude${if (markerTitle != null) "(${markerTitle.replace(
            " ",
            "+"
        )})" else ""}" else "geo:$latitude,$longitude"
    )
}

fun LatLng.distanceTo(other: LatLng): Double =
    SphericalUtil.computeDistanceBetween(this, other)

fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)

fun LatLngBounds.Builder.includeAll(points: Collection<LatLng>) {
    for (point in points)
        include(point)
}

fun Collection<LatLng>.bounds(): LatLngBounds {
    val bounds = LatLngBounds.builder()

    bounds.includeAll(this)

    return bounds.build()
}