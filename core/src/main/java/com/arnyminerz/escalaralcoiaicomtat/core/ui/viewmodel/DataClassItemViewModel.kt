package com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.utils.resourceUri
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadWorkerModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import timber.log.Timber

class DataClassItemViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val storage = Firebase.storage

    var imageUrls by mutableStateOf<Map<String, Uri>>(mapOf())

    private val downloadingStatusListeners = mutableMapOf<String, (workInfo: WorkInfo) -> Unit>()

    fun loadImage(dataClass: DataClass<*, *>) {
        @Suppress("BlockingMethodInNonBlockingContext")
        viewModelScope.launch {
            val map = mutableMapOf<String, Uri>()
            map.putAll(imageUrls)
            val storageUrl = try {
                // TODO: The storageUrl method does not work for offline usage
                dataClass.storageUrl(storage)
            } catch (e: StorageException) {
                Timber.e(e, "Could not load storageUrl.")
                app.resourceUri(
                    if (dataClass.displayOptions.downloadable)
                        R.drawable.ic_tall_placeholder
                    else
                        R.drawable.ic_wide_placeholder
                )
            }
            map[dataClass.pin] = storageUrl
            imageUrls = map
        }
    }

    fun startDownloading(
        context: Context,
        pin: String,
        path: String,
        displayName: String,
        overwrite: Boolean = true,
        quality: Int = 100,
    ) {
        val workerInfo = DataClass.scheduleDownload<DownloadWorkerModel>(
            context,
            pin,
            path,
            displayName,
            overwrite,
            quality
        )
        workerInfo.observe(context as LifecycleOwner) { workInfo ->
            for (listener in downloadingStatusListeners)
                if (listener.key == pin)
                    listener.value(workInfo)
            if (workInfo.state.isFinished)
                workerInfo.removeObservers(context as LifecycleOwner)
        }
    }

    fun addDownloadListener(pin: String, callback: (workInfo: WorkInfo) -> Unit) {
        downloadingStatusListeners[pin] = callback
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
