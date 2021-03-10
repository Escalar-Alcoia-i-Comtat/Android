package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes

class GeoIcon(val name: String, val icon: Bitmap) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readParcelable(Bitmap::class.java.classLoader)!!
    )

    constructor(name: String, resources: Resources, @DrawableRes res: Int) : this(
        name,
        BitmapFactory.decodeResource(resources, res)
    )

    constructor(resources: Resources, constant: GeoIconConstant) : this(
        constant.name,
        resources,
        constant.drawable
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

data class GeoIconConstant(val name: String, @DrawableRes val drawable: Int)
