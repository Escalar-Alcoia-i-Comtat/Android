package com.arnyminerz.escalaralcoiaicomtat.core.maps.nearbyzones

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.maps.NearbyZonesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.map.GoogleMap
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toLatLng
import com.google.android.gms.maps.CameraUpdateFactory
import timber.log.Timber

@Composable
fun NearbyZones() {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
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
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
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
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLng(location.toLatLng())
                            )
                        }
                    )
                    googleMap.isMyLocationEnabled = true
                }
            else
                Text(
                    text = stringResource(R.string.nearby_zones_permission),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {

                                },
                                onLongPress = {

                                }
                            )
                        }
                )
        }
    }
}
