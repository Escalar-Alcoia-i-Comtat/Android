package com.arnyminerz.escalaralcoiaicomtat.data.climb.area

import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

/**
 * Loads all the areas available in the server.
 * @author Arnau Mora
 * @since 20210313
 * @see AREAS
 * @return A collection of areas
 */
fun loadAreasFromCache(
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
        }
        .addOnSuccessListener { result ->
            val areas = arrayMapOf<String, Area>()
            val areasCount = result.size()
            Timber.d("Got $areasCount areas. Processing...")
            for ((a, areaData) in result.documents.withIndex()) {
                val area = Area(areaData)
                areas[area.objectId] = area
                progressCallback(a, areasCount)
            }
            AREAS.clear()
            AREAS.putAll(
                areas.toList().sortedBy { (_, value) -> value.displayName }.toMap()
            )
            callback()
        }
}
