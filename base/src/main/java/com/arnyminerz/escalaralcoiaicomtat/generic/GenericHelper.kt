package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


/**
 * Alias for ?.isNull()
 */
fun Any?.n(): Boolean = isNull()

fun Any?.isNull(): Boolean {
    return !isNotNull()
}

fun Any?.isNotNull(): Boolean {
    return if (this is String?)
        this != null && this != "null"
    else this != null
}

fun Collection<Any?>.nonNull(): Boolean {
    for (item in this)
        if (item.isNull()) return false
    return true
}

fun computeCentroid(points: List<LatLng>): LatLng {
    var latitude = 0.0
    var longitude = 0.0
    val n = points.size

    for (point in points) {
        latitude += point.latitude
        longitude += point.longitude
    }

    return LatLng(latitude / n, longitude / n)
}

fun polygonContains(test: LatLng, points: MutableList<LatLng>): Boolean {
    var result = false
    var i = 0
    var j = points.size - 1
    while (i < points.size) {
        if (points[i].longitude > test.longitude != points[j].longitude > test.longitude &&
            test.latitude < (points[j].latitude - points[i].latitude) * (test.longitude - points[i].longitude) / (points[j].longitude - points[i].longitude) + points[i].latitude
        ) {
            result = !result
        }
        j = i++
    }
    return result
}

fun generateUUID(): String {
    return UUID.randomUUID().toString()
}

fun Boolean.toInt(): Int =
    if (this) 1 else 0

fun Int.toBoolean(): Boolean =
    this == 1

fun Long.toBoolean(): Boolean =
    this == 1L

fun String.toBoolean(): Boolean =
    equals("true", true) || equals("1")

/**
 * This method converts dp unit to equivalent pixels, depending on device density.
 *
 * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
 * @param context Context to get resources and device specific display metrics
 * @return A float value to represent px equivalent to dp depending on device density
 */
fun convertDpToPixel(dp: Float, context: Context): Float {
    return dp * (context.resources
        .displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

@Suppress("DEPRECATION")
fun getDisplaySize(activity: Activity): Pair<Int, Int> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        Pair(bounds.width(), bounds.height())
    } else {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified E> Context.getSystemService(service: String): E? =
    getSystemService(service).let {
        if (it is E)
            it
        else null
    }

fun Int.drawable(context: Context) = ContextCompat.getDrawable(context, this)

fun runAsync(call: () -> Unit) =
    CoroutineScope(Dispatchers.IO).launch {
        runCatching(call)
    }