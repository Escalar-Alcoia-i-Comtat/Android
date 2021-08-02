package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

class SectorViewModel(
    activity: Activity,
    private val areaId: String,
    private val zoneId: String,
    private val sectorId: String
) : AndroidViewModel(activity.application) {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    private val innerSector: Sector?
        get() = AREAS[areaId]?.get(zoneId)?.get(sectorId)

    val sector: LiveData<Sector?> = liveData {
        if (AREAS.isEmpty()) {
            val application = (context as? Activity)?.application ?: context as Application
            firestore.loadAreas(application, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
            })
        }
        emit(innerSector)
    }

    val items: LiveData<List<Path>> = liveData {
        val paths = innerSector?.getChildren(context, storage)
        if (paths != null)
            emit(paths)
        else Timber.e("Could not find S/$sectorId in Z/$zoneId in A/$areaId")
    }
}
