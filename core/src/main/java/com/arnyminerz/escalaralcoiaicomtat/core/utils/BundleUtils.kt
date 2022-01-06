package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.os.Bundle
import android.os.Parcel

/**
 * Converts the [Bundle]'s contents into a map.
 * @author Arnau Mora
 * @since 20220106
 */
fun Bundle.toMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
    for (key in keySet())
        put(key, this@toMap.get(key))
}

/**
 * Returns the size in bytes of the [Bundle].
 * @author Arnau Mora
 * @since 20220106
 * @see <a href="https://stackoverflow.com/a/48447209">StackOverflow</a>
 */
fun Bundle.sizeInBytes(): Int {
    val parcel = Parcel.obtain()
    parcel.writeBundle(this)
    val size = parcel.dataSize()
    parcel.recycle()
    return size
}
