package com.arnyminerz.escalaralcoiaicomtat.core.utils

fun Collection<Boolean>.allTrue(): Boolean {
    for (i in this)
        if (!i)
            return false

    return true
}

/**
 * Adds [value] at [key] and returns the updated map.
 * @author Arnau Mora
 * @since 20220314
 * @param key The key to add the value to.
 * @param value The value to set.
 */
fun <A, B> MutableMap<A, B>.append(key: A, value: B): MutableMap<A, B> {
    put(key, value)
    return this
}
