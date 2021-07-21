package com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import timber.log.Timber

class ZonesViewModel(private val areaId: String) : DataClassViewModel<Zone>() {
    override val items: LiveData<List<Zone>> = liveData {
        val zones = AREAS[areaId]?.getChildren(firestore, storage)
        if (zones != null)
            emit(zones)
        else Timber.e("Could not find A/$areaId")
    }
}