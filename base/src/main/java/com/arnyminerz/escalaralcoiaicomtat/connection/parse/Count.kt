package com.arnyminerz.escalaralcoiaicomtat.connection.parse

import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkState
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.boltsinternal.Task
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Counts the amount of objects there are in a query
 * @author Arnau Mora
 * @since 20210314
 * @param callback What to call when completed
 * @return The loading task
 * @throws NoInternetAccessException If no data is stored, and there's no Internet connection available
 */
@Throws(NoInternetAccessException::class)
fun <A : ParseObject> ParseQuery<A>.count(
    callback: ((count: Int, error: Exception?) -> Unit)? = null
): Task<Int> = countInBackground().continueWithTask { task ->
    val error = task.error
    if (error != null) {
        if (error is ParseException && error.code == ParseException.CACHE_MISS) {
            Timber.w("No stored data found. Fetching from network.")
            if (!appNetworkState.hasInternet) {
                Timber.w("No Internet connection is available, and there's no stored data")
                throw NoInternetAccessException("No Internet connection is available, and there's no stored data")
            }
            return@continueWithTask fromNetwork().countInBackground()
        } else Timber.e(error, "Could not fetch data.")
    }
    return@continueWithTask task
}.continueWithTask { task ->
    callback?.invoke(task.result, task.error)
    return@continueWithTask task
}

/**
 * Counts the amount of objects there are in a query syncronously
 * @author Arnau Mora
 * @since 20210313
 * @param timeout The maximum amount of time that the task should take
 * @return The count
 * @throws NoInternetAccessException If no data is stored, and there's no Internet connection available
 * @throws TimeoutException If timeout passed before finishing the task
 */
@Throws(NoInternetAccessException::class, TimeoutException::class)
@WorkerThread
fun <A : ParseObject> ParseQuery<A>.countSync(timeout: Pair<Long, TimeUnit>? = null): Int {
    var result = -1
    val task = count { count, error ->
        error?.let { throw it }
        result = count
    }
    if (timeout != null && !task.waitForCompletion(timeout.first, timeout.second))
        throw TimeoutException("The task was not completed in less than ${timeout.first} ${timeout.second.name}")
    else
        task.waitForCompletion()
    return result
}
