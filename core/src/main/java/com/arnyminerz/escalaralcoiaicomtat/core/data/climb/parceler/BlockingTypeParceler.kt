package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.parceler

import android.os.Parcel
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingType
import kotlinx.parcelize.Parceler

class BlockingTypeParceler : Parceler<BlockingType> {
    override fun create(parcel: Parcel): BlockingType = BlockingType.find(parcel.readString())

    override fun BlockingType.write(parcel: Parcel, flags: Int) {
        parcel.writeString(idName)
    }
}