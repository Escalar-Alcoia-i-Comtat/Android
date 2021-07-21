package com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

class SectorViewModel(
    private val areaId: String,
    private val zoneId: String,
    private val sectorId: String
) : ViewModel() {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    val items: LiveData<List<Path>> = liveData {
        val paths = AREAS[areaId]?.get(zoneId)?.get(sectorId)?.getChildren(firestore, storage)
        if (paths != null)
            emit(paths)
        else Timber.e("Could not find S/$sectorId in Z/$zoneId in A/$areaId")
    }
}