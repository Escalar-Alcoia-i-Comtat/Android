package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.content.Intent
import android.location.Location
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

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

/**
 * Creates an [Intent] for launching Google Maps app showing the [LatLng] specified.
 * @author Arnau Mora
 * @since 20210830
 * @param showMarker If Google Maps should be showing a marker in the location.
 * @param markerTitle The title of the shown marker.
 * @return An [Intent] ready to be started that shows Google Maps with the configuration set.
 */
fun LatLng.mapsIntent(showMarker: Boolean = true, markerTitle: String? = null): Intent =
    Intent(Intent.ACTION_VIEW, toUri(showMarker, markerTitle))
        .setPackage("com.google.android.apps.maps")

fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)

/**
 * Converts a [LatLng] object into a [Location].
 * @author Arnau Mora
 * @since 20210603
 */
fun LatLng.toLocation(): Location {
    val location = Location(this.toString())
    location.latitude = this.latitude
    location.longitude = this.longitude
    return location
}

fun LatLngBounds.Builder.includeAll(points: Collection<LatLng>): LatLngBounds.Builder {
    for (point in points)
        include(point)
    return this
}

fun LatLng.distanceTo(other: LatLng): Float {
    val a = this.toLocation()
    val b = other.toLocation()
    return a.distanceTo(b)
}
