package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import kotlinx.coroutines.launch
import timber.log.Timber

class ExploreViewModel(application: Application) : AndroidViewModel(application) {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    /**
     * Serves as a cache of the loaded areas so the map screen can access them.
     * @author Arnau Mora
     * @since 20220105
     */
    var lastAreas = listOf<Area>()
        private set

    /**
     * Loads all the available areas, and posts them using a [MutableLiveData].
     * @author Arnau Mora
     * @since 20220105
     */
    fun loadAreas(): MutableLiveData<List<Area>> = MutableLiveData<List<Area>>().apply {
        viewModelScope.launch {
            val areasList = app.getAreas()
                .sortedBy { it.displayName }
            postValue(areasList)
            lastAreas = areasList
        }
    }

    /**
     * Loads the children of a set DataClass.
     * @author Arnau Mora
     * @since 20220102
     * @param namespace The namespace of the DataClass.
     * @param objectId The id of the DataClass to load.
     * @throws IllegalArgumentException If [namespace] is not valid.
     */
    @Throws(IllegalArgumentException::class)
    fun childrenLoader(@Namespace namespace: String, @ObjectId objectId: String):
            Pair<MutableState<DataClassImpl?>, MutableState<List<DataClassImpl>>> {
        val parentState = mutableStateOf<DataClassImpl?>(null)
        val childrenState = mutableStateOf<List<DataClassImpl>>(emptyList())
        viewModelScope.launch {
            val dataClass = when (namespace) {
                Area.NAMESPACE -> app.getArea(objectId)
                Zone.NAMESPACE -> app.getZone(objectId)
                Sector.NAMESPACE -> app.getSector(objectId)
                else -> throw IllegalArgumentException("The namespace introduced is not valid.")
            } ?: run {
                throw ClassNotFoundException("The dataclass $namespace::$objectId could not be found.")
            }
            parentState.value = dataClass
            childrenState.value = dataClass.getChildren(app.searchSession)
        }
        return parentState to childrenState
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            if (modelClass.isAssignableFrom(ExploreViewModel::class.java))
                return ExploreViewModel(application) as T
            error("Unknown view model class: $modelClass")
        }
    }
}