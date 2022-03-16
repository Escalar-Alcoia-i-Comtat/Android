package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import android.content.Context
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.toDataClassList
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Decodes the data from a json object retrieved from the server.
 * @author Arnau Mora
 * @since 20220219
 * @param D The [DataClass] type to decode.
 * @param I The [DataRoot] class to store in search indexation.
 * @param jsonData The data to decode.
 * @param childrenCount The amount of children the DataClass has.
 * @param namespace The namespace of [D].
 * @param constructor The constructor for building [D] from [JSONObject].
 * @return A pair of lists. The first is the objects in [jsonData], the second one is for indexing
 * search.
 */
private fun <D : DataClass<*, *, I>, I : DataRoot<D>> decode(
    jsonData: JSONObject,
    childrenCount: (objectId: String) -> Long,
    namespace: Namespace,
    constructor: (data: JSONObject, id: String, childrenCount: Long) -> D,
) = decode<D, I, Int>(jsonData, childrenCount, namespace, constructor, null)

/**
 * Decodes the data from a json object retrieved from the server.
 * @author Arnau Mora
 * @since 20220219
 * @param D The [DataClass] type to decode.
 * @param I The [DataRoot] class to store in search indexation.
 * @param R The type for sorting with [sortBy].
 * @param jsonData The data to decode.
 * @param childrenCount The amount of children the DataClass has.
 * @param namespace The namespace of [D].
 * @param constructor The constructor for building [D] from [JSONObject].
 * @param sortBy If set, the list, once constructed, will be sorted by the set parameter.
 * @return A pair of lists. The first is the objects in [jsonData], the second one is for indexing
 * search.
 */
private fun <D : DataClass<*, *, I>, I : DataRoot<D>, R : Comparable<R>> decode(
    jsonData: JSONObject,
    childrenCount: (objectId: String) -> Long,
    namespace: Namespace,
    constructor: (data: JSONObject, id: String, childrenCount: Long) -> D,
    sortBy: ((I) -> R?)? = null,
): List<I> {
    val index = arrayListOf<I>()
    val jsonObject = jsonData.getJSONObject(namespace.tableName)
    val keys = jsonObject.keys()
    for (id in keys) {
        val json = jsonObject.getJSONObject(id)

        // Process the DataClass data
        val dataClass = constructor(json, id, childrenCount(id))

        // Add the DataClass to the list
        index.add(dataClass.data())
    }
    sortBy?.let { index.sortBy(it) }
    return index
}

/**
 * Does the same than [decode] but for [Path]s.
 * @author Arnau Mora
 * @since 20220219
 * @param jsonData The data to decode.
 * @return A pair of lists. The first is the objects in [jsonData], the second one is for indexing
 * search.
 */
private fun decode(
    jsonData: JSONObject
): List<PathData> {
    val index = arrayListOf<PathData>()
    val jsonObject = jsonData.getJSONObject("${Path.NAMESPACE}s")
    val keys = jsonObject.keys()
    for (id in keys) {
        val json = jsonObject.getJSONObject(id)

        // Process the Path data
        val path = Path(json, id)

        // Add the path to the list
        index.add(path.data())
    }
    return index
}

/**
 * Loads all the areas available in the server.
 * Custom progress callbacks:
 * - 0/0 paths are being processed.
 * @author Arnau Mora
 * @since 20210313
 * @param context The context used for fetching and putting data into the index.
 * @param jsonData The data loaded from the data module
 * @return A collection of areas
 */
@WorkerThread
suspend fun loadAreas(
    context: Context,
    jsonData: JSONObject
): List<Area> {
    val dataSingleton = DataSingleton.getInstance(context)

    val indexedDataFlow = PreferencesModule
        .systemPreferencesRepository
        .indexedData
    val indexedSearch = indexedDataFlow.first()
    if (indexedSearch) {
        Timber.v("Search results already indexed. Fetching from application...")
        val list = dataSingleton
            .repository
            .getAreas() // If not empty, return areas
            .map { it.data() }
            .ifEmpty {
                // If empty, reset the preference, and launch loadAreas again
                Timber.w("Areas is empty, resetting search indexed pref and launching again.")
                PreferencesModule
                    .systemPreferencesRepository
                    .markDataIndexed(false)
                loadAreas(context, jsonData)
            }
        dataSingleton.areas.value = list
        return list
    }

    val performance = Firebase.performance
    val trace = performance.newTrace("loadAreasTrace")

    trace.start()

    Timber.d("Processing data...")
    try {
        val decodedPaths = decode(jsonData)
        val decodedSectors = decode(
            jsonData,
            { id -> decodedPaths.filter { it.parentSectorId == id }.size.toLong() },
            Sector.NAMESPACE,
            Sector.CONSTRUCTOR,
        )
        val decodedZones = decode(
            jsonData,
            { id -> decodedSectors.filter { it.zone == id }.size.toLong() },
            Zone.NAMESPACE,
            Zone.CONSTRUCTOR,
        )
        val decodedAreas = decode(
            jsonData,
            { id -> decodedZones.filter { it.area == id }.size.toLong() },
            Area.NAMESPACE,
            Area.CONSTRUCTOR
        ) { it.displayName }

        trace.putMetric("areasCount", decodedAreas.size.toLong())
        trace.putMetric("zonesCount", decodedZones.size.toLong())
        trace.putMetric("sectorsCount", decodedSectors.size.toLong())
        trace.putMetric("pathsCount", decodedPaths.size.toLong())

        Timber.v("Search > Adding documents...")
        dataSingleton.repository
            .addAll(decodedAreas)
            .addAll(decodedZones)
            .addAll(decodedSectors)
            .addAll(decodedPaths)

        Timber.i("Search > Added documents")

        Timber.v("Search > Storing to preferences...")
        PreferencesModule
            .systemPreferencesRepository
            .markDataIndexed()

        Timber.v("Storing version and update date...")
        val calendar = Calendar.getInstance()
        val now = calendar.time
        val versionDateFormatting = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
        val version = versionDateFormatting.format(now)
        Timber.v("New version: $version")
        PreferencesModule
            .systemPreferencesRepository
            .setDataVersion(now.time)

        trace.stop()

        return decodedAreas
            .toDataClassList()
            .also { dataSingleton.areas.value = it }
    } catch (e: Exception) {
        Timber.e(e, "Could not load areas.")
        trace.putAttribute("error", "true")
        trace.stop()
        uiContext { toast(context, R.string.toast_error_load_areas) }
        return emptyList()
    }
}
