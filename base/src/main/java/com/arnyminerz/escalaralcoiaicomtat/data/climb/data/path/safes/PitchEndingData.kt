package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.safes

import android.os.Parcel
import android.os.Parcelable

class PitchEndingData(val orientation: PitchEndingOrientation, val rappel: PitchEndingRappel): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(PitchEndingOrientation::class.java.classLoader)!!,
        parcel.readParcelable(PitchEndingRappel::class.java.classLoader)!!
    )

    override fun toString(): String = dbValue()

    private fun dbValue(): String = "$orientation $rappel"

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(orientation, flags)
        parcel.writeParcelable(rappel, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PitchEndingData> {
        override fun createFromParcel(parcel: Parcel): PitchEndingData {
            return PitchEndingData(parcel)
        }

        override fun newArray(size: Int): Array<PitchEndingData?> {
            return arrayOfNulls(size)
        }
    }
}
