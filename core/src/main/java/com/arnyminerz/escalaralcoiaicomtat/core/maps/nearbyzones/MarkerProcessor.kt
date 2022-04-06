package com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones

import android.content.Context
import android.location.Location
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.utils.append
import com.arnyminerz.escalaralcoiaicomtat.core.utils.distanceTo
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getBoundingBox
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toGeoPoint
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import kotlinx.coroutines.flow.collectLatest
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import timber.log.Timber

/**
 * All the markers that have been added to the GoogleMap by the nearby zones module.
 * @author Arnau Mora
 * @since 20220212
 */
internal val nearbyZonesMarkers = mutableStateOf<List<Marker>>(emptyList())

@UiThread
internal fun processNearbyZonesMarkers(
    mapView: MapView,
    currentLocation: Location?,
    onLoadedPoints: (count: Int) -> Unit,
    animate: Boolean = true,
) {
    val markersList = nearbyZonesMarkers.value

    // Remove all the old markers if added
    for (marker in markersList)
        mapView.overlays.remove(marker)
    // Re-add them all
    mapView.overlays.addAll(markersList)

    val points = markersList.map { it.position }.toMutableList()

    Timber.i("Nearby Zones points: $points")
    onLoadedPoints(points.size)

    // Add the current location when getting the bounding box
    points.append(currentLocation?.toGeoPoint())

    mapView.invalidate()
    if (points.size > 1)
        try {
            val boundingBox = points.getBoundingBox()
            Timber.i("Bounding box: $boundingBox")
            mapView.zoomToBoundingBox(boundingBox, animate, 30)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Cannot center map in markers")
        }
}

/**
 * Will get called when there's an update on the location of the device, therefore, nearby zones
 * should be updated.
 * @author Arnau Mora
 * @since 20220212
 */
@WorkerThread
internal suspend fun locationCallback(
    context: Context,
    model: NearbyZonesViewModel,
    mapView: MapView,
    location: Location,
    @UiThread onLoadedPoints: (count: Int) -> Unit,
) {
    for (marker in nearbyZonesMarkers.value)
        mapView.overlays.remove(marker)

    val newMarkersList = arrayListOf<Marker>()

    PreferencesModule
        .getNearbyZonesDistance()
        .collectLatest { nearbyZonesDistance ->
            model.zones.forEach { zone ->
                zone.location
                    ?.takeIf { it.distanceTo(location.toGeoPoint()) <= nearbyZonesDistance }
                    ?.let { markerPosition ->
                        Marker(mapView)
                            .apply {
                                val intent = zone.intent(context)

                                position = markerPosition
                                title = zone.displayName
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = ContextCompat.getDrawable(
                                    context,
                                    R.drawable.ic_waypoint_escalador_blanc
                                )
                                infoWindow
                                    .view
                                    .findViewById<TextView>(R.id.bubble_title)
                                    .setOnClickListener { context.launch(intent) }

                                newMarkersList.add(this)
                            }
                    }
            }

            uiContext {
                var newItems = false
                nearbyZonesMarkers.value.forEach { marker ->
                    newMarkersList.find { it.title == marker.title }
                        ?: run { newItems = true; return@forEach }
                }
                nearbyZonesMarkers.value = newMarkersList

                processNearbyZonesMarkers(mapView, location, onLoadedPoints, newItems)
            }
        }
}
