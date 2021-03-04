package com.arnyminerz.escalaralcoiaicomtat.data.map

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.plugins.annotation.FillManager
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import timber.log.Timber
import java.io.Serializable

data class GeoGeometry(
    val style: GeoStyle,
    val points: ArrayList<LatLng>,
    val windowData: MapObjectWindowData?,
    val closedShape: Boolean
) : Serializable {
    fun addToMap(fillManager: FillManager, lineManager: LineManager) {
        Timber.v("Creating new polygon with ${points.size} points...")
        if (closedShape) { // Polygon
            val fillOptions = FillOptions()
                .withLatLngs(listOf(points))
                .apply(style)
            val lineOptions = LineOptions()
                .withLatLngs(points)
                .apply(style)

            fillManager.create(fillOptions)
            lineManager.create(lineOptions)
        } else { // Polyline
            val lineOptions = LineOptions()
                .withLatLngs(points)
                .apply(style)
            lineManager.create(lineOptions)
        }
    }
}

@ExperimentalUnsignedTypes
fun Collection<GeoGeometry>.addToMap(fillManager: FillManager, lineManager: LineManager) {
    for (marker in this)
        marker.addToMap(fillManager, lineManager)
}

fun LatLngBounds.Builder.include(points: ArrayList<LatLng>) {
    for (point in points)
        include(point)
}