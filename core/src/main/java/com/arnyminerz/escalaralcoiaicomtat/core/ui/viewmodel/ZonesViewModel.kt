package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ZonesViewModel(application: Application, private val areaId: String) :
    DataClassViewModel<Zone>(application) {
    override val columnsPerRow: Int = 2

    override val items: LiveData<List<Zone>> = liveData {
        if (AREAS.isEmpty())
            suspendCoroutine<List<Area>> { cont ->
                loadAreas(context, firestore, storage, progressCallback = { current, total ->
                    Timber.i("Loading areas: $current/$total")
                }) {
                    cont.resume(AREAS)
                }
            }
        val zones = AREAS[areaId]?.getChildren(firestore, storage)
        if (zones != null)
            emit(zones)
        else Timber.e("Could not find A/$areaId")
    }
}