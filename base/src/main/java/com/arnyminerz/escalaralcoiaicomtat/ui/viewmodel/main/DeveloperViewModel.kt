package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main

import android.app.Application
import androidx.lifecycle.*
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import kotlinx.coroutines.launch
import timber.log.Timber

class DeveloperViewModel(application: Application) : AndroidViewModel(application) {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    val indexedDownloads: MutableLiveData<List<DataClass<*, *>>> =
        MutableLiveData()
    val indexTree: MutableLiveData<String> = MutableLiveData()

    fun loadIndexTree() {
        viewModelScope.launch {
            val tree = StringBuilder()
            val areas = app.getAreas()
            for (area in areas) {
                tree.appendLine("- ${area.displayName} ($area)")
                for (zone in area.getChildren(context) { it.objectId }) {
                    tree.appendLine("  - ${zone.displayName} ($zone)")
                    for (sector in zone.getChildren(context) { it.objectId }) {
                        tree.appendLine("    - ${sector.displayName} ($sector)")
                        for (path in sector.getChildren(context) { it.objectId })
                            tree.appendLine("      - ${path.displayName} ($path)")
                    }
                }
            }
            indexTree.postValue(tree.toString())
        }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            if (modelClass.isAssignableFrom(DeveloperViewModel::class.java))
                return DeveloperViewModel(application) as T
            error("Unknown view model class: $modelClass")
        }
    }
}