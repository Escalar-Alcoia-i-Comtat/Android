package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler

import android.os.Parcel
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Pitch
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.PitchEndingData
import kotlinx.parcelize.Parceler

class PitchParceler : Parceler<Pitch> {
    override fun create(parcel: Parcel): Pitch =
        Pitch(endingData = parcel.readParcelable(PitchEndingData::class.java.classLoader)!!)

    override fun Pitch.write(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(this.endingData, flags)
    }
}