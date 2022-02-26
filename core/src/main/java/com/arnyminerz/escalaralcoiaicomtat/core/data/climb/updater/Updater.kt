package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater

import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.WorkerThread
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.work.await
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_UPDATER_ENDPOINT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getJson
import timber.log.Timber

/**
 * Gets the millis time stored for the youngest element on [searchSession]. This is used for, if
 * in the server there's an even younger element, DB should be updated.
 * @author Arnau Mora
 * @since 20220226
 * @param searchSession The [AppSearchSession] instance for fetching values from db.
 */
private suspend fun lastDownloadedItem(searchSession: AppSearchSession): Long {
    var youngestTime = 0L
    Timber.d("Searching for all data resources from SearchSession...")
    val searchResults = searchSession.search(
        "",
        SearchSpec.Builder()
            .addFilterSchemas("AreaData", "ZoneData", "SectorData", "PathData")
            .setResultCountPerPage(100)
            .build()
    )
    Timber.d("Got data resources. Getting first page")
    var page = searchResults.nextPage.await()
    while (page.isNotEmpty()) {
        Timber.v("Page has ${page.size} elements. Searching for latest creation.")
        for (searchResult in page) {
            val genericDocument = searchResult.genericDocument
            val creationTimestampMillis = genericDocument.creationTimestampMillis
            Timber.v("Millis: $creationTimestampMillis")
            creationTimestampMillis
                .takeIf { it > youngestTime }
                ?.let { youngestTime = it }
        }
        Timber.v("Youngest time: $youngestTime")

        page = searchResults.nextPage.await()
    }
    Timber.d("Youngest time: $youngestTime")
    return youngestTime
}

@Target(AnnotationTarget.TYPE)
@IntDef(
    UPDATE_AVAILABLE,
    UPDATE_AVAILABLE_FALSE,
    UPDATE_AVAILABLE_FAIL_CLIENT,
    UPDATE_AVAILABLE_FAIL_SERVER,
    UPDATE_AVAILABLE_FAIL_FIELDS
)
@Retention(AnnotationRetention.SOURCE)
annotation class UpdateAvailableResult

/**
 * When there's no available update
 * @author Arnau Mora
 * @since 20220226
 */
const val UPDATE_AVAILABLE = 0

/**
 * When there's no available update
 * @author Arnau Mora
 * @since 20220226
 */
const val UPDATE_AVAILABLE_FALSE = 1

/**
 * When there has been an error on server-side while checking for updates.
 * @author Arnau Mora
 * @since 20220226
 */
const val UPDATE_AVAILABLE_FAIL_SERVER = 2

/**
 * When the server responded correctly, but there has been an unknown error while parsing the result.
 * @author Arnau Mora
 * @since 20220226
 */
const val UPDATE_AVAILABLE_FAIL_CLIENT = 3

/**
 * When the server responded with update available, but there's no "fields" list.
 * @author Arnau Mora
 * @since 20220226
 */
const val UPDATE_AVAILABLE_FAIL_FIELDS = 4

@WorkerThread
suspend fun updateAvailable(
    context: Context,
    searchSession: AppSearchSession
): @UpdateAvailableResult Int {
    val youngestTime = lastDownloadedItem(searchSession)
    try {
        // Fetch from server the updatable elements, and continue only if result has "result"
        val jsonData = context.getJson("$REST_API_UPDATER_ENDPOINT$youngestTime")
            .takeIf { it.has("result") }
            ?: return UPDATE_AVAILABLE_FAIL_CLIENT
        // Get the result
        val jsonResult = jsonData.getJSONObject("result")
        // Clear the singleton's possible stored updatable elements
        UpdaterSingleton
            .getInstance()
            .updateAvailableObjects
            .clear()
        // Get the fields required
        val updateAvailable = jsonResult.getBoolean("updateAvailable")
        val fieldsAvailable = jsonResult.has("fields")
        // If updateAvailable is true, and also result has "fields", parse the updatable elements
        if (updateAvailable && fieldsAvailable) {
            // Get the fields list
            val updatableFields = jsonResult.getJSONArray("fields")
            // Iterate the fields
            for (k in 0 until updatableFields.length()) {
                val field = updatableFields.getJSONObject(k)
                val table = field.getString("table")
                val ids = field.getJSONArray("ids").let { array ->
                    val idList = arrayListOf<String>()
                    for (i in 0 until array.length())
                        idList.add(array.getString(i))
                    idList.toList()
                }
                UpdaterSingleton
                    .getInstance()
                    .updateAvailableObjects[table] = ids
            }
            // If there were updatable fields, return UPDATE_AVAILABLE
            if (updatableFields.length() > 0)
                return UPDATE_AVAILABLE
        } else if (updateAvailable)
            return UPDATE_AVAILABLE_FAIL_FIELDS
    } catch (e: VolleyError) {
        Timber.e(e, "Could not check for updates.")
        return UPDATE_AVAILABLE_FAIL_SERVER
    }
    return UPDATE_AVAILABLE_FALSE
}

/**
 * Stores available elements to be updated.
 * @author Arnau Mora
 * @since 20220226
 */
class UpdaterSingleton {
    companion object {
        @Volatile
        private var INSTANCE: UpdaterSingleton? = null

        /**
         * Get the prepared [UpdaterSingleton] instance, or instantiate a new one.
         * @author Arnau Mora
         * @since 20220226
         */
        fun getInstance() =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: UpdaterSingleton()
                    .also { INSTANCE = it }
            }
    }

    /**
     * Stores the references that have a new update available.
     * @author Arnau Mora
     * @since 20220226
     */
    val updateAvailableObjects: HashMap<@Namespace String, List<@ObjectId String>> = hashMapOf()
}
