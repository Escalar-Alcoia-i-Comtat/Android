package com.arnyminerz.escalaralcoiaicomtat.core.utils

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
    fun percentage(): Int =
        ((value.toDouble() / max.toDouble()) * 100.0).toInt()
}
