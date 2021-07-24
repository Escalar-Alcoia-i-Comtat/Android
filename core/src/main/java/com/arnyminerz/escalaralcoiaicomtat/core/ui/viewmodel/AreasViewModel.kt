package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AreasViewModel(application: App) : DataClassViewModel<Area>(application) {
    override val columnsPerRow: Int = 1

    override val items: LiveData<List<Area>> = liveData {
        val areas = suspendCoroutine<List<Area>> { cont ->
            loadAreas(context, firestore, storage, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
            }) {
                cont.resume(AREAS)
            }
        }
        emit(areas)
    }
}