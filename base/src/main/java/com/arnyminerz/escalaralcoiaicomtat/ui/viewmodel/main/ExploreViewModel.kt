package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
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
     * @param dataClass The DataClass to load the children from.
     * @return A [MutableState] that contains a list of children. May be empty while loading.
     */
    inline fun <A : DataClassImpl, T : DataClass<A, *, *>, R : Comparable<R>> childrenLoader(
        dataClass: T,
        crossinline sortBy: (A) -> R?,
    ): MutableState<List<DataClassImpl>> =
        mutableStateOf<List<DataClassImpl>>(emptyList()).apply {
            viewModelScope.launch {
                val children = dataClass.getChildren(app.searchSession, sortBy)
                value = children
            }
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