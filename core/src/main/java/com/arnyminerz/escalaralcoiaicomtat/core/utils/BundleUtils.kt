package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.os.Bundle

/**
 * Converts the [Bundle]'s contents into a map.
 * @author Arnau Mora
 * @since 20220106
 */
fun Bundle.toMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
    for (key in keySet())
        put(key, this@toMap.get(key))
}
