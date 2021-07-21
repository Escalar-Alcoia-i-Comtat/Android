package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_FULL_DATA_LOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.utils.asyncCoroutineScope
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Loads all the areas available in the server.
 * @author Arnau Mora
 * @since 20210313
 * @param firestore The [FirebaseFirestore] reference for fetching data from the server.
 * @param storage The [FirebaseStorage] reference for fetching files from the server.
 * @param context The context to load the areas from. If null, no error toasts will be shown.
 * @param scope The [CoroutineScope] to run on.
 * @param progressCallback This will get called when the loading progress is updated.
 * @param callback This will get called when all the data has been loaded.
 * @see AREAS
 * @return A collection of areas
 */
@MainThread
fun loadAreas(
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    context: Context? = null,
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

            Timber.v("Getting sector documents...")
            val sectorDocuments = sectorsSnapshot.documents
            Timber.v("Getting zone documents...")
            val zoneDocuments = zonesSnapshot.documents
            Timber.v("Getting area documents...")
            val areaDocuments = areasSnapshot.documents

            Timber.v("Counting sectors...")
            val sectorsCount = sectorDocuments.size
            Timber.v("Counting zones...")
            val zonesCount = zoneDocuments.size
            Timber.v("Counting areas...")
            val areasCount = areaDocuments.size

            trace.putAttribute("areasCount", areasCount.toString())
            trace.putAttribute("zonesCount", zonesCount.toString())
            trace.putAttribute("sectorsCount", sectorsCount.toString())

            var counter = 0
            val count = areasCount + zonesCount + sectorsCount

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
                        zonesCache[zoneId] = Zone(zoneDocument)
                    }
                }
                Timber.v("S/$sectorId > Getting zone Z/$zoneId from cache...")
                val zone = zonesCache[zoneId]
                Timber.v("S/$sectorId > Processing sector data...")
                val sector = Sector(sectorDocument)
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

            Timber.v("Finished iterating documents.")
            Timber.v("Ordering areas...")
            val areas = areasCache.values.sortedBy { area -> area.displayName }

            Timber.v("Getting area download urls...")
            for (area in areas) {
                Timber.v("A/$area > Getting download url...")
                area?.storageUrl(storage)
            }

            Timber.v("Clearing AREAS...")
            AREAS.clear()
            Timber.v("Adding all areas to AREAS...")
            AREAS.addAll(areas)

            trace.stop()

            Timber.v("Running callback...")
            uiContext { callback() }
        } catch (e: FirebaseFirestoreException) {
            Timber.e(e, "Could not load areas.")
            trace.putAttribute("error", "true")
            trace.stop()
            if (context != null)
                uiContext { toast(context, R.string.toast_error_load_areas) }
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
