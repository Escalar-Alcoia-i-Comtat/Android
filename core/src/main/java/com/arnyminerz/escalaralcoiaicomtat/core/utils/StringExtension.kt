package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.util.Patterns

operator fun Char.times(times: Int): String {
    val builder = StringBuilder()

    for (c in 0 until times)
        builder.append(this)

    return builder.toString()
}

fun String.isEmail(): Boolean =
    !isEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
