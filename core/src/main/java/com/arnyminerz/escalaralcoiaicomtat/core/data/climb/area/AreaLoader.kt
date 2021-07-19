package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_FULL_DATA_LOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import timber.log.Timber

/**
 * Loads all the areas available in the server.
 * @author Arnau Mora
 * @since 20210313
 * @see AREAS
 * @return A collection of areas
 */
@MainThread
fun Context.loadAreas(
    firestore: FirebaseFirestore,
    @UiThread progressCallback: (current: Int, total: Int) -> Unit,
    @UiThread callback: () -> Unit
) {
    val trace = Firebase.performance.newTrace("loadAreasTrace")

    trace.start()

    val fullDataLoad = SETTINGS_FULL_DATA_LOAD_PREF.get()
    trace.putAttribute("full_load", fullDataLoad.toString())

    Timber.d("Querying areas...")
    Timber.d("Fetching areas...")
    firestore
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
        }
}
