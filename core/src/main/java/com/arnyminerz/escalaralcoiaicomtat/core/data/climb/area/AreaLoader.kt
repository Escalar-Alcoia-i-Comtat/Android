package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import android.content.Context
import androidx.annotation.MainThread
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.google.firebase.firestore.FirebaseFirestore
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
    progressCallback: (current: Int, total: Int) -> Unit,
    callback: () -> Unit
) {
    Timber.d("Querying areas...")
    Timber.d("Fetching areas...")
    firestore
        .collection("Areas")
        .get()
        .addOnFailureListener { e ->
            Timber.w(e, "Could not get areas.")
            toast(R.string.toast_error_load_areas)
        }
        .addOnSuccessListener { result ->
            val areas = arrayListOf<Area>()
            val areasCount = result.size()
            Timber.d("Got $areasCount areas. Processing...")
            for ((a, areaData) in result.documents.withIndex()) {
                if (Area.validate(areaData))
                    areas.add(
                        Area(areaData)
                    )
                else
                    Timber.w("Could not load Area (${areaData.reference}) data. Some parameters are missing.")
                progressCallback(a, areasCount)
            }
            Timber.v("Areas processed, ordering them...")
            areas.sortBy { area -> area.displayName }
            Timber.v("Storing loaded areas...")
            synchronized(AREAS) {
                AREAS.clear()
                AREAS.addAll(areas)
            }
            callback()
        }
}
