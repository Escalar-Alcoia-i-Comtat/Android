package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import com.android.volley.Request
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.exception.InitializationException
import com.arnyminerz.escalaralcoiaicomtat.core.exception.NotDownloadedException
import com.arnyminerz.escalaralcoiaicomtat.core.network.VolleySingleton
import com.arnyminerz.escalaralcoiaicomtat.core.network.addToRequestQueue
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_MAX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_MIN
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_CHILDREN_COUNT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DOWNLOAD_ENDPOINT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.InputStreamVolleyRequest
import com.arnyminerz.escalaralcoiaicomtat.core.utils.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.allTrue
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadData
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadWorkerFactory
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.skydoves.landscapist.ShimmerParams
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.parcelize.IgnoredOnParcel
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable
import java.util.Date
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * The main data storage class.
 * @author Arnau Mora
 * @since 20210830
 * @param A The children type.
 * @param B A reference of the current type.
 * @param displayName The name that will be displayed to the user.
 * @param timestampMillis The creation date of the [DataClass] in milliseconds.
 * @param imagePath The path of the DataClass' image on the server.
 * @param kmzPath The path of the DataClass' KMZ file on the server.
 * May be null if not applicable or non-existing.
 * @param location The coordinates of the [DataClass] to show in a map.
 * @param metadata Some metadata of the [DataClass].
 * @param displayOptions Options for displaying in the UI.
 */
abstract class DataClass<A : DataClassImpl, B : DataClassImpl, D : DataRoot<*>>(
    /**
     * The name that will be displayed to the user.
     * @author Arnau Mora
     * @since 20210830
     */
    override val displayName: String,
    /**
     * The creation date of the [DataClass] in milliseconds.
     * @author Arnau Mora
     * @since 20210830
     */
    override val timestampMillis: Long,
    /**
     * The path of the DataClass' image on the server.
     * @author Arnau Mora
     * @since 20220221
     */
    open val imagePath: String,
    /**
     * The path of the DataClass' KMZ file on the server.
     * May be null if not applicable or non-existing.
     * @author Arnau Mora
     * @since 20210830
     */
    open val kmzPath: String?,
    /**
     * The coordinates of the [DataClass] to show in a map.
     * @author Arnau Mora
     * @since 20210830
     */
    open val location: GeoPoint?,
    /**
     * Some metadata of the [DataClass].
     * @author Arnau Mora
     * @since 20210830
     */
    open val metadata: DataClassMetadata,
    /**
     * Options for displaying in the UI.
     * @author Arnau Mora
     * @since 20210830
     */
    val displayOptions: DataClassDisplayOptions,
) : DataClassImpl(
    metadata.objectId,
    metadata.namespace,
    timestampMillis,
    displayName,
) {
    companion object {
        /**
         * Generates a unique ID that represents a DataClass unequivocally.
         * @author Arnau Mora
         * @since 20211231
         */
        fun generatePin(namespace: Namespace, objectId: String) = "${namespace}_$objectId"

        /**
         * Returns the correct image name for the desired [objectId] and [namespace].
         * @author Arnau Mora
         * @since 20210822
         */
        fun imageName(namespace: Namespace, objectId: String, suffix: String?) =
            "$namespace-$objectId${suffix ?: ""}"

        /**
         * Returns the File that represents the image of the DataClass
         * @author Arnau Mora
         * @date 2020/09/10
         * @param context The context to run from
         * @param namespace The namespace of the DataClass
         * @param objectId The id of the DataClass
         * @return The path of the image file that can be downloaded
         */
        private fun imageFile(context: Context, namespace: Namespace, objectId: String): File =
            File(dataDir(context), imageName(namespace, objectId, null))

        /**
         * Returns the File that represents the image of the DataClass in cache.
         * @author Arnau Mora
         * @date 20210724
         * @param context The context to run from
         * @param namespace The namespace of the DataClass
         * @param objectId The id of the DataClass
         * @param suffix If not null, will be added to the end of the file name.
         * @return The path of the image file that can be downloaded
         */
        private fun cacheImageFile(
            context: Context,
            namespace: Namespace,
            objectId: String,
            suffix: String? = null
        ): File =
            File(context.cacheDir, imageName(namespace, objectId, suffix))

        fun kmzFile(context: Context, permanent: Boolean, namespace: Namespace, objectId: String) =
            File(
                if (permanent) dataDir(context) else context.cacheDir,
                generatePin(namespace, objectId)
            )

        /**
         * Gets the [Intent] used to launch the [Activity] of a [DataClass] using [query] as the
         * search requirement.
         * @author Arnau Mora
         * @since 20210825
         * @param context The [Context] that is requesting the [Intent].
         * @param query What to search for. May be [DataClass.displayName] or [DataClassMetadata.webURL].
         * @return An [Intent] if the [DataClass] was found, or null.
         */
        @WorkerThread
        suspend fun getIntent(
            context: Context,
            activity: Class<*>,
            query: String
        ): Intent? = DataSingleton
            .getInstance(context)
            .repository
            .find(query)
            .takeIf { it.isNotEmpty() }
            ?.let { resultList ->
                val dataClass = resultList[0].data()
                Intent(context, activity)
                    .putExtra(EXTRA_DATACLASS, dataClass)
                    .apply {
                        if (dataClass is Sector)
                            dataClass
                                .getParent<Zone>(context)
                                ?.let { zone ->
                                    val sectors =
                                        zone.getChildren(context) { it.weight }
                                    putExtra(EXTRA_DATACLASS, zone)
                                    putExtra(EXTRA_CHILDREN_COUNT, sectors.size)
                                    putExtra(EXTRA_INDEX, sectors.indexOf(dataClass))
                                }
                    }
            }

        /**
         * Gets the [Intent] used to launch the [Activity] of a [DataClass] with [namespace] and if
         * [objectId].
         * @author Arnau Mora
         * @since 20220128
         * @param context The [Context] that is requesting the [Intent].
         * @param namespace The [DataClass.namespace] of the [objectId].
         * @param objectId The [DataClass.objectId] to search for.
         * @return An [Intent] if the [DataClass] was found, or null.
         */
        suspend inline fun <A : DataClass<*, *, *>, reified B : DataRoot<A>> getIntent(
            context: Context,
            activity: Class<*>,
            namespace: Namespace,
            @ObjectId objectId: String
        ): Intent? = DataSingleton
            .getInstance(context)
            .repository
            .find(namespace, objectId)
            ?.let { data ->
                val dataClass = data.data()
                Intent(context, activity)
                    .putExtra(EXTRA_DATACLASS, dataClass)
                    .apply {
                        if (dataClass is Sector)
                            dataClass
                                .getParent<Zone>(context)
                                ?.let { zone ->
                                    val sectors = zone.getChildren(context) { it.weight }
                                    putExtra(EXTRA_DATACLASS, zone)
                                    putExtra(EXTRA_CHILDREN_COUNT, sectors.size)
                                    putExtra(EXTRA_INDEX, sectors.indexOf(dataClass))
                                }
                    }
            }

        /**
         * Deletes a DataClass from the device.
         * @author Arnau Mora
         * @since 20211231
         * @param A The type of the children of the DataClass
         * @param context The [Context] that is requesting the deletion.
         * @param namespace The namespace of the DataClass to delete.
         * @param objectId The id of the DataClass to delete.
         * @return True if the DataClass was deleted successfully.
         */
        suspend fun <A : DataClassImpl> delete(
            context: Context,
            namespace: Namespace,
            objectId: String
        ): Boolean {
            Timber.v("Deleting $objectId")
            val lst = arrayListOf<Boolean>() // Stores all the delete success statuses

            // KMZ should be deleted
            val kmzFile = kmzFile(context, true, namespace, objectId)
            if (kmzFile.exists()) {
                Timber.v("$this > Deleting \"$kmzFile\"")
                lst.add(kmzFile.deleteIfExists())
            }

            // Instead of deleting image, move to cache. System will manage it if necessary.
            val imgFile = imageFile(context, namespace, objectId)
            if (imgFile.exists()) {
                val cacheImageFile = cacheImageFile(context, namespace, objectId)
                Timber.v("$this > Copying \"$imgFile\" to \"$cacheImageFile\"...")
                imgFile.copyTo(cacheImageFile, true)
                Timber.v("$this > Deleting \"$imgFile\"")
                lst.add(imgFile.delete())
            }

            val dataRepository = DataSingleton.getInstance(context).repository

            namespace.ChildrenNamespace?.let { childrenNamespace ->
                Timber.d("Deleting all elements from $childrenNamespace and parent object id $objectId")
                dataRepository.deleteFromParentId(childrenNamespace, objectId)
            }

            // Remove dataclass from index.
            // Since Sectors that are children of a Zone also contain its ID this will also remove them
            // from the index.
            val newElements = dataRepository.getAll(namespace)
                .filter { it is ZoneData || it is SectorData }
                .onEach {
                    if (it is ZoneData) it.downloaded = false
                    else if (it is SectorData) it.downloaded = false
                }
            dataRepository.updateAll(newElements)

            return lst.allTrue()
        }

        /**
         * Checks if the DataClass with id [objectId] is indexed as a download.
         * @author Arnau Mora
         * @since 20220101
         * @param context Used for searching through the index.
         * @param objectId The id of the DataClass to search for.
         */
        suspend fun isDownloadIndexed(
            context: Context,
            objectId: String
        ): DownloadStatus {
            Timber.d("Finding for element in indexed downloads...")
            val searchResults = DataSingleton.getInstance(context)
                .repository
                .getAllByDownloadedObjectId(objectId)
            Timber.d("Got ${searchResults.size} indexed downloads for $this")

            var isDownloaded = false
            var childrenCount = -1L
            var downloadedChildrenCount = 0
            if (searchResults.isNotEmpty()) {
                for (data in searchResults) {
                    val dataClass = data.data()
                    val isChildren = dataClass.metadata.parentId == objectId
                    if (isChildren)
                    // If it's a children, increase the counter
                        downloadedChildrenCount++
                    else {
                        // If it's not a children, it means that the dataclass is downloaded
                        isDownloaded = true
                        childrenCount =
                            dataClass.getChildren(context) { it.displayName }.size.toLong()
                    }
                }
            }
            Timber.d("$this is downloaded: $isDownloaded. Has $downloadedChildrenCount/$childrenCount downloaded children.")

            // If the Dataclass is downloaded:
            // - There are not any downloaded children: PARTIALLY
            // - If all the children are downloaded: DOWNLOADED
            // - If there's a non-downloaded children: PARTIALLY
            // If the DataClass is not downloaded:
            // NOT_DOWNLOADED

            return if (!isDownloaded || childrenCount < 0)
                DownloadStatus.NOT_DOWNLOADED
            else if (downloadedChildrenCount >= childrenCount)
                DownloadStatus.DOWNLOADED
            else DownloadStatus.PARTIALLY
        }

        /**
         * Downloads the image data of the DataClass.
         * @author Arnau Mora
         * @since 20210313
         * @param context The context to run from.
         * @param overwrite If the new data should overwrite the old one
         * @param quality The quality in which do the codification
         * @return A LiveData object with the download work info
         *
         * @throws IllegalArgumentException If the specified quality is out of bounds
         */
        @WorkerThread
        @Throws(IllegalArgumentException::class)
        suspend inline fun <reified W : DownloadWorkerFactory> scheduleDownload(
            context: Context,
            pin: String,
            displayName: String,
            companion: W,
            overwrite: Boolean = true,
            quality: Int = 100,
        ): LiveData<WorkInfo> {
            if (quality < DOWNLOAD_QUALITY_MIN || quality > DOWNLOAD_QUALITY_MAX)
                throw IllegalArgumentException(
                    "Quality must be between $DOWNLOAD_QUALITY_MIN and $DOWNLOAD_QUALITY_MAX"
                )
            Timber.v("Downloading $pin...")
            val (namespace, objectId) = decodePin(pin)
            Timber.v("Preparing DownloadData...")
            val downloadData = DownloadData(displayName, namespace, objectId, overwrite, quality)
            Timber.v("Scheduling download...")
            val workerClass = W::class.java
            val schedule = workerClass.getMethod(
                "schedule",
                Context::class.java,
                String::class.java,
                DownloadData::class.java,
                Continuation::class.java,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Timber.i("Download Worker schedule args: ${schedule.parameters.map { "${it.name}: ${it.type.simpleName}" }}")

            return suspendCoroutine { continuation ->
                schedule(companion, context, pin, downloadData, continuation)
            }
        }

        /**
         * Builds a DataClass from its namespace, object id and JSON data. Just Area, Zone and
         * Sector.
         * @author Arnau Mora
         * @since 20220304
         * @param namespace The namespace of the DataClass.
         * @param objectId The id of the DataClass.
         * @param data The JSON data from the server.
         * @throws InitializationException When the [namespace] is unknown or not a container.
         */
        @Throws(InitializationException::class)
        fun buildContainers(
            namespace: Namespace,
            @ObjectId objectId: String,
            data: JSONObject,
            childrenCount: Long,
        ) = when (namespace) {
            Area.NAMESPACE -> Area(data, objectId, childrenCount)
            Zone.NAMESPACE -> Zone(data, objectId, childrenCount)
            Sector.NAMESPACE -> Sector(data, objectId, childrenCount)
            else -> throw InitializationException(
                "Could not initialize DataClass with namespace \"$namespace\"",
            )
        }
    }

    /**
     * Sets the quality which the images should be loaded in.
     * @author Arnau Mora
     * @since 20210724
     */
    @IgnoredOnParcel
    protected abstract val imageQuality: Int

    /**
     * Tells whether or not the DataClass has parent elements, or if it's root.
     * Used in functions such as [getParent] to be quicker.
     * @author Arnau Mora
     * @since 20220106
     */
    @IgnoredOnParcel
    abstract val hasParents: Boolean

    /**
     * The download url from Firebase Storage for the [DataClass]' image.
     * @author Arnau Mora
     * @since 20210721
     */
    var downloadUrl: Uri? = null

    /**
     * Provides a generic name for the [DataClass].
     * @author Arnau Mora
     * @since 20210724
     */
    val pin: String
        get() = generatePin(namespace, objectId)

    /**
     * Gets the parent element of the [DataClass].
     * @author Arnau Mora
     * @since 20220106
     * @param D The parent type of the DataClass.
     * @param context The context used for initializing the index if not ready.
     */
    suspend fun <D : DataClass<*, *, *>> getParent(context: Context): D? {
        // If the DataClass is root, return null
        if (!hasParents)
            return null

        // Get the parentId, if it's null, return null
        val parentId: String = metadata.parentId ?: return null

        val dataRepository = DataSingleton.getInstance(context)
            .repository

        // From the different possibilities for the namespace, fetch from searchSession
        @Suppress("UNCHECKED_CAST")
        return when (namespace.ParentNamespace) {
            Area.NAMESPACE -> dataRepository.getArea(parentId) as D?
            Zone.NAMESPACE -> dataRepository.getZone(parentId) as D?
            Sector.NAMESPACE -> dataRepository.getSector(parentId) as D?
            else -> null
        }
    }

    /**
     * Returns the children of the [DataClass].
     * @author Arnau Mora
     * @since 20210313
     * @param context Used for initializing the index loader if not ready.
     * @throws NullPointerException When [namespace] does not have children.
     */
    @Suppress("UNCHECKED_CAST")
    @WorkerThread
    @Throws(NullPointerException::class)
    suspend inline fun <R : Comparable<R>> getChildren(
        context: Context,
        crossinline sortBy: (A) -> R?
    ): List<A> = DataSingleton.getInstance(context)
        .repository
        .getChildren(namespace, objectId)
        ?.map { it.data() as A }
        ?.sortedBy(sortBy)
        ?: emptyList()

    /**
     * Gets the children element at [index].
     * May throw [IndexOutOfBoundsException] if children have not been loaded.
     * @author Arnau Mora
     * @since 20210413
     * @throws IndexOutOfBoundsException When the specified [index] does not exist
     */
    @Throws(IndexOutOfBoundsException::class)
    @WorkerThread
    suspend fun get(context: Context, index: Int): A = getChildren(context) { it.objectId }[index]

    /**
     * Finds an [DataClass] inside a list with an specific id. If it's not found, null is returned.
     * @author Arnau Mora
     * @since 20210413
     * @param objectId The id to search
     */
    @WorkerThread
    suspend fun get(context: Context, objectId: String): A? {
        for (o in getChildren(context) { it.objectId })
            if (o.objectId == objectId)
                return o
        return null
    }

    @WorkerThread
    suspend fun has(context: Context, objectId: String): Boolean {
        for (o in getChildren(context) { it.displayName })
            if (o.objectId == objectId)
                return true
        return false
    }

    /**
     * Checks if the data class has children.
     * Note: won't load children, will just use the already loaded ones.
     * @author Arnau Mora
     * @since 20210411
     */
    @WorkerThread
    suspend fun isEmpty(context: Context): Boolean = getSize(context) <= 0

    /**
     * Checks if the data class doesn't have any children
     * @author Arnau Mora
     * @since 20210411
     */
    @WorkerThread
    suspend fun isNotEmpty(context: Context): Boolean = getSize(context) > 0

    /**
     * Returns the amount of children the [DataClass] has.
     * @author Arnau Mora
     * @since 20210724
     */
    @WorkerThread
    suspend fun getSize(context: Context): Int =
        getChildren(context) { it.objectId }.size

    /**
     * Checks if the [DataClass] is the same as another one.
     * @author Arnau Mora
     * @since 20210724
     */
    override fun equals(other: Any?): Boolean {
        if (other !is DataClass<*, *, *>)
            return super.equals(other)
        return pin == other.pin
    }

    /**
     * Converts the [DataClass] to a [String] for showing in debug or to the user. The string contains
     * the first letter of the [namespace], followed by a slash (/) and the [objectId].
     * @author Arnau Mora
     * @since 20210724
     */
    override fun toString(): String = namespace.namespace[0] + "/" + objectId

    /**
     * Gets the KMZ file of the [DataClass] and stores it into [targetFile].
     * @author Arnau Mora
     * @since 20210416
     * @param targetFile The [File] to store the KMZ at.
     */
    @WorkerThread
    private suspend fun storeKmz(
        context: Context,
        targetFile: File,
    ) = kmzPath?.run {
        suspendCoroutine<Void?> { cont ->
            val request = InputStreamVolleyRequest(
                Request.Method.GET,
                "$REST_API_DOWNLOAD_ENDPOINT$kmzPath",
                { bytes ->
                    targetFile
                        .outputStream()
                        .use { stream ->
                            stream.write(bytes)
                        }
                    cont.resume(null)
                },
                { error ->
                    cont.resumeWithException(error)
                }, mapOf(), mapOf()
            )
            VolleySingleton.getInstance(context).addToRequestQueue(request)
        }
    }

    /**
     * Gets the KMZ file path.
     * If it has never been loaded, it gets loaded from the server. Otherwise, it gets loaded from
     * cache.
     * @author Arnau Mora
     * @since 20210416
     * @param context The context to run from.
     * @param permanent If true, the KMZ will get stored in the data directory, if false, it will
     * be cached.
     * @throws IllegalStateException When [kmzPath] is null, so a [File] can't be retrieved.
     * @throws VolleyError When there's an exception while downloading the KMZ file.
     */
    @Throws(IllegalStateException::class, VolleyError::class)
    @WorkerThread
    suspend fun kmzFile(
        context: Context,
        permanent: Boolean,
    ): File {
        val kmzFile = kmzFile(context, permanent, namespace, objectId)

        if (!kmzFile.exists()) {
            Timber.v("Storing KMZ file...")
            try {
                storeKmz(context, kmzFile)
                Timber.v("KMZ stored successfully.")
            } catch (e: Exception) {
                Timber.e(e, "Could not store KMZ File ($kmzPath).")
            }
        }

        return kmzFile
    }

    inner class WorkInfoObserver(
        private val context: Context,
        private val liveData: LiveData<WorkInfo>,
        private val namespace: Namespace,
        private val objectId: String,
    ) : Observer<WorkInfo> {
        override fun onChanged(workInfo: WorkInfo?) {
            doAsync {
                if (workInfo == null || workInfo.state.isFinished) {
                    DownloadSingleton.getInstance()
                        .finishedDownloading(namespace, objectId)
                    uiContext { liveData.removeObserver(this@WorkInfoObserver) }
                } else
                    DownloadSingleton.getInstance()
                        .putState(context, namespace, objectId, workInfo)
            }
        }
    }

    /**
     * Downloads the image data of the DataClass.
     * @author Arnau Mora
     * @since 20210313
     * @param context The context to run from.
     * @param overwrite If the new data should overwrite the old one
     * @param quality The quality in which do the codification
     * @return A LiveData object with the download work info
     *
     * @throws IllegalArgumentException If the specified quality is out of bounds
     */
    @WorkerThread
    @Throws(IllegalArgumentException::class)
    suspend inline fun <reified W : DownloadWorkerFactory> download(
        context: Context,
        companion: W,
        overwrite: Boolean = true,
        quality: Int = 100
    ): LiveData<WorkInfo> =
        scheduleDownload(context, pin, displayName, companion, overwrite, quality)
            .also { it.observeForever(WorkInfoObserver(context, it, namespace, objectId)) }

    /**
     * Gets the DownloadStatus of the DataClass
     * @author Arnau Mora
     * @since 20210313
     * @return a matching DownloadStatus representing the Data Class' download status
     */
    fun downloadStatus(): DownloadStatus = DownloadSingleton.getInstance()
        .states
        .value
        ?.get(namespace to objectId)
        ?: DownloadStatus.UNKNOWN

    /**
     * Checks if the data class has any children that has been downloaded
     * @author Arnau Mora
     * @date 2020/09/14
     * @param context The currently calling [Context].
     * @return If the data class has any downloaded children
     */
    @WorkerThread
    suspend fun hasAnyDownloadedChildren(context: Context): Boolean {
        val children = getChildren(context) { it.objectId }
        for (child in children)
            if (child is DataClass<*, *, *> && child.downloadStatus() == DownloadStatus.DOWNLOADED)
                return true
        return false
    }

    /**
     * Converts the DataClass into a Search Data class.
     * @author Arnau Mora
     * @since 20220219
     */
    abstract fun data(): D

    /**
     * Returns a map used to display the stored data to the user. Keys should be the parameter name,
     * and the value the value to display. Should be overridden by target class.
     * @author Arnau Mora
     * @since 20220315
     */
    abstract override fun displayMap(): Map<String, Serializable?>

    /**
     * Deletes the downloaded content if downloaded
     * @author Arnau Mora
     * @date 20210724
     * @param context The currently calling [Context].
     * @return If the content was deleted successfully. Note: returns true if not downloaded
     */
    @WorkerThread
    suspend fun delete(context: Context): Boolean =
        Companion.delete<A>(context, namespace, objectId)

    /**
     * Gets the space that is occupied by the data class' downloaded data in the system
     * @author Arnau Mora
     * @date 2020/09/11
     * @patch 2020/09/12 - Arnau Mora: Added child space computation
     * @param context The currently calling context.
     * @return The size in bytes that is used by the downloaded data
     * @throws NotDownloadedException If tried to get size when not downloaded
     */
    @Throws(NotDownloadedException::class)
    suspend fun size(context: Context): Long {
        val imgFile = imageFile(context)

        if (!imgFile.exists()) throw NotDownloadedException(this)

        var size = imgFile.length()

        val children = getChildren(context) { it.objectId }
        for (child in children)
            if (child is DataClass<*, *, *>)
                size += child.size(context)

        Timber.v("$this > Storage usage: $size")

        return size
    }

    /**
     * Gets when the data was downloaded
     * @author Arnau Mora
     * @date 2020/09/11
     * @param context The context to run from
     * @return The date when the data class was downloaded or null if not downloaded
     */
    fun downloadDate(context: Context): Date? = imageFile(context).let {
        if (it.exists())
            Date(it.lastModified())
        else null
    }

    /**
     * Returns the File that represents the image of the DataClass
     * @author Arnau Mora
     * @date 2020/09/10
     * @param context The context to run from
     * @return The path of the image file that can be downloaded
     */
    fun imageFile(context: Context): File =
        Companion.imageFile(context, namespace, objectId)

    /**
     * Returns the File that represents the image of the DataClass in cache.
     * @author Arnau Mora
     * @date 20210724
     * @param context The context to run from
     * @param suffix If not null, will be added to the end of the file name.
     * @return The path of the image file that can be downloaded
     */
    private fun cacheImageFile(context: Context, suffix: String? = null): File =
        Companion.cacheImageFile(context, namespace, objectId, suffix)

    /**
     * Checks if the [DataClass] has a stored [downloadUrl].
     * @author Arnau Mora
     * @since 20210722
     */
    fun hasStorageUrl() = downloadUrl != null

    /**
     * Gets the data of the image that should be loaded for representing the [DataClass].
     * @author Arnau Mora
     * @since 20220118
     * @param context The [Context] that is requesting to load the image.
     * @param imageLoadParameters The parameters for loading the image.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(ArithmeticException::class)
    fun imageData(
        context: Context,
        imageLoadParameters: ImageLoadParameters? = null
    ): Any {
        val downloadedImageFile = imageFile(context)
        val scale = imageLoadParameters?.resultImageScale ?: 1f
        val cacheImage = cacheImageFile(context, "scale$scale")

        // If image is downloaded, load it
        val image = when {
            downloadedImageFile.exists() -> downloadedImageFile
            cacheImage.exists() -> cacheImage
            else -> "$REST_API_DOWNLOAD_ENDPOINT$imagePath"
        }

        if (image is String)
            InputStreamVolleyRequest(
                Request.Method.GET,
                "$REST_API_DOWNLOAD_ENDPOINT$imagePath",
                { bytes ->
                    val bitmap: Bitmap? = BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size,
                        BitmapFactory
                            .Options()
                            .apply {
                                inSampleSize = (1 / scale).toInt()
                            }
                    )
                    if (bitmap != null) {
                        Timber.v("$this > Compressing image...")
                        val baos = ByteArrayOutputStream()
                        val compressedBitmap: Boolean =
                            bitmap.compress(WEBP_LOSSY_LEGACY, imageQuality, baos)
                        if (!compressedBitmap) {
                            Timber.e("$this > Could not compress image!")
                            throw ArithmeticException("Could not compress image for $this.")
                        } else {
                            Timber.v("$this > Storing image...")
                            cacheImage
                                .outputStream()
                                .use { baos.writeTo(it) }
                            Timber.v("$this > Image stored.")
                        }
                    }
                },
                { error ->
                    Timber.e(error, "Could not download image from server. Url: $image")
                }, mapOf(), mapOf()
            ).addToRequestQueue(context)

        return image
    }

    @Composable
    fun Image(
        modifier: Modifier = Modifier,
        imageLoadParameters: ImageLoadParameters? = null
    ) {
        val context = LocalContext.current
        val image = imageData(context, imageLoadParameters)

        GlideImage(
            imageModel = image,
            requestOptions = {
                RequestOptions
                    .placeholderOf(displayOptions.placeholderDrawable)
                    .error(displayOptions.placeholderDrawable)
                    .downsample(DownsampleStrategy.CENTER_OUTSIDE)
                    .encodeQuality(imageQuality)
                    .sizeMultiplier(imageLoadParameters?.resultImageScale ?: 1f)
            },
            shimmerParams = ShimmerParams(
                baseColor = MaterialTheme.colorScheme.surfaceVariant,
                highlightColor = MaterialTheme.colorScheme.onSurfaceVariant,
                durationMillis = 350,
                dropOff = 0.65f,
                tilt = 20f
            ),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = modifier,
        )
    }

    override fun hashCode(): Int {
        var result = objectId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + kmzPath.hashCode()
        result = 31 * result + displayOptions.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + namespace.hashCode()
        return result
    }
}

/**
 * Gets an element from the [Iterable].
 * @author Arnau Mora
 * @since 20210724
 * @param objectId The id of the object to search for.
 * @return The object you are searching for, or null if not found.
 */
operator fun <D : DataClass<*, *, *>> Iterable<D>.get(objectId: String): D? {
    for (item in this)
        if (item.objectId == objectId)
            return item
    return null
}

/**
 * Checks if an [Iterable] contains a [DataClass] with the set [objectId].
 * @author Arnau Mora
 * @since 20210724
 * @param objectId The id of the object to search for.
 * @return The object you are searching for, or null if not found.
 */
fun <D : DataClass<*, *, *>> Iterable<D>.has(objectId: String): Boolean {
    for (item in this)
        if (item.objectId == objectId)
            return true
    return false
}

/**
 * Checks if the DataClass with the specified object id is indexed in the downloads.
 * @author Arnau Mora
 * @since 20220101
 * @param context The context to initialize the search session from if not read.
 */
suspend fun @receiver:ObjectId String.isDownloadIndexed(context: Context) =
    DataClass.isDownloadIndexed(context, this)

/**
 * Fetches all the children of the DataClass with the specified object id.
 * @author Arnau Mora
 * @since 20220101
 * @param A The children object type.
 * @param context The context to initialize the search session from if not read.
 * @param namespace The namespace of [A].
 */
@Suppress("UNCHECKED_CAST")
suspend inline fun <A : DataClassImpl, R : Comparable<R>> @receiver:ObjectId
String.getChildren(
    context: Context,
    namespace: Namespace,
    crossinline sortBy: (A) -> R?
) = DataSingleton
    .getInstance(context)
    .repository
    .getChildren(namespace, this)
    ?.map { it.data() as A }
    ?.sortedBy(sortBy)
