package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater

import android.content.Context
import androidx.annotation.IntDef
import androidx.annotation.WorkerThread
import androidx.compose.runtime.mutableStateListOf
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DATA_FETCH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DATA_LIST
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getJson
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.firebase.perf.metrics.AddTrace
import org.json.JSONObject
import timber.log.Timber
import java.io.Serializable

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

private fun <R : DataClassImpl, D : DataRoot<R>> findUpdatableObjects(
    dataList: List<D>,
    json: JSONObject,
    childrenCount: (objectId: String) -> Long,
    constructor: (json: JSONObject, objectId: String, childrenCount: Long) -> R
) = mutableListOf<UpdaterSingleton.Item>().apply {
    json.keys()
        .forEach { objectId ->
            val jsonData = json.getJSONObject(objectId)
            val serverData = constructor(jsonData, objectId, childrenCount(objectId))
            val localData = dataList
                .map { it.data() }
                .find { it.objectId == objectId }
            if (localData == null || localData.hashCode() != serverData.hashCode())
                add(
                    UpdaterSingleton.Item(
                        serverData.namespace,
                        serverData.objectId,
                        serverData.hashCode(),
                        localData.hashCode(),
                        0, // TODO: Find a valid score
                        serverData.displayName,
                        serverData.displayMap(),
                        localData?.displayMap() ?: emptyMap()
                    )
                )
        }
}

@AddTrace(name = "CheckForUpdates")
@WorkerThread
suspend fun updateAvailable(
    context: Context
): @UpdateAvailableResult Int {
    try {
        // Fetch from server the list of data, and continue only if result has "result"
        val jsonData = context.getJson("$REST_API_DATA_LIST/*")
            .takeIf { it.has("result") }
            ?: return UPDATE_AVAILABLE_FAIL_CLIENT

        // Get the result
        val jsonResult = jsonData.getJSONObject("result")

        // Clear the singleton's possible stored updatable elements
        val updaterSingleton = UpdaterSingleton.getInstance()

        // Get all the lists of elements
        val jsonAreas = jsonResult.getJSONObject("Areas")
        val jsonZones = jsonResult.getJSONObject("Zones")
        val jsonSectors = jsonResult.getJSONObject("Sectors")
        val jsonPaths = jsonResult.getJSONObject("Paths")

        val dataRepository = DataSingleton.getInstance(context)
            .repository

        // Find the locally stored elements lists
        val areas = dataRepository.getAreas()
        val zones = dataRepository.getZones()
        val sectors = dataRepository.getSectors()
        val paths = dataRepository.getPaths()

        // Add all the updatable areas
        val updatablePaths = findUpdatableObjects(paths, jsonPaths, { 0L })
        { json, objectId, childrenCount -> Path(json, objectId) }
        val updatableSectors = findUpdatableObjects(
            sectors,
            jsonSectors,
            { id -> updatablePaths.filter { it.objectId == id }.size.toLong() },
            Sector.CONSTRUCTOR,
        )
        val updatableZones = findUpdatableObjects(
            zones,
            jsonZones,
            { id -> updatableSectors.filter { it.objectId == id }.size.toLong() },
            Zone.CONSTRUCTOR,
        )
        val updatableAreas = findUpdatableObjects(
            areas,
            jsonAreas,
            { id -> updatableZones.filter { it.objectId == id }.size.toLong() },
            Area.CONSTRUCTOR,
        )

        // Add all the found elements to the lists
        updaterSingleton.updateAvailableObjects.clear()
        updaterSingleton.updateAvailableObjects.addAll(
            listOf(updatableAreas, updatableZones, updatableSectors, updatablePaths).flatten()
        )

        return when {
            updatableAreas.isNotEmpty() ||
                    updatableZones.isNotEmpty() ||
                    updatableSectors.isNotEmpty() ||
                    updatablePaths.isNotEmpty() -> UPDATE_AVAILABLE
            else -> UPDATE_AVAILABLE_FALSE
        }
    } catch (e: VolleyError) {
        Timber.e(e, "Could not check for updates.")
        return UPDATE_AVAILABLE_FAIL_SERVER
    }
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

    data class Item(
        val namespace: Namespace,
        @ObjectId val objectId: String,
        val serverHash: Int,
        val localHash: Int,
        val score: Int,
        val displayName: String,
        val serverDisplayMap: Map<String, Serializable?>,
        val localDisplayMap: Map<String, Serializable?>,
    )

    /**
     * Stores the references that have a new update available.
     * @author Arnau Mora
     * @since 20220226
     */
    var updateAvailableObjects = mutableStateListOf<Item>()

    /**
     * Updates the selected [namespace] and [objectId].
     * @author Arnau Mora
     * @since 20220315
     * @param context The context to run from.
     * @param namespace The namespace of the object to update.
     * @param objectId The id of the object to update.
     */
    suspend fun update(
        context: Context,
        namespace: Namespace,
        @ObjectId objectId: String,
    ) {
        Timber.i("Updating data of $objectId at $namespace...")
        val at = namespace.tableName
        try {
            Timber.d("Making data request to server... $namespace/$objectId")
            val jsonData = context.getJson("$REST_API_DATA_FETCH$at/$objectId")
            Timber.d("Decoding server response... $namespace/$objectId")
            val jsonResult = jsonData.getJSONObject("result")
            val jsonElement = jsonResult.getJSONObject(objectId) ?: run {
                Timber.e("Server returned invalid result. $namespace/$objectId")
                return
            }

            val childrenCount = namespace.ChildrenNamespace?.let { childrenNamespace ->
                DataSingleton.getInstance(context)
                    .repository
                    .getChildren(childrenNamespace, objectId)
                    ?.size
                    ?.toLong()
            } ?: 0L

            Timber.d("Constructing data... $namespace/$objectId")
            val data = when (namespace) {
                Area.NAMESPACE -> Area(jsonElement, objectId, childrenCount)
                Zone.NAMESPACE -> Zone(jsonElement, objectId, childrenCount)
                Sector.NAMESPACE -> Sector(jsonElement, objectId, childrenCount)
                Path.NAMESPACE -> Path(jsonElement, objectId)
                else -> {
                    Timber.e("Namespace ($namespace) is not valid.")
                    return
                }
            }
            Timber.d("Getting document... $namespace/$objectId")
            val doc = if (data is DataClass<*, *, *>)
                data.data()
            else
                (data as Path).data()
            Timber.d("Putting document into db... $namespace/$objectId")
            DataSingleton.getInstance(context)
                .repository
                .update(doc)
            Timber.v("Added to SearchSession, removing element from updateAvailableObjects...")
            updateAvailableObjects.removeIf { it.objectId == objectId }
        } catch (e: VolleyError) {
            Timber.e(e, "Could not update element at $namespace with id $objectId.")
            uiContext {
                context.toast(R.string.toast_error_update)
            }
        }
    }
}
