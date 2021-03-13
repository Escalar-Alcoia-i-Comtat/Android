package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcel
import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.R

data class FixedSafesData(
    val stringCount: Int,
    val paraboltCount: Int,
    val spitCount: Int,
    val tensorCount: Int,
    val pitonCount: Int,
    val burilCount: Int
) : SafesData {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun count(): Int = 5

    override fun sum(): Int = paraboltCount + spitCount + tensorCount + pitonCount + burilCount

    @kotlin.jvm.Throws(ArrayIndexOutOfBoundsException::class)
    override operator fun get(index: Int): SafeCountData =
        arrayOf(
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
        )[index]

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
        parcel.writeInt(stringCount)
        parcel.writeInt(paraboltCount)
        parcel.writeInt(spitCount)
        parcel.writeInt(tensorCount)
        parcel.writeInt(pitonCount)
        parcel.writeInt(burilCount)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FixedSafesData> {
        override fun createFromParcel(parcel: Parcel): FixedSafesData = FixedSafesData(parcel)

        override fun newArray(size: Int): Array<FixedSafesData?> = arrayOfNulls(size)
    }
}
