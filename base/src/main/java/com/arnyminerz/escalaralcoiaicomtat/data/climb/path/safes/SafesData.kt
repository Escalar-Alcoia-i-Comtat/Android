package com.arnyminerz.escalaralcoiaicomtat.data.climb.path.safes

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

    /**
     * Checks if there's at least one count of safes.
     * @author Arnau Mora
     * @since 20210406
     * @return If there's a safes count.
     */
    fun hasSafeCount(): Boolean {
        for ((_, value) in this)
            if (value > 1)
                return true

        return false
    }

    abstract fun list(): List<SafeCountData>

    abstract fun toJSONString(): String
}
