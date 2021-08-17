package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import android.app.Application
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SetSchemaRequest
import androidx.collection.arrayMapOf
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
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREF_INDEXED_SEARCH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_FULL_DATA_LOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Loads all the areas available in the server.
 * Custom progress callbacks:
 * - 0/0 paths are being processed.
 * @author Arnau Mora
 * @since 20210313
 * @param application The [Application] that owns the app execution.
 * @param progressCallback This will get called when the loading progress is updated.
 * @return A collection of areas
 */
@WorkerThread
suspend fun FirebaseFirestore.loadAreas(
    application: App,
    @UiThread progressCallback: ((current: Int, total: Int) -> Unit)? = null
): List<Area> {
    val indexedSearch = PREF_INDEXED_SEARCH.get()
    if (indexedSearch)
        return application.getAreas()

    val performance = Firebase.performance
    val trace = performance.newTrace("loadAreasTrace")

    trace.start()

    val fullDataLoad = SETTINGS_FULL_DATA_LOAD_PREF.get()
    trace.putAttribute("full_load", fullDataLoad.toString())

    Timber.d("Fetching areas...")
    try {
        Timber.v("Getting paths...") // Around 3.8 seconds
        val pathsSnapshot = collectionGroup("Paths")
            .get()
            .await()

        Timber.v("Getting sectors...") // Around .5 seconds
        val sectorsSnapshot = collectionGroup("Sectors")
            .orderBy("displayName", Query.Direction.ASCENDING)
            .get()
            .await()

        Timber.v("Getting zones...") // Around .3 seconds
        val zonesSnapshot = collectionGroup("Zones")
            .orderBy("displayName", Query.Direction.ASCENDING)
            .get()
            .await()

        val areasFetchTrace = performance.newTrace("areasFetchTrace")
        areasFetchTrace.start()
        Timber.v("Getting areas...") // Around .3 seconds
        val areasSnapshot = collectionGroup("Areas")
            .orderBy("displayName", Query.Direction.ASCENDING)
            .get()
            .await()
        areasFetchTrace.stop()

        Timber.v("Initializing zones cache...")
        val zonesCache = arrayMapOf<String, Zone>()
        Timber.v("Initializing areas cache...")
        val areasCache = arrayMapOf<String, Area>()

        Timber.v("Initializing search index list...")
        val areasIndex = arrayListOf<AreaData>()
        val zonesIndex = arrayListOf<ZoneData>()
        val sectorsIndex = arrayListOf<SectorData>()
        val pathsIndex = arrayListOf<PathData>()

        Timber.v("Getting path documents...")
        val pathDocuments = pathsSnapshot.documents
            .sortedBy { snapshot -> snapshot.getString("sketchId")?.toInt() }
        Timber.v("Getting sector documents...")
        val sectorDocuments = sectorsSnapshot.documents
        Timber.v("Getting zone documents...")
        val zoneDocuments = zonesSnapshot.documents
        Timber.v("Getting area documents...")
        val areaDocuments = areasSnapshot.documents

        // Count time is approx 0ms, so shouldn't bother about this taking a lot of time.
        // Timber.v("Counting paths...")
        // val pathsCount = pathDocuments.size
        Timber.v("Counting sectors...")
        val sectorsCount = sectorDocuments.size
        Timber.v("Counting zones...")
        val zonesCount = zoneDocuments.size
        Timber.v("Counting areas...")
        val areasCount = areaDocuments.size

        var progressCounter = 0
        val progressMax = areasCount + zonesCount + sectorsCount

        Timber.v("Creation relation with sector and path documents...")
        // The key is the sector id, the value the snapshot of the path
        // This is what takes the most of the time, more than 7 seconds.
        val sectorIdPathDocument = arrayMapOf<String, ArrayList<Path>>()
        // Notify 0 progress, 0 max, for telling the UI that paths are being processed
        uiContext { progressCallback?.invoke(0, 0) }
        for (pathDocument in pathDocuments) {
            val pathId = pathDocument.id
            Timber.v("P/$pathId > Processing path...")
            val pathInitTime = System.currentTimeMillis()
            val path = Path(pathDocument)
            Timber.i("Path init time: ${System.currentTimeMillis() - pathInitTime}")
            val sectorId = pathDocument.reference.parent.parent!!.id
            Timber.v("P/$pathId > Adding to sector S/$sectorId...")
            sectorIdPathDocument[sectorId]?.add(path) ?: run {
                sectorIdPathDocument[sectorId] = arrayListOf(path)
            }
        }

        Timber.v("Iterating $sectorsCount sector documents...")
        for (sectorDocument in sectorDocuments) {
            uiContext { progressCallback?.invoke(++progressCounter, progressMax) }

            val sectorId = sectorDocument.id
            Timber.v("S/$sectorId > Getting sector's reference...")
            val sectorReference = sectorDocument.reference
            Timber.v("S/$sectorId > Getting sector's parent zone.")
            val sectorParentZone = sectorReference.parent.parent ?: run {
                Timber.e("S/$sectorId > Could not find parent zone.")
                return emptyList()
            }
            Timber.v("S/$sectorId > Getting sector's parent zone's id.")
            val zoneId = sectorParentZone.id
            if (!zonesCache.containsKey(zoneId)) {
                uiContext { progressCallback?.invoke(++progressCounter, progressMax) }
                Timber.v("S/$sectorId > There's no cached version for Z/$zoneId.")
                val zoneDocument = zoneDocuments.find { it.id == zoneId }
                if (zoneDocument == null) {
                    Timber.e("S/$sectorId > Could not find zone (Z/$zoneId) in documents.")
                    return emptyList()
                } else {
                    Timber.v("S/$sectorId > Caching zone Z/$zoneId...")
                    val zone = Zone(zoneDocument)
                    zonesCache[zoneId] = zone
                }
            }
            Timber.v("S/$sectorId > Getting zone Z/$zoneId from cache...")
            val zone = zonesCache[zoneId]
            Timber.v("S/$sectorId > Processing sector data...")
            val sector = Sector(sectorDocument)
            Timber.v("S/$sectorId > Adding sector to the search index...")
            sectorsIndex.add(sector.data())

            Timber.v("S/$sectorId > Loading paths...")
            val paths = sectorIdPathDocument[sectorId]
            if (paths != null) {
                for (path in paths) {
                    Timber.v("$path > Adding to the search index...")
                    pathsIndex.add(path.data())
                    Timber.v("$path > Adding to the sector ($sector)...")
                    sector.add(path)
                }
            } else
                Timber.w("S/$sectorId > There isn't any path for the sector.")

            Timber.v("S/$sectorId > Adding sector to zone Z/$zoneId...")
            zone?.add(sector)
        }

        Timber.v("Iterating $zonesCount zone documents...")
        for (zoneDocument in zoneDocuments) {
            val zoneId = zoneDocument.id
            Timber.v("Z/$zoneId > Getting Zone...")
            val zone = zonesCache[zoneId] ?: run {
                Timber.e("Z/$zoneId > Could not find zone in cache, maybe it doesn't have any sector?")
                return emptyList()
            }
            Timber.v("Z/$zoneId > Getting Zone reference...")
            val zoneReference = zoneDocument.reference
            Timber.v("Z/$zoneId > Getting Zone's parent Area reference...")
            val zoneParentAreaReference = zoneReference.parent.parent ?: run {
                Timber.e("Z/$zoneId > Could not find parent area.")
                return emptyList()
            }
            Timber.v("Z/$zoneId > Getting Zone's parent Area id...")
            val zoneParentAreaId = zoneParentAreaReference.id
            if (!areasCache.containsKey(zoneParentAreaId)) {
                uiContext { progressCallback?.invoke(++progressCounter, progressMax) }
                Timber.v("Z/$zoneId > Could not find A/$zoneParentAreaId in cache...")
                val areaDocument = areaDocuments.find { it.id == zoneParentAreaId }
                if (areaDocument == null) {
                    Timber.e("Z/$zoneId > Could not find area (A/$zoneParentAreaId) in documents.")
                    return emptyList()
                } else {
                    Timber.v("Z/$zoneId > Caching area A/$zoneParentAreaId...")
                    areasCache[zoneParentAreaId] = Area(areaDocument)
                }
            }
            Timber.v("Z/$zoneId > Getting area A/$zoneParentAreaId from cache...")
            val area = areasCache[zoneParentAreaId]
            Timber.v("Z/$zoneId > Adding zone to area A/$zoneParentAreaId...")
            area?.add(zone)
        }

        Timber.v("Adding zones to the search index...")
        for (zone in zonesCache.values)
            zonesIndex.add(zone.data())
        Timber.v("Adding areas to the search index...")
        for (area in areasCache.values)
            areasIndex.add(area.data())

        Timber.v("Finished iterating documents.")

        Timber.v("Search > Initializing session future...")
        val session = application.searchSession
        val time = System.currentTimeMillis()
        Timber.v("Search > Adding document classes...")
        val setSchemaRequest = SetSchemaRequest.Builder()
            .addDocumentClasses(AreaData::class.java)
            .addDocumentClasses(ZoneData::class.java)
            .addDocumentClasses(SectorData::class.java)
            .addDocumentClasses(PathData::class.java)
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

        trace.stop()

        return arrayListOf<Area>().apply { addAll(areasCache.values) }
    } catch (e: FirebaseFirestoreException) {
        Timber.e(e, "Could not load areas.")
        trace.putAttribute("error", "true")
        trace.stop()
        uiContext { toast(application, R.string.toast_error_load_areas) }
        return emptyList()
    }
}
