package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.core.ui.map.MapBottomDialog
import com.arnyminerz.escalaralcoiaicomtat.core.ui.map.MapView
import com.arnyminerz.escalaralcoiaicomtat.core.utils.computeCentroid
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import timber.log.Timber

@Composable
@SuppressLint("PotentialBehaviorOverride")
fun MainActivity.MapScreen() {
    var mapView: MapView? = null

    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        var bottomDialogVisible by remember { mutableStateOf(false) }
        var bottomDialogTitle by remember { mutableStateOf("") }
        var bottomDialogImage by remember { mutableStateOf<Uri?>(null) }

        MapView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) { map ->
            mapView = map

            map.setMultiTouchControls(true)
            map.maxZoomLevel = 18.0

            map.setOnTouchListener { view, _ ->
                view.performClick()
                bottomDialogVisible = false
                false
            }

            if (exploreViewModel.lastAreas.isNotEmpty())
                mapViewModel.loadAreasIntoMap(map, exploreViewModel.lastAreas) {
                    fun explodeFolder(folderOverlay: FolderOverlay) {
                        folderOverlay.items.forEach { overlayItem ->
                            // Keep exploding until a non-folder is obtained
                            when (overlayItem) {
                                is FolderOverlay -> explodeFolder(overlayItem)
                                is Marker -> overlayItem.setOnMarkerClickListener { marker, mapView ->
                                    // Center marker only if enabled
                                    val markerCenteringEnabled = mapViewModel.centerMarkerOnClick
                                    if (markerCenteringEnabled.value)
                                        mapView.zoomToBoundingBox(marker.bounds, true)

                                    // Show dialog
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

                                        Timber.i("Marker snippet: ${marker.snippet}")
                                    }

                                    false
                                }
                                else -> Timber.i("Overlay item type: ${overlayItem.javaClass.name}")
                            }
                        }
                    }

                    map.overlays.forEach { overlay ->
                        // Explode the folder overlay
                        (overlay as? FolderOverlay)?.let { explodeFolder(it) }
                    }
                }
            else Timber.w("Won't load KMZs since lastAreas is not loaded")
        }

        AnimatedVisibility(
            visible = bottomDialogVisible,
            enter = slideInVertically { with(density) { 40.dp.roundToPx() } } +
                    fadeIn(initialAlpha = .3f),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
        ) {
            MapBottomDialog(
                DataClassActivity::class.java,
                bottomDialogTitle,
                bottomDialogImage,
            )
        }
    }
    mapViewModel.locations.observe(this as LifecycleOwner) { locations ->
        try {
            Timber.i("Detected ${locations.size} locations in map.")
            // TODO: Adjust zoom
            mapView
                ?.controller
                ?.animateTo(locations.computeCentroid())
        } catch (e: IllegalStateException) {
            Timber.e("No markers were loaded.")
        }
    }
    exploreViewModel.loadAreas()
}
