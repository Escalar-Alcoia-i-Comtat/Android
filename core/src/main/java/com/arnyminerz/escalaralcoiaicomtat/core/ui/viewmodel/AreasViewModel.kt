package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AreasViewModel<A : Activity>(activity: A) : DataClassViewModel<Area, A>(activity) {
    override val columnsPerRow: Int = 1

    override val items: LiveData<List<Area>> = liveData {
        val areas = suspendCoroutine<List<Area>> { cont ->
            loadAreas(context as Activity, firestore, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
            }) {
                cont.resume(AREAS)
            }
        }
        emit(areas)
    }
}