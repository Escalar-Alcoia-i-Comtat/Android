package com.arnyminerz.escalaralcoiaicomtat.view

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.arnyminerz.escalaralcoiaicomtat.generic.onUiThread
import com.arnyminerz.escalaralcoiaicomtat.list.ViewList
import timber.log.Timber

@UiThread
fun <T : View?> visibility(
    views: ViewList<T>,
    visible: Boolean,
    setGone: Boolean = true,
    debug: Boolean = false
) {
    views.visibility(visible, setGone, debug)
}

@UiThread
fun visibility(
    view: View?,
    visible: Boolean,
    setGone: Boolean = true,
    debug: Boolean = false,
    clearAnimation: Boolean = true
) {
    if (debug) Timber.d(
        "Setting visibility for ${
            view?.context?.resources?.getResourceEntryName(
                view.id
            )
        } to $visible"
    )
    if (clearAnimation)
        view?.clearAnimation()
    view?.visibility = if (visible) View.VISIBLE else if (setGone) View.GONE else View.INVISIBLE
}

@UiThread
fun visibility(view: MenuItem, visible: Boolean, debug: Boolean = false) {
    if (debug) Timber.d("Setting visibility for ${view.itemId} to $visible")
    view.isVisible = visible
}

@UiThread
fun visibility(view: Preference?, visible: Boolean, debug: Boolean = false) {
    if (debug) Timber.d("Setting visibility for ${view?.key} to $visible")
    view?.isVisible = visible
}

@UiThread
fun Context?.visibility(
    view: View?,
    visible: Boolean,
    setGone: Boolean = true,
    debug: Boolean = false,
    clearAnimation: Boolean = true
) {
    if (this != null)
        this.onUiThread {
            com.arnyminerz.escalaralcoiaicomtat.view.visibility(
                view,
                visible,
                setGone,
                debug,
                clearAnimation
            )
        }
    else com.arnyminerz.escalaralcoiaicomtat.view.visibility(
        view,
        visible,
        setGone,
        debug,
        clearAnimation
    )
}

fun Activity?.visibility(
    view: View?,
    visible: Boolean,
    setGone: Boolean = true,
    debug: Boolean = false,
    clearAnimation: Boolean = true
) =
    (this as? Context?).visibility(view, visible, setGone, debug, clearAnimation)

fun Fragment.visibility(
    view: View?,
    visible: Boolean,
    setGone: Boolean = true,
    debug: Boolean = false,
    clearAnimation: Boolean = true
) = context.visibility(view, visible, setGone, debug, clearAnimation)

/**
 * Checks if a view is visible
 * @author ArnyminerZ
 * @date 15/05/2020
 * @param view The view to check
 * @return True if visible. False if invisible or null
 */
@UiThread
fun visibility(view: View?): Boolean =
    view != null && view.visibility == View.VISIBLE

/**
 * Sets the visibility of the view
 * @author ArnyminerZ
 * @since 07/09/2020
 */
@JvmName("visibility_own")
fun View.visibility(
    visible: Boolean,
    setGone: Boolean = true,
    debug: Boolean = false,
    clearAnimation: Boolean = true
) =
    context.visibility(this, visible, setGone, debug, clearAnimation)

/**
 * Sets the visibility of a view to gone
 */
@UiThread
fun View.hide(setGone: Boolean = true) = context.visibility(this, false, setGone = setGone)

/**
 * Sets the visibility of a view to visible
 */
@UiThread
fun View.show() = context.visibility(this, true)

@UiThread
@Suppress("DEPRECATION")
fun setTextColor(view: TextView, context: Context, @ColorRes color: Int) {
    view.setTextColor(context.resources.getColor(color, context.theme))
}

@UiThread
@Suppress("DEPRECATION")
fun getColor(context: Context, @ColorRes color: Int): Int = ContextCompat.getColor(context, color)

/**
 * Gets a color stored in attribute
 * @author Arnau Mora
 * @since 20210321
 * @param context The context to get from
 * @param attributeRes The attribute to get
 * @return The loaded color
 */
fun getColorFromAttribute(context: Context, @AttrRes attributeRes: Int): Int {
    val theme = context.theme
    val resources = context.resources
    val typedValue = TypedValue()
    theme.resolveAttribute(attributeRes, typedValue, true)
    val colorRes = typedValue.resourceId
    return resources.getColor(colorRes, theme)
}

fun getAttribute(context: Context, resId: Int): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(resId, typedValue, true)
    return typedValue.data
}
