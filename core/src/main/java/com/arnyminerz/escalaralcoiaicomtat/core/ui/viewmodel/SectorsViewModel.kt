package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SectorsViewModel(
    application: App,
    private val areaId: String,
    private val zoneId: String
) : DataClassViewModel<Sector>(application) {
    override val columnsPerRow: Int = 1

    override val fixedHeight: Dp = 200.dp

    override val items: LiveData<List<Sector>> = liveData {
        if (AREAS.isEmpty())
            suspendCoroutine<List<Area>> { cont ->
                loadAreas(context, firestore, storage, progressCallback = { current, total ->
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