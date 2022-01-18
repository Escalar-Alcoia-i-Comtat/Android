package com.arnyminerz.escalaralcoiaicomtat.core.worker.download

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo

interface DownloadWorkerFactory {
    @WorkerThread
    suspend fun schedule(context: Context, tag: String, data: DownloadData): LiveData<WorkInfo>
}

interface DownloadWorkerModel {
    val factory: DownloadWorkerFactory
}
