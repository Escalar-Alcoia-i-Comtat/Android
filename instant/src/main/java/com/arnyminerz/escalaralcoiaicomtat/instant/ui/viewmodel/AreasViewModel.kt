package com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AreasViewModel : ViewModel() {
    val firestore = Firebase.firestore
    val storage = Firebase.storage

    val areas: LiveData<List<Area>> = liveData {
        val areas = suspendCoroutine<List<Area>> { cont ->
            loadAreas(firestore, storage, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
            }) {
                cont.resume(AREAS)
            }
        }
        emit(areas)
    }
}