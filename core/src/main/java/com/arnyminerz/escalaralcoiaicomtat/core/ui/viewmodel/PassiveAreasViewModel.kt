package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS

class PassiveAreasViewModel : DataClassViewModel<Area>() {
    override val columnsPerRow: Int = 1

    override val items: LiveData<List<Area>> = liveData { emit(AREAS) }
}