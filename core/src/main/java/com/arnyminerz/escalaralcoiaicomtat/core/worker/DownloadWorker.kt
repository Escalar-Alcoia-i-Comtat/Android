package com.arnyminerz.escalaralcoiaicomtat.core.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_COMPLETE_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_OVERWRITE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_MOBILE_DOWNLOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ROAMING_DOWNLOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.exception_handler.handleStorageException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.notification.Notification
import com.google.android.gms.tasks.Tasks
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.IOException

const val DOWNLOAD_QUALITY_MIN = 1
const val DOWNLOAD_QUALITY_MAX = 100

const val DOWNLOAD_DISPLAY_NAME = "display_name"
const val DOWNLOAD_NAMESPACE = "namespace"
const val DOWNLOAD_PATH = "path"
const val DOWNLOAD_OVERWRITE = "overwrite"
const val DOWNLOAD_QUALITY = "quality"

/**
 * When the DownloadWorker was ran with missing data
 * @since 20210313
 */
const val ERROR_MISSING_DATA = "missing_data"

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

/**
 * When trying to fetch data from the server
 * @since 20210411
 */
const val ERROR_DATA_FETCH = "data_fetch"

/**
 * When there's an unkown error while storing the image.
 * @since 20210411
 */
const val ERROR_STORE_IMAGE = "store_image"

/**
 * When the specified namespace is not downloadable.
 * @since 20210412
 */
const val ERROR_UNKNOWN_NAMESPACE = "unknown_namespace"

/**
 * When the image reference could not be updated.
 * @since 20210422
 */
const val ERROR_UPDATE_IMAGE_REF = "update_image_ref"

class DownloadData
/**
 * Initializes the class with specific parameters
 * @param dataClass The [DataClass] to download.
 * @param overwrite If the download should be overwritten if already downloaded. Note that if this
 * is false, if the download already exists the task will fail.
 * @param quality The compression quality of the image
 * @see DOWNLOAD_OVERWRITE_DEFAULT
 * @see DOWNLOAD_QUALITY_DEFAULT
 */
constructor(
    val dataClass: DataClass<*, *>,
    val overwrite: Boolean = DOWNLOAD_OVERWRITE_DEFAULT,
    val quality: Int = DOWNLOAD_QUALITY_DEFAULT
)

class DownloadWorker private constructor(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    private lateinit var namespace: String
    private lateinit var displayName: String
    private var downloadPath: String? = null
    private var overwrite: Boolean = false
    private var quality: Int = -1

    private lateinit var storage: FirebaseStorage

    /**
     * Specifies the downloading notification. For modifying it later.
     * @since 20210323
     */
    private lateinit var notification: Notification

    private fun downloadImageFile(
        imageReferenceUrl: String,
        imageFile: File,
        objectId: String
    ): Result {
        val dataDir = imageFile.parentFile!!

        var error: Result? = null
        if (imageFile.exists() && !overwrite)
            error = failure(ERROR_ALREADY_DOWNLOADED)
        if (!imageFile.deleteIfExists())
            error = failure(ERROR_DELETE_OLD)
        if (!dataDir.exists() && !dataDir.mkdirs())
            error = failure(ERROR_CREATE_PARENT)
        if (error != null)
            return error

        notification
            .edit()
            .withInfoText(R.string.notification_download_progress_info_downloading_image)
            .buildAndShow()

        try {
            val tempFile = File(applicationContext.cacheDir, "dataClass_$objectId")
            if (tempFile.exists()) {
                Timber.d("Copying cached image file from \"$tempFile\" to \"$imageFile\"")
                tempFile.copyTo(imageFile, overwrite = true)
            } else
                runBlocking {
                    Timber.d("Downloading image from Firebase Storage: $imageReferenceUrl...")
                    storage.getReferenceFromUrl(imageReferenceUrl).getFile(imageFile).await()
                }
        } catch (e: StorageException) {
            Timber.w(e, "Could not get image")
            return failure(ERROR_STORE_IMAGE)
        } catch (e: IOException) {
            Timber.e(e, "Could not copy image file")
            return failure(ERROR_STORE_IMAGE)
        }

        if (!imageFile.exists())
            return failure(ERROR_STORE_IMAGE)

        return Result.success()
    }

    private fun fixImageReferenceUrl(
        image: String,
        firestore: FirebaseFirestore,
        path: String
    ): Pair<String?, Result?> =
        if (image.startsWith("https://escalaralcoiaicomtat.centrexcursionistalcoi.org/"))
            try {
                runBlocking {
                    Timber.w("Fixing zone image reference ($image)...")
                    val i = image.lastIndexOf('/') + 1
                    val newImage =
                        "gs://escalaralcoiaicomtat.appspot.com/images/sectors/" + image.substring(i)
                    Timber.w("Changing image address to \"$newImage\"...")
                    firestore
                        .document(path)
                        .update(mapOf("image" to newImage))
                        .await()
                    Timber.w("Image address updated.")
                    newImage to null
                }
            } catch (e: FirebaseFirestoreException) {
                Firebase.crashlytics.recordException(e)
                Timber.e(e, "Could not update zone image reference")
                null to failure(ERROR_UPDATE_IMAGE_REF)
            }
        else image to null

    /**
     * Downloads the data of a [Zone] for using it offline.
     * @author Arnau Mora
     * @since 20210323
     * @param firestore The [FirebaseFirestore] instance.
     * @param path The path to download
     *
     * @see ERROR_MISSING_DATA
     * @see ERROR_ALREADY_DOWNLOADED
     * @see ERROR_DELETE_OLD
     * @see ERROR_NOT_FOUND
     * @see ERROR_DATA_FETCH
     * @see ERROR_STORE_IMAGE
     */
    private fun downloadZone(firestore: FirebaseFirestore, path: String): Result {
        Timber.d("Downloading Zone $path...")
        Timber.v("Getting document...")
        val task = firestore.document(path).get()
        Timber.v("Awaiting document task...")
        Tasks.await(task)
        val exception = task.exception
        if (exception != null) {
            Timber.e(exception, "Could not get data")
            return failure(ERROR_DATA_FETCH)
        }
        Timber.v("Got Zone document!")

        val result = task.result!!
        val zone = Zone(result)

        Timber.v("Updating notification...")
        val newText = applicationContext.getString(
            R.string.notification_download_progress_message,
            zone.displayName
        )
        notification = notification
            .edit()
            .withText(newText)
            .withInfoText(R.string.notification_download_progress_info_fetching)
            .withProgress(ValueMax(0, -1))
            .buildAndShow()

        Timber.v("Fixing image reference URL...")
        val image = fixImageReferenceUrl(zone.imageReferenceUrl, firestore, path)
        if (image.second != null)
            return image.second!!
        Timber.v("Got valid URL!")
        val imageRef = image.first!!

        val imageFile = zone.imageFile(applicationContext)
        downloadImageFile(imageRef, imageFile, zone.objectId)

        try {
            Timber.d("Downloading KMZ file...")
            runBlocking {
                zone.kmzFile(applicationContext, storage, true)
            }
        } catch (e: IllegalStateException) {
            Firebase.crashlytics.recordException(e)
            Timber.w("The Zone ($zone) does not contain a KMZ address")
        } catch (e: StorageException) {
            Firebase.crashlytics.recordException(e)
            val handler = handleStorageException(e)
            if (handler != null)
                Timber.e(e, handler.second)
        }

        val sectors = runBlocking {
            Timber.d("Downloading child sectors...")
            val sectors = arrayListOf<Sector>()
            zone.getChildren(firestore)
                .toCollection(sectors)
            sectors
        }
        val total = sectors.size
        for ((s, sector) in sectors.withIndex())
            downloadSector(firestore, sector.metadata.documentPath, ValueMax(s, total))

        return Result.success()
    }

    /**
     * Downloads the data of a [Sector] for using it offline.
     * @author Arnau Mora
     * @since 20210323
     * @param firestore The [FirebaseFirestore] instance.
     * @param path The path to download
     *
     * @see ERROR_MISSING_DATA
     * @see ERROR_ALREADY_DOWNLOADED
     * @see ERROR_DELETE_OLD
     * @see ERROR_NOT_FOUND
     * @see ERROR_DATA_FETCH
     * @see ERROR_STORE_IMAGE
     */
    private fun downloadSector(
        firestore: FirebaseFirestore,
        path: String,
        progress: ValueMax<Int>?
    ): Result {
        Timber.d("Downloading Sector $path...")
        Timber.v("Getting document...")
        val task = firestore.document(path).get()
        Timber.v("Awaiting document task...")
        Tasks.await(task)
        val exception = task.exception
        if (exception != null)
            return failure(ERROR_DATA_FETCH)

        val result = task.result!!
        val sector = Sector(result)

        Timber.v("Updating notification...")
        val newText = applicationContext.getString(
            R.string.notification_download_progress_message,
            sector.displayName
        )
        notification = notification
            .edit()
            .apply {
                withText(newText)
                withInfoText(R.string.notification_download_progress_info_fetching)
                withProgress(progress ?: ValueMax(0, -1))
            }
            .buildAndShow()

        val image = fixImageReferenceUrl(sector.imageReferenceUrl, firestore, path)
        if (image.second != null)
            return image.second!!
        val imageRef = image.first!!

        val imageFile = sector.imageFile(applicationContext)
        downloadImageFile(imageRef, imageFile, sector.objectId)

        try {
            Timber.d("Downloading KMZ file...")
            runBlocking {
                sector.kmzFile(applicationContext, storage, true)
            }
        } catch (e: IllegalStateException) {
            Firebase.crashlytics.recordException(e)
            Timber.w("The Sector ($sector) does not contain a KMZ address")
        } catch (e: StorageException) {
            Firebase.crashlytics.recordException(e)
            val handler = handleStorageException(e)
            if (handler != null)
                Timber.e(e, handler.second)
        }

        return Result.success()
    }

    override fun doWork(): Result {
        // Get all data
        val namespace = inputData.getString(DOWNLOAD_NAMESPACE)
        downloadPath = inputData.getString(DOWNLOAD_PATH)
        val displayName = inputData.getString(DOWNLOAD_DISPLAY_NAME)
        overwrite = inputData.getBoolean(DOWNLOAD_OVERWRITE, DOWNLOAD_OVERWRITE_DEFAULT)
        quality = inputData.getInt(DOWNLOAD_OVERWRITE, DOWNLOAD_QUALITY_DEFAULT)

        Timber.v("Starting download for %s".format(displayName))

        // Check if any required data is missing
        return if (namespace == null || downloadPath == null || displayName == null)
            failure(ERROR_MISSING_DATA)
        else {
            Timber.v("Initializing Firebase Storage instance...")
            storage = Firebase.storage

            Timber.v("Downloading $namespace at $downloadPath...")
            this.namespace = namespace
            this.displayName = displayName

            val message = applicationContext.getString(
                R.string.notification_download_progress_message,
                displayName
            )

            // Build the notification
            val notificationBuilder = Notification.Builder(applicationContext)
                .withChannelId(DOWNLOAD_PROGRESS_CHANNEL_ID)
                .withIcon(R.drawable.ic_notifications)
                .withTitle(R.string.notification_download_progress_title)
                .withInfoText(R.string.notification_download_progress_info_fetching)
                .withText(message)
                .withProgress(-1, 0)
                .setPersistent(true)
            notification = notificationBuilder.buildAndShow()

            Timber.v("Getting Firestore instance...")
            val firestore = Firebase.firestore

            val downloadResult = when (namespace) {
                Zone.NAMESPACE -> {
                    Timber.d("Downloading Zone...")
                    downloadZone(firestore, downloadPath!!)
                }
                Sector.NAMESPACE -> {
                    Timber.d("Downloading Sector...")
                    downloadSector(firestore, downloadPath!!, null)
                }
                else -> failure(ERROR_UNKNOWN_NAMESPACE)
            }

            Timber.v("Finished downloading $displayName. Result: $downloadResult")
            notification.destroy()

            if (downloadResult == Result.success()) {
                val intent = runBlocking {
                    Timber.v("Getting intent...")
                    Intent()
                    DataClass.getIntent(applicationContext, displayName, firestore)
                        ?.let { intent ->
                            PendingIntent.getActivity(
                                applicationContext,
                                0,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE
                            )
                        }
                }
                Timber.v("Showing download finished notification")
                val text = applicationContext.getString(
                    R.string.notification_download_complete_message,
                    this@DownloadWorker.displayName
                )
                Notification.Builder(applicationContext)
                    .withChannelId(DOWNLOAD_COMPLETE_CHANNEL_ID)
                    .withIcon(R.drawable.ic_notifications)
                    .withTitle(R.string.notification_download_complete_title)
                    .withText(text)
                    .withIntent(intent)
                    .buildAndShow()
            } else {
                Timber.v("Download failed! Result: $downloadResult. Showing notification.")
                val text = applicationContext.getString(
                    R.string.notification_download_failed_message,
                    this.displayName
                )
                Notification.Builder(applicationContext)
                    .withChannelId(DOWNLOAD_COMPLETE_CHANNEL_ID)
                    .withIcon(R.drawable.ic_notifications)
                    .withTitle(R.string.notification_download_failed_title)
                    .withText(text)
                    .withLongText(
                        R.string.notification_download_failed_message_long,
                        displayName,
                        downloadResult.outputData.getString("error") ?: "unknown"
                    )
                    .buildAndShow()
            }

            downloadResult
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
         * @see ERROR_DATA_FETCH
         * @see ERROR_STORE_IMAGE
         * @see ERROR_UPDATE_IMAGE_REF
         */
        fun schedule(context: Context, tag: String, data: DownloadData): LiveData<WorkInfo> {
            Timber.v("Scheduling new download...")
            Timber.v("Building download constraints...")
            var constraints = Constraints.Builder()
            constraints = if (SETTINGS_MOBILE_DOWNLOAD_PREF.get())
                constraints.setRequiredNetworkType(NetworkType.UNMETERED)
            else
                constraints.setRequiredNetworkType(NetworkType.METERED)
            if (!SETTINGS_ROAMING_DOWNLOAD_PREF.get())
                constraints = constraints.setRequiredNetworkType(NetworkType.NOT_ROAMING)
            constraints = constraints.setRequiredNetworkType(NetworkType.CONNECTED)

            Timber.v("Building DownloadWorker request...")
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints.build())
                .addTag(tag)
                .setInputData(
                    with(data) {
                        workDataOf(
                            DOWNLOAD_NAMESPACE to dataClass.namespace,
                            DOWNLOAD_PATH to dataClass.metadata.documentPath,
                            DOWNLOAD_DISPLAY_NAME to dataClass.displayName,
                            DOWNLOAD_OVERWRITE to overwrite,
                            DOWNLOAD_QUALITY to quality
                        )
                    }
                )
                .build()

            Timber.v("Getting WorkManager instance, and enqueueing job.")
            val workManager = WorkManager
                .getInstance(context)
            workManager.enqueue(request)

            return workManager.getWorkInfoByIdLiveData(request.id)
        }
    }
}
