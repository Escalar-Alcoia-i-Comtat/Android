package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DownloadWorker
import kotlinx.coroutines.launch
import java.util.*

class DataClassItemViewModel(
    application: Application
) : AndroidViewModel(application) {
    val images = mutableStateListOf<Bitmap?>()

    private val downloadingStatusListeners =
        mutableMapOf<String, (status: DownloadStatus, workInfo: WorkInfo?) -> Unit>()

    fun startDownloading(
        context: Context,
        pin: String,
        path: String,
        displayName: String,
        overwrite: Boolean = true,
        quality: Int = 100,
    ) {
        val workerInfo = DataClass.scheduleDownload<DownloadWorker>(
            context,
            pin,
            path,
            displayName,
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
                    listener.value(state, workInfo)
        }
    }

    fun addDownloadListener(
        pin: String,
        callback: (status: DownloadStatus, workInfo: WorkInfo?) -> Unit
    ): MutableLiveData<DownloadStatus> {
        val mutableLiveData = MutableLiveData<DownloadStatus>()
        downloadingStatusListeners[pin] = { status, workInfo ->
            mutableLiveData.postValue(status)
            callback(status, workInfo)
        }
        viewModelScope.launch {
            val state = DataClass.downloadStatus(context, app.searchSession, pin)
            val status = state.first
            val workInfo = state.second

            if (workInfo != null)
                WorkManager
                    .getInstance(context)
                    .getWorkInfoByIdLiveData(workInfo.id)
                    .observe(context as LifecycleOwner) { newInfo ->
                        val newState = newInfo.state
                        val newStatus = if (newState.isFinished)
                        // This should be a more exhaustive check
                            DownloadStatus.DOWNLOADED
                        else
                            DownloadStatus.DOWNLOADING
                        downloadingStatusListeners[pin]?.invoke(newStatus, newInfo)
                    }

            downloadingStatusListeners[pin]?.invoke(status, workInfo)
        }
        return mutableLiveData
    }

    fun downloadInfo(
        dataClass: DataClass<*, *>
    ): MutableLiveData<Pair<Date, Long>?> =
        MutableLiveData<Pair<Date, Long>?>().apply {
            viewModelScope.launch {
                val downloadDate = dataClass.downloadDate(context)!!
                val size = dataClass.size(context, app.searchSession)
                postValue(downloadDate to size)
            }
        }

    fun deleteDataClass(
        dataClass: DataClass<*, *>
    ): MutableLiveData<Boolean?> {
        val result = MutableLiveData<Boolean?>(null)
        viewModelScope.launch {
            val deleted = dataClass.delete(context, app.searchSession)
            result.postValue(deleted)
        }
        return result
    }

    fun childrenCounter(dataClass: DataClass<*, *>): MutableLiveData<Int> =
        MutableLiveData<Int>().apply {
            viewModelScope.launch {
                val childrenCount = dataClass.getSize(app.searchSession)
                postValue(childrenCount)
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
