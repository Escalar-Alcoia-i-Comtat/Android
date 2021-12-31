package com.arnyminerz.escalaralcoiaicomtat.worker

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.PutDocumentsRequest
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
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_COMPLETE_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.Notification
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DATACLASS_PREVIEW_SCALE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_OVERWRITE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_MOBILE_DOWNLOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ROAMING_DOWNLOAD_PREF
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.core.utils.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.createSearchSession
import com.arnyminerz.escalaralcoiaicomtat.core.utils.progress
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toInt
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_DISPLAY_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_OVERWRITE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_PATH
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_QUALITY
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadData
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadWorkerFactory
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadWorkerModel
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_ALREADY_DOWNLOADED
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_COMPRESS_IMAGE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_CREATE_PARENT
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_DATA_FETCH
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_DATA_FRAGMENTED
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_DATA_TRANSFERENCE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_DATA_TYPE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_FETCH_IMAGE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_MISSING_DATA
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_STORE_IMAGE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_UNKNOWN_NAMESPACE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.WORKER_TAG_DOWNLOAD
import com.arnyminerz.escalaralcoiaicomtat.core.worker.error
import com.arnyminerz.escalaralcoiaicomtat.core.worker.failure
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * The [CoroutineWorker] for scheduling [DataClass] downloads.
 * @author Arnau Mora
 * @since 20210928
 */
@ExperimentalBadgeUtils
class DownloadWorker
private constructor(appContext: Context, workerParams: WorkerParameters) :
    DownloadWorkerModel, CoroutineWorker(appContext, workerParams) {
    override val factory: DownloadWorkerFactory = Companion

    private lateinit var displayName: String
    private var downloadPath: String? = null
    private var overwrite: Boolean = false
    private var quality: Int = -1

    /**
     * A reference to a [FirebaseStorage] instance for doing file-related operations.
     * @author Arnau Mora
     * @since 20210928
     */
    private lateinit var storage: FirebaseStorage

    /**
     * A reference to a [FirebaseFirestore] instance for fetching data from the server.
     * @author Arnau Mora
     * @since 20210928
     */
    private lateinit var firestore: FirebaseFirestore

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

    private data class ImageDownloadData(
        val imageReference: StorageReference,
        val imageFile: File,
        val objectId: String,
        val namespace: String,
        val scale: Float
    )

    private data class KMZDownloadData(
        val kmzReference: StorageReference,
        val kmzFile: File
    )

    /**
     * Downloads the image file for the specified object.
     * @author Arnau Mora
     * @since 20210822
     * @param imageReference The [FirebaseStorage] reference of the image to download.
     * @param imageFile The [File] instance referencing where the object's image should be stored at.
     * @param objectId The id of the object to download.
     * @param namespace The namespace of the object to download.
     * @param scale The scale with which to download the object.
     * @param progressListener A listener for observing the download progress.
     */
    private suspend fun downloadImageFile(
        imageReference: StorageReference,
        imageFile: File,
        objectId: String,
        namespace: String,
        scale: Float,
        progressListener: suspend (progress: ValueMax<Long>) -> Unit
    ): Result = coroutineScope {
        val dataDir = imageFile.parentFile!!

        // Check that everything is ready for downloading the image file.
        when {
            imageFile.exists() && !overwrite ->
                // If the image has already been downloaded and override is set to false
                failure(ERROR_ALREADY_DOWNLOADED)
            !dataDir.exists() && !dataDir.mkdirs() ->
                // If the images data dir could not be created
                failure(ERROR_CREATE_PARENT)
            else -> null
        }?.let { error ->
            return@coroutineScope error
        }

        // Update the info text
        notification.update {
            withInfoText(R.string.notification_download_progress_info_downloading_image)
        }

        try {
            // Check if there's already an image downloaded to be used.
            // This is done this way so the server is not overloaded
            var existingImage: File? = null
            val cacheDir = applicationContext.cacheDir

            // If there's a full version cached version of the image, select it
            val cacheFile = File(cacheDir, DataClass.imageName(namespace, objectId, null))
            if (existingImage == null && cacheFile.exists())
                existingImage = cacheFile

            // If there's an scaled (given scale) version of the image, select it
            val scaledFile = File(
                cacheDir,
                DataClass.imageName(namespace, objectId, "scale$scale")
            )
            if (existingImage == null && scaledFile.exists())
                existingImage = scaledFile

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
                Timber.d("Downloading image from Firebase Storage: $imageReference...")
                Timber.v("Fetching stream...")
                val snapshot = imageReference
                    .stream
                    .addOnProgressListener { snapshot ->
                        val progress = snapshot.progress()
                        CoroutineScope(coroutineContext).launch { progressListener(progress) }
                        Timber.v("Image progress: ${progress.percentage}")
                    }
                    .await()

                // First decode the stream into a Bitmap, and then encode it to the file. This way
                // the compression can be controlled better, as well of the strain on the server.
                Timber.v("Got image stream, decoding...")
                val stream = snapshot.stream
                val bitmap: Bitmap? = BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply {
                        inSampleSize = (1 / scale).toInt()
                    }
                )

                if (bitmap != null) {
                    Timber.v("Got bitmap.")
                    val quality = (scale * 100).toInt()
                    Timber.v("Compression quality: $quality")
                    Timber.v("Compressing bitmap to \"$imageFile\"...")
                    val compressed =
                        bitmap.compress(WEBP_LOSSY_LEGACY, quality, imageFile.outputStream())
                    if (compressed)
                        Timber.v("Bitmap compressed successfully.")
                    else
                        return@coroutineScope failure(ERROR_COMPRESS_IMAGE)
                } else
                    return@coroutineScope failure(ERROR_FETCH_IMAGE)
            }
        } catch (e: StorageException) {
            Timber.w(e, "Could not get image")
            return@coroutineScope failure(ERROR_FETCH_IMAGE)
        } catch (e: IOException) {
            Timber.e(e, "Could not copy image file")
            return@coroutineScope failure(ERROR_STORE_IMAGE)
        }

        if (!imageFile.exists())
            return@coroutineScope failure(ERROR_STORE_IMAGE)

        return@coroutineScope Result.success()
    }

    /**
     * Downloads the KMZ file from the server.
     * @author Arnau Mora
     * @since 20210926
     * @param reference The [StorageReference] of the target kmz file to download.
     * @param targetFile The [File] to store the KMZ data at.
     * @param progressListener A callback function for observing the download progress.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadKmz(
        reference: StorageReference,
        targetFile: File,
        progressListener: suspend (progress: ValueMax<Long>) -> Unit
    ) = coroutineScope {
        progressListener(ValueMax(0, -1))

        Timber.v("Getting the stream of the KMZ file ($reference)...")

        val snapshot = reference
            .stream
            .addOnProgressListener { snapshot ->
                CoroutineScope(coroutineContext).launch { progressListener(snapshot.progress()) }
            }
            .await()
        progressListener(snapshot.progress())

        Timber.v("Got the KMZ stream. Writing to the output file...")
        val stream = snapshot.stream
        val outputStream = targetFile.outputStream()
        var read = stream.read()
        while (read > 0) {
            outputStream.write(read)
            read = stream.read()
        }
        outputStream.close()
        stream.close()
    }

    /**
     * A class used for transferring data between the download function and the conclusion function.
     * This is used then into the [Result.success] if the download was successful to index the
     * downloaded data, and display it to the user.
     * @author Arnau Mora
     * @since 20211231
     * @param namespace The namespace of the DataClass.
     * @param objectId The id of the DataClass.
     * @param childrenCount The amount of children the DataClass has. It's null if the DataClass is
     * a Sector.
     * @param parentId The id of the parent DataClass. It's null for Zones since it's not used.
     */
    internal data class DownloadData(
        val namespace: String,
        val objectId: String,
        val childrenCount: Long? = null,
        val parentId: String? = null
    )

    /**
     * Fetches the data from [FirebaseFirestore] at the specified [path]. And downloads the image
     * file.
     * @author Arnau Mora
     * @since 20210926
     * @param path The path where the data is stored at.
     * @param progressListener A callback function for observing the download progress. When the
     * data is being fetched from the server, an undetermined state will be returned. Once files
     * start getting downloaded, an overall progress will be passed.
     * @return A [DownloadData] object with the data of the downloaded object.
     * @throws FirebaseFirestoreException If there happens an exception while fetching the data
     * from the server.
     * @throws RuntimeException When there is an unexpected type exception in the data from the server.
     * @throws IllegalStateException If the document at [path] doesn't contain a required field.
     */
    @Throws(FirebaseFirestoreException::class)
    private suspend fun downloadData(
        path: String,
        progressListener: suspend (progress: ValueMax<Long>) -> Unit
    ): DownloadData = coroutineScope {
        val imageFiles = arrayListOf<ImageDownloadData>()
        val kmzFiles = arrayListOf<KMZDownloadData>()

        /**
         * Fetches the required data from [path], and adds the requests to imageFiles and kmzFiles
         * if necessary.
         * @author Arnau Mora
         * @since 20210928
         * @param path The path inside [firestore] where to download the data from.
         * @return An instance of the [DownloadData] class. If it's zone, [DownloadData.childrenCount]
         * should match the amount of sectors inside the zone. If it's sector, it should be -1.
         */
        suspend fun scheduleDownload(path: String): DownloadData {
            // Get the document data from Firestore
            Timber.v("Getting document ($path)...")
            val document = firestore.document(path).get().await()

            Timber.v("Processing namespace...")
            val namespace = document.reference.parent.id.let { collectionName ->
                // Remove the last letter to get singular: Ex. from "Areas" get "Area"
                collectionName.substring(0, collectionName.length - 1)
            }

            // Get all the fields from the document
            Timber.v("Getting fields...")
            val objectId = document.id
            val imageReferenceUrl = document.getString("image") ?: run {
                // All documents should contain an image, if not set, throw an exception.
                Timber.w("Object at \"$path\" doesn't have an image field.")
                throw IllegalStateException("Object at \"$path\" doesn't have an image field.")
            }
            val kmzReferenceUrl = document.getString("kmz")

            Timber.v("Processing image file download data...")
            val imageFileName = DataClass.imageName(namespace, objectId, null)
            val imageFile = File(dataDir(applicationContext), imageFileName)
            // If the download is Zone, store the image in lower res, if it's Sector, download HD
            val isPreview = namespace == Zone.NAMESPACE
            val scale = if (isPreview) DATACLASS_PREVIEW_SCALE else 1f
            val imageReference = storage.getReferenceFromUrl(imageReferenceUrl)

            Timber.v("Adding image download request data to the imageFiles list...")
            imageFiles.add(ImageDownloadData(imageReference, imageFile, objectId, namespace, scale))

            if (kmzReferenceUrl != null) {
                Timber.v("Processing KMZ file download data...")
                val kmzFileName = "${namespace}_$objectId"
                val kmzFile = File(dataDir(applicationContext), kmzFileName)
                val kmzReference = storage.getReferenceFromUrl(kmzReferenceUrl)

                Timber.v("Adding kmz download request data to the kmzFiles list...")
                kmzFiles.add(KMZDownloadData(kmzReference, kmzFile))
            }

            // Download the children if it's a zone
            var childrenCount: Long? = null
            var parentId: String? = null
            if (namespace == Zone.NAMESPACE) {
                // Get all the documents inside the "Sectors" collection in the Zone document.
                Timber.v("Getting children sectors...")
                val sectorsCollection = document.reference.collection("Sectors")
                val collectionsReference = sectorsCollection.get().await()
                val documents = collectionsReference.documents
                Timber.v("Got ${documents.size} sector documents...")
                for (sectorDocument in documents) {
                    val documentPath: String = sectorDocument.reference.path
                    scheduleDownload(documentPath)
                    childrenCount?.plus(1L) ?: run { childrenCount = 1 }
                }
            } else if (namespace == Sector.NAMESPACE) {
                // If it's a sector, parentId should be loaded.
                // First split the path, and get the zone id
                val splitPath = path.split('/')
                parentId = splitPath[splitPath.size - 3]
            }

            return DownloadData(namespace, objectId, childrenCount, parentId)
        }

        Timber.v("Calling progress listener...")
        progressListener(ValueMax(0, -1)) // Set to -1 for indeterminate
        Timber.v("Initializing data fetch...")
        val parentData = scheduleDownload(path)

        // This total byte count is used for displaying the total progress, not individually on each
        // item.
        Timber.v("Suming total byte count...")
        val imageTotalBytes = imageFiles.sumOf { it.imageReference.metadata.await().sizeBytes }
        val kmzTotalBytes = kmzFiles.sumOf { it.kmzReference.metadata.await().sizeBytes }
        val totalBytes = imageTotalBytes + kmzTotalBytes
        var bytesCounter = 0L
        var lastElementByteCount = 0L

        val downloadProgressListener: suspend (progress: ValueMax<Long>) -> Unit = { progress ->
            val vm = ValueMax(progress.value + bytesCounter, totalBytes)
            Timber.v("Download progress: ${vm.percentage}%. Item: ${progress.percentage}%")
            lastElementByteCount = progress.max
            progressListener(vm)
        }

        Timber.v("Downloading image files...")
        for (downloadRequest in imageFiles)
            downloadRequest.run {
                Timber.d("Downloading \"$imageReference\" to \"$imageFile\"...")
                downloadImageFile(
                    imageReference,
                    imageFile,
                    objectId,
                    namespace,
                    scale,
                    downloadProgressListener
                )
                bytesCounter += lastElementByteCount
            }

        Timber.v("Downloading KMZ files...")
        for (downloadRequest in kmzFiles)
            downloadRequest.run {
                Timber.d("Downloading \"$kmzReference\" to \"$kmzFile\"...")
                downloadKmz(kmzReference, kmzFile, downloadProgressListener)
                bytesCounter += lastElementByteCount
            }

        parentData
    }

    override suspend fun doWork(): Result {
        // Get all data
        downloadPath = inputData.getString(DOWNLOAD_PATH)
        val displayName = inputData.getString(DOWNLOAD_DISPLAY_NAME)
        overwrite = inputData.getBoolean(DOWNLOAD_OVERWRITE, DOWNLOAD_OVERWRITE_DEFAULT)
        quality = inputData.getInt(DOWNLOAD_OVERWRITE, DOWNLOAD_QUALITY_DEFAULT)

        Timber.v("Starting download for %s".format(displayName))

        // Check if any required data is missing
        return if (downloadPath == null || displayName == null)
            failure(ERROR_MISSING_DATA)
        else {
            Timber.v("Initializing Firebase Storage instance...")
            storage = Firebase.storage

            Timber.v("Initializing Firebase Firestore instance...")
            firestore = Firebase.firestore

            Timber.v("Initializing search session...")
            appSearchSession = createSearchSession(applicationContext)

            Timber.v("Downloading $downloadPath...")
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

            var namespace: String? = null
            var downloadResult = try {
                downloadData(downloadPath!!) { progress ->
                    setProgress(workDataOf("progress" to progress.percentage))
                    notification = notification.update {
                        withProgress(progress.toInt())
                    }
                }.let { downloadData ->
                    namespace = downloadData.namespace
                    Result.success(
                        workDataOf(
                            "namespace" to downloadData.namespace,
                            "objectId" to downloadData.objectId,
                            "childrenCount" to downloadData.childrenCount,
                            "parentId" to downloadData.parentId,
                        )
                    )
                }
            } catch (e: FirebaseFirestoreException) {
                Timber.e(e, "There was an error while fetching data from the database.")
                Timber.v("Destroying the progress notification...")
                failure(ERROR_DATA_FETCH)
            } catch (e: RuntimeException) {
                Timber.e(e, "The type of a field from the server was not expected.")
                Timber.v("Destroying the progress notification...")
                failure(ERROR_DATA_TYPE)
            } catch (e: IllegalStateException) {
                Timber.e(e, "The type of a field from the server was not expected.")
                Timber.v("Destroying the progress notification...")
                failure(ERROR_DATA_FRAGMENTED)
            }

            Timber.v("Finished downloading $displayName. Result: $downloadResult")
            notification.destroy()

            val intent: PendingIntent? =
                if (downloadResult == Result.success()) {
                    Timber.v("Getting intent...")
                    val downloadPathSplit = downloadPath!!.split('/')
                    when (namespace) {
                        // Area Skipped since not-downloadable
                        Zone.NAMESPACE -> {
                            // Example: Areas/<Area ID>/Zones/<Zone ID>
                            val zoneId = downloadPathSplit[3]
                            Timber.v("Intent will launch zone with id $zoneId")
                            ZoneActivity.intent(applicationContext, zoneId)
                        }
                        Sector.NAMESPACE -> {
                            // Example: Areas/<Area ID>/Zones/<Zone ID>/Sectors/<Sector ID>
                            val zoneId = downloadPathSplit[3]
                            val sectorId = downloadPathSplit[5]
                            Timber.v("Intent will launch sector with id $sectorId in $zoneId.")
                            SectorActivity.intent(applicationContext, zoneId, sectorId)
                        }
                        else -> {
                            downloadResult = failure(ERROR_UNKNOWN_NAMESPACE)
                            null
                        }
                    }?.let { intent ->
                        val pendingIntent = PendingIntent.getActivity(
                            applicationContext,
                            (System.currentTimeMillis() and 0xffffff).toInt(),
                            intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                        )
                        pendingIntent
                    }
                } else null

            if (downloadResult == Result.success()) {
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

                val downloadResultData = downloadResult.outputData
                if (namespace != null && downloadPath != null) {
                    // This is for making it easier to recover later on which DataClasses are downloaded
                    Timber.v("Indexing downloaded element...")
                    // First get all the data from the downloaded result
                    val objectId = downloadResultData.getString("objectId")
                        ?: return failure(ERROR_DATA_TRANSFERENCE)
                    val parentId = downloadResultData.getString("parentId")
                        ?: return failure(ERROR_DATA_TRANSFERENCE)
                    val childrenCount = downloadResultData.getLong("childrenCount", -1)
                    // Process the data, and index it into AppSearch
                    val downloadedData = DownloadedData(
                        objectId,
                        System.currentTimeMillis(),
                        namespace!!,
                        displayName,
                        downloadPath!!,
                        childrenCount,
                        parentId
                    )
                    Timber.d("DownloadedData: $downloadedData")
                    val request = PutDocumentsRequest.Builder()
                        .addDocuments(downloadedData)
                        .build()
                    val result = appSearchSession.put(request).await()
                    if (result.isSuccess)
                        Timber.i("Indexed download correctly.")
                    else {
                        Timber.i("Failures:")
                        for (failure in result.failures) {
                            val value = failure.value
                            Timber.i("- ${value.resultCode} :: ${value.errorMessage}")
                        }
                    }
                }
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
                        downloadResult.outputData.error ?: "unknown"
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
         */
        @JvmStatic
        override fun schedule(
            context: Context,
            tag: String,
            data: com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadData
        ): LiveData<WorkInfo> {
            Timber.v("Scheduling new download...")
            Timber.v("Building download constraints...")
            val constraints = Constraints.Builder()
                .apply {
                    if (SETTINGS_MOBILE_DOWNLOAD_PREF.get())
                        setRequiredNetworkType(NetworkType.UNMETERED)
                    else if (!SETTINGS_ROAMING_DOWNLOAD_PREF.get())
                        setRequiredNetworkType(NetworkType.NOT_ROAMING)
                    else
                        setRequiredNetworkType(NetworkType.CONNECTED)
                }

            Timber.v("Building DownloadWorker request...")
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints.build())
                .addTag(WORKER_TAG_DOWNLOAD)
                .addTag(tag)
                .setInputData(
                    with(data) {
                        workDataOf(
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
