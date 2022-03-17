package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UpdaterSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.updateAvailable
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.utils.humanReadableByteCountBin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class StorageViewModel(application: Application) : AndroidViewModel(application) {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    val sizeString = mutableStateOf(humanReadableByteCountBin(0))

    val updatesAvailable = UpdaterSingleton.getInstance().updateAvailableObjects

    /**
     * Used for checking which elements are currently being updated.
     * @author Arnau Mora
     * @since 20220316
     */
    var currentlyUpdatingItems by
    mutableStateOf(emptyMap<Pair<Namespace, @ObjectId String>, UpdaterSingleton.Item>())
        private set

    private suspend inline fun <D : DataClassImpl, reified R : DataRoot<D>> performSearch(
        namespace: Namespace,
        @ObjectId objectId: String,
    ) = DataSingleton.getInstance(context)
        .repository
        .get(namespace, objectId)

    @Suppress("UNCHECKED_CAST")
    fun <D : DataClassImpl> getDataClass(
        namespace: Namespace,
        @ObjectId objectId: String,
    ) = mutableStateOf<D?>(null)
        .apply {
            viewModelScope.launch {
                Timber.d("Loading %s: %s", namespace, objectId)

                val dataClass = when (namespace) {
                    Area.NAMESPACE -> performSearch<Area, AreaData>(namespace, objectId)
                    Zone.NAMESPACE -> performSearch<Zone, ZoneData>(namespace, objectId)
                    Sector.NAMESPACE -> performSearch<Sector, SectorData>(namespace, objectId)
                    Path.NAMESPACE -> performSearch<Path, PathData>(namespace, objectId)
                    else -> {
                        Timber.e(
                            "Could not load data of %s:%s since the namespace is not valid",
                            namespace, objectId,
                        )
                        return@launch
                    }
                }
                Timber.d("Got $namespace: $dataClass")
                value = dataClass as D
            }
        }

    fun checkForUpdates() {
        viewModelScope.launch {
            launch(Dispatchers.IO) {
                val updateAvailable = updateAvailable(getApplication())
                Timber.i("Update available: $updateAvailable")
            }
        }
    }

    private fun update(data: UpdaterSingleton.Item, addToUpdatingList: Boolean) {
        viewModelScope.launch {
            Timber.i("Updating ${data.namespace}/${data.objectId}...")
            if (addToUpdatingList)
                currentlyUpdatingItems = currentlyUpdatingItems
                    .toMutableMap()
                    .apply {
                        put(data.namespace to data.objectId, data)
                    }
            UpdaterSingleton.getInstance()
                .update(
                    getApplication(),
                    data.namespace,
                    data.objectId,
                )
            currentlyUpdatingItems = currentlyUpdatingItems
                .toMutableMap()
                .filterKeys { it.first != data.namespace && it.second != data.objectId }
        }
    }

    fun update(data: UpdaterSingleton.Item) = update(data, true)

    /**
     * Updates all the elements from [updatesAvailable].
     * @author Arnau Mora
     * @since 20220316
     */
    fun updateAll() {
        // Add all the items to currentlyUpdatingItems
        currentlyUpdatingItems = currentlyUpdatingItems
            .toMutableMap()
            .apply {
                putAll(updatesAvailable.map { (it.namespace to it.objectId) to it })
            }
        // Start requesting updates
        currentlyUpdatingItems.forEach { (_, data) ->
            update(data, false)
        }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            if (modelClass.isAssignableFrom(StorageViewModel::class.java))
                return StorageViewModel(application) as T
            error("Unknown view model class: $modelClass")
        }
    }
}