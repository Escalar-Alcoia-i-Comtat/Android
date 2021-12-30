package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.shared.context
import com.arnyminerz.escalaralcoiaicomtat.core.utils.livedata.MutableListLiveData
import kotlinx.coroutines.launch
import timber.log.Timber

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    init {
        Timber.d("$this::init")
    }

    private val downloadingObservers = arrayListOf<Observer<List<WorkInfo>>>()

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    val downloads = MutableListLiveData<DataClass<*, *>>().apply { value = mutableListOf() }
    val downloading =
        MutableListLiveData<Pair<DataClass<*, *>, Int>>().apply { value = mutableListOf() }

    /**
     * Starts downloading all the dataclasses that have been downloaded.
     * @author Arnau Mora
     * @since 20211230
     */
    fun loadDownloads() {
        Timber.i("Loading downloads...")
        viewModelScope.launch {
            val areas = app.getAreas()
            areas.forEach { area ->
                Timber.d("Loading downloads for $area...")

                suspend fun iterateChildStatus(items: List<DataClass<*, *>>) {
                    items.forEach { item ->
                        val downloadStatus = item.downloadStatus(context, app.searchSession)
                        when {
                            downloadStatus.downloaded -> {
                                Timber.i("$item is downloaded")
                                downloads.add(item)
                            }
                            downloadStatus.downloading -> {
                                val workInfoLiveData = item.downloadWorkInfoLiveData(context)
                                val downloadingIndex = downloading.size
                                val observerIndex = downloadingObservers.size
                                val observer = Observer<List<WorkInfo>> { workInfos ->
                                    if (workInfos.isEmpty())
                                        return@Observer
                                    val workInfo = workInfos[0]
                                    when {
                                        workInfo.state.isFinished -> {
                                            val selfObserver = downloadingObservers[observerIndex]
                                            workInfoLiveData.removeObserver(selfObserver)
                                            downloading.removeAt(downloadingIndex)
                                            downloads.add(item)
                                        }
                                        workInfo.state == WorkInfo.State.RUNNING -> {
                                            val progress = workInfo.progress.getInt("progress", -1)
                                            downloading.set(downloadingIndex, item to progress)
                                        }
                                    }
                                }
                                downloadingObservers[observerIndex] = observer
                                downloading.set(downloadingIndex, item to -1)
                                workInfoLiveData.observeForever(observer)
                            }
                        }
                    }
                }

                val zones = area.getChildren(app.searchSession)
                iterateChildStatus(zones)

                zones.forEach { zone ->
                    val sectors = zone.getChildren(app.searchSession)
                    iterateChildStatus(sectors)
                }
            }
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