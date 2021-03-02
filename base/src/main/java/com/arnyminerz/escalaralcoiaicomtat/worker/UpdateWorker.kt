package com.arnyminerz.escalaralcoiaicomtat.worker

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.loadAreas
import com.arnyminerz.escalaralcoiaicomtat.fragment.intro.DownloadAreasIntroFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import timber.log.Timber
import java.io.IOException

const val ERROR_NAME_KEY = "error"
const val ERROR_MESSAGE_KEY = "error_message"

/**
 * Searches for updates on the downloaded data. This includes the cached one.
 */
@ExperimentalUnsignedTypes
class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Timber.v("Will check for updates")
        Timber.d("Getting areas...")
        val areas = loadAreas(applicationContext)
        var shouldUpdate = false
        for (area in areas)
            if (area.updateAvailable()) {
                shouldUpdate = true
                break
            }
        if (shouldUpdate)
            try {
                Timber.v("Cached areas should be updated. Updating...")
                Timber.d("Removing old cached areas...")
                val cacheFile = IntroActivity.cacheFile(applicationContext)
                if (!cacheFile.deleteIfExists()) {
                    Timber.e("Could not delete cache file.")
                    return Result.failure(
                        Data.Builder()
                            .putString(ERROR_NAME_KEY, "delete-failed")
                            .putString(ERROR_MESSAGE_KEY, "Could not delete cache file")
                            .build()
                    )
                }
                Timber.d("Downloading new areas...")
                DownloadAreasIntroFragment.downloadAreasCache(
                    applicationContext,
                    ConnectivityProvider.NetworkState.CONNECTED_NO_WIFI,
                    null, null
                )
                Timber.v("New areas downloaded successfully")
            } catch (e: IOException) {
                // Could not create data dir
                return Result.failure(
                    Data.Builder()
                        .putString(ERROR_NAME_KEY, "create-failed")
                        .putString(ERROR_MESSAGE_KEY, "Could not create data dir")
                        .build()
                )
            }
        return Result.success()
    }
}