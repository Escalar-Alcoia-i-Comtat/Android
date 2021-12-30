package com.arnyminerz.escalaralcoiaicomtat.core.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A composable Google Map.
 * @author Arnau Mora
 * @since 20211230
 * @param modifier Modifiers to apply to the map.
 * @param onLoad This will get called once the map has been loaded.
 */
@Composable
fun GoogleMap(modifier: Modifier = Modifier, onLoad: ((googleMap: GoogleMap) -> Unit)? = null) {
    val mapViewState = rememberMapViewWithLifecycle()
    val darkMode = isSystemInDarkTheme()
    val context = LocalContext.current

    AndroidView(
        { mapViewState },
        modifier
    ) { mapView ->
        CoroutineScope(Dispatchers.Main).launch {
            val map = mapView.awaitMap()
            if (darkMode)
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_night))
            onLoad?.let { it(map) }
        }
    }
}
