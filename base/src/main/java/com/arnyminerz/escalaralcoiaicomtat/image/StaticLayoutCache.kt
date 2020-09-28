package com.arnyminerz.escalaralcoiaicomtat.image

import android.text.StaticLayout
import androidx.core.util.lruCache

object StaticLayoutCache {

    private const val MAX_SIZE = 50 // Max number of cached items
    private val cache = lruCache<String, StaticLayout>(MAX_SIZE)

    operator fun set(key: String, staticLayout: StaticLayout) {
        cache.put(key, staticLayout)
    }

    operator fun get(key: String): StaticLayout? {
        return cache[key]
    }
}