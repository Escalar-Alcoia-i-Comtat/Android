package com.arnyminerz.escalaralcoiaicomtat.core.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource
import org.osmdroid.views.MapView

/**
 * A composable Google Map.
 * @author Arnau Mora
 * @since 20211230
 * @param bingMapsKey The key for using bing maps.
 * @param modifier Modifiers to apply to the map.
 * @param onLoad This will get called once the map has been loaded.
 */
@Composable
fun MapView(
    bingMapsKey: String?,
    modifier: Modifier = Modifier,
    onLoad: ((map: MapView) -> Unit)? = null
) {
    val mapViewState = rememberMapViewWithLifecycle()

    AndroidView(
        { mapViewState },
        modifier
    ) { mapView ->
        BingMapTileSource.setBingKey(bingMapsKey)
        val bing = BingMapTileSource(null)
        bing.style = BingMapTileSource.IMAGERYSET_AERIALWITHLABELS
        mapView.setTileSource(bing)

        onLoad?.invoke(mapView)
    }
}
