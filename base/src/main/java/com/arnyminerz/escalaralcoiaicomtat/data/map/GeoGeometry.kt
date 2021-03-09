package com.arnyminerz.escalaralcoiaicomtat.data.map

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.plugins.annotation.*
import timber.log.Timber
import java.io.Serializable

data class GeoGeometry(
    val style: GeoStyle,
    val points: ArrayList<LatLng>,
    val windowData: MapObjectWindowData?,
    val closedShape: Boolean
) : Serializable {
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
}

@ExperimentalUnsignedTypes
fun Collection<GeoGeometry>.addToMap(fillManager: FillManager, lineManager: LineManager): List<Pair<Line, Fill?>> {
    val list = arrayListOf<Pair<Line, Fill?>>()
    for (marker in this)
        list.add(marker.addToMap(fillManager, lineManager))
    return list.toList()
}

fun LatLngBounds.Builder.include(points: ArrayList<LatLng>) {
    for (point in points)
        include(point)
}
