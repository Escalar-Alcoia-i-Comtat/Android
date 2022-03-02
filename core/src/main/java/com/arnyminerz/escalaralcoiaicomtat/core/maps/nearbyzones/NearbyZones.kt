package com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
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
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.maps.NearbyZonesModule
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.map.GoogleMap
import com.arnyminerz.escalaralcoiaicomtat.core.utils.distanceTo
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toLatLng
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.utils.vibrate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
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
private suspend fun locationCallback(
    model: NearbyZonesViewModel,
    map: GoogleMap,
    location: Location
) {
    for (marker in markers)
        marker.remove()
    markers.clear()

    PreferencesModule
        .getNearbyZonesDistance()
        .collect { nearbyZonesDistance ->
            for (zone in model.zones)
                zone.location
                    ?.takeIf { it.distanceTo(location.toLatLng()) <= nearbyZonesDistance }
                    ?.let { markerPosition ->
                        uiContext {
                            map.addMarker(
                                MarkerOptions()
                                    .title(zone.displayName)
                                    .position(markerPosition)
                            )?.let { markers.add(it) }
                        }
                    }
        }

    // TODO: Move camera to fit markers
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
                        .padding(start = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                )
            }
            if (locationPermissionState.allPermissionsGranted)
                GoogleMap(
                    modifier = Modifier
                        .padding(end = 4.dp, start = 4.dp, bottom = 4.dp)
                        .graphicsLayer {
                            shape = RoundedCornerShape(8.dp)
                            clip = true
                        }
                        .height(150.dp)
                        .fillMaxWidth()
                ) { googleMap ->
                    Timber.i("Loaded NearbyZones Google Map.")
                    googleMap.uiSettings.apply {
                        isMyLocationButtonEnabled = false
                        setAllGesturesEnabled(false)
                    }
                    googleMap.setLocationSource(
                        NearbyZonesModule(context) { location ->
                            doAsync { locationCallback(model, googleMap, location) }
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLng(location.toLatLng())
                            )
                        }
                    )
                    googleMap.isMyLocationEnabled = true
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
