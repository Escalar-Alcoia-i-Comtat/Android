package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Activity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import timber.log.Timber

class SectorsViewModel<A : Activity>(
    activity: A,
    private val areaId: String,
    private val zoneId: String
) : DataClassViewModel<Sector, A>(activity) {
    override val columnsPerRow: Int = 1

    override val fixedHeight: Dp = 200.dp

    override val items: LiveData<List<Sector>> = liveData {
        if (AREAS.isEmpty())
            loadAreas(context, firestore, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
            })
        val sectors = AREAS[areaId]?.get(zoneId)?.getChildren(context, storage)
        if (sectors != null) {
            for (sector in sectors)
                sector.image(context, storage)
            emit(sectors)
        } else Timber.e("Could not find Z/$zoneId in A/$areaId")
    }
}