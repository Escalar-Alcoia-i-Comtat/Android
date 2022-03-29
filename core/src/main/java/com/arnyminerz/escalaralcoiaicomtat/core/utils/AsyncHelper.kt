package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.app.Activity
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The [CoroutineScope] used in functions such as [doAsync].
 * @author Arnau Mora
 * @since 20210510
 */
val asyncCoroutineScope
    get() = CoroutineScope(Dispatchers.IO)

/**
 * The [CoroutineScope] used in functions such as [doOnMain].
 * @author Arnau Mora
 * @since 20210510
 */
val mainCoroutineScope
    get() = CoroutineScope(Dispatchers.Main)

/**
 * Runs some code asynchronously.
 * Use [doOnMain] for updating UI elements.
 * @author Arnau Mora
 * @since 20210413
 * @param block The code to run
 * @see doOnMain Use this for updating UI elements.
 */
@MainThread
fun doAsync(@WorkerThread block: suspend CoroutineScope.() -> Unit) =
    asyncCoroutineScope.launch {
        block(this)
    }

/**
 * Runs some code on the UI thread.
 * @author Arnau Mora
 * @since 20210413
 * @param block The code to run
 */
fun doOnMain(@UiThread block: suspend CoroutineScope.() -> Unit) =
    mainCoroutineScope.launch {
        block(this)
    }

/**
 * Runs a block of code on the main dispatcher (UI thread).
 * @author Arnau Mora
 * @since 20210413
 * @param block The code to run
 */
suspend fun <T> uiContext(@UiThread block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Main, block)

/**
 * Runs a block of code on the main dispatcher (UI thread).
 * @author Arnau Mora
 * @since 20210413
 * @param block The code to run
 */
suspend fun <T, A : Activity> A.uiContext(@UiThread block: suspend A.() -> T) =
    withContext(Dispatchers.Main) {
        block(this@uiContext)
    }
