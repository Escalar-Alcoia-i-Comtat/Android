package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.core.utils.asyncCoroutineScope
import timber.log.Timber

class AreasViewModel<A : Activity>(activity: A) : DataClassViewModel<Area, A>(activity) {
    override val columnsPerRow: Int = 1

    val progress: MutableLiveData<ValueMax<Int>> by lazy { MutableLiveData(ValueMax(0, 0)) }

    override val items: LiveData<List<Area>> = liveData(asyncCoroutineScope.coroutineContext) {
        val app = getApplication<App>()
        val areas = app.getAreas()
        if (areas.isEmpty()) {
            Timber.v("Areas is empty, loading...")
            val application = (context as? Activity)?.application ?: context as Application
            firestore.loadAreas(application as App, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
                progress.value = ValueMax(current, total)
            })
        }
        emit(areas)
    }
}
