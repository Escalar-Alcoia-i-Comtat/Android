package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.*

val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)

operator fun Char.times(times: Int): String {
    val builder = StringBuilder()

    for (c in 0 until times)
        builder.append(this)

    return builder.toString()
}

fun String.isEmail(): Boolean =
    !isEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
