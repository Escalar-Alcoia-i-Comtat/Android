package com.arnyminerz.escalaralcoiaicomtat.core.loader

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StreamDownloadTask
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

class StorageDataFetcher(private val model: StorageReference) : DataFetcher<InputStream> {
    private var streamDownloadTask: StreamDownloadTask? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        streamDownloadTask = model.stream
        streamDownloadTask!!
            .addOnSuccessListener { callback.onDataReady(it.stream) }
            .addOnFailureListener { callback.onLoadFailed(it) }
    }

    override fun cleanup() {
        try {
            streamDownloadTask?.result?.stream?.close()
        } catch (e: IllegalStateException) {
            Timber.d("Won't close stream since it has not finished yet.")
        } catch (e: RuntimeExecutionException) {
            Timber.d("Won't close stream since it has returned an exception.")
        } catch (e: IOException) {
            Timber.d(e, "Won't close stream since there was an error.")
        }
    }

    override fun cancel() {
        streamDownloadTask?.cancel()
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.REMOTE
}