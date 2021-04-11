package com.arnyminerz.escalaralcoiaicomtat.data.climb.path.safes

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.R

data class RequiredSafesData(
    val lanyardRequired: Boolean,
    val crackerRequired: Boolean,
    val friendRequired: Boolean,
    val stripsRequired: Boolean,
    val pitonRequired: Boolean,
    val nailRequired: Boolean
) : SafesData() {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun toJSONString(): String {
        return "{" +
                "\"lanyard_required\":\"$lanyardRequired\"," +
                "\"cracker_required\":\"$crackerRequired\"," +
                "\"friend_required\":\"$friendRequired\"," +
                "\"strips_required\":\"$stripsRequired\"," +
                "\"piton_required\":\"$pitonRequired\"," +
                "\"nail_required\":\"$nailRequired\"" +
                "}"
    }

    override fun list(): List<SafeCountData> =
        listOf(
            SafeCountData(
                lanyardRequired,
                R.string.safe_lanyard,
                R.drawable.ic_lanyard
            ),
            SafeCountData(
                crackerRequired,
                R.string.safe_cracker,
                R.drawable.ic_cracker
            ),
            SafeCountData(
                friendRequired,
                R.string.safe_friend,
                R.drawable.ic_friend
            ),
            SafeCountData(
                stripsRequired,
                R.string.safe_strips,
                R.drawable.ic_strips
            ),
            SafeCountData(
                pitonRequired,
                R.string.safe_piton,
                R.drawable.ic_buril
            ),
            SafeCountData(
                nailRequired,
                R.string.safe_nail,
                R.drawable.ic_ungla
            )
        )

    fun any(): Boolean = lanyardRequired || crackerRequired || friendRequired || stripsRequired ||
            pitonRequired || nailRequired

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (lanyardRequired) 1 else 0)
        parcel.writeByte(if (crackerRequired) 1 else 0)
        parcel.writeByte(if (friendRequired) 1 else 0)
        parcel.writeByte(if (stripsRequired) 1 else 0)
        parcel.writeByte(if (pitonRequired) 1 else 0)
        parcel.writeByte(if (nailRequired) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<RequiredSafesData> {
        override fun createFromParcel(parcel: Parcel): RequiredSafesData =
            RequiredSafesData(parcel)

        override fun newArray(size: Int): Array<RequiredSafesData?> = arrayOfNulls(size)
    }
}
