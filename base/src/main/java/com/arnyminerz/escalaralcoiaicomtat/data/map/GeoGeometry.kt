package com.arnyminerz.escalaralcoiaicomtat.data.map

import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.LatLngBounds
import com.google.android.libraries.maps.model.PolygonOptions
import com.google.android.libraries.maps.model.PolylineOptions
import timber.log.Timber
import java.io.Serializable

data class GeoGeometry(
    val style: GeoStyle,
    val points: ArrayList<LatLng>,
    val windowData: MapObjectWindowData,
    val closedShape: Boolean
) : Serializable {
    companion object {
        private const val TAG = "GeoGeometry"
    }

    fun addToMap(googleMap: GoogleMap) {
        Timber.v("Creating new polygon with ${points.size} points...")
        if (closedShape) { // Polygon
            val options = PolygonOptions()
                .addAll(points)
            with(style) {
                val fillColor = fillColor()
                val strokeColor = strokeColor()
                if (strokeColor != null) options.strokeColor(strokeColor()!!)
                if (fillColor != null) options.fillColor(fillColor()!!)
                if (lineJoint != null) options.strokeJointType(lineJoint)
                if (lineWidth != null) options.strokeWidth(lineWidth)
            }
            googleMap.addPolygon(options)
        } else { // Polyline
            val options = PolylineOptions()
                .addAll(points)
            with(style) {
                Timber.v("Polyline fill color: $fillColor")
                val strokeColor = strokeColor()
                if (strokeColor != null) options.color(strokeColor)
                if (lineCap != null) options.startCap(lineCap).endCap(lineCap)
                if (lineJoint != null) options.jointType(lineJoint)
                if (lineWidth != null) options.width(lineWidth)
            }
            googleMap.addPolyline(options)
        }
    }
}

@ExperimentalUnsignedTypes
fun Collection<GeoGeometry>.addToMap(googleMap: GoogleMap) {
    for (marker in this)
        marker.addToMap(googleMap)
}

fun LatLngBounds.Builder.include(points: ArrayList<LatLng>) {
    for (point in points)
        include(point)
}