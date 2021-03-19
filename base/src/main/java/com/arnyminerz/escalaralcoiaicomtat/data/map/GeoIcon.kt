package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.collection.arrayMapOf
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.generic.MapHelper
import timber.log.Timber

val addedIcons = arrayMapOf<MapHelper, String>()

class GeoIcon(val name: String, val icon: Bitmap) : Parcelable {
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

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable)
        return drawable.bitmap

    var width = drawable.intrinsicWidth
    width = if (width > 0) width else 1
    var height = drawable.intrinsicHeight
    height = if (height > 0) height else 1

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
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
