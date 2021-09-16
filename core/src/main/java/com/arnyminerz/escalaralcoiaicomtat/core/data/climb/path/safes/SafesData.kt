package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes

import android.os.Parcelable
import androidx.annotation.ColorRes

/**
 * Specifies some kind of safes data.
 * @author Arnau Mora
 * @since 20210316
 */
abstract class SafesData : Parcelable, Iterable<SafeCountData> {
    /**
     * The color that will be shown on the card's background when displaying the data to the user.
     * @author Arnau Mora
     * @since 20210916
     */
    @get:ColorRes
    abstract val color: Int

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
     * @return The attribute at [index] of [list].
     * @see list
     */
    operator fun get(index: Int): SafeCountData = list()[index]

    override fun iterator(): Iterator<SafeCountData> = list().iterator()

    /**
     * @author Arnau Mora
     * @since 20210316
     * @return The total count of safes. If using booleans, it will be the amount of trues
     */
    fun sum(): Long {
        var count: Long = 0
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

    /**
     * Returns the class as a [SafeCountData] which can be used for displaying in the UI easily.
     * @author Arnau Mora
     * @since 20210916
     * @return A list of [SafeCountData] for each of the class' attributes.
     */
    abstract fun list(): List<SafeCountData>

    /**
     * Converts the class to a JSON-formatted [String].
     * @author Arnau Mora
     * @since 20210916
     * @return The class as a JSON-formatted [String].
     */
    abstract fun toJSONString(): String
}
