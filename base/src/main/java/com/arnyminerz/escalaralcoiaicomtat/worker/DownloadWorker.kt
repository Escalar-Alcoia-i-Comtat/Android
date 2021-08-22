package com.arnyminerz.escalaralcoiaicomtat.worker

import android.app.PendingIntent
import android.content.Context
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.localstorage.LocalStorage
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.workDataOf
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.ZoneActivity
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_COMPLETE_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DATACLASS_PREVIEW_SCALE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_OVERWRITE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SEARCH_DATABASE_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_MOBILE_DOWNLOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ROAMING_DOWNLOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.exception_handler.handleStorageException
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_DISPLAY_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_NAMESPACE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_OVERWRITE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_PATH
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_QUALITY
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadData
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadWorkerFactory
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadWorkerModel
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_ALREADY_DOWNLOADED
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_CREATE_PARENT
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_DATA_FETCH
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_DELETE_OLD
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_MISSING_DATA
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_STORE_IMAGE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_UNKNOWN_NAMESPACE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_UPDATE_IMAGE_REF
import com.arnyminerz.escalaralcoiaicomtat.core.worker.failure
import com.arnyminerz.escalaralcoiaicomtat.notification.Notification
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.IOException

class DownloadWorker private constructor(appContext: Context, workerParams: WorkerParameters) :
    DownloadWorkerModel, CoroutineWorker(appContext, workerParams) {
    override val factory: DownloadWorkerFactory = Companion

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

    /**
     * The session for performing data loads.
     * @author Arnau Mora
     * @since 20210818
     */
    private lateinit var appSearchSession: AppSearchSession

    private suspend fun downloadImageFile(
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
            var existingImage: File? = null
            val cacheDir = applicationContext.cacheDir

            // If there's a full version cached version of the image, select it
            val cacheFile = File(cacheDir, DataClass.imageName(namespace, objectId, null))
            if (existingImage == null && cacheFile.exists())
                existingImage = cacheFile

            // If there's an scaled version of the image, select it
            val scaledCacheFile = File(
                cacheDir,
                DataClass.imageName(namespace, objectId, "scale$DATACLASS_PREVIEW_SCALE")
            )
            if (existingImage == null && scaledCacheFile.exists())
                existingImage = scaledCacheFile

            // This is the old image format. If there's one cached, select it
            val tempFile = File(applicationContext.cacheDir, "dataClass_$objectId")
            if (existingImage == null && tempFile.exists())
                existingImage = tempFile

            // If an image has been selected, copy it to imageFile.
            if (existingImage?.exists() == true) {
                Timber.d("Copying cached image file from \"$existingImage\" to \"$imageFile\"")
                existingImage.copyTo(imageFile, overwrite = true)
            } else {
                // Otherwise, download it from the server.
                Timber.d("Downloading image from Firebase Storage: $imageReferenceUrl...")
                Timber.v("Getting reference...")
                val reference = storage.getReferenceFromUrl(imageReferenceUrl)
                val taskSnapshot = reference
                    .getFile(imageFile)
                    .await()
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

    private suspend fun fixImageReferenceUrl(
        image: String,
        firestore: FirebaseFirestore,
        path: String
    ): Pair<String?, Result?> =
        if (image.startsWith("https://escalaralcoiaicomtat.centrexcursionistalcoi.org/"))
            try {
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
     * @see ERROR_DATA_FETCH
     * @see ERROR_STORE_IMAGE
     */
    private suspend fun downloadZone(firestore: FirebaseFirestore, path: String): Result {
        Timber.d("Downloading Zone $path...")
        val document = try {
            Timber.v("Getting document...")
            firestore.document(path).get().await()
        } catch (e: Exception) {
            Timber.e(e, "Could not get data")
            return failure(ERROR_DATA_FETCH)
        }
        Timber.v("Got Zone document!")

        val zone = Zone(document)

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
            zone.kmzFile(applicationContext, storage, true)
        } catch (e: IllegalStateException) {
            Firebase.crashlytics.recordException(e)
            Timber.w("The Zone ($zone) does not contain a KMZ address")
        } catch (e: StorageException) {
            Firebase.crashlytics.recordException(e)
            val handler = handleStorageException(e)
            if (handler != null)
                Timber.e(e, handler.second)
        }

        Timber.d("Downloading child sectors...")
        val sectors = zone.getChildren(appSearchSession)
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
     * @see ERROR_DATA_FETCH
     * @see ERROR_STORE_IMAGE
     */
    private suspend fun downloadSector(
        firestore: FirebaseFirestore,
        path: String,
        progress: ValueMax<Int>?
    ): Result {
        Timber.d("Downloading Sector $path...")
        val document = try {
            Timber.v("Getting document...")
            firestore.document(path).get().await()
        } catch (e: Exception) {
            return failure(ERROR_DATA_FETCH)
        }
        val sector = Sector(document)

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
            sector.kmzFile(applicationContext, storage, true)
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

    override suspend fun doWork(): Result {
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

            Timber.v("Initializing search session...")
            appSearchSession =
                LocalStorage.createSearchSession(
                    LocalStorage.SearchContext.Builder(applicationContext, SEARCH_DATABASE_NAME)
                        .build()
                ).await()

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

            var downloadResult = when (namespace) {
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
                val intent: PendingIntent? = run {
                    Timber.v("Getting intent...")
                    when (namespace) {
                        // Area Skipped since not-downloadable
                        Zone.NAMESPACE -> {
                            // Example: Areas/<Area ID>/Zones/<Zone ID>
                            val zoneId = downloadPath!!.split('/')[3]
                            ZoneActivity.intent(applicationContext, zoneId)
                        }
                        Sector.NAMESPACE -> {
                            // Example: Areas/<Area ID>/Zones/<Zone ID>/Sectors/<Sector ID>
                            val zoneId = downloadPath!!.split('/')[3]
                            val sectorId = downloadPath!!.split('/')[5]
                            SectorActivity.intent(applicationContext, zoneId, sectorId)
                        }
                        else -> {
                            downloadResult = failure(ERROR_UNKNOWN_NAMESPACE)
                            null
                        }
                    }?.let { intent ->
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

            Timber.v("Closing search session...")
            appSearchSession.close()

            downloadResult
        }
    }

    companion object : DownloadWorkerFactory {
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
        @JvmStatic
        override fun schedule(
            context: Context,
            tag: String,
            data: DownloadData
        ): LiveData<WorkInfo> {
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
