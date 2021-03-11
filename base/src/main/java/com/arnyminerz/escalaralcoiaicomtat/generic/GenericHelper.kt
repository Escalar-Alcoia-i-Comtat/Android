package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

fun generateUUID(): String {
    return UUID.randomUUID().toString()
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

fun Int.drawable(context: Context) = ContextCompat.getDrawable(context, this)

fun runAsync(call: () -> Unit) =
    CoroutineScope(Dispatchers.IO).launch {
        runCatching(call)
    }

/**
 * Runs the action in the UI thread.
 * @author Arnau Mora
 * @since 20210311
 * @param action The runnable to execute
 * @see Activity.runOnUiThread
 * @see Fragment.getActivity
 * @throws IllegalStateException If not currently associated with an activity or if associated only with a context
 */
@Throws(IllegalStateException::class)
fun Fragment.runOnUiThread(action: Activity.() -> Unit) = requireActivity().runOnUiThread { action(requireActivity()) }
