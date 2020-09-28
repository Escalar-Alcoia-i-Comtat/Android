package com.arnyminerz.escalaralcoiaicomtat.view

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import com.arnyminerz.escalaralcoiaicomtat.generic.isNotNull
import com.arnyminerz.escalaralcoiaicomtat.list.ViewList
import org.jetbrains.anko.runOnUiThread
import timber.log.Timber

fun <T : View?> visibility(
    views: ViewList<T>,
    visible: Boolean,
    setGone: Boolean = true,
    debug: Boolean = false
) {
    views.visibility(visible, setGone, debug)
}

fun visibility(view: View?, visible: Boolean, setGone: Boolean = true, debug: Boolean = false) {
    if (debug) Timber.d(
        "Setting visibility for ${
            view?.context?.resources?.getResourceEntryName(
                view.id
            )
        } to $visible"
    )
    view?.visibility = if (visible) View.VISIBLE else if (setGone) View.GONE else View.INVISIBLE
}

fun visibility(view: MenuItem, visible: Boolean, debug: Boolean = false) {
    if (debug) Timber.d("Setting visibility for ${view.itemId} to $visible")
    view.isVisible = visible
}

fun visibility(view: Preference?, visible: Boolean, debug: Boolean = false) {
    if (debug) Timber.d("Setting visibility for ${view?.key} to $visible")
    view?.isVisible = visible
}

fun Context?.visibility(
    view: View?,
    visible: Boolean,
    setGone: Boolean = true,
    debug: Boolean = false
) =
    if (isNotNull())
        this!!.runOnUiThread {
            com.arnyminerz.escalaralcoiaicomtat.view.visibility(
                view,
                visible,
                setGone,
                debug
            )
        }
    else com.arnyminerz.escalaralcoiaicomtat.view.visibility(view, visible, setGone, debug)

/**
 * Checks if a view is visible
 * @author ArnyminerZ
 * @date 15/05/2020
 * @param view The view to check
 * @return True if visible. False if invisible or null
 */
fun visibility(view: View?): Boolean {
    return view != null && view.visibility == View.VISIBLE
}

/**
 * Sets the visibility of the view
 * @author ArnyminerZ
 * @date 07/09/2020
 */
@JvmName("visibility_own")
fun View.visibility(visible: Boolean, setGone: Boolean = true, debug: Boolean = false) =
    visibility(this, visible, setGone, debug)

/**
 * Sets the visibility of a view to gone
 */
fun View.hide() = visibility(false)

/**
 * Sets the visibility of a view to visible
 */
fun View.show() = visibility(true)

@Suppress("DEPRECATION")
fun setTextColor(view: TextView, context: Context, @ColorRes color: Int) {
    view.setTextColor(context.resources.getColor(color, context.theme))
}

@Suppress("DEPRECATION")
fun getColor(context: Context, @ColorRes color: Int): Int = ContextCompat.getColor(context, color)

fun View.isInside(x: Float, y: Float) =
    this.x < x && this.x + width > x && this.y < y && this.y + height > y

fun View.isInside(point: Pair<Float, Float>) = isInside(point.first, point.second)
