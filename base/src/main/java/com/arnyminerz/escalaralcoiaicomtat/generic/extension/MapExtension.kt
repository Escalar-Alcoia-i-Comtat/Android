package com.arnyminerz.escalaralcoiaicomtat.generic.extension

inline fun <A, reified B> Map<*, *>.castingGet(key: A): B? {
    for (k in keys)
        if (k == key)
            get(k).let { item ->
                return if (item is B)
                    item
                else null
            }

    return null
}