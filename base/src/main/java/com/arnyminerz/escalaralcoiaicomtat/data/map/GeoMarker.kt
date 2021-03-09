package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_MARKER_SIZE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.generateUUID
import com.arnyminerz.escalaralcoiaicomtat.generic.mapFloat
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import timber.log.Timber

private const val ICON_SIZE_IN_MIN = 1f
private const val ICON_SIZE_IN_MAX = 5f
private const val ICON_SIZE_OUT_MIN = .1f
private const val ICON_SIZE_OUT_MAX = 1.2f

@Suppress("unused")
data class GeoMarker(
    val position: LatLng,
    val iconSize: Float? = null,
    val windowData: MapObjectWindowData? = null
) : Parcelable {
    val id = generateUUID()

    private var iconImage: String? = null

    constructor(parcel: Parcel) : this(
        parcel.readParcelable<LatLng>(LatLng::class.java.classLoader)!!,
        parcel.readFloat(),
        if (parcel.readInt() == 1)
            MapObjectWindowData(
                parcel.readString()!!,
                parcel.readString()
            )
        else null
    ) {
        iconImage = parcel.readString()
    }

    fun withImage(style: Style, bitmap: Bitmap): GeoMarker {
        Timber.d("Setting image for GeoMarker...")
        Timber.d("Adding image to Style...")
        style.addImage(id, bitmap, false)
        iconImage = id
        return this
    }

    fun withImage(icon: GeoIcon): GeoMarker {
        iconImage = icon.name
        return this
    }

    fun addToMap(context: Context, symbolManager: SymbolManager): Symbol? {
        var symbolOptions = SymbolOptions()
            .withLatLng(LatLng(position.latitude, position.longitude))

        if (iconImage != null) {
            Timber.d("Marker $id has an icon named $iconImage")
            val iconSize = iconSize ?: mapFloat(
                SETTINGS_MARKER_SIZE_PREF.get(context.sharedPreferences)
                    .toFloat(),
                ICON_SIZE_IN_MIN, ICON_SIZE_IN_MAX,
                ICON_SIZE_OUT_MIN, ICON_SIZE_OUT_MAX
            )
            symbolOptions = symbolOptions
                .withIconImage(iconImage)
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
        iconSize?.let { dest.writeFloat(it) }
        if (windowData == null)
            dest.writeInt(0)
        else {
            dest.writeInt(1)
            dest.writeString(windowData.title)
            windowData.message?.let { dest.writeString(it) }
        }
        iconImage?.let { dest.writeString(it) }
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
