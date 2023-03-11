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
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UPDATE_AVAILABLE_FAIL_VERSION
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UpdaterSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.updateAvailable
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
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
     * Stores whether or not the version of the server matches the expected by the app.
     * @author Arnau Mora
     * @since 20220627
     */
    val serverIncompatible = mutableStateOf(false)

    /**
     * Used for checking which elements are currently being updated.
     * @author Arnau Mora
     * @since 20220316
     */
    var currentlyUpdatingItems by
    mutableStateOf(emptyMap<Pair<Namespace, @ObjectId String>, UpdaterSingleton.Item>())
        private set

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun <D : DataClassImpl> performSearch(
        namespace: Namespace,
        @ObjectId objectId: String,
    ) = DataSingleton.getInstance(context)
        .repository
        .get(namespace, objectId) as D

    @Suppress("UNCHECKED_CAST")
    fun <D : DataClassImpl> getDataClass(
        namespace: Namespace,
        @ObjectId objectId: String,
    ) = mutableStateOf<D?>(null)
        .apply {
            viewModelScope.launch {
                Timber.d("Loading %s: %s", namespace, objectId)

                val dataClass = when (namespace) {
                    Area.NAMESPACE -> performSearch<Area>(namespace, objectId)
                    Zone.NAMESPACE -> performSearch<Zone>(namespace, objectId)
                    Sector.NAMESPACE -> performSearch<Sector>(namespace, objectId)
                    Path.NAMESPACE -> performSearch<Path>(namespace, objectId)
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

                if (updateAvailable == UPDATE_AVAILABLE_FAIL_VERSION)
                    serverIncompatible.value = true
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

            if (currentlyUpdatingItems.isEmpty()) {
                Timber.i("Finished updating, loading Areas again...")
                DataSingleton.getInstance(context).areas.value = app.getAreas()
            }
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