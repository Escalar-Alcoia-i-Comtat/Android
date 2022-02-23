package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class PitchEndingData(val rappel: PitchEndingRappel, val orientation: PitchEndingOrientation) :
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(PitchEndingOrientation::class.java.classLoader)!!,
        parcel.readParcelable(PitchEndingRappel::class.java.classLoader)!!
    )

    override fun toString(): String = dbValue()

    private fun dbValue(): String = "$rappel $orientation"
}
