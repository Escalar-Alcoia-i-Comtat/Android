package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.content.Intent
import android.location.Location
import android.net.Uri
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

/**
 * Computes the center point of all the ones in the list.
 * @author Arnau Mora
 * @since 20220302
 * @return The center point between all the ones in the list.
 */
fun List<GeoPoint>.computeCentroid(): GeoPoint {
    var latitude = 0.0
    var longitude = 0.0

    for (point in this) {
        latitude += point.latitude
        longitude += point.longitude
    }

    return GeoPoint(latitude / size, longitude / size)
}

fun List<GeoPoint>.getBoundingBox(): BoundingBox {
    // First get bottom-left and top-right points
    var bottomLeft: GeoPoint? = null
    var topRight: GeoPoint? = null

    for (point in this) {
        if (bottomLeft == null)
            bottomLeft = point
        if (topRight == null)
            topRight = point
        if (point.latitude < bottomLeft.latitude && point.longitude < bottomLeft.longitude)
            bottomLeft = point
        else if (point.latitude > bottomLeft.latitude && point.longitude > bottomLeft.longitude)
            topRight = point
    }

    if (bottomLeft == null || topRight == null)
        throw IllegalStateException("Could not get bottom left nor top right points.")

    // Now get bounds
    val north: Double
    val south: Double
    val east: Double
    val west: Double

    if (bottomLeft.latitude > topRight.latitude) {
        north = bottomLeft.latitude
        south = topRight.latitude
    } else {
        north = topRight.latitude
        south = bottomLeft.latitude
    }
    if (bottomLeft.longitude > topRight.longitude) {
        east = bottomLeft.longitude
        west = topRight.longitude
    } else {
        east = topRight.longitude
        west = bottomLeft.longitude
    }

    return BoundingBox(north, east, south, west)
}

fun GeoPoint.toUri(showMarker: Boolean = false, markerTitle: String? = null): Uri {
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
 * Creates an [Intent] for launching Google Maps app showing the [GeoPoint] specified.
 * @author Arnau Mora
 * @since 20210830
 * @param showMarker If Google Maps should be showing a marker in the location.
 * @param markerTitle The title of the shown marker.
 * @return An [Intent] ready to be started that shows Google Maps with the configuration set.
 */
fun GeoPoint.mapsIntent(showMarker: Boolean = true, markerTitle: String? = null): Intent =
    Intent(Intent.ACTION_VIEW, toUri(showMarker, markerTitle))
        .setPackage("com.google.android.apps.maps")

fun Location.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

/**
 * Converts a [GeoPoint] object into a [Location].
 * @author Arnau Mora
 * @since 20210603
 */
fun GeoPoint.toLocation(): Location {
    val location = Location(this.toString())
    location.latitude = this.latitude
    location.longitude = this.longitude
    return location
}

fun GeoPoint.distanceTo(other: GeoPoint): Float {
    val a = this.toLocation()
    val b = other.toLocation()
    return a.distanceTo(b)
}
