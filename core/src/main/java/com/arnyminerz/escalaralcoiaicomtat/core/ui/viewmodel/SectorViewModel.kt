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
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.currentUrl
import com.arnyminerz.escalaralcoiaicomtat.core.utils.asyncCoroutineScope
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
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

    suspend fun getInnerSector(): Sector? {
        val app = getApplication<App>()
        val areas = app.getAreas()
        return areas[areaId]?.get(zoneId)?.get(sectorId)
    }

    val sector: LiveData<Sector?> = liveData(asyncCoroutineScope.coroutineContext) {
        val app = getApplication<App>()
        val areas = app.getAreas()
        if (areas.isEmpty()) {
            val application = (context as? Activity)?.application ?: context as Application
            firestore.loadAreas(application as App, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
            })
        }
        emit(getInnerSector())
    }

    val items: LiveData<List<Path>> = liveData {
        val innerSector = getInnerSector()
        uiContext { currentUrl.value = innerSector?.webUrl }
        val paths = innerSector?.getChildren()
        if (paths != null)
            emit(paths)
        else Timber.e("Could not find S/$sectorId in Z/$zoneId in A/$areaId")
    }
}
