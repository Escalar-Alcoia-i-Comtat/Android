package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)

operator fun Char.times(times: Int): String {
    val builder = StringBuilder()

    for (c in 0 until times)
        builder.append(this)

    return builder.toString()
}

fun String?.toTimestamp(): Date? =
    this?.let {
        try {
            TIMESTAMP_FORMAT.parse(this)
        } catch (ex: ParseException) {
            null
        } catch (ex: ArrayIndexOutOfBoundsException) {
            null
        }
    }