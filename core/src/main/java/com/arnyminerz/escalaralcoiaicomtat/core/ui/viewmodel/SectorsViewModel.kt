package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SectorsViewModel(private val areaId: String, private val zoneId: String) :
    DataClassViewModel<Sector>() {
    override val columnsPerRow: Int = 1

    override val items: LiveData<List<Sector>> = liveData {
        if (AREAS.isEmpty())
            suspendCoroutine<List<Area>> { cont ->
                loadAreas(firestore, storage, progressCallback = { current, total ->
                    Timber.i("Loading areas: $current/$total")
                }) {
                    cont.resume(AREAS)
                }
            }
        val sectors = AREAS[areaId]?.get(zoneId)?.getChildren(firestore, storage)
        if (sectors != null)
            emit(sectors)
        else Timber.e("Could not find Z/$zoneId in A/$areaId")
    }
}