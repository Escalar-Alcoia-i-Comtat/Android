package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.connection.parse.fetchPinOrNetworkSync
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.area.Area
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.DATA_FIX_LABEL
import com.arnyminerz.escalaralcoiaicomtat.shared.MAX_BATCH_SIZE
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import timber.log.Timber

/**
 * Loads all the areas available in the server.
 * @author Arnau Mora
 * @since 20210313
 * @see MAX_BATCH_SIZE
 * @return A collection of areas
 * @throws ParseException If there's an error while fetching from parse
 * @throws NoInternetAccessException If no data is stored, and there's no Internet connection available
 */
@WorkerThread
@Throws(ParseException::class, NoInternetAccessException::class)
fun loadAreasFromCache(progressCallback: (current: Int, total: Int) -> Unit, callback: () -> Unit) {
    Timber.d("Querying paths...")
    try {
        val query = ParseQuery.getQuery<ParseObject>("Area")
        query.addAscendingOrder("displayName")
        AREAS.clear()
        query.limit = MAX_BATCH_SIZE
        val objects = query.fetchPinOrNetworkSync(DATA_FIX_LABEL)

        Timber.d("Got ${objects.size} areas. Processing...")
        progressCallback(0, objects.size)
        for ((a, areaData) in objects.withIndex()) {
            val area = Area(areaData)
            AREAS[area.objectId] = area
            progressCallback(a, objects.size)
        }

        Timber.v("Pinning...")
        progressCallback(0, -1)
        ParseObject.pinAllInBackground(DATA_FIX_LABEL, objects) { err ->
            if (err != null)
                Timber.w(err, "Could not pin data!")
            else {
                Timber.v("Pinned data.")
                callback()
            }
        }
    } catch (e: NoInternetAccessException) {
        Timber.w(e, "Could not load areas.")
        throw e
    } catch (e: ParseException) {
        Timber.w(e, "Could not load areas.")
        throw e
    }
}
