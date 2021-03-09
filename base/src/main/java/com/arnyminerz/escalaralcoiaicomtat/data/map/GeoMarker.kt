package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_MARKER_SIZE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteIfExists
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import timber.log.Timber
import java.io.File

const val ICON_SIZE_MULTIPLIER = .35f

private const val BITMAP_COMPRESSION = 100

private fun extractUUID(position: LatLng, iconSizeMultiplier: Float, windowData: MapObjectWindowData?): String {
    val text = position.latitude.toString() + position.longitude.toString() +
            iconSizeMultiplier.toString() + windowData?.title
    return Base64.encodeToString(text.toByteArray(), Base64.DEFAULT)
}

@Suppress("unused")
class GeoMarker(
    val position: LatLng,
    id: String? = null,
    var iconSizeMultiplier: Float = ICON_SIZE_MULTIPLIER,
    val windowData: MapObjectWindowData? = null
) : Parcelable {
    val id = id ?: extractUUID(position, iconSizeMultiplier, windowData)
    private var icon: GeoIcon? = null
    private var bitmap: Bitmap? = null
    private var iconLoaded: Boolean = false

    constructor(parcel: Parcel) : this(
        parcel.readParcelable<LatLng>(LatLng::class.java.classLoader)!!,
        parcel.readString()!!,
        parcel.readFloat(),
        if (parcel.readInt() == 1)
            MapObjectWindowData(
                parcel.readString()!!,
                parcel.readString()
            )
        else null
    ) {
        icon = parcel.readSerializable() as GeoIcon?
    }

    fun withImage(bitmap: Bitmap): GeoMarker {
        Timber.d("Setting image for GeoMarker...")
        this.bitmap = bitmap
        icon = GeoIcon(id)
        return this
    }

    fun withImage(icon: GeoIcon): GeoMarker {
        this.icon = icon
        iconLoaded = true
        return this
    }

    fun addToMap(context: Context, style: Style, symbolManager: SymbolManager): Symbol? {
        var symbolOptions = SymbolOptions()
            .withLatLng(LatLng(position.latitude, position.longitude))

        val imgFile = File(context.cacheDir, "$id.webp")
        if (bitmap != null) {
            Timber.d("Adding image to Style...")
            style.addImage(id, bitmap!!, false)
            Timber.d("Storing image to cache...")
            if (!imgFile.deleteIfExists())
                Timber.w("Could not delete already existing image file!")
            else {
                val stream = imgFile.outputStream()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    bitmap!!.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, BITMAP_COMPRESSION, stream)
                else
                    bitmap!!.compress(Bitmap.CompressFormat.WEBP, BITMAP_COMPRESSION, stream)
                stream.close()
                Timber.d("Stored image into $imgFile.")
            }
            iconLoaded = true
        }

        if (icon != null && !iconLoaded && imgFile.exists()) {
            Timber.d("Found a stored image file. Reading from it...")
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(imgFile.path, options)
            Timber.d("Adding image to style...")
            style.addImage(id, bitmap!!, false)
            iconLoaded = true
        }

        if (icon != null && iconLoaded) {
            Timber.d("Marker $id has an icon named ${icon!!.name}")
            val iconSize = SETTINGS_MARKER_SIZE_PREF.get(context.sharedPreferences) * iconSizeMultiplier
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
        if (windowData == null)
            dest.writeInt(0)
        else {
            dest.writeInt(1)
            dest.writeString(windowData.title)
            windowData.message?.let { dest.writeString(it) }
        }
        icon?.let { dest.writeSerializable(it) }
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

@ExperimentalUnsignedTypes
fun Collection<GeoMarker>.addToMap(context: Context, style: Style, symbolManager: SymbolManager): List<Symbol> {
    val symbols = arrayListOf<Symbol>()
    for (marker in this) {
        val symbol = marker.addToMap(context, style, symbolManager) ?: continue
        symbols.add(symbol)
    }
    return symbols.toList()
}
