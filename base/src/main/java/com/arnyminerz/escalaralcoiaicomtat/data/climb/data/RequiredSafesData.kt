package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.getBooleanFromString
import org.json.JSONException
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
        @Throws(JSONException::class)
        fun fromJSON(json: JSONObject): RequiredSafesData {
            // This is for compatibility for v1.0.0-pre8-
            val pitonRequired = if (json.has("piton_required"))
                json.getBooleanFromString("piton_required") else false
            val nailRequired = if (json.has("nail_required"))
                json.getBooleanFromString("nail_required") else false
            return RequiredSafesData(
                json.getBooleanFromString("lanyard_required"),
                json.getBooleanFromString("cracker_required"),
                json.getBooleanFromString("friend_required"),
                json.getBooleanFromString("strips_required"),
                pitonRequired,
                nailRequired
            )
        }

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

        @Throws(JSONException::class)
        fun transformOldSafesData(json: JSONObject): JSONObject {
            return json.put("required_safes_data", fromJSON(json))
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

    override operator fun get(index: Int): SafeCountData? {
        return when (index) {
            0 -> SafeCountData(
                lanyardRequired,
                R.string.safe_lanyard,
                R.drawable.ic_lanyard
            )
            1 -> SafeCountData(
                crackerRequired,
                R.string.safe_cracker,
                R.drawable.ic_cracker
            )
            2 -> SafeCountData(
                friendRequired,
                R.string.safe_friend,
                R.drawable.ic_friend
            )
            3 -> SafeCountData(
                stripsRequired,
                R.string.safe_strips,
                R.drawable.ic_strips
            )
            4 -> SafeCountData(
                pitonRequired,
                R.string.safe_piton,
                R.drawable.ic_buril
            )
            5 -> SafeCountData(
                nailRequired,
                R.string.safe_nail,
                R.drawable.ic_ungla
            )
            else -> null
        }
    }

    fun any(): Boolean = lanyardRequired || crackerRequired || friendRequired || stripsRequired || pitonRequired || nailRequired
}