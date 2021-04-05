package com.arnyminerz.escalaralcoiaicomtat.worker

import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.connection.parse.fetchPinOrNetworkSync
import com.arnyminerz.escalaralcoiaicomtat.connection.web.download
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.generic.WEBP_LOSSLESS_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.generic.storeToFile
import com.arnyminerz.escalaralcoiaicomtat.notification.DOWNLOAD_COMPLETE_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.notification.DOWNLOAD_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.notification.Notification
import com.arnyminerz.escalaralcoiaicomtat.shared.DATA_FIX_LABEL
import com.arnyminerz.escalaralcoiaicomtat.shared.DOWNLOAD_OVERWRITE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.shared.DOWNLOAD_QUALITY_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.shared.MAX_BATCH_SIZE
import com.arnyminerz.escalaralcoiaicomtat.storage.dataDir
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import timber.log.Timber
import java.io.File
import java.util.Locale

const val DOWNLOAD_QUALITY_MIN = 1
const val DOWNLOAD_QUALITY_MAX = 100

const val DOWNLOAD_DISPLAY_NAME = "display_name"
const val DOWNLOAD_NAMESPACE = "namespace"
const val DOWNLOAD_ID = "id"
const val DOWNLOAD_OVERWRITE = "overwrite"
const val DOWNLOAD_QUALITY = "quality"
const val DOWNLOAD_INTENT_AREA_ID = "area_id"
const val DOWNLOAD_INTENT_ZONE_ID = "zone_id"
const val DOWNLOAD_INTENT_SECTOR_ID = "sector_id"

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

/**
 * When trying to store an image, and the parent dir could not be created.
 * @since 20210323
 */
const val ERROR_CREATE_PARENT = "create_parent"

class DownloadData
/**
 * Initializes the class with specific parameters
 * @param dataClass The [DataClass] to download.
 * @param overwrite If the download should be overwritten if already downloaded. Note that if this
 * is false, if the download already exists the task will fail.
 * @param quality The compression quality of the image
 */
constructor(
    val dataClass: DataClass<*, *>,
    val overwrite: Boolean = DOWNLOAD_OVERWRITE_DEFAULT,
    val quality: Int = DOWNLOAD_QUALITY_DEFAULT
)

class DownloadWorker private constructor(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    /**
     * Specifies the downloading notification. For modifying it later.
     * @since 20210323
     */
    private lateinit var notification: Notification

    /**
     * Creates a new failure Result with some error data.
     * @author Arnau Mora
     * @since 20210323
     * @param error The error name
     * @return A new failure Result with the set error data with key "error" for outputData.
     */
    private fun failure(error: String): Pair<ParseQuery<*>?, Result> {
        if (this::notification.isInitialized)
            notification.destroy()
        return null to Result.failure(
            dataOf(
                "error" to error
            )
        )
    }

    /**
     * Generates a pin from a [namespace] and an [objectID].
     * @author Arnau Mora
     * @since 20210405
     * @param namespace The namespace of the object
     * @param objectID The ID in [namespace] of the object.
     * @return The pin [String]
     */
    private fun pin(namespace: String, objectID: String) =
        "${DATA_FIX_LABEL}_${namespace}_$objectID"

    /**
     * This is the first step on a download, and consists on removing any possible already stored
     * data that may be on the device, so the downloaded data is as updated as possible.
     * Also updates the notification with the correct info text.
     * @author Arnau Mora
     * @since 20210405
     * @param namespace The namespace of the object
     * @param objectID The ID in [namespace] of the object.
     * @return null if everything was correct, a [failure] otherwise.
     * @see ERROR_UNPIN
     */
    private fun removeOldData(namespace: String, objectID: String) =
        try {
            notification
                .edit()
                .withInfoText(R.string.notification_download_progress_info_unpinning)
                .buildAndShow()
            ParseObject.unpinAll(pin(namespace, objectID))
            null
        } catch (_: ParseException) {
            failure(ERROR_UNPIN)
        }

    /**
     * Fetches the data from the server, and gets pinned through [pin] with [objectID] and [namespace].
     * Also updates the notification's info text.
     * @author Arnau Mora
     * @since 20210405
     * @param namespace The namespace of the object
     * @param objectID The ID in [namespace] of the object.
     * @param query If there was another query made before, for a parent object, for example.
     * @param overwrite If the already downloaded content should be overridden
     * @param quality The compression quality of the downloaded images
     * @return A [Result] that should be used as the [Worker]'s result.
     * @see pin
     * @see MAX_BATCH_SIZE
     * @see ERROR_ALREADY_DOWNLOADED
     */
    private fun fetchData(
        objectID: String,
        namespace: String,
        query: ParseQuery<*>?,
        overwrite: Boolean,
        quality: Int
    ): Pair<ParseQuery<*>?, Result?> {
        Timber.d("Fetching data from server ($namespace)...")
        notification
            .edit()
            .withInfoText(R.string.notification_download_progress_info_fetching)
            .buildAndShow()
        val fetchQuery = ParseQuery.getQuery<ParseObject>(namespace)
        fetchQuery.limit = MAX_BATCH_SIZE
        if (query != null) {
            Timber.d("Downloading ${query.className}...")
            val parameter = query.className.toLowerCase(Locale.getDefault())
            Timber.d("  Should match $parameter at $namespace")
            fetchQuery.whereMatchesQuery(parameter, query)
        } else fetchQuery.whereEqualTo("objectId", objectID)
        val result =
            fetchQuery.fetchPinOrNetworkSync(pin(namespace, objectID), true) // Make sure to pin it
        return if (result.isEmpty()) { // Object not found
            Timber.d("Result list is empty.")
            failure(ERROR_ALREADY_DOWNLOADED)
        } else
            processFetchedData(namespace, overwrite, quality, result, fetchQuery)
    }

    /**
     * Once the data has been fetched in [fetchData], it gets processed, which downloads the image
     * and maps so they are available offline.
     * @author Arnau Mora
     * @since 20210405
     * @param namespace The namespace of the object
     * @param overwrite If the already downloaded content should be overridden
     * @param quality The compression quality of the downloaded images
     * @param result The loaded list of [ParseObject]s.
     * @param fetchQuery The [ParseQuery] where [result] was loaded from.
     */
    private fun processFetchedData(
        namespace: String,
        overwrite: Boolean,
        quality: Int,
        result: List<ParseObject>,
        fetchQuery: ParseQuery<ParseObject>
    ): Pair<ParseQuery<*>?, Result?> {
        Timber.d("Fetched ${result.size} elements")
        for (data in result) {
            val objectId = data.objectId
            val image = data.getString("image")!!
            // This is the image file
            val filename = "$namespace-$objectId.webp"
            val dataDir = dataDir(applicationContext)
            val imageFile = File(dataDir, filename)

            var error: Pair<ParseQuery<*>?, Result>? = null
            if (imageFile.exists() && !overwrite)
                error = failure(ERROR_ALREADY_DOWNLOADED)
            if (!imageFile.deleteIfExists())
                error = failure(ERROR_DELETE_OLD)
            if (!dataDir.exists() && !dataDir.mkdirs())
                error = failure(ERROR_CREATE_PARENT)
            if (error != null)
                return error

            Timber.d("Downloading image ($image)...")
            notification
                .edit()
                .withInfoText(R.string.notification_download_progress_info_downloading_image)
                .buildAndShow()
            val stream = download(image)
            Timber.d("Storing image ($imageFile)...")
            notification
                .edit()
                .withInfoText(R.string.notification_download_progress_info_decoding_image)
                .buildAndShow()
            val bitmap = BitmapFactory.decodeStream(stream)
            bitmap.storeToFile(imageFile, format = WEBP_LOSSLESS_LEGACY, quality = quality)
        }

        return fetchQuery to Result.success()
    }

    /**
     * Downloads an object for using it offline.
     * @author Arnau Mora
     * @since 20210323
     * @param objectID The ID of the object to download
     * @param namespace The namespace of the object
     * @param overwrite If the already downloaded content should be overridden
     * @param quality The compression quality of the downloaded images
     * @param query If downloading a children, this should be the parent's query.
     * @return A pair of a nullable [ParseQuery], and a nullable [Result].
     *
     * @see ERROR_MISSING_DATA
     * @see ERROR_ALREADY_DOWNLOADED
     * @see ERROR_DELETE_OLD
     * @see ERROR_NOT_FOUND
     * @see ERROR_UNPIN
     */
    private fun download(
        objectID: String,
        namespace: String,
        overwrite: Boolean,
        quality: Int,
        query: ParseQuery<*>?
    ): Pair<ParseQuery<*>?, Result?> {
        // Remove old data
        val oldDataRemoved = removeOldData(namespace, objectID)
        if (oldDataRemoved != null)
            return oldDataRemoved

        // Fetch, process and store the data
        return fetchData(objectID, namespace, query, overwrite, quality)
    }

    override fun doWork(): Result {
        // Get all data
        val namespace = inputData.getString(DOWNLOAD_NAMESPACE)
        val objectID = inputData.getString(DOWNLOAD_ID)
        val displayName = inputData.getString(DOWNLOAD_DISPLAY_NAME)
        val overwrite = inputData.getBoolean(DOWNLOAD_OVERWRITE, DOWNLOAD_OVERWRITE_DEFAULT)
        val quality = inputData.getInt(DOWNLOAD_OVERWRITE, DOWNLOAD_QUALITY_DEFAULT)

        // Check if any required data is missing
        return if (namespace == null || objectID == null || displayName == null)
            failure(ERROR_MISSING_DATA).second
        else {
            Timber.v("Downloading $objectID from $namespace...")

            // Build the notification
            val notificationBuilder = Notification.Builder(applicationContext)
                .withChannelId(DOWNLOAD_PROGRESS_CHANNEL_ID)
                .withIcon(R.drawable.ic_notifications)
                .withTitle(R.string.notification_download_progress_title)
                .withText(R.string.notification_download_progress_message, displayName)
                .setPersistent(true)
            notification = notificationBuilder.buildAndShow()

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

            Timber.v("Showing download finished notification")
            Notification.Builder(applicationContext)
                .withChannelId(DOWNLOAD_COMPLETE_CHANNEL_ID)
                .withIcon(R.drawable.ic_notifications)
                .withTitle(R.string.notification_download_complete_title)
                .withText(R.string.notification_download_complete_message)
                .withIntent(
                    DataClass.getIntent(applicationContext, displayName)?.let { intent ->
                        PendingIntent.getActivity(
                            applicationContext,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                )
                .buildAndShow()

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
                    with(data) {
                        workDataOf(
                            DOWNLOAD_NAMESPACE to dataClass.namespace,
                            DOWNLOAD_ID to dataClass.objectId,
                            DOWNLOAD_DISPLAY_NAME to dataClass.displayName,
                            DOWNLOAD_OVERWRITE to overwrite,
                            DOWNLOAD_QUALITY to quality
                        )
                    }
                )
                .build()

            val workManager = WorkManager
                .getInstance(context)
            workManager.enqueue(request)

            return workManager.getWorkInfoByIdLiveData(request.id)
        }
    }
}
