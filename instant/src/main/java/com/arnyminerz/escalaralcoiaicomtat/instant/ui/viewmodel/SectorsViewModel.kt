package com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import timber.log.Timber

class SectorsViewModel(private val areaId: String, private val zoneId: String) :
    DataClassViewModel<Sector>() {
    override val items: LiveData<List<Sector>> = liveData {
        val sectors = AREAS[areaId]?.get(zoneId)?.getChildren(firestore, storage)
        if (sectors != null)
            emit(sectors)
        else Timber.e("Could not find Z/$zoneId in A/$areaId")
    }
}