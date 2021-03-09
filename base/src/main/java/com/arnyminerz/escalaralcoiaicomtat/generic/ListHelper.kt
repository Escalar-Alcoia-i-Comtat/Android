package com.arnyminerz.escalaralcoiaicomtat.generic

fun Collection<Boolean>.allTrue(): Boolean {
    for (i in this)
        if (!i)
            return false

    return true
}
