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
 * Fetches data asyncronously from datastore, or from network if not available
 * @author Arnau Mora
 * @since 20210313
 * @param label The label to search in datastore
 * @param shouldPin If the data should be stored in datastore when loaded
 * @param callback What to call when completed
 * @return The loading task
 * @throws NoInternetAccessException If no data is stored, and there's no Internet connection available
 */
@Throws(NoInternetAccessException::class)
fun <A : ParseObject> ParseQuery<A>.fetchPinOrNetwork(
    label: String,
    shouldPin: Boolean = false,
    callback: ((objects: List<A>, error: Exception?) -> Unit)? = null
): Task<List<A>> = fromPin(label).findInBackground().continueWithTask { task ->
    val error = task.error
    val objects = task.result
    if (error != null) {
        if (error is ParseException && error.code == ParseException.CACHE_MISS) {
            Timber.w("No stored data found. Fetching from network.")
            if (!appNetworkState.hasInternet) {
                Timber.w("No Internet connection is available, and there's no stored data")
                throw NoInternetAccessException("No Internet connection is available, and there's no stored data")
            }
            return@continueWithTask fromNetwork().findInBackground()
        } else Timber.e(error, "Could not fetch data.")
    }
    if (objects.size <= 0) {
        Timber.w("No stored data found. Fetching from network.")
        if (!appNetworkState.hasInternet) {
            Timber.w("No Internet connection is available, and there's no stored data")
            throw NoInternetAccessException("No Internet connection is available, and there's no stored data")
        }
        return@continueWithTask fromNetwork().findInBackground()
    }
    Timber.d("Loading from pin...")
    return@continueWithTask task
}.continueWithTask { task ->
    callback?.invoke(task.result, task.error)
    if (shouldPin) {
        Timber.d("Pinning...")
        ParseObject.pinAll(label, task.result)
    }
    return@continueWithTask task
}

/**
 * Fetches data syncronously from datastore, or from network if not available
 * @author Arnau Mora
 * @since 20210313
 * @param label The label to search in datastore
 * @param shouldPin If the data should be stored in datastore when loaded
 * @param timeout The maximum amount of time that the task should take
 * @return The fetch result
 * @throws NoInternetAccessException If no data is stored, and there's no Internet connection available
 * @throws TimeoutException If timeout passed before finishing the task
 */
@Throws(NoInternetAccessException::class, TimeoutException::class)
@WorkerThread
fun <A : ParseObject> ParseQuery<A>.fetchPinOrNetworkSync(
    label: String,
    shouldPin: Boolean = false,
    timeout: Pair<Long, TimeUnit>? = null
): List<A> {
    val list = arrayListOf<A>()
    val task = fetchPinOrNetwork(label, shouldPin) { result, error ->
        error?.let { throw it }
        list.addAll(result)
    }
    if (timeout != null && !task.waitForCompletion(timeout.first, timeout.second))
        throw TimeoutException("The task was not completed in less than ${timeout.first} ${timeout.second.name}")
    else
        task.waitForCompletion()
    return list
}
