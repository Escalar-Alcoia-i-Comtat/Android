package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main

import android.app.Application
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
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.isDownloadIndexed
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getList
import com.arnyminerz.escalaralcoiaicomtat.core.utils.humanReadableByteCountBin
import kotlinx.coroutines.launch
import timber.log.Timber

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    val downloads: MutableLiveData<List<Pair<DownloadedData, Boolean>>> = MutableLiveData()
    val sizeString = mutableStateOf(humanReadableByteCountBin(0))

    /**
     * Loads the downloaded DataClasses and adds them to [downloads]. [sizeString] will be updated.
     * @author Arnau Mora
     * @since 20220101
     */
    fun loadDownloads() {
        viewModelScope.launch {
            Timber.i("Loading downloads...")
            var size = 0L
            val list = arrayListOf<Pair<DownloadedData, Boolean>>()
            val downloadsFlow = app.getDownloads()
            downloadsFlow.collect { data ->
                val parentIndexed =
                    data.parentId.ifEmpty { null }?.isDownloadIndexed(app.searchSession)
                Timber.i("Collected ${data.namespace}:${data.objectId}, adding. Parent (${data.parentId}) indexed: $parentIndexed")
                list.add(data to (parentIndexed?.downloaded ?: false))
                size += data.sizeBytes
            }
            Timber.i("Size: $size")
            this@DownloadsViewModel.sizeString.value = humanReadableByteCountBin(size)
            downloads.postValue(list)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <D : DataClassImpl> getDataClass(
        @Namespace namespace: String,
        @ObjectId objectId: String
    ) = mutableStateOf<D?>(null)
        .apply {
            viewModelScope.launch {
                Timber.d("Loading %s: %s", namespace, objectId)
                val dataClass = when (namespace) {
                    Area.NAMESPACE -> app.getAreas().find { it.objectId == objectId }
                    Zone.NAMESPACE -> app.searchSession
                        .getList<Zone, ZoneData>("", Zone.NAMESPACE)
                        .find { it.objectId == objectId }
                    Sector.NAMESPACE -> app.searchSession
                        .getList<Sector, SectorData>("", Sector.NAMESPACE)
                        .find { it.objectId == objectId }
                    Path.NAMESPACE -> app.searchSession
                        .getList<Path, PathData>("", Path.NAMESPACE)
                        .find { it.objectId == objectId }
                    else -> {
                        Timber.e(
                            "Could not load data of %s:%s since the namespace is not valid",
                            namespace, objectId,
                        )
                        null
                    }
                }
                Timber.d("Got $namespace: $dataClass")
                value = dataClass as D?
            }
        }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            if (modelClass.isAssignableFrom(DownloadsViewModel::class.java))
                return DownloadsViewModel(application) as T
            error("Unknown view model class: $modelClass")
        }
    }
}