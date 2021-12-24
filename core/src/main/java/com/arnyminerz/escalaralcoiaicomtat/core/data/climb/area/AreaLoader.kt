package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import android.app.Application
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SetSchemaRequest
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.data
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.data
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.data
import com.arnyminerz.escalaralcoiaicomtat.core.shared.*
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Loads all the areas available in the server.
 * Custom progress callbacks:
 * - 0/0 paths are being processed.
 * @author Arnau Mora
 * @since 20210313
 * @param application The [Application] that owns the app execution.
 * @param jsonData The data loaded from the data module
 * @param progressCallback This will get called when the loading progress is updated.
 * @return A collection of areas
 */
@WorkerThread
suspend fun loadAreas(
    application: App,
    jsonData: JSONObject,
    @UiThread progressCallback: ((progress: ValueMax<Int>) -> Unit)? = null
): List<Area> {
    val indexedSearch = PREF_INDEXED_SEARCH.get()
    if (indexedSearch) {
        Timber.v("Search results already indexed. Fetching from application...")
        return application.getAreas() // If not empty, return areas
            .ifEmpty {
                // If empty, reset the preference, and launch loadAreas again
                Timber.w("Areas is empty, reseting search indexed pref and launching again.")
                PREF_INDEXED_SEARCH.put(false)
                loadAreas(application, jsonData, progressCallback)
            }
    }

    val performance = Firebase.performance
    val trace = performance.newTrace("loadAreasTrace")

    trace.start()

    Timber.d("Fetching data from data module...")
    try {
        val areas = arrayListOf<Area>()
        val zones = arrayListOf<Zone>()
        val sectors = arrayListOf<Sector>()
        val paths = arrayListOf<Path>()

        val areasIndex = arrayListOf<AreaData>()
        val zonesIndex = arrayListOf<ZoneData>()
        val sectorsIndex = arrayListOf<SectorData>()
        val pathsIndex = arrayListOf<PathData>()

        val areaKeys = jsonData.keys()
        for ((a, areaKey) in areaKeys.withIndex()) {
            val areaJson = jsonData.getJSONObject(areaKey)
            val areaPath = "/Areas/$areaKey"

            // Process the Area data
            val area = Area(areaJson, areaPath)

            // Add the area to the list
            areas.add(area)
            areasIndex.add(area.data(a))

            val zonesJson = areaJson
                .getJSONObject("__collections__")
                .getJSONObject("Zones")
            val zoneKeys = zonesJson.keys()
            for ((z, zoneKey) in zoneKeys.withIndex()) {
                val zoneJson = zonesJson.getJSONObject(zoneKey)
                val zonePath = "$areaPath/Zones/$zoneKey"

                // Process the Zone data
                val zone = Zone(zoneJson, zonePath)

                // Add the zone to the list
                zones.add(zone)
                zonesIndex.add(zone.data(z))

                val sectorsJson = zoneJson
                    .getJSONObject("__collections__")
                    .getJSONObject("Sectors")
                val sectorKeys = sectorsJson.keys()
                for ((s, sectorKey) in sectorKeys.withIndex()) {
                    val sectorJson = sectorsJson.getJSONObject(sectorKey)
                    val sectorPath = "$zonePath/Sectors/$sectorKey"

                    // Process the Sector data
                    val sector = Sector(sectorJson, sectorPath)

                    // Add the sector to the list
                    sectors.add(sector)
                    sectorsIndex.add(sector.data(s))

                    val pathsJson = sectorJson
                        .getJSONObject("__collections__")
                        .getJSONObject("Paths")
                    val pathKeys = pathsJson.keys()
                    for (pathKey in pathKeys) {
                        val pathJson = pathsJson.getJSONObject(pathKey)
                        val pathPath = "$sectorPath/Sectors/$pathKey"

                        // Process the path data
                        val path = Path(pathJson, pathPath, sectorKey)

                        // Add the path to the list
                        paths.add(path)
                        pathsIndex.add(path.data())
                    }
                }
            }
        }

        Timber.v("Sorting areas...")
        areas.sortBy { it.displayName }
        areasIndex.sortBy { it.displayName }

        Timber.v("Search > Initializing session future...")
        val session = application.searchSession
        val time = System.currentTimeMillis()
        Timber.v("Search > Adding document classes...")
        val setSchemaRequest = SetSchemaRequest.Builder()
            .addDocumentClasses(SEARCH_SCHEMAS)
            .build()
        session.setSchema(setSchemaRequest).await()
        Timber.i("Set schema time: ${System.currentTimeMillis() - time}")

        Timber.v("Search > Adding documents...")
        val putRequest = PutDocumentsRequest.Builder()
            .addDocuments(areasIndex)
            .addDocuments(zonesIndex)
            .addDocuments(sectorsIndex)
            .addDocuments(pathsIndex)
            .build()
        val putResponse = session.put(putRequest).await()
        val successfulResults = putResponse?.successes
        val failedResults = putResponse?.failures
        if (successfulResults?.isEmpty() != true)
            Timber.i("Search > Added ${successfulResults?.size} documents...")
        if (failedResults?.isEmpty() != true)
            Timber.w("Search > Could not add ${failedResults?.size} documents...")

        // This persists the data to the disk
        Timber.v("Search > Flushing database...")
        session.requestFlush().await()

        Timber.v("Search > Storing to preferences...")
        PREF_INDEXED_SEARCH.put(true)

        Timber.v("Storing version and update date...")
        val calendar = Calendar.getInstance()
        val now = calendar.time
        val versionDateFormatting = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
        val version = versionDateFormatting.format(now)
        Timber.v("New version: $version")
        PREF_DATA_VERSION.put(version)
        PREF_DATA_DATE.put(now.time)

        trace.stop()

        return areas
    } catch (e: FirebaseFirestoreException) {
        Timber.e(e, "Could not load areas.")
        trace.putAttribute("error", "true")
        trace.stop()
        uiContext { toast(application, R.string.toast_error_load_areas) }
        return emptyList()
    }
}
