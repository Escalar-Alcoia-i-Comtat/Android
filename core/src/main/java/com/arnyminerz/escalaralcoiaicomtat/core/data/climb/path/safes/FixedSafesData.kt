package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.core.R

data class FixedSafesData(
    val stringCount: Long,
    val paraboltCount: Long,
    val spitCount: Long,
    val tensorCount: Long,
    val pitonCount: Long,
    val burilCount: Long
) : SafesData() {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong()
    )

    override fun list(): List<SafeCountData> =
        listOf(
            SafeCountData(
                paraboltCount,
                R.string.safe_parabolt,
                R.drawable.ic_parabolt
            ),
            SafeCountData(
                spitCount,
                R.string.safe_spit,
                R.drawable.ic_spit
            ),
            SafeCountData(
                tensorCount,
                R.string.safe_tensor,
                R.drawable.ic_tensor
            ),
            SafeCountData(
                pitonCount,
                R.string.safe_piton,
                R.drawable.ic_reunio_clau
            ),
            SafeCountData(
                burilCount,
                R.string.safe_buril,
                R.drawable.ic_buril
            )
        )

    override fun toJSONString(): String {
        return "{" +
                "\"string_count\":\"$stringCount\"," +
                "\"parabolt_count\":\"$paraboltCount\"," +
                "\"spit_count\":\"$spitCount\"," +
                "\"tensor_count\":\"$tensorCount\"," +
                "\"piton_count\":\"$pitonCount\"," +
                "\"buril_count\":\"$burilCount\"" +
                "}"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(stringCount)
        parcel.writeLong(paraboltCount)
        parcel.writeLong(spitCount)
        parcel.writeLong(tensorCount)
        parcel.writeLong(pitonCount)
        parcel.writeLong(burilCount)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FixedSafesData> {
        override fun createFromParcel(parcel: Parcel): FixedSafesData = FixedSafesData(parcel)

        override fun newArray(size: Int): Array<FixedSafesData?> = arrayOfNulls(size)
    }
}
