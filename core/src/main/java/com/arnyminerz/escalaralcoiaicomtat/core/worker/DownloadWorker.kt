package com.arnyminerz.escalaralcoiaicomtat.core.worker

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.android.volley.Request
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DOWNLOADABLE_NAMESPACES
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.DataClassDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.repository.DataClassRepository
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.exception.InitializationException
import com.arnyminerz.escalaralcoiaicomtat.core.network.VolleySingleton
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_COMPLETE_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.DOWNLOAD_PROGRESS_CHANNEL_ID
import com.arnyminerz.escalaralcoiaicomtat.core.notification.Notification
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_OVERWRITE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DATA_FETCH
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DATA_LIST
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DOWNLOAD_ENDPOINT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.InputStreamVolleyRequest
import com.arnyminerz.escalaralcoiaicomtat.core.utils.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getJson
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_DISPLAY_NAME
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_NAMESPACE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_OBJECT_ID
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_OVERWRITE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DOWNLOAD_QUALITY
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
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_INVALID_NAMESPACE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_MISSING_DATA
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_NAMESPACE_NOT_SET
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_OBJECT_ID_NOT_SET
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.ERROR_STORE_IMAGE
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.WORKER_TAG_DOWNLOAD
import com.google.android.material.badge.ExperimentalBadgeUtils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
    private var overwrite: Boolean = false
    private var quality: Int = -1

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
    private lateinit var repository: DataClassRepository

    private data class ImageDownloadData(
        val imagePath: String,
        val imageFile: File,
        @ObjectId val objectId: String,
        val namespace: Namespace,
        val scale: Float
    )

    private data class KMZDownloadData(
        val kmzPath: String,
        val kmzFile: File
    )

    /**
     * Downloads the image file for the specified object.
     * @author Arnau Mora
     * @since 20210822
     * @param data The data class that sets what to download
     */
    private suspend fun downloadImageFile(
        data: ImageDownloadData,
    ): Result = coroutineScope {
        val imagePath: String = data.imagePath
        val imageFile: File = data.imageFile
        val objectId: String = data.objectId
        val namespace: Namespace = data.namespace
        val scale: Float = data.scale

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
                Timber.d("Downloading image... Path: $imagePath...")
                Timber.v("Fetching stream...")
                val bytes = suspendCoroutine<ByteArray> { cont ->
                    val request = InputStreamVolleyRequest(
                        Request.Method.GET,
                        "$REST_API_DOWNLOAD_ENDPOINT$imagePath",
                        { bytes -> cont.resume(bytes) },
                        { error -> cont.resumeWithException(error) },
                        emptyMap(),
                        emptyMap()
                    )
                    VolleySingleton.getInstance(applicationContext).addToRequestQueue(request)
                }

                // First decode the stream into a Bitmap, and then encode it to the file. This way
                // the compression can be controlled better, as well of the strain on the server.
                Timber.v("Got image stream, decoding...")
                val bitmap: Bitmap? = BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size,
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
        } catch (e: VolleyError) {
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
     * @param data The instance that sets what to download.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadKmz(
        data: KMZDownloadData,
    ) = coroutineScope {
        val kmzPath: String = data.kmzPath
        val targetFile: File = data.kmzFile

        Timber.v("Getting the stream of the KMZ file ($kmzPath)...")

        val bytes = suspendCoroutine<ByteArray> { cont ->
            val request = InputStreamVolleyRequest(
                Request.Method.GET,
                "$REST_API_DOWNLOAD_ENDPOINT$kmzPath",
                { cont.resume(it) },
                { cont.resumeWithException(it) },
                emptyMap(),
                emptyMap(),
            )
            VolleySingleton.getInstance(applicationContext).addToRequestQueue(request)
        }

        Timber.v("Got the KMZ stream. Writing to the output file...")
        targetFile
            .outputStream()
            .use { it.write(bytes) }
    }

    /**
     * A class used for transferring data between the download function and the conclusion function.
     * This is used then into the [Result.success] if the download was successful to index the
     * downloaded data, and display it to the user.
     * @author Arnau Mora
     * @since 20211231
     * @param namespace The namespace of the DataClass.
     * @param objectId The id of the DataClass.
     * @param displayName The display name of the item.
     * @param childrenCount The amount of children the DataClass has. It's null if the DataClass is
     * a Sector.
     * @param parentId The id of the parent DataClass. It's null for Zones since it's not used.
     */
    internal data class DownloadData(
        val namespace: Namespace,
        @ObjectId
        val objectId: String,
        val displayName: String,
        var childrenCount: Long = 0,
        val parentId: String? = null
    ) {
        var size: Long? = null
    }

    /**
     * Fetches the data from the server with [objectId] at [namespace]. And downloads the image
     * file.
     * @author Arnau Mora
     * @since 20210926
     * @param objectId The id of the object to download.
     * @param namespace The namespace of the object to download.
     * @return A pair with a [DownloadData] and a list of [DownloadData]. The first is the parent's
     * data instance. The second is a list of the children's download data. If downloading a sector
     * the list should be empty.
     * @throws RuntimeException When there is an unexpected type exception in the data from the server.
     * @throws IllegalStateException If the object doesn't contain a required field.
     * @throws NoSuchElementException If the specified combination of [objectId] and [namespace]
     * does not have any match on the server.
     * @throws InitializationException When the [namespace] is not valid.
     */
    private suspend fun downloadData(
        namespace: Namespace,
        @ObjectId objectId: String,
    ): Pair<DownloadData, List<DownloadData>> = coroutineScope {
        val imageFiles = arrayListOf<ImageDownloadData>()
        val kmzFiles = arrayListOf<KMZDownloadData>()

        /**
         * Fetches data from the server, adds images and kmz to the download lists, and returns
         * a list of [DownloadData] with the data of the objects.
         * @author Arnau Mora
         * @since 20220304
         * @param namespace The namespace of the DataClass to download.
         * @param objectId The id of the parent DataClass if [isParent] is false, or the id of the
         * DataClass to download otherwise.
         * @param isParent If the [objectId] represents the item to download (false), or the parent
         * of a list (true).
         */
        suspend fun generateData(
            namespace: Namespace,
            @ObjectId objectId: String,
            isParent: Boolean
        ): List<DownloadData> {
            // Get data of the DataClass
            Timber.v("Getting data ($namespace/$objectId)...")
            val endpoint = if (isParent)
                REST_API_DATA_LIST
            else
                REST_API_DATA_FETCH
            val url = "$endpoint${namespace.tableName}/$objectId"

            // Disabled since currently server not working
            // if (namespace == Zone.NAMESPACE)
            //     url += "?loadChildren=true"

            val jsonData = applicationContext.getJson(url)
            Timber.v("Parsing result data...")
            val jsonResult = if (jsonData.has("result")) jsonData.getJSONObject("result")
            else throw IllegalStateException("Could not find \"result\" field.")

            val resultList = arrayListOf<DownloadData>()

            val objectIds = jsonResult.keys()
            for (jsonObjectId in objectIds) {
                val jsonObject = jsonResult.getJSONObject(jsonObjectId)

                Timber.v("Getting children count...")
                val childrenCount = namespace.ChildrenNamespace?.let { childrenNamespace ->
                    repository
                        .getChildren(childrenNamespace, objectId)
                        ?.size
                        ?.toLong()
                } ?: 0L

                Timber.v("Building DataClass object...")
                val dataClass =
                    DataClass.buildContainers(namespace, jsonObjectId, jsonObject, childrenCount)

                Timber.v("Adding image file to downloads list...")
                imageFiles.add(
                    ImageDownloadData(
                        dataClass.imagePath,
                        dataClass.imageFile(applicationContext),
                        jsonObjectId,
                        namespace,
                        // TODO: Scale may be bigger for sector images
                        .7f
                    )
                )

                if (dataClass.kmzPath != null) {
                    Timber.v("Adding KMZ file to downloads list...")
                    kmzFiles.add(
                        KMZDownloadData(
                            dataClass.kmzPath!!,
                            dataClass.kmzFile(applicationContext, true)
                        )
                    )
                }

                resultList.add(
                    DownloadData(
                        namespace,
                        jsonObjectId,
                        displayName,
                        0,
                        objectId.takeIf { isParent }
                    )
                )
            }

            return resultList
        }

        // Get the parent data
        val sourceDataList = generateData(namespace, objectId, false)
        if (sourceDataList.isEmpty())
            throw NoSuchElementException("Could not find DataClass at $namespace with $objectId.")
        val sourceData = sourceDataList.first()

        // Download children data
        val childrenList = if (namespace == Zone.NAMESPACE)
            generateData(Sector.NAMESPACE, objectId, true)
        else emptyList()

        // Update the children count of sourceData
        sourceData.childrenCount = childrenList.size.toLong()

        Timber.v("Downloading image files...")
        for (downloadRequest in imageFiles) {
            Timber.d(
                "Downloading \"%s\" to \"%s\"...",
                downloadRequest.imagePath,
                downloadRequest.imageFile.toString()
            )
            downloadImageFile(downloadRequest)
        }

        Timber.v("Downloading KMZ files...")
        for (downloadRequest in kmzFiles) {
            Timber.d(
                "Downloading \"%s\" to \"%s\"...",
                downloadRequest.kmzPath,
                downloadRequest.kmzFile
            )
            downloadKmz(downloadRequest)
        }

        sourceData to childrenList
    }

    override suspend fun doWork(): Result {
        // Get all data
        Timber.v("Getting work input data...")
        val displayName = inputData.getString(DOWNLOAD_DISPLAY_NAME)
            ?: return failure(ERROR_MISSING_DATA)
        val objectId = inputData.getString(DOWNLOAD_OBJECT_ID)
            ?: return failure(ERROR_OBJECT_ID_NOT_SET)
        val namespace = inputData.getString(DOWNLOAD_NAMESPACE)
            ?.let { Namespace.find(it) }
            ?: return failure(ERROR_NAMESPACE_NOT_SET)
        overwrite = inputData.getBoolean(DOWNLOAD_OVERWRITE, DOWNLOAD_OVERWRITE_DEFAULT)
        quality = inputData.getInt(DOWNLOAD_OVERWRITE, DOWNLOAD_QUALITY_DEFAULT)

        // Check for namespace validity
        Timber.v("Got input data. Checking if namespace is valid...")
        if (!DOWNLOADABLE_NAMESPACES.contains(namespace)) {
            Timber.e("Tried to download a non-downloadable or unknown namespace: $namespace")
            return failure(ERROR_INVALID_NAMESPACE)
        }

        Timber.v("Starting download for %s".format(displayName))

        // Check if any required data is missing
        Timber.v("Initializing repository...")
        val database = DataClassDatabase.getInstance(applicationContext)
        repository = DataClassRepository(
            database.areasDao(),
            database.zonesDao(),
            database.sectorsDao(),
            database.pathsDao(),
        )

        Timber.v("Downloading \"$displayName\"...")
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

        val downloadResult = try {
            downloadData(namespace, objectId).let { downloadData ->
                val parentData = downloadData.first
                val childrenData = downloadData.second
                if (childrenData.size.toLong() != parentData.childrenCount)
                    Timber.w("Loaded children data and theoretical children do not match.")

                val childrenObjectIds = arrayListOf<String>()
                val childrenNames = arrayListOf<String>()
                val childrenSizes = arrayListOf<Long>()
                for (data in childrenData) {
                    childrenObjectIds.add(data.objectId)
                    childrenNames.add(data.displayName)
                    childrenSizes.add(data.size ?: 0)
                }

                Result.success(
                    workDataOf(
                        "namespace" to parentData.namespace,
                        "objectId" to parentData.objectId,
                        "childrenCount" to parentData.childrenCount,
                        "parentId" to parentData.parentId,
                        "size" to parentData.size,
                        "childrenIds" to childrenObjectIds.toTypedArray(),
                        "childrenNames" to childrenNames.toTypedArray(),
                        "childrenSizes" to childrenSizes.toTypedArray(),
                    )
                )
            }
        } catch (e: VolleyError) {
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

        val downloadResultData = downloadResult.outputData
        val intent: PendingIntent? =
            if (downloadResultData.error == null) {
                Timber.v("Getting intent...")
                //DataClass.getIntent(context, appSearchSession, downloadPath)
                // TODO: Fix the launching intent
                null
                /*when (namespace) {
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
                }*/
            } else null

        if (downloadResultData.error == null) {
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

            // This is for making it easier to recover later on which DataClasses are downloaded
            Timber.v("Indexing downloaded element...")
            // First get all the data from the downloaded result
            // TODO: parentId may not be correct
            val parentId = downloadResultData.getString("parentId") ?: run {
                // Zones won't have any parentId since Areas are not downloadable, so
                // if the downloaded item is a zone, skip parentId check and return
                // an empty string
                if (namespace == Zone.NAMESPACE)
                    ""
                else {
                    Timber.w("Could not get \"parentId\". Data: $downloadResultData")
                    return failure(ERROR_DATA_TRANSFERENCE)
                }
            }
            val size = downloadResultData.getLong("size", 0)
            val childrenCount = downloadResultData.getLong("childrenCount", -1)

            val childrenIds = downloadResultData.getStringArray("childrenIds") ?: arrayOf()
            val childrenDisplayNames =
                downloadResultData.getStringArray("childrenNames") ?: arrayOf()
            val childrenSizes =
                downloadResultData.getLongArray("childrenSizes") ?: longArrayOf()

            // Process the data
            val timestamp = System.currentTimeMillis()
            val dataClass = repository.get(namespace, objectId)

            Timber.d("DataClass: $dataClass")

            // Add the data of all the children
            if (childrenIds.size.toLong() != childrenCount)
                Timber.w("Children id count does not match childrenCount.")
            for (i in childrenIds.indices)
                repository.getSector(objectId)
                    ?.apply {
                        downloaded = true
                        downloadSize = childrenSizes[i]
                    }
                    ?.let { repository.update(it) }

            // Index it into AppSearch
            if (dataClass != null) {
                Timber.d("Updating DataClass...")
                (dataClass as? ZoneData)?.apply { downloaded = true; downloadSize = size }
                    ?: (dataClass as? SectorData)?.apply { downloaded = true; downloadSize = size }
                repository.update(dataClass)
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

        Timber.v("Closing db...")
        database.close()

        return downloadResult
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
        @WorkerThread
        override suspend fun schedule(
            context: Context,
            tag: String,
            data: com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadData
        ): LiveData<WorkInfo> {
            Timber.v("Scheduling new download...")
            Timber.v("Building download constraints...")
            val constraints = Constraints.Builder()
                .apply {
                    val getMobileDownloadsEnabled = PreferencesModule.getMobileDownloadsEnabled()
                    val getRoamingDownloadsEnabled = PreferencesModule.getRoamingDownloadsEnabled()
                    val getMeteredDownloadsEnabled = PreferencesModule.getMeteredDownloadsEnabled()
                    if (getMobileDownloadsEnabled.first() && getMeteredDownloadsEnabled.first())
                        setRequiredNetworkType(NetworkType.UNMETERED)
                    else if (!getRoamingDownloadsEnabled.first())
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
                            DOWNLOAD_DISPLAY_NAME to displayName,
                            DOWNLOAD_NAMESPACE to namespace,
                            DOWNLOAD_OBJECT_ID to objectId,
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
