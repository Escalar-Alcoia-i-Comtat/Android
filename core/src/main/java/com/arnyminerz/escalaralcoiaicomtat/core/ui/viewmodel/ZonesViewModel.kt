package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Activity
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

class ZonesViewModel<A : Activity>(activity: A, private val areaId: String) :
    DataClassViewModel<Zone, A>(activity) {
    override val columnsPerRow: Int = 2

    override val items: LiveData<List<Zone>> = liveData {
        if (AREAS.isEmpty())
            suspendCoroutine<List<Area>> { cont ->
                loadAreas(context as Activity, firestore, progressCallback = { current, total ->
                    Timber.i("Loading areas: $current/$total")
                }) {
                    cont.resume(AREAS)
                }
            }
        val zones = AREAS[areaId]?.getChildren(context, storage)
        if (zones != null) {
            for (zone in zones)
                zone.image(context, storage)
            emit(zones)
        } else Timber.e("Could not find A/$areaId")
    }
}