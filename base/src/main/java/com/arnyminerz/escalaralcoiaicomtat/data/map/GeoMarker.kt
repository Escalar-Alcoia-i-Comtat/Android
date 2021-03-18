package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_MARKER_SIZE_PREF
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import timber.log.Timber

const val ICON_SIZE_MULTIPLIER = .35f

private const val BITMAP_COMPRESSION = 100

private fun extractUUID(
    position: LatLng,
    iconSizeMultiplier: Float,
    windowData: MapObjectWindowData?
): String {
    val text = position.latitude.toString() + position.longitude.toString() +
            iconSizeMultiplier.toString() + windowData?.title
    return Base64.encodeToString(text.toByteArray(), Base64.DEFAULT)
}

@Suppress("unused")
class GeoMarker(
    val position: LatLng,
    id: String? = null,
    var iconSizeMultiplier: Float = ICON_SIZE_MULTIPLIER,
    val windowData: MapObjectWindowData? = null,
    icon: GeoIcon? = null
) : Parcelable {
    val id = id ?: extractUUID(position, iconSizeMultiplier, windowData)
    var icon: GeoIcon? = icon
        private set
    private var iconLoaded: Boolean = false

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(LatLng::class.java.classLoader)!!,
        parcel.readString()!!,
        parcel.readFloat(),
        parcel.readParcelable(MapObjectWindowData::class.java.classLoader),
        parcel.readParcelable(GeoIcon::class.java.classLoader),
    )

    fun withImage(bitmap: Bitmap): GeoMarker =
        withImage(GeoIcon(id, bitmap))

    fun withImage(icon: GeoIcon): GeoMarker {
        Timber.d("Setting image for GeoMarker...")
        this.icon = icon
        iconLoaded = true
        return this
    }

    fun addToMap(context: Context, style: Style, symbolManager: SymbolManager): Symbol? {
        var symbolOptions = SymbolOptions()
            .withLatLng(LatLng(position.latitude, position.longitude))

        if (icon != null) {
            Timber.d("Adding image to Style...")
            style.addImage(id, icon!!.icon, false)
            iconLoaded = true
        }

        if (icon != null && iconLoaded) {
            Timber.d("Marker $id has an icon named ${icon!!.name}")
            val iconSize =
                SETTINGS_MARKER_SIZE_PREF.get(context.sharedPreferences) * iconSizeMultiplier
            symbolOptions = symbolOptions
                .withIconImage(icon!!.name)
                .withIconSize(iconSize)
        } else
            Timber.d("Marker $id doesn't have an icon.")

        if (windowData != null)
            symbolOptions.withData(windowData.data())

        return symbolManager.create(symbolOptions)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(position, 0)
        dest.writeString(id)
        dest.writeFloat(iconSizeMultiplier)
        dest.writeParcelable(windowData, 0)
        dest.writeParcelable(icon, 0)
    }

    companion object CREATOR : Parcelable.Creator<GeoMarker> {
        override fun createFromParcel(parcel: Parcel): GeoMarker {
            return GeoMarker(parcel)
        }

        override fun newArray(size: Int): Array<GeoMarker?> {
            return arrayOfNulls(size)
        }
    }
}

fun Symbol.getWindow(): MapObjectWindowData =
    load(this)

fun Collection<GeoMarker>.addToMap(
    context: Context,
    style: Style,
    symbolManager: SymbolManager
): List<Symbol> {
    val symbols = arrayListOf<Symbol>()
    for (marker in this) {
        val symbol = marker.addToMap(context, style, symbolManager) ?: continue
        symbols.add(symbol)
    }
    return symbols.toList()
}
