package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.currentUrl
import com.arnyminerz.escalaralcoiaicomtat.core.utils.asyncCoroutineScope
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import timber.log.Timber

class ZonesViewModel<A : Activity>(activity: A, private val areaId: String) :
    DataClassViewModel<Zone, A>(activity) {
    override val columnsPerRow: Int = 2

    override val items: LiveData<List<Zone>> = liveData(asyncCoroutineScope.coroutineContext) {
        val areas = app.getAreas()
        if (areas.isEmpty()) {
            val application = (context as? Activity)?.application ?: context as Application
            firestore.loadAreas(application as App, progressCallback = { current, total ->
                Timber.i("Loading areas: $current/$total")
            })
        }
        val area = areas[areaId]
        uiContext { currentUrl.value = area?.webUrl }
        val zones = area?.getChildren(app.searchSession)
        if (zones != null) {
            for (zone in zones)
                // TODO: Add error handlers
                zone.image(context, storage)
            emit(zones)
        } else Timber.e("Could not find A/$areaId")
    }
}
