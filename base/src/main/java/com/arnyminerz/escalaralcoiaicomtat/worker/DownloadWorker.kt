package com.arnyminerz.escalaralcoiaicomtat.worker

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.work.*
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.connection.web.download
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.*
import com.arnyminerz.escalaralcoiaicomtat.generic.WEBP_LOSSLESS_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.generic.storeToFile
import com.arnyminerz.escalaralcoiaicomtat.notification.DOWNLOAD_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.notification.Notification
import com.arnyminerz.escalaralcoiaicomtat.storage.dataDir
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import timber.log.Timber
import java.io.File
import java.util.*

const val DOWNLOAD_QUALITY_MIN = 1
const val DOWNLOAD_QUALITY_MAX = 100

const val DOWNLOAD_DISPLAY_NAME = "display_name"
const val DOWNLOAD_NAMESPACE = "namespace"
const val DOWNLOAD_ID = "id"
const val DOWNLOAD_OVERWRITE = "overwrite"
const val DOWNLOAD_QUALITY = "quality"

private const val OVERWRITE_DEFAULT = true
private const val QUALITY_DEFAULT = 100

/**
 * When the DownloadWorker was ran with missing data
 * @since 20210313
 */
const val ERROR_MISSING_DATA = "missing_data"

/**
 * When old data could not be removed from Datastore
 * @since 20210313
 */
const val ERROR_UNPIN = "unpin_old"

/**
 * When old data was tried to be deleted but was not possible
 * @since 20210313
 */
const val ERROR_DELETE_OLD = "delete_old"

/**
 * When the target download could not be found
 * @since 20210313
 */
const val ERROR_NOT_FOUND = "not_found"

/**
 * When the target download has already been downloaded and overwrite is false
 * @since 20210313
 */
const val ERROR_ALREADY_DOWNLOADED = "already_downloaded"

class DownloadData
/**
 * Initializes the class with specific parameters
 * @param type The DataClass type, it's used to infer namespace
 * @param id The id to fetch
 * @param tempDisplayName The display name to show in the progress notification
 * @param overwrite If the download should be overwritten if already downloaded. Note that if this
 * is false, if the download already exists the task will fail.
 * @param quality The compression quality of the image
 */
private constructor(
    val type: DataClasses,
    val id: String,
    val tempDisplayName: String,
    val overwrite: Boolean = OVERWRITE_DEFAULT,
    val quality: Int = QUALITY_DEFAULT
) {
    /**
     * Initializes the class with a DataClass
     * @param dataClass The DataClass to download.
     * @param overwrite If the download should be overwritten if already downloaded. Note that if this
     * is false, if the download already exists the task will fail.
     * @param quality The compression quality of the image
     */
    constructor(
        dataClass: DataClass<*, *>,
        overwrite: Boolean = OVERWRITE_DEFAULT,
        quality: Int = QUALITY_DEFAULT
    ) : this(
        DataClasses.find(dataClass.namespace)!!,
        dataClass.objectId,
        dataClass.displayName,
        overwrite,
        quality
    )
}

class DownloadWorker private constructor(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    private fun download(
        objectID: String,
        namespace: String,
        overwrite: Boolean,
        quality: Int,
        query: ParseQuery<*>?
    ): Pair<ParseQuery<*>?, Result?> {
        val pin = "${DATA_FIX_LABEL}_${namespace}_$objectID"

        // Remove old data
        try {
            ParseObject.unpinAll(pin)
        } catch (e: ParseException) {
            return null to failure(ERROR_UNPIN)
        }

        // Fetch the data
        Timber.d("Fetching data from server ($namespace)...")
        val fetchQuery = ParseQuery.getQuery<ParseObject>(namespace)
        fetchQuery.limit = MAX_BATCH_SIZE
        if (query != null) {
            Timber.d("Downloading ${query.className}...")
            val parameter = query.className.toLowerCase(Locale.getDefault())
            Timber.d("  Should match $parameter at $namespace")
            fetchQuery.whereMatchesQuery(parameter, query)
        } else fetchQuery.whereEqualTo("objectId", objectID)
        val result = fetchQuery.fetchPinOrNetworkSync(pin, true) // Make sure to pin it
        if (result.isEmpty()) { // Object not found
            Timber.d("Result list is empty.")
            return null to failure(ERROR_ALREADY_DOWNLOADED)
        }

        // Get the fetched data
        Timber.d("Fetched ${result.size} elements")
        for (data in result) {
            val objectId = data.objectId
            val image = data.getString("image")!!
            // This is the image file
            val imageFile = File(dataDir(applicationContext), "$namespace-$objectId.webp")

            if (imageFile.exists() && !overwrite)
                return null to failure(ERROR_ALREADY_DOWNLOADED)
            if (!imageFile.deleteIfExists())
                return null to failure(ERROR_DELETE_OLD)

            Timber.d("Downloading image ($image)...")
            val stream = download(image)
            val bitmap = BitmapFactory.decodeStream(stream)
            bitmap.storeToFile(imageFile, format = WEBP_LOSSLESS_LEGACY, quality = quality)
        }

        return fetchQuery to null
    }

    override fun doWork(): Result {
        // Get all data
        val namespace = inputData.getString(DOWNLOAD_NAMESPACE)
        val objectID = inputData.getString(DOWNLOAD_ID)
        val displayName = inputData.getString(DOWNLOAD_DISPLAY_NAME)
        val overwrite = inputData.getBoolean(DOWNLOAD_OVERWRITE, OVERWRITE_DEFAULT)
        val quality = inputData.getInt(DOWNLOAD_OVERWRITE, QUALITY_DEFAULT)

        // Check if any required data is missing
        return if (namespace == null || objectID == null || displayName == null)
            failure(ERROR_MISSING_DATA)
        else {
            Timber.v("Downloading $objectID from $namespace...")

            // Build the notification
            val notificationBuilder = Notification.Builder(applicationContext)
                .withChannelId(DOWNLOAD_PROGRESS_CHANNEL_ID)
                .withIcon(R.drawable.ic_notifications)
                .withTitle(R.string.notification_download_progress_title)
                .withText(R.string.notification_download_progress_message, displayName)
            val notification = notificationBuilder.build()
            notification.show() // Show the notification

            val currentDownload = download(objectID, namespace, overwrite, quality, null)
            val query = currentDownload.first
            val result = currentDownload.second
            if (result != null)
                return result

            if (namespace == Zone.NAMESPACE)
                if (query == null) {
                    Timber.w("Cannot get Zone's children since query is null")
                } else {
                    Timber.d("Downloading Zone's children...")
                    download(objectID, Sector.NAMESPACE, overwrite, quality, query)
                }

            Timber.v("Finished downloading $displayName")
            notification.destroy()

            Result.success()
        }
    }

    companion object {
        /**
         * Schedules a new download as a new Worker.
         * @author Arnau Mora
         * @since 20210313
         * @param context The context to run from
         * @param tag The tag of the worker
         * @param data The data of the download
         * @return The scheduled operation
         *
         * @see DownloadData
         * @see DownloadWorker
         * @see ERROR_MISSING_DATA
         * @see ERROR_ALREADY_DOWNLOADED
         * @see ERROR_DELETE_OLD
         * @see ERROR_NOT_FOUND
         * @see ERROR_UNPIN
         */
        fun schedule(context: Context, tag: String, data: DownloadData): LiveData<WorkInfo> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .addTag(tag)
                .setInputData(
                    workDataOf(
                        DOWNLOAD_NAMESPACE to data.type.namespace,
                        DOWNLOAD_ID to data.id,
                        DOWNLOAD_DISPLAY_NAME to data.tempDisplayName,
                        DOWNLOAD_OVERWRITE to data.overwrite,
                        DOWNLOAD_QUALITY to data.quality
                    )
                )
                .build()

            val workManager = WorkManager
                .getInstance(context)
            workManager.enqueue(request)

            return workManager.getWorkInfoByIdLiveData(request.id)
        }
    }
}
