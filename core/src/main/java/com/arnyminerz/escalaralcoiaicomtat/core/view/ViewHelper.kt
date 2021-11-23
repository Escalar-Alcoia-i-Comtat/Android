package com.arnyminerz.escalaralcoiaicomtat.core.view

import android.content.Context
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.preference.Preference
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
@UiThread
fun View.visibility(
    visible: Boolean,
    setGone: Boolean = true,
    debug: Boolean = false,
    clearAnimation: Boolean = true
) =
    visibility(this, visible, setGone, debug, clearAnimation)

/**
 * Updates the visibility of the [MenuItem] to [visible].
 * Works as an alias for [MenuItem.setVisible].
 * @author Arnau Mora
 * @since 20211120
 */
@UiThread
fun MenuItem?.visibility(
    visible: Boolean
): MenuItem? = this?.setVisible(visible)

/**
 * Forces the [MenuItem] to be visible.
 * @author Arnau Mora
 * @since 20211120
 */
@UiThread
fun MenuItem?.show(): MenuItem? = this?.visibility(true)

/**
 * Forces the [MenuItem] to be invisible.
 * @author Arnau Mora
 * @since 20211120
 */
@UiThread
fun MenuItem?.hide(): MenuItem? = this?.visibility(false)

/**
 * Sets the visibility of a view to gone
 */
@UiThread
fun View.hide(setGone: Boolean = true) = visibility(this, false, setGone = setGone)

/**
 * Sets the visibility of a view to visible
 */
@UiThread
fun View.show() = visibility(this, true)

@UiThread
@Suppress("DEPRECATION")
fun setTextColor(view: TextView, context: Context, @ColorRes color: Int) {
    view.setTextColor(context.resources.getColor(color, context.theme))
}

/**
 * Sets the text color of the [view] from the set attribute [color].
 * @author Arnau Mora
 * @since 20211123
 * @param view The [TextView] to update.
 * @param context The [Context] that is requesting the update.
 * @param color The color attribute to set.
 */
@UiThread
fun setTextColorAttr(view: TextView, context: Context, @AttrRes color: Int) {
    view.setTextColor(
        getColorFromAttribute(context, color)
    )
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

/**
 * Gets an attribute with id [resId], and returns it as [TypedValue].
 * @author Arnau Mora
 * @since 20211006
 * @param resId The id of the attribute to get.
 */
fun Context.getTypedAttribute(resId: Int): TypedValue {
    val typedValue = TypedValue()
    theme.resolveAttribute(resId, typedValue, true)
    return typedValue
}

/**
 * Gets the value of an attribute with id [resId].
 * @author Arnau Mora
 * @since 20211006
 * @param context The context to get the attribute from.
 * @param resId The id of the attribute to get.
 */
fun getAttribute(context: Context, resId: Int): Int {
    return context.getTypedAttribute(resId).data
}
