package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import com.mapbox.mapboxsdk.geometry.LatLng
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        }
    }

/**
 * Converts the [String] into a [LatLng].
 * The format must be <latitude>,<longitude>
 * @author Arnau Mora
 * @since 20210411
 * @return a new [LatLng], or null if [String] is null.
 * @throws ParseException If there was an error while parsing the [LatLng]
 * @throws NumberFormatException If the latitude or longitude are not a number
 */
fun String?.toLatLng(): LatLng? =
    this?.replace(" ", "")?.let {
        var comma = it.indexOf(',')
        if (comma < 0)
            throw ParseException(
                "There is no \",\" in \"$it\". The format to follow is <latitude>,<longitude>.",
                0
            )
        val latitude = it.substring(0, comma).toDouble()
        val longitude = it.substring(++comma).toDouble()
        LatLng(latitude, longitude)
    }
