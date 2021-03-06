package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.UiThread
import timber.log.Timber
import java.io.Serializable

fun <T> Intent.putExtra(key: DataExtra<T>, value: T): Intent {
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
        else -> Timber.e("Unsupported type ${value!!::class.java.simpleName}. Could not add to intent")
    }

    return this
}

inline fun <reified T> Intent.getExtra(key: DataExtra<T>): T? {
    if (extras?.containsKey(key.key) != true)
        return null

    val result = extras?.get(key.key)
    return if (result is T)
        result
    else null
}

inline fun <reified T> Intent.getExtra(key: DataExtra<T>, default: T): T {
    val result = extras?.get(key.key)
    return if (result is T)
        result
    else default
}

/**
 * Gets the intent's size in bytes
 * @return The Intent's size in bytes
 */
fun Intent.getSize(): Int {
    val parcel = Parcel.obtain()
    parcel.writeBundle(extras)
    val size: Int = parcel.dataSize()
    parcel.recycle()
    return size
}

inline fun <reified T> Activity.getExtra(key: DataExtra<T>): T? {
    val extras = intent?.extras
    return when {
        extras == null -> null
        !extras.containsKey(key.key) -> null
        else -> {
            val result = extras.get(key.key)
            if (result is T)
                result
            else null
        }
    }
}

inline fun <reified T> Activity.getExtra(key: DataExtra<T>, default: T): T {
    val extras = intent?.extras
    return when {
        extras == null -> default
        !extras.containsKey(key.key) -> default
        else -> {
            val result = extras.get(key.key)
            if (result is T)
                result
            else default
        }
    }
}

/**
 * Starts an activity with the specified properties for the Intent.
 * @author Arnau Mora
 * @since 20210521
 * @param target The target activity's class.
 * @param properties The setter for the properties of the Intent.
 */
@UiThread
fun Activity.launch(target: Class<*>, properties: (Intent.() -> Unit)? = null) =
    startActivity(Intent(this, target).also {
        properties?.invoke(it)
    })
