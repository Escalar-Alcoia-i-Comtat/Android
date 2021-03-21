package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.path.safes

import android.os.Parcelable

/**
 * Specifies some kind of safes data.
 * @author Arnau Mora
 * @since 20210316
 */
abstract class SafesData : Parcelable, Iterable<SafeCountData> {
    /**
     * @author Arnau Mora
     * @since 20210316
     * @return The amount of parameters there are
     */
    fun count(): Int = list().size

    /**
     * Gets the SafeCountData at a index
     * @author Arnau Mora
     * @since 20210316
     */
    operator fun get(index: Int): SafeCountData = list()[index]

    override fun iterator(): Iterator<SafeCountData> =
        list().iterator()

    /**
     * @author Arnau Mora
     * @since 20210316
     * @return The total count of safes. If using booleans, it will be the amount of trues
     */
    fun sum(): Int {
        var count = 0
        for (i in this)
            count += i.count
        return count
    }

    abstract fun list(): List<SafeCountData>

    abstract fun toJSONString(): String
}
