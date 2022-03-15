package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DownloadStatus

class DownloadSingleton {
    companion object {
        @Volatile
        private var INSTANCE: DownloadSingleton? = null

        /**
         * Get the current [DownloadSingleton] or initializes a new one if necessary.
         * @author Arnau Mora
         * @since 20220315
         */
        fun getInstance() =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadSingleton()
                    .also { INSTANCE = it }
            }
    }

    private var workInfoList by mutableStateOf(mapOf<Pair<Namespace, @ObjectId String>, WorkInfo>())

    val states: MutableLiveData<Map<Pair<Namespace, @ObjectId String>, DownloadStatus>> =
        MutableLiveData(emptyMap())

    /**
     * Sets the state of [objectId] in [namespace] to [workInfo].
     * @author Arnau Mora
     * @since 20220315
     * @param namespace The namespace of the object observed.
     * @param objectId The id of the object observed.
     * @param workInfo The [WorkInfo] object to use as state.
     */
    @WorkerThread
    suspend fun putState(
        context: Context,
        namespace: Namespace,
        @ObjectId objectId: String,
        workInfo: WorkInfo
    ) {
        workInfoList = workInfoList
            .toMutableMap()
            .apply {
                put(namespace to objectId, workInfo)
            }
        states.postValue(
            (states.value ?: emptyMap())
                .toMutableMap()
                .apply {
                    put(
                        namespace to objectId,
                        when {
                            workInfoList.containsKey(namespace to objectId) -> DownloadStatus.DOWNLOADING
                            else -> DataClass.isDownloadIndexed(context, objectId)
                        }
                    )
                }
        )
    }

    /**
     * Tells the system that the object with id [objectId] at [namespace] has finished downloading.
     */
    fun finishedDownloading(namespace: Namespace, @ObjectId objectId: String) {
        workInfoList = workInfoList
            .toMutableMap()
            .filter { it.key != namespace to objectId }
        states.value = (states.value ?: emptyMap())
            .toMutableMap()
            .filter { it.key != namespace to objectId }
    }
}