package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
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
fun Context.launch(target: Class<*>, properties: (Intent.() -> Unit)? = null) =
    startActivity(Intent(this, target).also {
        properties?.invoke(it)
    })

/**
 * Starts an activity with the specified properties for the Intent, and the specified options for
 * the launching.
 * @author Arnau Mora
 * @since 20210521
 * @param target The target activity's class.
 * @param options Additional options for how the Activity should be started.
 * @param properties The setter for the properties of the Intent.
 */
@UiThread
fun Context.launch(target: Class<*>, options: Bundle, properties: (Intent.() -> Unit)? = null) =
    startActivity(Intent(this, target).also {
        properties?.invoke(it)
    }, options)

/**
 * Launches an intent from the select context and adding some options and applying properties in a
 * more Kotlin-like syntax.
 * @author Arnau Mora
 * @since 20210521
 * @param intent The intent to launch.
 * @param options Additional options for how the Intent should be launched.
 * @param properties The setter for the properties of the Intent.
 */
@UiThread
fun Context.launch(
    intent: Intent,
    options: Bundle? = null,
    properties: (Intent.() -> Unit)? = null
) =
    startActivity(intent.also {
        properties?.invoke(it)
    }, options)

/**
 * Shares [text] through the system UI.
 * @author Arnau Mora
 * @since 20220330
 * @param text The text to share.
 */
fun Context.share(text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    startActivity(Intent.createChooser(sendIntent, null))
}

/**
 * Runs [Context.startActivity] with `this`.
 * @author Arnau Mora
 * @since 20220406
 * @param context The context to run from.
 */
fun Intent.launch(context: Context) =
    context.startActivity(this)

/**
 * Runs [Context.startActivity] with `this` in the UI thread.
 * @author Arnau Mora
 * @since 20220406
 * @param context The context to run from.
 */
@WorkerThread
suspend fun Intent.launchAsync(context: Context) =
    this.uiLet { context.startActivity(it) }
