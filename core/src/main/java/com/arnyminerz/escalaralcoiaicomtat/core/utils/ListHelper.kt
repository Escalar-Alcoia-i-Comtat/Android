package com.arnyminerz.escalaralcoiaicomtat.core.utils

fun Collection<Boolean>.allTrue(): Boolean {
    for (i in this)
        if (!i)
            return false

    return true
}
