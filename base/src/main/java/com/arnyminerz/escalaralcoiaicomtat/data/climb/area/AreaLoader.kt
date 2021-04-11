package com.arnyminerz.escalaralcoiaicomtat.data.climb.area

import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.MAX_BATCH_SIZE
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import timber.log.Timber

/**
 * Loads all the areas available in the server.
 * @author Arnau Mora
 * @since 20210313
 * @see MAX_BATCH_SIZE
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
        .orderBy("displayName", Query.Direction.ASCENDING)
        .get()
        .addOnFailureListener { e ->
            Timber.w(e, "Could not get areas.")
        }
        .addOnSuccessListener { result ->
            AREAS.clear()
            val areasCount = result.size()
            Timber.d("Got $areasCount areas. Processing...")
            for ((a, areaData) in result.documents.withIndex()) {
                val area = Area(areaData)
                AREAS[area.objectId] = area
                progressCallback(a, areasCount)
            }
            callback()
        }
}
