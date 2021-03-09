package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.getBooleanFromString
import org.json.JSONObject

@ExperimentalUnsignedTypes
data class RequiredSafesData(
    val lanyardRequired: Boolean,
    val crackerRequired: Boolean,
    val friendRequired: Boolean,
    val stripsRequired: Boolean,
    val pitonRequired: Boolean,
    val nailRequired: Boolean
) : SafesData {
    companion object {
        fun fromDB(obj: JSONObject): RequiredSafesData {
            return RequiredSafesData(
                obj.getBooleanFromString("lanyard_required"),
                obj.getBooleanFromString("cracker_required"),
                obj.getBooleanFromString("friend_required"),
                obj.getBooleanFromString("strips_required"),
                obj.getBooleanFromString("piton_required"),
                obj.getBooleanFromString("nail_required")
            )
        }
    }

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

    override fun count(): Int = 6
    override fun sum(): UInt = 0u

    @kotlin.jvm.Throws(ArrayIndexOutOfBoundsException::class)
    override operator fun get(index: Int): SafeCountData =
        arrayOf(
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
        )[index]

    fun any(): Boolean = lanyardRequired || crackerRequired || friendRequired || stripsRequired ||
            pitonRequired || nailRequired
}
