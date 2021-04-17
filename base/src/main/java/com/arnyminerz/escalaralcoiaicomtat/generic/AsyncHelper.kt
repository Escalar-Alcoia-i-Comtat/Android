package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Runs some code asyncronously.
 * Use [doOnMain] for updating UI elements.
 * @author Arnau Mora
 * @since 20210413
 * @param block The code to run
 * @see doOnMain Use this for updating UI elements.
 */
fun doAsync(block: suspend CoroutineScope.() -> Unit) =
    CoroutineScope(Dispatchers.Default).launch {
        block(this)
    }

/**
 * Runs some code on the UI thread.
 * @author Arnau Mora
 * @since 20210413
 * @param block The code to run
 */
fun doOnMain(block: suspend CoroutineScope.() -> Unit) =
    CoroutineScope(Dispatchers.Main).launch {
        block(this)
    }

/**
 * Runs a block of code on the main dispatcher (UI thread).
 * @author Arnau Mora
 * @since 20210413
 * @param block The code to run
 */
suspend fun <T> uiContext(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Main, block)

/**
 * Runs a block of code on the main dispatcher (UI thread).
 * @author Arnau Mora
 * @since 20210413
 * @param block The code to run
 */
suspend fun <T, A : Activity> A.uiContext(block: suspend A.() -> T) =
    withContext(Dispatchers.Main) {
        block(this@uiContext)
    }

suspend fun <T> Task<T>.awaitTask(): T? = suspendCoroutine { continuation ->
    this.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception!!)
        }
    }
}
