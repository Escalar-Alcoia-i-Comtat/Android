package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

fun generateUUID(): String {
    return UUID.randomUUID().toString()
}

fun Boolean.toInt(): Int =
    if (this) 1 else 0

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

fun Int.drawable(context: Context) = ContextCompat.getDrawable(context, this)

fun runAsync(call: () -> Unit) =
    CoroutineScope(Dispatchers.IO).launch {
        runCatching(call)
    }

fun mapFloat(x: Float, in_min: Float, in_max: Float, out_min: Float, out_max: Float): Float =
    (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min