package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.ui.map.GoogleMap
import com.arnyminerz.escalaralcoiaicomtat.core.ui.map.MapBottomDialog
import com.arnyminerz.escalaralcoiaicomtat.core.utils.includeAll
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import timber.log.Timber

@Composable
@SuppressLint("PotentialBehaviorOverride")
fun MainActivity.MapScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        var bottomDialogVisible by remember { mutableStateOf(false) }
        var bottomDialogTitle by remember { mutableStateOf("") }
        var bottomDialogWebUrl by remember { mutableStateOf<String?>(null) }
        var bottomDialogImage by remember { mutableStateOf<Uri?>(null) }

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) { googleMap ->
            this@MapScreen.googleMap = googleMap
            googleMap.setOnMapClickListener { bottomDialogVisible = false }
            googleMap.setOnMarkerClickListener { marker ->
                val markerCenteringEnabled = mapViewModel.centerMarkerOnClick
                if (markerCenteringEnabled.value) {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(
                                marker.position,
                                googleMap.cameraPosition.zoom
                            )
                        )
                    )
                }
                val markerTitle = marker.title
                if (markerTitle != null) {
                    bottomDialogTitle = markerTitle
                    bottomDialogVisible = true
                    val loadedImageUrl = marker.snippet?.let { snippet ->
                        val srcPos = snippet.indexOf("src=\"")
                            .takeIf { it >= 0 } ?: return@let false
                        val cutUrl = snippet.substring(srcPos + 5)
                        val endPos = cutUrl.indexOf('"')
                            .takeIf { it >= 0 } ?: return@let false
                        val imageUrl = cutUrl.substring(0, endPos)
                        bottomDialogImage = Uri.parse(imageUrl)
                        true
                    } ?: false
                    if (!loadedImageUrl)
                        bottomDialogImage = null

                    bottomDialogWebUrl = marker.snippet?.let { snippet ->
                        val index = snippet.indexOf("https://escalaralcoiaicomtat")
                            .takeIf { it >= 0 }
                            ?: return@let null
                        snippet.substring(index)
                    }

                    Timber.i("Marker snippet: ${marker.snippet}")
                }
                true
            }
        }

        MapBottomDialog(
            this@MapScreen,
            app,
            bottomDialogVisible,
            bottomDialogTitle,
            bottomDialogImage,
            bottomDialogWebUrl
        )

        if (googleMap != null && exploreViewModel.lastAreas.isNotEmpty())
            for (area in exploreViewModel.lastAreas)
                mapViewModel.loadGoogleMap(googleMap!!, area)
    }
    mapViewModel.locations.observe(this as LifecycleOwner) { locations ->
        try {
            val bounds = LatLngBounds.builder()
                .includeAll(locations)
                .build()

            Timber.i("Detected ${locations.size} locations in map.")
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 45))
        } catch (e: IllegalStateException) {
            Timber.e("No markers were loaded.")
        }
    }
    exploreViewModel.loadAreas()
}
