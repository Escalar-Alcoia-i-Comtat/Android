package com.arnyminerz.escalaralcoiaicomtat.list

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes

fun <T : View?> viewListOf(vararg views: T): ViewList<T> {
    val list = ViewList<T>()
    for (view in views)
        list.add(view)

    return list
}

fun ViewList<ImageView>.setScaleType(scaleType: ImageView.ScaleType){
    for (imageView in this)
        imageView.scaleType = scaleType
}

class ViewList<T : View?> : ArrayList<T>() {
    fun setImageResource(@DrawableRes res: Int) {
        for (view in this)
            if (view is ImageView)
                view.setImageResource(res)
    }

    fun invalidate() {
        for (view in this)
            view?.invalidate()
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap != null)
            for (view in this)
                if (view is ImageView)
                    view.setImageBitmap(bitmap)
    }

    @Suppress("unused")
    fun setImageDrawable(drawable: Drawable?) {
        if (drawable != null)
            for (view in this)
                if (view is ImageView)
                    view.setImageDrawable(drawable)
    }

    fun visibility(visible: Boolean, setGone: Boolean = true, debug: Boolean = false) {
        for (view in this)
            com.arnyminerz.escalaralcoiaicomtat.view.visibility(view, visible, setGone, debug)
    }

    /**
     * Clears the focus of the views in the list.
     * @author Arnau Mora
     * @since 20210424
     */
    fun clearFocus() {
        for (view in this)
            view?.clearFocus()
    }

    /**
     * Changes the status of the fields to enabled ([View.isEnabled]=true).
     * @author Arnau Mora
     * @since 20210424
     */
    fun enable() {
        for (view in this)
            view?.isEnabled = true
    }

    /**
     * Changes the status of the fields to disabled ([View.isEnabled]=false).
     * @author Arnau Mora
     * @since 20210424
     */
    fun disable() {
        for (view in this)
            view?.isEnabled = false
    }

    /**
     * Changes the status of the fields to [status] ([View.isEnabled]=[status]).
     * @author Arnau Mora
     * @since 20210424
     * @param status If true, the views will be enabled, if false, they will be disabled.
     */
    fun enabled(status: Boolean) {
        for (view in this)
            view?.isEnabled = status
    }
}
