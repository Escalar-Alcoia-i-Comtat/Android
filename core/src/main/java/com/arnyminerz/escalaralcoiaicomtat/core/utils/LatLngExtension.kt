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

/**
 * Gets the [BoundingBox] of a list of [GeoPoint]s. Will cause issues when called with less than
 * 2 elements on the list.
 * @author Arnau Mora
 * @since 20220401
 * @return A [BoundingBox] instance containing all the points of the list.
 * @throws IllegalStateException When the bounding box could not have been obtained.
 */
@Throws(IllegalStateException::class)
fun List<GeoPoint>.getBoundingBox(): BoundingBox {
    // Now get bounds
    var north: Double? = null
    var south: Double? = null
    var east: Double? = null
    var west: Double? = null

    for (position in this) {
        north = north?.let { Math.max(position.latitude, it) } ?: position.latitude
        south = south?.let { Math.min(position.latitude, it) } ?: position.latitude

        east = east?.let { Math.max(position.longitude, it) } ?: position.longitude
        west = west?.let { Math.min(position.longitude, it) } ?: position.longitude
    }

    if (north == null || south == null || east == null || west == null)
        throw IllegalStateException("Could not get directions. north=$north, south=$south, east=$east, west=$west")

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

/**
 * Computes the distance between two [GeoPoint]. Quite a dirty approach, using conversions to
 * [Location], but works as expected.
 * @author Arnau Mora
 * @since 20220401
 * @param other The point to compute the distance to.
 * @return The distance in meters between the two points.
 */
fun GeoPoint.distanceTo(other: GeoPoint): Float {
    val a = this.toLocation()
    val b = other.toLocation()
    return a.distanceTo(b)
}
