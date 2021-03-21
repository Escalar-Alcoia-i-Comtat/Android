package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.safes

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class SafeCountData(
    val count: Int,
    @StringRes val displayName: Int,
    @DrawableRes val image: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    constructor(required: Boolean, @StringRes displayName: Int, @DrawableRes image: Int) :
            this(if (required) 1 else 0, displayName, image)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(count)
        parcel.writeInt(displayName)
        parcel.writeInt(image)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SafeCountData> {
        override fun createFromParcel(parcel: Parcel): SafeCountData {
            return SafeCountData(parcel)
        }

        override fun newArray(size: Int): Array<SafeCountData?> {
            return arrayOfNulls(size)
        }
    }
}
