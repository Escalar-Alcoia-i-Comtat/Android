package com.arnyminerz.escalaralcoiaicomtat.generic

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
}
