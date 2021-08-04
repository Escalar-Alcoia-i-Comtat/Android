package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.utils.asyncCoroutineScope
import timber.log.Timber

class AreasViewModel<A : Activity>(activity: A) : DataClassViewModel<Area, A>(activity) {
    override val columnsPerRow: Int = 1

    override val items: LiveData<List<Area>> = liveData(asyncCoroutineScope.coroutineContext) {
        if (AREAS.isEmpty()) {
            val application = (context as? Activity)?.application ?: context as Application
            firestore.loadAreas(application, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
            })
        }
        emit(AREAS)
    }
}
