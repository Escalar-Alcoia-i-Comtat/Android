package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import android.app.Activity
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.appsearch.app.AppSearchBatchResult
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.app.SetSchemaResponse
import androidx.appsearch.localstorage.LocalStorage
import androidx.collection.arrayMapOf
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
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_FULL_DATA_LOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.utils.asyncCoroutineScope
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Loads all the areas available in the server.
 * @author Arnau Mora
 * @since 20210313
 * @param activity The [Activity]
 * @param firestore The [FirebaseFirestore] reference for fetching data from the server.
 * @param scope The [CoroutineScope] to run on.
 * @param progressCallback This will get called when the loading progress is updated.
 * @param callback This will get called when all the data has been loaded.
 * @see AREAS
 * @return A collection of areas
 */
@MainThread
fun loadAreas(
    activity: Activity,
    firestore: FirebaseFirestore,
    enableSearch: Boolean = false,
    scope: CoroutineScope = asyncCoroutineScope,
    @UiThread progressCallback: (current: Int, total: Int) -> Unit,
    @UiThread callback: () -> Unit
) {
    val trace = Firebase.performance.newTrace("loadAreasTrace")

    trace.start()

    val fullDataLoad = SETTINGS_FULL_DATA_LOAD_PREF.get()
    trace.putAttribute("full_load", fullDataLoad.toString())

    Timber.d("Fetching areas...")
    scope.launch {
        try {
            Timber.v("Getting paths...")
            val pathsSnapshot = firestore
                .collectionGroup("Paths")
                .get()
                .await()
            Timber.v("Getting sectors...")
            val sectorsSnapshot = firestore
                .collectionGroup("Sectors")
                .get()
                .await()
            Timber.v("Getting zones...")
            val zonesSnapshot = firestore
                .collectionGroup("Zones")
                .get()
                .await()
            Timber.v("Getting areas...")
            val areasSnapshot = firestore
                .collectionGroup("Areas")
                .get()
                .await()

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
                .sortedBy { snapshot -> snapshot.getString("displayName") }
            Timber.v("Getting zone documents...")
            val zoneDocuments = zonesSnapshot.documents
                .sortedBy { snapshot -> snapshot.getString("displayName") }
            Timber.v("Getting area documents...")
            val areaDocuments = areasSnapshot.documents
                .sortedBy { snapshot -> snapshot.getString("displayName") }

            Timber.v("Counting paths...")
            val pathsCount = pathDocuments.size
            Timber.v("Counting sectors...")
            val sectorsCount = sectorDocuments.size
            Timber.v("Counting zones...")
            val zonesCount = zoneDocuments.size
            Timber.v("Counting areas...")
            val areasCount = areaDocuments.size

            trace.putAttribute("areasCount", areasCount.toString())
            trace.putAttribute("zonesCount", zonesCount.toString())
            trace.putAttribute("sectorsCount", sectorsCount.toString())
            trace.putAttribute("pathsCount", pathsCount.toString())

            var counter = 0
            val count = areasCount + zonesCount + sectorsCount + pathsCount

            Timber.v("Creation relation with sector and path documents...")
            // The key is the sector id, the value the snapshot of the path
            val sectorIdPathDocument = arrayMapOf<String, ArrayList<DocumentSnapshot>>()
            for (pathDocument in pathDocuments) {
                val sectorId = pathDocument.reference.parent.parent!!.id
                if (sectorIdPathDocument.containsKey(sectorId))
                    sectorIdPathDocument[sectorId] = arrayListOf(pathDocument)
                else
                    sectorIdPathDocument[sectorId]?.add(pathDocument)
            }

            Timber.v("Expanding zone documents...")
            val expandedZoneDocuments = arrayMapOf<String, DocumentSnapshot>()
            for (zoneDocument in zoneDocuments)
                expandedZoneDocuments[zoneDocument.id] = zoneDocument
            Timber.v("Expanding area documents...")
            val expandedAreaDocuments = arrayMapOf<String, DocumentSnapshot>()
            for (areaDocument in areaDocuments)
                expandedAreaDocuments[areaDocument.id] = areaDocument

            Timber.v("Iterating $sectorsCount sector documents...")
            for (sectorDocument in sectorDocuments) {
                uiContext { progressCallback(++counter, count) }

                val sectorId = sectorDocument.id
                Timber.v("S/$sectorId > Getting sector's reference...")
                val sectorReference = sectorDocument.reference
                Timber.v("S/$sectorId > Getting sector's parent zone.")
                val sectorParentZone = sectorReference.parent.parent ?: run {
                    Timber.e("S/$sectorId > Could not find parent zone.")
                    return@launch
                }
                Timber.v("S/$sectorId > Getting sector's parent zone's id.")
                val zoneId = sectorParentZone.id
                if (!zonesCache.containsKey(zoneId)) {
                    uiContext { progressCallback(++counter, count) }
                    Timber.v("S/$sectorId > There's no cached version for Z/$zoneId.")
                    val zoneDocument = expandedZoneDocuments[zoneId]
                    if (zoneDocument == null) {
                        Timber.e("S/$sectorId > Could not find zone (Z/$zoneId) in documents.")
                        return@launch
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
                    for (pathDocument in paths) {
                        Timber.v("P/${pathDocument.id} > Processing...")
                        val path = Path(activity, pathDocument)
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
                    return@launch
                }
                Timber.v("Z/$zoneId > Getting Zone reference...")
                val zoneReference = zoneDocument.reference
                Timber.v("Z/$zoneId > Getting Zone's parent Area reference...")
                val zoneParentAreaReference = zoneReference.parent.parent ?: run {
                    Timber.e("Z/$zoneId > Could not find parent area.")
                    return@launch
                }
                Timber.v("Z/$zoneId > Getting Zone's parent Area id...")
                val zoneParentAreaId = zoneParentAreaReference.id
                if (!areasCache.containsKey(zoneParentAreaId)) {
                    uiContext { progressCallback(++counter, count) }
                    Timber.v("Z/$zoneId > Could not find A/$zoneParentAreaId in cache...")
                    val areaDocument = expandedAreaDocuments[zoneParentAreaId]
                    if (areaDocument == null) {
                        Timber.e("Z/$zoneId > Could not find area (A/$zoneParentAreaId) in documents.")
                        return@launch
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
            Timber.v("Ordering areas...")
            val areas = areasCache.values

            Timber.v("Clearing AREAS...")
            AREAS.clear()
            Timber.v("Adding all areas to AREAS...")
            AREAS.addAll(areas)

            if (enableSearch) {
                Timber.v("Search > Initializing session future...")
                val sessionFuture = LocalStorage.createSearchSession(
                    LocalStorage.SearchContext.Builder(activity, "escalaralcoiaicomtat")
                        .build()
                )
                val executor = Executors.newSingleThreadExecutor()
                Timber.v("Search > Adding document classes...")
                val setSchemaRequest = SetSchemaRequest.Builder()
                    .addDocumentClasses(AreaData::class.java)
                    .addDocumentClasses(ZoneData::class.java)
                    .addDocumentClasses(SectorData::class.java)
                    .addDocumentClasses(PathData::class.java)
                    .build()
                val setSchemaFuture = Futures.transformAsync(
                    sessionFuture,
                    { session ->
                        session?.setSchema(setSchemaRequest)
                    }, executor
                )
                suspendCoroutine<SetSchemaResponse?> { cont ->
                    Futures.addCallback(
                        setSchemaFuture,
                        object : FutureCallback<SetSchemaResponse?> {
                            override fun onSuccess(result: SetSchemaResponse?) {
                                cont.resume(result)
                            }

                            override fun onFailure(t: Throwable) {
                                Timber.e(t, "Search > Could not add document classes.")
                                cont.resumeWithException(t)
                            }
                        },
                        executor
                    )
                }

                Timber.v("Search > Adding documents...")
                val putRequest = PutDocumentsRequest.Builder()
                    .addDocuments(areasIndex)
                    .addDocuments(zonesIndex)
                    .addDocuments(sectorsIndex)
                    .addDocuments(pathsIndex)
                    .build()
                val putFuture = Futures.transformAsync(
                    sessionFuture,
                    { session -> session?.put(putRequest) },
                    executor
                )
                val putResponse = suspendCoroutine<AppSearchBatchResult<String, Void>?> { cont ->
                    Futures.addCallback(
                        putFuture,
                        object : FutureCallback<AppSearchBatchResult<String, Void>?> {
                            override fun onSuccess(result: AppSearchBatchResult<String, Void>?) {
                                cont.resume(result)
                            }

                            override fun onFailure(t: Throwable) {
                                Timber.e(t, "Search > Could not add documents.")
                                cont.resumeWithException(t)
                            }
                        },
                        executor
                    )
                }
                val successfulResults = putResponse?.successes
                val failedResults = putResponse?.failures
                if (successfulResults?.isEmpty() != true)
                    Timber.i("Search > Added ${successfulResults?.size} documents...")
                if (failedResults?.isEmpty() != true)
                    Timber.w("Search > Could not add ${failedResults?.size} documents...")

                Timber.v("Search > Flushing database...")
                val flushFuture = Futures.transformAsync(
                    sessionFuture,
                    { session -> session?.requestFlush() },
                    executor
                )
                suspendCoroutine<Void?> { cont ->
                    Futures.addCallback(
                        flushFuture,
                        object : FutureCallback<Void?> {
                            override fun onSuccess(result: Void?) {
                                cont.resume(result)
                            }

                            override fun onFailure(t: Throwable) {
                                Timber.e(t, "Search > Could not flush database.")
                                cont.resumeWithException(t)
                            }
                        },
                        executor
                    )
                }

                Timber.v("Search > Closing database...")
                val closeFuture = Futures.transform<AppSearchSession, Unit>(
                    sessionFuture,
                    { session -> session?.close() },
                    executor
                )
                suspendCoroutine<Unit?> { cont ->
                    Futures.addCallback(
                        closeFuture,
                        object : FutureCallback<Unit> {
                            override fun onSuccess(result: Unit?) {
                                cont.resume(result)
                            }

                            override fun onFailure(t: Throwable) {
                                Timber.e(t, "Search > Could not flush database.")
                                cont.resumeWithException(t)
                            }
                        },
                        executor
                    )
                }
            }

            trace.stop()

            Timber.v("Running callback...")
            uiContext { callback() }
        } catch (e: FirebaseFirestoreException) {
            Timber.e(e, "Could not load areas.")
            trace.putAttribute("error", "true")
            trace.stop()
            uiContext { toast(activity, R.string.toast_error_load_areas) }
        }
    }

    /*firestore
        .collection("Areas")
        .get()
        .addOnFailureListener { e ->
            Timber.w(e, "Could not get areas.")
            trace.putAttribute("error", "true")
            trace.stop()
            toast(R.string.toast_error_load_areas)
        }
        .addOnSuccessListener { result ->
            val areas = arrayListOf<Area>()
            val areasCount = result.size()
            Timber.d("Got $areasCount areas. Processing...")
            for ((a, areaData) in result.documents.withIndex()) {
                Timber.v("Validating area from collection: ${areaData.reference.parent.id}")
                if (Area.validate(areaData))
                    areas.add(Area(areaData))
                else
                    Timber.w("Could not load Area (${areaData.reference}) data. Some parameters are missing.")
                progressCallback(a, areasCount)
                trace.incrementMetric("dataClassCount", 1)
            }
            Timber.v("Areas processed, ordering them...")
            areas.sortBy { area -> area.displayName }
            Timber.v("Storing loaded areas...")
            synchronized(AREAS) {
                AREAS.clear()
                AREAS.addAll(areas)
            }

            if (fullDataLoad)
                doAsync {
                    Timber.v("Getting all areas' children...")
                    for ((a, area) in areas.withIndex()) {
                        uiContext { progressCallback(a, areasCount) }
                        trace.incrementMetric("dataClassCount", 1)

                        Timber.v("Getting zones of ${area.objectId}")
                        val zones = area.getChildren(firestore)
                        val zonesCount = zones.count()
                        for ((z, zone) in zones.withIndex()) {
                            uiContext { progressCallback(z, zonesCount) }
                            trace.incrementMetric("dataClassCount", 1)

                            Timber.v("Getting sectors of ${zone.objectId}")
                            zone.getChildren(firestore)
                        }
                    }
                    Timber.v("Finished loading children. Calling callback...")
                    trace.stop()
                    uiContext { callback() }
                }
            else {
                Timber.v("Calling callback...")
                trace.stop()
                callback()
            }
        }*/
}
