package com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.ui

import androidx.annotation.UiThread
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.arnyminerz.escalaralcoiaicomtat.core.maps.NearbyZonesModule
import com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.NearbyZonesViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.locationCallback
import com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.nearbyZonesMarkers
import com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.processNearbyZonesMarkers
import com.arnyminerz.escalaralcoiaicomtat.core.ui.map.MapView
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toGeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber

@Composable
internal fun NearbyZonesMap(
    model: NearbyZonesViewModel,
    lifecycle: Lifecycle,
    @UiThread setPointsCount: (count: Int) -> Unit,
) {
    val context = LocalContext.current

    MapView(
        modifier = Modifier
            .padding(end = 4.dp, start = 4.dp, bottom = 4.dp)
            .graphicsLayer {
                shape = RoundedCornerShape(8.dp)
                clip = true
            }
            .height(150.dp)
            .fillMaxWidth(),
        onCreate = { mapView ->
            Timber.i("Loaded NearbyZones Map.")
            mapView.setMultiTouchControls(false)
            mapView.isFlingEnabled = false
            mapView.zoomController.apply {
                setZoomInEnabled(false)
                setZoomOutEnabled(false)
                setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            }

            val nearbyZonesModule = NearbyZonesModule(context) { location ->
                doAsync {
                    locationCallback(context, model, mapView, location, setPointsCount)
                }
                mapView.controller.animateTo(location.toGeoPoint())
                mapView.controller.setZoom(14.0)
            }
            nearbyZonesMarkers.value = emptyList()

            var locationOverlay = MyLocationNewOverlay(nearbyZonesModule, mapView)
            locationOverlay.enableMyLocation()
            mapView.overlays.add(locationOverlay)

            lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> try {
                            locationOverlay.enableMyLocation()
                        } catch (e: RuntimeException) {
                            locationOverlay =
                                MyLocationNewOverlay(nearbyZonesModule, mapView)
                            locationOverlay.enableMyLocation()
                            mapView.overlays.add(locationOverlay)
                        }
                        Lifecycle.Event.ON_PAUSE -> locationOverlay.disableMyLocation()
                        else -> {}
                    }
                }
            })

            // Add all the markers if not added. This is for UI refreshes
            processNearbyZonesMarkers(
                mapView,
                nearbyZonesModule.lastKnownLocation,
                false,
            )
            setPointsCount(nearbyZonesMarkers.value.size)
        }
    )
}
