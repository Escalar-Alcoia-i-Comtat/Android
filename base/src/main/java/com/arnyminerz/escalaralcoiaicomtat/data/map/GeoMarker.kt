package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.Context
import android.graphics.Bitmap
import com.arnyminerz.escalaralcoiaicomtat.data.SerializableBitmap
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_MARKER_SIZE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.generateUUID
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
    private var iconImage: String? = null

    fun getIcon(): Bitmap? = icon?.bitmap

    fun withImage(style: Style, bitmap: Bitmap): GeoMarker {
        Timber.d("Setting image for GeoMarker...")
        Timber.d("Adding image to Style...")
        style.addImage(id, bitmap, false)
        Timber.d("Storing image to class...")
        icon = SerializableBitmap(bitmap)
        iconImage = id
        return this
    }

    fun withImage(image: String): GeoMarker {
        iconImage = image
        return this
    }

    fun addToMap(context: Context, symbolManager: SymbolManager): Symbol? {
        var symbolOptions = SymbolOptions()
            .withLatLng(LatLng(position.latitude, position.longitude))

        if (iconImage != null) {
            val iconSize = iconSize ?: mapFloat(
                SETTINGS_MARKER_SIZE_PREF.get(context.sharedPreferences)
                    .toFloat(),
                1f, 5f,
                .1f, 1.2f
            )
            symbolOptions = symbolOptions
                .withIconImage(iconImage)
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
fun Collection<GeoMarker>.addToMap(context: Context, symbolManager: SymbolManager): List<Symbol> {
    val symbols = arrayListOf<Symbol>()
    for (marker in this) {
        val symbol = marker.addToMap(context, symbolManager) ?: continue
        symbols.add(symbol)
    }
    return symbols.toList()
}