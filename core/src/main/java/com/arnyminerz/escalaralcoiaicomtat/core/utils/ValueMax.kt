package com.arnyminerz.escalaralcoiaicomtat.core.utils

import androidx.annotation.FloatRange
import com.google.firebase.storage.StreamDownloadTask

data class ValueMax<T : Number>
/**
 * Initializes the ValueMax class
 * @author Arnau Mora
 * @since 20210313
 * @param value The current value
 * @param max The maximum value
 * @throws IllegalStateException When value is greater than max
 */
@Throws(IllegalStateException::class)
constructor(val value: T, val max: T) {
    fun <R> let(block: (value: T, max: T) -> R) =
        block(value, max)

    /**
     * Returns the percentage of [value] in [max].
     * @author Arnau Mora
     * @since 20210430
     */
    val percentage: Int
        get() = ((value.toDouble() / max.toDouble()) * 100.0).toInt()

    /**
     * Converts the proportion between [value] and [max] to a float. This is, a value between 0 and
     * 1.
     * @author Arnau Mora
     * @since 20210810
     */
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
    val toFloat: Float = (value.toDouble() / max.toDouble()).toFloat()
}

/**
 * Converts the [ValueMax] to a [Int]-based one, using the [ValueMax.percentage] function.
 * @author Arnau Mora
 * @since 20210928
 * @return A new instance of [ValueMax] that uses the percentage as value and 100 as the max.
 */
fun ValueMax<Long>.toInt(): ValueMax<Int> = ValueMax(percentage, 100)

/**
 * Gets a [ValueMax] instance from the current progress of the task.
 * @author Arnau Mora
 * @since 20210926
 */
fun StreamDownloadTask.TaskSnapshot.progress(): ValueMax<Long> =
    ValueMax(bytesTransferred, totalByteCount)
