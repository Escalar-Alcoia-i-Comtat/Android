package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.livedata.MutableListLiveData
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

    val downloads = MutableListLiveData<DownloadedData>().apply { value = mutableListOf() }

    val dataClasses = MutableListLiveData<DataClass<*, *>>().apply { value = mutableListOf() }

    /**
     * Starts downloading all the dataclasses that have been downloaded.
     * @author Arnau Mora
     * @since 20211230
     */
    fun loadDownloads() {
        Timber.i("Loading downloads...")
        viewModelScope.launch {
            val downloadsFlow = app.getDownloads()
            downloadsFlow.collect { data ->
                Timber.i("Collected ${data.namespace}:${data.objectId}, adding.")
                downloads.add(data)
            }
            // TODO: Add currently downloading tasks
        }
    }

    /**
     * Extracts the [DataClass] from the [data].
     * @author Arnau Mora
     * @since 20211231
     * @param index The position in the list which the data class is at.
     * @param data The data to extract from.
     */
    fun exportDataClass(
        index: Int,
        data: DownloadedData,
        onExport: (@WorkerThread suspend (dataClass: DataClass<*, *>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val dataClass = data.export(app)
            dataClasses.set(index, dataClass)
            onExport?.let { it(dataClass) }
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