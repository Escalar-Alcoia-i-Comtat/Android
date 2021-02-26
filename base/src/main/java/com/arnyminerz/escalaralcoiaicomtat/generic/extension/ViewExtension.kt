package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import android.content.Context
import android.view.View
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread

fun View.disable() {
    isEnabled = false
}

fun View.enable() {
    isEnabled = true
}

fun Collection<View>.disable(context: Context? = null) {
    if (context != null)
        context.runOnUiThread {
            this@disable.forEach { it.disable() }
        }
    else
        this.forEach { it.disable() }
}

fun Collection<View>.enable(context: Context? = null) {
    if (context != null)
        context.runOnUiThread {
            this@enable.forEach { it.enable() }
        }
    else
        this.forEach { it.enable() }
}