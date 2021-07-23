package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.DataClassViewModel
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AreasViewModel : DataClassViewModel<Area>() {
    override val items: LiveData<List<Area>> = liveData {
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