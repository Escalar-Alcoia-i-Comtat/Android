package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.UUID

fun generateUUID(): String = UUID.randomUUID().toString()

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

/**
 * Converts a drawable into a bitmap
 * @author Arnau Mora
 * @since 20210321
 * @param drawable The object to convert
 * @return The created bitmap
 */
fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable)
        return drawable.bitmap

    var width = drawable.intrinsicWidth
    width = if (width > 0) width else 1
    var height = drawable.intrinsicHeight
    height = if (height > 0) height else 1

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
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
fun Fragment.runOnUiThread(action: Activity.() -> Unit) =
    requireActivity().runOnUiThread { action(requireActivity()) }
