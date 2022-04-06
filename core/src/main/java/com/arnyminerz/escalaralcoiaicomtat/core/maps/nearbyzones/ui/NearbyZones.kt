package com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.ui

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.NearbyZonesViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones.nearbyZonesMarkers
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.vibrate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@SuppressLint("MissingPermission")
@ExperimentalMaterial3Api
@ExperimentalPermissionsApi
@Composable
fun ComponentActivity.NearbyZones() {
    val model: NearbyZonesViewModel by viewModels()
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    var pointsCount: Int? by remember { mutableStateOf(null) }
    var mapVisible by remember { mutableStateOf(true) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mapVisible = !mapVisible },
                verticalAlignment = Alignment.CenterVertically,
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
                AnimatedVisibility(visible = mapVisible) {
                    Column {
                        NearbyZonesMap(model, lifecycle) { pointsCount = it }

                        val markersList by remember { nearbyZonesMarkers }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            items(markersList) { marker ->
                                NearbyZonesRow(marker)
                            }
                        }
                    }
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
