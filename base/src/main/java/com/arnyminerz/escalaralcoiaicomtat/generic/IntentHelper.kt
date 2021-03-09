package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.app.ShareCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import java.io.Serializable


fun Intent.hasExtras(vararg extras: String): Boolean {
    val hases = arrayListOf<Boolean>()
    for (extra in extras)
        hases.add(hasExtra(extra))

    return !hases.contains(false)
}

fun <T> Intent.putExtra(key: IntentExtra<T>, value: T): Intent {
    when (value) {
        is Parcelable -> putExtra(key.key, value)
        is LongArray -> putExtra(key.key, value)
        is Byte -> putExtra(key.key, value)
        is DoubleArray -> putExtra(key.key, value)
        is CharSequence -> putExtra(key.key, value)
        is BooleanArray -> putExtra(key.key, value)
        is Int -> putExtra(key.key, value)
        is CharArray -> putExtra(key.key, value)
        is ByteArray -> putExtra(key.key, value)
        is Array<*> -> putExtra(key.key, value)
        is Bundle -> putExtra(key.key, value)
        is FloatArray -> putExtra(key.key, value)
        is Double -> putExtra(key.key, value)
        is IntArray -> putExtra(key.key, value)
        is ShortArray -> putExtra(key.key, value)
        is Boolean -> putExtra(key.key, value)
        is String -> putExtra(key.key, value)
        is Long -> putExtra(key.key, value)
        is Char -> putExtra(key.key, value)
        is Serializable -> putExtra(key.key, value)
        is Float -> putExtra(key.key, value)
        is Short -> putExtra(key.key, value)
    }

    return this
}

fun Activity.shareString(text: String) {
    ShareCompat.IntentBuilder
        // getActivity() or activity field if within Fragment
        .from(this)
        // The text that will be shared
        .setText(text)
        // most general text sharing MIME type
        .setType("text/plain")
        .setChooserTitle(R.string.action_share_with)
        .startChooser()
}

inline fun <reified T> Intent.getExtra(key: IntentExtra<T>): T? {
    val result = extras?.get(key.key)
    return if (result is T)
        result
    else null
}

inline fun <reified T> Intent.getExtra(key: IntentExtra<T>, default: T): T {
    val result = extras?.get(key.key)
    return if (result is T)
        result
    else default
}
