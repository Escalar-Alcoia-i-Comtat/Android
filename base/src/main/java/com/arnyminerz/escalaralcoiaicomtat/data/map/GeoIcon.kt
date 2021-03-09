package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes

abstract class GeoIcon(open val name: String) : Parcelable

data class GeoIconGeneric(override val name: String): GeoIcon(name) {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(name)
    }

    companion object CREATOR : Parcelable.Creator<GeoIconGeneric> {
        override fun createFromParcel(parcel: Parcel): GeoIconGeneric {
            return GeoIconGeneric(parcel)
        }

        override fun newArray(size: Int): Array<GeoIconGeneric?> {
            return arrayOfNulls(size)
        }
    }
}

data class GeoIconDrawable(override val name: String, @DrawableRes val icon: Int) : GeoIcon(name) {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt()
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(name)
        dest?.writeInt(icon)
    }

    companion object CREATOR : Parcelable.Creator<GeoIconDrawable> {
        override fun createFromParcel(parcel: Parcel): GeoIconDrawable {
            return GeoIconDrawable(parcel)
        }

        override fun newArray(size: Int): Array<GeoIconDrawable?> {
            return arrayOfNulls(size)
        }
    }
}
