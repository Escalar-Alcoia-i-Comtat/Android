package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import android.app.Application
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.exceptions.AppSearchException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.await
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

    /**
     * Starts downloading all the dataclasses that have been downloaded.
     * @author Arnau Mora
     * @since 20211230
     */
    fun loadDownloads() {
        Timber.i("Loading downloads...")
        viewModelScope.launch {
            val searchSession = app.searchSession
            val searchResults = searchSession.search(
                "",
                SearchSpec.Builder()
                    .addFilterDocumentClasses(DownloadedData::class.java)
                    .build()
            )
            var results = searchResults.nextPage.await()
            while (results.isNotEmpty()) {
                for (result in results) {
                    val document = result.genericDocument
                    try {
                        val downloadedData = document.toDocumentClass(DownloadedData::class.java)
                        downloads.add(downloadedData)
                    } catch (e: AppSearchException) {
                        Timber.e(e, "Could not convert data to DownloadedData.")
                    }
                }
                results = searchResults.nextPage.await()
            }
            // TODO: Add currently downloading tasks
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