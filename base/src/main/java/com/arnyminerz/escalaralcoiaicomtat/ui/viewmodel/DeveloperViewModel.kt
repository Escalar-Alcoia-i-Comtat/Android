package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import android.app.Application
import androidx.appsearch.app.SearchSpec
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import kotlinx.coroutines.launch
import timber.log.Timber

class DeveloperViewModel(application: Application) : AndroidViewModel(application) {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    val indexedDownloads: MutableLiveData<List<String>> = MutableLiveData()

    /**
     * Loads all the downloads that have been indexed into [indexedDownloads].
     * @author Arnau Mora
     * @since 20220101
     */
    fun loadIndexedDownloads() {
        viewModelScope.launch {
            val searchResults = app.searchSession.search(
                "",
                SearchSpec.Builder()
                    .addFilterDocumentClasses(DownloadedData::class.java)
                    .build()
            )
            var page = searchResults.nextPage.await()
            val items = mutableListOf<String>()
            while (page.isNotEmpty()) {
                for (result in page) {
                    val genericDocument = result.genericDocument
                    val data = genericDocument.toDocumentClass(DownloadedData::class.java)
                    items.add(data.toString())
                }
                page = searchResults.nextPage.await()
            }
            indexedDownloads.postValue(items)
        }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            if (modelClass.isAssignableFrom(DeveloperViewModel::class.java))
                return DeveloperViewModel(application) as T
            error("Unknown view model class: $modelClass")
        }
    }
}