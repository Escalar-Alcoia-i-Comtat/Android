package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.data.SerializableBitmap
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_MARKER_SIZE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.drawableToBitmap
import com.arnyminerz.escalaralcoiaicomtat.generic.generateUUID
import com.arnyminerz.escalaralcoiaicomtat.generic.isNotNull
import com.arnyminerz.escalaralcoiaicomtat.generic.mapFloat
import com.arnyminerz.escalaralcoiaicomtat.location.SerializableLatLng
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import timber.log.Timber
import java.io.Serializable

@Suppress("unused")
data class GeoMarker(
    val position: SerializableLatLng,
    val iconSize: Float? = null,
    val windowData: MapObjectWindowData? = null
) : Serializable {
    val id = generateUUID()

    private var icon: SerializableBitmap? = null

    fun getIcon(): Bitmap? = icon?.bitmap

    /**
     * Sets an icon for the marker.
     * @param context The context to call from
     * @param style The Mapbox map style
     * @param drawable The icon to set
     * @return The same GeoMarker instance updated
     * @throws UnsupportedOperationException When the drawable could not be converted to bitmap
     */
    @Throws(UnsupportedOperationException::class)
    fun withImage(context: Context, style: Style, @DrawableRes drawable: Int): GeoMarker {
        ContextCompat.getDrawable(context, drawable)?.let {
            val bitmap = drawableToBitmap(it)
            if (bitmap != null) {
                style.addImage(id, bitmap, true)
                icon = SerializableBitmap(bitmap)
            } else
                throw UnsupportedOperationException("Could not convert drawable to bitmap")
        } ?: Timber.e("Could not find drawable image.")
        return this
    }

    fun withImage(style: Style, bitmap: Bitmap): GeoMarker {
        style.addImage(id, bitmap, false)
        icon = SerializableBitmap(bitmap)
        return this
    }

    fun addToMap(context: Context, symbolManager: SymbolManager): Symbol? {
        var symbolOptions = SymbolOptions()
            .withLatLng(LatLng(position.latitude, position.longitude))

        if (icon.isNotNull()) {
            val iconSize = iconSize ?: mapFloat(
                SETTINGS_MARKER_SIZE_PREF.get(context.sharedPreferences)
                    .toFloat(),
                1f, 5f,
                .1f, 2.5f
            )
            symbolOptions = symbolOptions
                .withIconImage(id)
                .withIconSize(iconSize)
        }

        if (windowData != null)
            symbolOptions.withData(windowData.data())

        return symbolManager.create(symbolOptions)
    }
}

fun Symbol.getWindow(): MapObjectWindowData =
    MapObjectWindowData.load(this)

@ExperimentalUnsignedTypes
fun Collection<GeoMarker>.addToMap(context: Context, symbolManager: SymbolManager) {
    for (marker in this)
        marker.addToMap(context, symbolManager)
}