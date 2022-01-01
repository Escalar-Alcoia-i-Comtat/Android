package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.isDownloadIndexed
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
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