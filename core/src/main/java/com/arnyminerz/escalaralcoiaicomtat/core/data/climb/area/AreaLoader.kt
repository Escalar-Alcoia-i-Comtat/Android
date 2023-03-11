package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import android.content.Context
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.data.SemVer
import com.arnyminerz.escalaralcoiaicomtat.core.data.SemVer.Companion.DIFF_PATCH
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.Keys
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.get
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.set
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXPECTED_SERVER_VERSION
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

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
private fun <D : DataClass<*, *>> decode(
    jsonData: JSONObject,
    childrenCount: (objectId: String) -> Long,
    namespace: Namespace,
    constructor: (data: JSONObject, id: String, childrenCount: Long) -> D,
) = decode<D, Int>(jsonData, childrenCount, namespace, constructor, null)

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
private fun <D : DataClass<*, *>, R : Comparable<R>> decode(
    jsonData: JSONObject,
    childrenCount: (objectId: String) -> Long,
    namespace: Namespace,
    constructor: (data: JSONObject, id: String, childrenCount: Long) -> D,
    sortBy: ((D) -> R?)? = null,
): List<D> {
    val index = arrayListOf<D>()
    val jsonObject = jsonData.getJSONObject(namespace.tableName)
    val keys = jsonObject.keys()
    for (id in keys) {
        val json = jsonObject.getJSONObject(id)

        // Process the DataClass data
        val dataClass = constructor(json, id, childrenCount(id))

        // Add the DataClass to the list
        index.add(dataClass)
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
): List<Path> {
    val index = arrayListOf<Path>()
    val jsonObject = jsonData.getJSONObject(Path.NAMESPACE.tableName)
    val keys = jsonObject.keys()
    for (id in keys) {
        val json = jsonObject.getJSONObject(id)

        // Process the Path data
        val path = Path(json, id)

        // Add the path to the list
        index.add(path)
    }
    return index
}

/**
 * Loads all the areas available in the server.
 * Custom progress callbacks:
 * * 0/0 paths are being processed.
 * @author Arnau Mora
 * @since 20210313
 * @param context The context used for fetching and putting data into the index.
 * @param jsonData The data loaded from the data module
 * @param infoJson The JSON given by the information endpoint on the server.
 * @param firstIteration Used for making sure no [StackOverflowError] are thrown.
 * @return A collection of areas
 * @throws IllegalArgumentException When it's the second time the function is called, and no areas
 * get loaded.
 * @throws SecurityException When the version given by the server and the expected by the app
 * do not match.
 */
@WorkerThread
@Throws(IllegalArgumentException::class, SecurityException::class)
suspend fun loadAreas(
    context: Context,
    jsonData: JSONObject,
    infoJson: JSONObject,
    firstIteration: Boolean = true,
): List<Area> {
    // Get the DataSingleton instance, for modifying the Room storage
    val dataSingleton = DataSingleton.getInstance(context)

    // Get from the preferences module if the data has ever been indexed
    val indexedSearch = context.get(Keys.indexedData, false)

    // If the data has been indexed before, check if it really has been stored, this is, there
    // are stored Areas.
    if (indexedSearch) {
        Timber.v("Search results already indexed. Fetching from application...")
        val list = dataSingleton
            .repository
            .getAreas() // If not empty, return areas
            .ifEmpty {
                // If empty, reset the preference, and launch loadAreas again
                Timber.w("Areas is empty, resetting search indexed pref and launching again.")
                context.set(Keys.indexedData, false)
                if (firstIteration)
                    loadAreas(context, jsonData, infoJson, false)
                else
                    throw IllegalArgumentException("Data from server is not valid, does not contain any Areas.")
            }
        dataSingleton.areas.value = list
        return list
    }

    val serverVersion = try {
        SemVer.fromString(infoJson.getString("version"))
    } catch (e: IllegalStateException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
    val serverProduction = infoJson.getBoolean("isProduction")

    Timber.i("Server version: $serverVersion. Production: $serverProduction")

    if (serverVersion != null && serverVersion.compare(EXPECTED_SERVER_VERSION) > DIFF_PATCH) {
        throw SecurityException("The version of the server ($serverVersion) doesn't match the one expected ($EXPECTED_SERVER_VERSION), load cannot be performed.")
    }

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
            { id -> decodedSectors.filter { it.parentZoneId == id }.size.toLong() },
            Zone.NAMESPACE,
            Zone.CONSTRUCTOR,
        )
        val decodedAreas = decode(
            jsonData,
            { id -> decodedZones.filter { it.parentAreaId == id }.size.toLong() },
            Area.NAMESPACE,
            Area.CONSTRUCTOR
        ) { it.displayName }

        Timber.v("Search > Adding documents...")
        dataSingleton.repository
            .addAll(decodedAreas)
            .addAll(decodedZones)
            .addAll(decodedSectors)
            .addAll(decodedPaths)

        Timber.i("Search > Added documents")

        Timber.v("Search > Storing to preferences...")
        context.set(Keys.indexedData, true)

        Timber.v("Storing version and update date...")
        val calendar = Calendar.getInstance()
        val now = calendar.time
        val versionDateFormatting = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
        val version = versionDateFormatting.format(now)
        Timber.v("New version: $version")
        context.set(Keys.dataVersion, now.time)

        Timber.v("Storing server info...")
        context.set(Keys.serverVersion, serverVersion.toString())
        context.set(Keys.serverIsProduction, serverProduction)

        return decodedAreas
            .also { dataSingleton.areas.value = it }
    } catch (e: ExceptionInInitializerError) {
        Timber.e(e, "Could not load areas.")
        throw ExceptionInInitializerError(
            "Exception: $e. JSON: $jsonData",
        )
    } catch (e: Exception) {
        Timber.e(e, "Could not load areas.")
        uiContext { toast(context, R.string.toast_error_load_areas) }
        return emptyList()
    }
}
