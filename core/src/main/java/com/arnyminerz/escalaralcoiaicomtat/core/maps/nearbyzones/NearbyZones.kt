package com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.maps.NearbyZonesModule
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.map.MapView
import com.arnyminerz.escalaralcoiaicomtat.core.utils.distanceTo
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getBoundingBox
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toGeoPoint
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.utils.vibrate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber

/**
 * All the markers that have been added to the GoogleMap by the nearby zones module.
 * @author Arnau Mora
 * @since 20220212
 */
private val markers = arrayListOf<Marker>()

/**
 * Will get called when there's an update on the location of the device, therefore, nearby zones
 * should be updated.
 * @author Arnau Mora
 * @since 20220212
 */
@WorkerThread
private suspend fun locationCallback(
    context: Context,
    model: NearbyZonesViewModel,
    mapView: MapView,
    location: Location,
    @UiThread onLoadedPoints: (count: Int) -> Unit,
) {
    for (marker in markers)
        mapView.overlays.remove(marker)
    markers.clear()

    PreferencesModule
        .getNearbyZonesDistance()
        .collect { nearbyZonesDistance ->
            val points = model.zones.mapNotNull { zone ->
                zone.location
                    ?.takeIf { it.distanceTo(location.toGeoPoint()) <= nearbyZonesDistance }
                    ?.let { markerPosition ->
                        Marker(mapView)
                            .apply {
                                position = markerPosition
                                title = zone.displayName
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = ContextCompat.getDrawable(
                                    context,
                                    R.drawable.ic_waypoint_escalador_blanc
                                )

                                markers.add(this)
                            }
                        markerPosition
                    }
            }.toMutableList()

            uiContext {
                mapView.overlays.addAll(markers)

                Timber.i("Nearby Zones points: $points")
                onLoadedPoints(points.size)

                // Add the current location when getting the bounding box
                points.add(location.toGeoPoint())

                mapView.invalidate()
                if (points.size > 1)
                    mapView.zoomToBoundingBox(points.getBoundingBox(), true, 30)
            }
        }
}

@SuppressLint("MissingPermission")
@ExperimentalMaterial3Api
@ExperimentalPermissionsApi
@Composable
fun ComponentActivity.NearbyZones() {
    val context = LocalContext.current
    val model: NearbyZonesViewModel by viewModels()
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    var pointsCount: Int? by remember { mutableStateOf(null) }

    model.loadZones()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    imageVector = Icons.Rounded.Explore,
                    contentDescription = stringResource(R.string.nearby_zones_title),
                    colorFilter = ColorFilter.tint(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                )
                Text(
                    text = stringResource(R.string.nearby_zones_title),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                )
                Badge(
                    containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = if (pointsCount != null && pointsCount!! > 0)
                            stringResource(R.string.downloads_zones_title, pointsCount ?: 0)
                        else
                            stringResource(R.string.zones_count_any)
                    )
                }
            }
            if (locationPermissionState.allPermissionsGranted)
                MapView(
                    modifier = Modifier
                        .padding(end = 4.dp, start = 4.dp, bottom = 4.dp)
                        .graphicsLayer {
                            shape = RoundedCornerShape(8.dp)
                            clip = true
                        }
                        .height(150.dp)
                        .fillMaxWidth()
                ) { mapView ->
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
                            locationCallback(context, model, mapView, location) { pointsCount = it }
                        }
                        mapView.controller.animateTo(location.toGeoPoint())
                        mapView.controller.setZoom(14.0)
                    }

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
                }
            else {
                var showRationale by remember { mutableStateOf(false) }
                Column {
                    Text(
                        text = stringResource(R.string.nearby_zones_permission),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        showRationale = false
                                        if (locationPermissionState.shouldShowRationale)
                                            showRationale = true
                                        else
                                            locationPermissionState.launchMultiplePermissionRequest()
                                    },
                                    onLongPress = {
                                        vibrate(50)
                                        doAsync {
                                            PreferencesModule.setNearbyZonesEnabled(false)
                                        }
                                    }
                                )
                            }
                    )
                    AnimatedVisibility(showRationale) {
                        Text(
                            text = stringResource(R.string.nearby_zones_permission_rationale),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
