package com.arnyminerz.escalaralcoiaicomtat.core.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.views.MapView

/**
 * A composable MapView.
 * @author Arnau Mora
 * @since 20211230
 * @param modifier Modifiers to apply to the map.
 * @param onCreate This will get called once the map view's onCreate method is called.
 * @param onUpdate This will get called whenever the map is updated.
 */
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    onCreate: ((mapView: MapView) -> Unit)? = null,
    onUpdate: ((mapView: MapView) -> Unit)? = null,
) {
    val mapViewState = rememberMapViewWithLifecycle { onCreate?.invoke(it) }

    AndroidView(
        { mapViewState },
        modifier
    ) { mapView -> onUpdate?.invoke(mapView) }
}
