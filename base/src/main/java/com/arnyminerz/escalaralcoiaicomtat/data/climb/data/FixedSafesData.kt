package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.getInt
import org.json.JSONException
import org.json.JSONObject

@ExperimentalUnsignedTypes
data class FixedSafesData(
    val stringCount: UInt,
    val paraboltCount: UInt,
    val spitCount: UInt,
    val tensorCount: UInt,
    val pitonCount: UInt,
    val burilCount: UInt
) : SafesData {
    companion object {
        @Throws(JSONException::class)
        fun fromJSON(json: JSONObject): FixedSafesData {
            return FixedSafesData(
                json.getInt("string_count", 0).toUInt(),
                json.getInt("parabolt_count", 0).toUInt(),
                json.getInt("spit_count", 0).toUInt(),
                json.getInt("tensor_count", 0).toUInt(),
                json.getInt("piton_count", 0).toUInt(),
                json.getInt("buril_count", 0).toUInt()
            )
        }

        fun fromDB(obj: JSONObject): FixedSafesData {
            return FixedSafesData(
                obj.getInt("string_count", 0).toUInt(),
                obj.getInt("parabolt_count", 0).toUInt(),
                obj.getInt("spit_count", 0).toUInt(),
                obj.getInt("tensor_count", 0).toUInt(),
                obj.getInt("piton_count", 0).toUInt(),
                obj.getInt("buril_count", 0).toUInt()
            )
        }
    }

    override fun count(): Int = 5

    override fun sum(): UInt {
        return paraboltCount + spitCount + tensorCount + pitonCount + burilCount
    }

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
}
