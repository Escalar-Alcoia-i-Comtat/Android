package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import java.io.Serializable

@ExperimentalUnsignedTypes
interface SafesData : Serializable {
    /**
     * @return The amount of parameters there are
     */
    fun count(): Int

    /**
     * May not apply if using booleans
     * @return The total count of safes
     */
    fun sum(): UInt

    operator fun get(index: Int): SafeCountData?

    fun toJSONString(): String
}
