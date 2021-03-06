package com.arnyminerz.escalaralcoiaicomtat.core.data.map

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.core.utils.drawableToBitmap
import timber.log.Timber

/**
 * Represents an icon that will be displayed on a Map.
 * @author Arnau Mora
 * @since 20210604
 * @param name The name of the icon, can be anything.
 * @param icon The [Bitmap] that will be shown in the map.
 * @see GeoMarker
 */
class GeoIcon(val name: String, icon: Bitmap) : Parcelable {
    val icon: Bitmap = Bitmap.createScaledBitmap(icon, MARKER_SIZE, MARKER_SIZE, false)

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readParcelable(Bitmap::class.java.classLoader)!!
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(name)
        dest?.writeParcelable(icon, 0)
    }

    companion object CREATOR : Parcelable.Creator<GeoIcon> {
        override fun createFromParcel(parcel: Parcel): GeoIcon {
            return GeoIcon(parcel)
        }

        override fun newArray(size: Int): Array<GeoIcon?> {
            return arrayOfNulls(size)
        }
    }
}

data class GeoIconConstant(val name: String, @DrawableRes val drawable: Int) {
    fun toGeoIcon(context: Context): GeoIcon? {
        Timber.d("Converting GeoIconConstant to GeoIcon...")
        val drawable = ContextCompat.getDrawable(context, drawable)
        if (drawable == null) {
            Timber.w("Could not get drawable!")
            return null
        }
        val bitmap = drawableToBitmap(drawable)
        return GeoIcon(name, bitmap)
    }
}
