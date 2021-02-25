package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.activity.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.data.SerializableBitmap
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_MARKER_SIZE_PREF
import com.arnyminerz.escalaralcoiaicomtat.generic.drawableToBitmap
import com.arnyminerz.escalaralcoiaicomtat.generic.generateUUID
import com.arnyminerz.escalaralcoiaicomtat.generic.isNotNull
import com.arnyminerz.escalaralcoiaicomtat.location.SerializableLatLng
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.MarkerOptions
import timber.log.Timber
import java.io.Serializable

@Suppress("unused")
data class GeoMarker(
    val position: SerializableLatLng,
    val iconSize: Float = 2f,
    val windowData: MapObjectWindowData? = null
) : Serializable {
    val id = generateUUID()

    private var icon: SerializableBitmap? = null

    fun getIcon(): Bitmap? = icon?.bitmap

    fun withImage(context: Context, @DrawableRes drawable: Int): GeoMarker {
        ContextCompat.getDrawable(context, drawable)?.let {
            val bitmap = drawableToBitmap(it)
            if (bitmap != null)
                icon = SerializableBitmap(bitmap)
        } ?: Timber.e("Could not find drawable image.")
        return this
    }

    fun withImage(bitmap: Bitmap): GeoMarker {
        icon = SerializableBitmap(bitmap)
        return this
    }

    @ExperimentalUnsignedTypes
    fun addToMap(googleMap: GoogleMap) {
        val marker = MarkerOptions()
            .position(position.toLatLng())

        if (icon.isNotNull())
            icon!!.let { fullSizeBitmap ->
                val size = (30 * SETTINGS_MARKER_SIZE_PREF.get(sharedPreferences).toFloat()).toInt()
                marker.icon(
                    BitmapDescriptorFactory.fromBitmap(
                        Bitmap.createScaledBitmap(
                            fullSizeBitmap.bitmap,
                            size,
                            size,
                            false
                        )
                    )
                )
            }

        if (windowData != null)
            marker.apply {
                snippet(windowData.message)
                title(windowData.title)
            }

        googleMap.addMarker(marker).apply {
            tag = id
        }
    }
}

@ExperimentalUnsignedTypes
fun Collection<GeoMarker>.addToMap(googleMap: GoogleMap) {
    for (marker in this)
        marker.addToMap(googleMap)
}