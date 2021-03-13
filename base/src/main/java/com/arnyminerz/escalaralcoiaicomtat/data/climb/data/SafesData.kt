package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.os.Parcelable

interface SafesData : Parcelable {
    /**
     * @return The amount of parameters there are
     */
    fun count(): Int

    /**
     * May not apply if using booleans
     * @return The total count of safes
     */
    fun sum(): Int

    operator fun get(index: Int): SafeCountData?

    fun toJSONString(): String
}
