package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DownloadWorker
import kotlinx.coroutines.launch
import java.util.*

class DataClassItemViewModel(
    application: Application
) : AndroidViewModel(application) {
    val images = mutableStateListOf<Bitmap?>()

    private val downloadingStatusListeners =
        mutableMapOf<String, (status: DownloadStatus) -> Unit>()

    val downloadStatuses = mutableMapOf<String, MutableState<DownloadStatus>>()

    val children by DataSingleton.getInstance(application).children

    fun startDownloading(
        context: Context,
        pin: String,
        displayName: String,
        overwrite: Boolean = true,
        quality: Int = 100,
    ) {
        viewModelScope.launch {
            val workerInfo = DataClass.scheduleDownload(
                context,
                pin,
                displayName,
                DownloadWorker.Companion,
                overwrite,
                quality
            )
            workerInfo.observe(context as LifecycleOwner) { workInfo ->
                var state = DownloadStatus.DOWNLOADING

                if (workInfo.state.isFinished) {
                    // TODO: This should not be hardcoded, should be checked for download state
                    state = DownloadStatus.DOWNLOADED
                }

                for (listener in downloadingStatusListeners)
                    if (listener.key == pin)
                        listener.value(state)
            }
        }
    }

    fun downloadInfo(
        dataClass: DataClass<*, *, *>
    ): MutableLiveData<Pair<Date, Long>?> =
        MutableLiveData<Pair<Date, Long>?>().apply {
            viewModelScope.launch {
                val downloadDate = dataClass.downloadDate(context)!!
                val size = dataClass.size(context)
                postValue(downloadDate to size)
            }
        }

    fun deleteDataClass(
        dataClass: DataClass<*, *, *>
    ): MutableLiveData<Boolean?> {
        val result = MutableLiveData<Boolean?>(null)
        viewModelScope.launch {
            val deleted = dataClass.delete(context)
            result.postValue(deleted)
        }
        return result
    }

    fun <T : DataClass<*, *, *>, R : Comparable<R>> loadChildren(
        dataClass: T,
        sortBy: (DataClassImpl) -> R
    ) {
        viewModelScope.launch {
            DataSingleton.getInstance(getApplication()).apply {
                children.value = emptyList()
                children.value = dataClass.getChildren(context, sortBy)
            }
        }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            if (modelClass.isAssignableFrom(DataClassItemViewModel::class.java))
                return DataClassItemViewModel(application) as T
            error("Unknown view model class: $modelClass")
        }
    }
}
