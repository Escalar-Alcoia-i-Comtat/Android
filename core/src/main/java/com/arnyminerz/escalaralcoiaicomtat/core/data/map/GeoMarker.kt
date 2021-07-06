package com.arnyminerz.escalaralcoiaicomtat.core.data.map

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.core.utils.maps.MapHelper
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import timber.log.Timber
import java.io.InvalidObjectException

@Suppress("unused")
class GeoMarker(
    val position: LatLng,
    id: String? = null,
    val windowData: MapObjectWindowData? = null,
    var icon: GeoIcon
) : Parcelable {
    val id = id ?: extractUUID(position, windowData)

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(LatLng::class.java.classLoader)!!,
        parcel.readString()!!,
        parcel.readParcelable(MapObjectWindowData::class.java.classLoader),
        parcel.readParcelable(GeoIcon::class.java.classLoader)!!,
    )

    fun withImage(bitmap: Bitmap, id: String? = null): GeoMarker =
        withImage(GeoIcon(id ?: this.id, bitmap))

    fun withImage(icon: GeoIcon): GeoMarker {
        Timber.d("Setting image for GeoMarker...")
        this.icon = icon
        return this
    }

    fun addToMap(mapHelper: MapHelper): Marker? {
        val symbolOptions = MarkerOptions()
            .position(position)
            .title(windowData?.title ?: "")
            .icon(BitmapDescriptorFactory.fromBitmap(icon.icon))

        return mapHelper.createMarker(symbolOptions, windowData)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(position, 0)
        dest.writeString(id)
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

        private fun extractUUID(
            position: LatLng,
            windowData: MapObjectWindowData?
        ): String {
            val text = position.latitude.toString() + position.longitude.toString() +
                    windowData?.title
            return Base64.encodeToString(text.toByteArray(), Base64.DEFAULT)
        }
    }
}

/**
 * Extracts [MapObjectWindowData] from the marker.
 * @author Arnau Mora
 * @since 20210602
 * @throws InvalidObjectException When the marker doesn't have any data (tag).
 */
@UiThread
fun Marker.getWindow(): MapObjectWindowData = load(this)

fun Collection<GeoMarker>.addToMap(mapHelper: MapHelper): List<Marker> {
    val symbols = arrayListOf<Marker>()
    for (marker in this)
        marker.addToMap(mapHelper)?.let {
            symbols.add(it)
        }
    return symbols.toList()
}
