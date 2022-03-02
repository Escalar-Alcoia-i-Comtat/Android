package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater

import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.WorkerThread
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.exceptions.AppSearchException
import androidx.work.await
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DATA_FETCH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_UPDATER_ENDPOINT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getJson
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
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
        val updaterSingleton = UpdaterSingleton.getInstance()
        updaterSingleton.updateAvailableObjects = mapOf()

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
                updaterSingleton.updateAvailableObjects = updaterSingleton
                    .updateAvailableObjects
                    .toMutableMap()
                    .apply {
                        put(table, ids)
                    }
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
    var updateAvailableObjects: Map<@Namespace String, List<@ObjectId String>> = mapOf()

    suspend fun update(
        context: Context,
        @Namespace namespace: String,
        @ObjectId objectId: String,
        score: Int
    ) {
        Timber.i("Updating data of $objectId at $namespace...")
        val at = namespace + "s"
        try {
            Timber.d("Making data request to server... $namespace/$objectId")
            val jsonData = context.getJson("$REST_API_DATA_FETCH$at/$objectId")
            Timber.d("Decoding server response... $namespace/$objectId")
            val jsonResult = jsonData.getJSONObject("result")
            val jsonElement = jsonResult.getJSONObject(objectId) ?: run {
                Timber.e("Server returned invalid result. $namespace/$objectId")
                return
            }
            Timber.d("Constructing data... $namespace/$objectId")
            val data = when (namespace) {
                Area.NAMESPACE -> Area(jsonElement, objectId)
                Zone.NAMESPACE -> Zone(jsonElement, objectId)
                Sector.NAMESPACE -> Sector(jsonElement, objectId)
                Path.NAMESPACE -> Path(jsonElement, objectId)
                else -> {
                    Timber.e("Namespace ($namespace) is not valid.")
                    return
                }
            }
            val app = context.applicationContext as App
            Timber.d("Getting AppSearch document... $namespace/$objectId")
            val doc = if (data is DataClass<*, *, *>)
                data.data(score)
            else
                (data as Path).data()
            Timber.d("Putting document into search session... $namespace/$objectId")
            Timber.v("Building PutDocumentsRequest... $namespace/$objectId")
            val request = PutDocumentsRequest.Builder()
                .addDocuments(doc)
                .build()
            Timber.v("Requesting documents put... $namespace/$objectId")
            app.searchSession
                .put(request)
                .await()
            Timber.v("Added to SearchSession, removing element from updateAvailableObjects...")
            updateAvailableObjects = updateAvailableObjects
                .toMutableMap()
                .apply {
                    val newNamespaceList = arrayListOf<String>()
                    get(namespace)?.forEach { if (it != objectId) newNamespaceList.add(it) }
                    Timber.v("Updated id list for $namespace: $newNamespaceList")
                    put(namespace, newNamespaceList)
                }
        } catch (e: VolleyError) {
            Timber.e(e, "Could not update element at $namespace with id $objectId.")
            uiContext {
                context.toast(R.string.toast_error_update)
            }
        } catch (e: AppSearchException) {
            Timber.e(e, "Could not store element at $namespace with id $objectId.")
            uiContext {
                context.toast(R.string.toast_error_update)
            }
        }
    }
}
