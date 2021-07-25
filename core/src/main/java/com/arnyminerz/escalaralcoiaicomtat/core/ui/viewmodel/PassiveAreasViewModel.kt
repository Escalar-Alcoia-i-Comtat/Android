package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS

class PassiveAreasViewModel<A : Activity>(activity: A) : DataClassViewModel<Area, A>(activity) {
    override val columnsPerRow: Int = 1

    override val items: LiveData<List<Area>> = liveData { emit(AREAS) }
}