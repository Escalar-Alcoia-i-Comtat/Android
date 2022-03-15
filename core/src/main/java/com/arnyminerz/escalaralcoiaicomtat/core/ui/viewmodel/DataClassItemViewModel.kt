package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DownloadWorker
import kotlinx.coroutines.launch
import java.util.Date

class DataClassItemViewModel(
    application: Application
) : AndroidViewModel(application) {
    val images = mutableStateListOf<Bitmap?>()

    private val downloadingStatusListeners =
        mutableMapOf<String, (status: DownloadStatus) -> Unit>()

    val downloadStatuses = mutableMapOf<String, MutableState<DownloadStatus>>()

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

    fun addDownloadListener(
        namespace: Namespace,
        @ObjectId objectId: String,
        callback: (status: DownloadStatus) -> Unit
    ) {
        val pin = DataClass.generatePin(namespace, objectId)
        val downloadStatus = downloadStatuses.getOrPut(pin) {
            mutableStateOf(DownloadStatus.UNKNOWN)
        }
        downloadingStatusListeners[pin] = { status ->
            downloadStatus.value = status
            callback(status)
        }
        viewModelScope.launch {
            val state = DownloadSingleton.getInstance().states

            state.observeForever { newMap ->
                downloadingStatusListeners[pin]?.invoke(
                    newMap[namespace to objectId] ?: DownloadStatus.UNKNOWN
                )
            }

            downloadingStatusListeners[pin]?.invoke(
                state.value?.get(namespace to objectId) ?: DownloadStatus.UNKNOWN
            )
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

    fun childrenCounter(dataClass: DataClass<*, *, *>): MutableLiveData<Int> =
        MutableLiveData<Int>().apply {
            viewModelScope.launch {
                val childrenCount = dataClass.getSize(context)
                postValue(childrenCount)
            }
        }

    /**
     * Initializes the download status of [at] in [DownloadSingleton] with the current status,
     * indexed or downloading.
     * @author Arnau Mora
     * @since 20220315
     * @param at The location of the element to update.
     */
    fun initializeDownloadStatus(at: Pair<Namespace, @ObjectId String>) {
        viewModelScope.launch {
            DownloadSingleton.getInstance()
                .putState(context, at, null)
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
