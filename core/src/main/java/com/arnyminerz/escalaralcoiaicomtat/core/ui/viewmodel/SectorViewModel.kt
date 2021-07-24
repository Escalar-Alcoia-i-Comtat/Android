package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SectorViewModel(
    application: App,
    private val areaId: String,
    private val zoneId: String,
    private val sectorId: String
) : AndroidViewModel(application) {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    private val innerSector: Sector?
        get() = AREAS[areaId]?.get(zoneId)?.get(sectorId)

    val sector: LiveData<Sector?> = liveData {
        if (AREAS.isEmpty())
            suspendCoroutine<List<Area>> { cont ->
                loadAreas(context, firestore, storage, progressCallback = { current, total ->
                    Timber.i("Loading areas: $current/$total")
                }) {
                    cont.resume(AREAS)
                }
            }
        emit(innerSector)
    }

    val items: LiveData<List<Path>> = liveData {
        val paths = innerSector?.getChildren(firestore, storage)
        if (paths != null)
            emit(paths)
        else Timber.e("Could not find S/$sectorId in Z/$zoneId in A/$areaId")
    }
}