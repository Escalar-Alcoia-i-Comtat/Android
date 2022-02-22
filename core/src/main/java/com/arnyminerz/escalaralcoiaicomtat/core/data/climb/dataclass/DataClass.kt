package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchResult
import androidx.appsearch.app.SearchSpec
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.android.volley.Request
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.exception.NotDownloadedException
import com.arnyminerz.escalaralcoiaicomtat.core.network.VolleySingleton
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_MAX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_MIN
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PARENT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DOWNLOAD_ENDPOINT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.InputStreamVolleyRequest
import com.arnyminerz.escalaralcoiaicomtat.core.utils.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.allTrue
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getArea
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getChildren
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getData
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getSector
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getZone
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadData
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadWorkerModel
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.model.LatLng
import com.skydoves.landscapist.ShimmerParams
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.parcelize.IgnoredOnParcel
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
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
    open val location: LatLng?,
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
        private fun generatePin(namespace: String, objectId: String) = "${namespace}_$objectId"

        /**
         * Returns the correct image name for the desired [objectId] and [namespace].
         * @author Arnau Mora
         * @since 20210822
         */
        fun imageName(namespace: String, objectId: String, suffix: String?) =
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
        private fun imageFile(context: Context, namespace: String, objectId: String): File =
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
            namespace: String,
            objectId: String,
            suffix: String? = null
        ): File =
            File(context.cacheDir, imageName(namespace, objectId, suffix))

        fun kmzFile(context: Context, permanent: Boolean, namespace: String, objectId: String) =
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
         * @param searchSession The [AppSearchSession] that has all the data stored.
         * @param query What to search for. May be [DataClass.displayName] or [DataClassMetadata.webURL].
         * @return An [Intent] if the [DataClass] was found, or null.
         */
        @WorkerThread
        suspend fun getIntent(
            context: Context,
            activity: Class<*>,
            searchSession: AppSearchSession,
            query: String
        ): Intent? = (searchSession.getData<Area, AreaData>(query, Area.NAMESPACE)
            ?: searchSession.getData<Zone, ZoneData>(query, Zone.NAMESPACE)
            ?: searchSession.getData<Sector, SectorData>(query, Sector.NAMESPACE))
            ?.let { dataClass ->
                Intent(context, activity)
                    .putExtra(EXTRA_DATACLASS, dataClass)
                    .apply {
                        dataClass
                            .takeIf { it.hasParents }
                            ?.getParent<DataClass<*, *, *>>(searchSession)
                            ?.let {
                                putExtra(EXTRA_PARENT, it)
                            }
                    }
            }

        /**
         * Gets the [Intent] used to launch the [Activity] of a [DataClass] with [namespace] and if
         * [objectId].
         * @author Arnau Mora
         * @since 20220128
         * @param context The [Context] that is requesting the [Intent].
         * @param searchSession The [AppSearchSession] that has all the data stored.
         * @param namespace The [DataClass.namespace] of the [objectId].
         * @param objectId The [DataClass.objectId] to search for.
         * @return An [Intent] if the [DataClass] was found, or null.
         */
        suspend inline fun <A : DataClass<*, *, *>, reified B : DataRoot<A>> getIntent(
            context: Context,
            activity: Class<*>,
            searchSession: AppSearchSession,
            @Namespace namespace: String,
            @ObjectId objectId: String
        ): Intent? = searchSession.getData<A, B>(objectId, namespace)
            ?.let { dataClass ->
                Intent(context, activity)
                    .putExtra(EXTRA_DATACLASS, dataClass)
                    .apply {
                        dataClass
                            .takeIf { it.hasParents }
                            ?.getParent<DataClass<*, *, *>>(searchSession)
                            ?.let {
                                putExtra(EXTRA_PARENT, it)
                            }
                    }
            }

        /**
         * Deletes a DataClass from the device.
         * @author Arnau Mora
         * @since 20211231
         * @param A The type of the children of the DataClass
         * @param context The [Context] that is requesting the deletion.
         * @param searchSession The [AppSearchSession] for fetching children and un-indexing the
         * download.
         * @param namespace The namespace of the DataClass to delete.
         * @param objectId The id of the DataClass to delete.
         * @return True if the DataClass was deleted successfully.
         */
        suspend fun <A : DataClassImpl> delete(
            context: Context,
            searchSession: AppSearchSession,
            namespace: String,
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

            // This may not be the best method, but for now it works
            when (namespace) {
                Area.NAMESPACE -> Zone.NAMESPACE
                Zone.NAMESPACE -> Sector.NAMESPACE
                Sector.NAMESPACE -> Path.NAMESPACE
                else -> null
            }?.let { childrenNamespace ->
                Timber.d("Deleting children...")
                val children = searchSession.getChildren<A, String>(childrenNamespace, objectId)
                { it.objectId }
                for (child in children)
                    if (child is DataClass<*, *, *>)
                        lst.add(child.delete(context, searchSession))
            }

            // Remove dataclass from index.
            // Since Sectors that are children of a Zone also contain its ID this will also remove them
            // from the index.
            searchSession.remove(
                objectId,
                SearchSpec.Builder()
                    .addFilterNamespaces(namespace)
                    .addFilterDocumentClasses(DownloadedData::class.java)
                    .build()
            ).await()

            return lst.allTrue()
        }

        /**
         * Checks if the DataClass with id [objectId] is indexed as a download.
         * @author Arnau Mora
         * @since 20220101
         * @param searchSession The search session to fetch the data from.
         * @param objectId The id of the DataClass to search for.
         */
        suspend fun isDownloadIndexed(
            searchSession: AppSearchSession,
            objectId: String
        ): DownloadStatus {
            Timber.d("Finding for element in indexed downloads...")
            val searchResults = searchSession.search(
                objectId,
                SearchSpec.Builder()
                    .addFilterDocumentClasses(DownloadedData::class.java)
                    .build()
            )
            val searchResultsList = arrayListOf<SearchResult>()
            var page = searchResults.nextPage.await()
            while (page.isNotEmpty()) {
                searchResultsList.addAll(page)
                page = searchResults.nextPage.await()
            }
            Timber.d("Got ${searchResultsList.size} indexed downloads for $this")

            var isDownloaded = false
            var childrenCount = -1L
            var downloadedChildrenCount = 0
            if (searchResultsList.isNotEmpty()) {
                for (result in searchResultsList) {
                    val document = result.genericDocument
                    val downloadedData = document.toDocumentClass(DownloadedData::class.java)
                    val isChildren = downloadedData.parentId == objectId
                    if (isChildren)
                    // If it's a children, increase the counter
                        downloadedChildrenCount++
                    else {
                        // If it's not a children, it means that the dataclass is downloaded
                        isDownloaded = true
                        childrenCount = downloadedData.childrenCount
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
        @Throws(IllegalArgumentException::class)
        inline fun <reified W : DownloadWorkerModel> scheduleDownload(
            context: Context,
            pin: String,
            displayName: String,
            overwrite: Boolean = true,
            quality: Int = 100
        ): LiveData<WorkInfo> {
            if (quality < DOWNLOAD_QUALITY_MIN || quality > DOWNLOAD_QUALITY_MAX)
                throw IllegalArgumentException(
                    "Quality must be between $DOWNLOAD_QUALITY_MIN and $DOWNLOAD_QUALITY_MAX"
                )
            Timber.v("Downloading $pin...")
            Timber.v("Preparing DownloadData...")
            val downloadData = DownloadData(displayName, overwrite, quality)
            Timber.v("Scheduling download...")
            val workerClass = W::class.java
            val schedule = workerClass.getMethod(
                "schedule",
                Context::class.java,
                String::class.java,
                DownloadData::class.java
            )
            @Suppress("UNCHECKED_CAST")
            return schedule.invoke(null, context, pin, downloadData) as LiveData<WorkInfo>
        }

        /**
         * Gets the [WorkInfo] if the DataClass is being downloaded, or null otherwise.
         * @author Arnau Mora
         * @since 20210417
         * @param context The context to check from
         * @param pin The [DataClass.pin] to check the state for.
         */
        @WorkerThread
        fun downloadWorkInfo(context: Context, pin: String): WorkInfo? {
            val workManager = WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosByTag(pin).get()
            var result: WorkInfo? = null
            if (workInfos.isNotEmpty())
                for (workInfo in workInfos)
                    if (!workInfo.state.isFinished) {
                        result = workInfos[0]
                        break
                    }
            return result
        }

        /**
         * Gets a [LiveData] of the [WorkInfo] for all works for the current [DataClass].
         * @author Arnau Mora
         * @since 20210417
         * @param context The context to check from.
         * @param pin The [DataClass.pin] to get the download work info for.
         */
        @WorkerThread
        fun downloadWorkInfoLiveData(context: Context, pin: String): LiveData<List<WorkInfo>> {
            val workManager = WorkManager.getInstance(context)
            return workManager.getWorkInfosByTagLiveData(pin)
        }

        /**
         * Gets the DownloadStatus of the DataClass
         * @author Arnau Mora
         * @since 20210313
         * @param context The currently calling [Context].
         * @return a matching DownloadStatus representing the Data Class' download status
         */
        @WorkerThread
        suspend fun downloadStatus(
            context: Context,
            searchSession: AppSearchSession,
            pin: String,
        ): Pair<DownloadStatus, WorkInfo?> {
            Timber.d("$pin Checking if downloaded")

            val objectId = pin.substring(pin.indexOf('_') + 1)
            val downloadWorkInfo = downloadWorkInfo(context, pin)
            val result = if (downloadWorkInfo != null)
                DownloadStatus.DOWNLOADING
            else isDownloadIndexed(searchSession, objectId)

            Timber.d("$pin Finished checking download status. Result: $result")
            return result to downloadWorkInfo
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
     * @param searchSession The [AppSearchSession] to get the data from.
     */
    suspend fun <D : DataClass<*, *, *>> getParent(searchSession: AppSearchSession): D? {
        // If the DataClass is root, return null
        if (!hasParents)
            return null

        // Get the parentId, if it's null, return null
        val parentId: String = metadata.parentId ?: return null
        // Get the parent's namespace, it null, return null
        val parentNamespace: String = metadata.parentNamespace ?: return null

        // From the different possibilities for the namespace, fetch from searchSession
        @Suppress("UNCHECKED_CAST")
        return when (parentNamespace) {
            Area.NAMESPACE -> searchSession.getArea(parentId) as D?
            Zone.NAMESPACE -> searchSession.getZone(parentId) as D?
            Sector.NAMESPACE -> searchSession.getSector(parentId) as D?
            else -> null
        }
    }

    /**
     * Returns the children of the [DataClass].
     * @author Arnau Mora
     * @since 20210313
     * @param searchSession The session for performing searches.
     * @throws IllegalStateException When no [DataClassMetadata.childNamespace] is set in
     * [DataClass.metadata].
     */
    @WorkerThread
    @Throws(IllegalStateException::class)
    suspend inline fun <R : Comparable<R>> getChildren(
        searchSession: AppSearchSession,
        crossinline sortBy: (A) -> R?
    ): List<A> =
        metadata.childNamespace?.let { childNamespace ->
            searchSession.getChildren(childNamespace, objectId, sortBy)
        } ?: throw IllegalStateException("If no child namespace is set in metadata.")

    /**
     * Gets the children element at [index].
     * May throw [IndexOutOfBoundsException] if children have not been loaded.
     * @author Arnau Mora
     * @since 20210413
     * @throws IndexOutOfBoundsException When the specified [index] does not exist
     */
    @Throws(IndexOutOfBoundsException::class)
    @WorkerThread
    suspend fun get(searchSession: AppSearchSession, index: Int): A =
        getChildren(searchSession) { it.objectId }[index]

    /**
     * Finds an [DataClass] inside a list with an specific id. If it's not found, null is returned.
     * @author Arnau Mora
     * @since 20210413
     * @param objectId The id to search
     */
    @WorkerThread
    suspend fun get(searchSession: AppSearchSession, objectId: String): A? {
        for (o in getChildren(searchSession) { it.objectId })
            if (o.objectId == objectId)
                return o
        return null
    }

    @WorkerThread
    suspend fun has(searchSession: AppSearchSession, objectId: String): Boolean {
        for (o in getChildren(searchSession) { it.displayName })
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
    suspend fun isEmpty(searchSession: AppSearchSession): Boolean = getSize(searchSession) <= 0

    /**
     * Checks if the data class doesn't have any children
     * @author Arnau Mora
     * @since 20210411
     */
    @WorkerThread
    suspend fun isNotEmpty(searchSession: AppSearchSession): Boolean = getSize(searchSession) > 0

    /**
     * Returns the amount of children the [DataClass] has.
     * @author Arnau Mora
     * @since 20210724
     */
    @WorkerThread
    suspend fun getSize(searchSession: AppSearchSession): Int =
        getChildren(searchSession) { it.objectId }.size

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
    override fun toString(): String = namespace[0] + "/" + objectId

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
     */
    @Throws(IllegalStateException::class)
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

    /**
     * Gets the [WorkInfo] if the DataClass is being downloaded, or null otherwise.
     * @author Arnau Mora
     * @since 20210417
     * @param context The context to check from
     */
    @WorkerThread
    fun downloadWorkInfo(context: Context): WorkInfo? = downloadWorkInfo(context, pin)

    /**
     * Gets a [LiveData] of the [WorkInfo] for all works for the current [DataClass].
     * @author Arnau Mora
     * @since 20210417
     * @param context The context to check from
     */
    @WorkerThread
    fun downloadWorkInfoLiveData(context: Context): LiveData<List<WorkInfo>> =
        downloadWorkInfoLiveData(context, pin)

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
    @Throws(IllegalArgumentException::class)
    inline fun <reified W : DownloadWorkerModel> download(
        context: Context,
        overwrite: Boolean = true,
        quality: Int = 100
    ): LiveData<WorkInfo> =
        scheduleDownload<W>(context, pin, displayName, overwrite, quality)

    /**
     * Gets the DownloadStatus of the DataClass
     * @author Arnau Mora
     * @since 20210313
     * @param context The currently calling [Context].
     * @return a matching DownloadStatus representing the Data Class' download status
     */
    @WorkerThread
    suspend fun downloadStatus(
        context: Context,
        searchSession: AppSearchSession
    ): Pair<DownloadStatus, WorkInfo?> = Companion.downloadStatus(context, searchSession, pin)

    /**
     * Checks if the data class has any children that has been downloaded
     * @author Arnau Mora
     * @date 2020/09/14
     * @param context The currently calling [Context].
     * @param searchSession The search session for fetching data.
     * @return If the data class has any downloaded children
     */
    @WorkerThread
    suspend fun hasAnyDownloadedChildren(
        context: Context,
        searchSession: AppSearchSession
    ): Boolean {
        val children = getChildren(searchSession) { it.objectId }
        for (child in children)
            if (child is DataClass<*, *, *> &&
                child.downloadStatus(context, searchSession).first == DownloadStatus.DOWNLOADED
            ) return true
        return false
    }

    /**
     * Converts the DataClass into a Search Data class.
     * @author Arnau Mora
     * @since 20220219
     * @param index The position of the DataClass
     */
    abstract fun data(index: Int): D

    /**
     * Deletes the downloaded content if downloaded
     * @author Arnau Mora
     * @date 20210724
     * @param context The currently calling [Context].
     * @param searchSession The search session for fetching data.
     * @return If the content was deleted successfully. Note: returns true if not downloaded
     */
    @WorkerThread
    suspend fun delete(context: Context, searchSession: AppSearchSession): Boolean =
        Companion.delete<A>(context, searchSession, namespace, objectId)

    /**
     * Gets the space that is occupied by the data class' downloaded data in the system
     * @author Arnau Mora
     * @date 2020/09/11
     * @patch 2020/09/12 - Arnau Mora: Added child space computation
     * @param context The currently calling context.
     * @param searchSession The search session for fetching data.
     * @return The size in bytes that is used by the downloaded data
     *
     * @throws NotDownloadedException If tried to get size when not downloaded
     */
    @Throws(NotDownloadedException::class)
    suspend fun size(context: Context, searchSession: AppSearchSession): Long {
        val imgFile = imageFile(context)

        if (!imgFile.exists()) throw NotDownloadedException(this)

        var size = imgFile.length()

        val children = getChildren(searchSession) { it.objectId }
        for (child in children)
            if (child is DataClass<*, *, *>)
                size += child.size(context, searchSession)

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
    private fun imageFile(context: Context): File =
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
    @Throws(VolleyError::class, ArithmeticException::class)
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

        val request = InputStreamVolleyRequest(
            Request.Method.GET,
            "$REST_API_DOWNLOAD_ENDPOINT$kmzPath",
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
            { error -> throw error }, mapOf(), mapOf()
        )
        VolleySingleton.getInstance(context).addToRequestQueue(request)

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
 * @param searchSession The search session where to search for the DataClass.
 */
suspend fun @receiver:ObjectId String.isDownloadIndexed(searchSession: AppSearchSession) =
    DataClass.isDownloadIndexed(searchSession, this)

/**
 * Fetches all the children of the DataClass with the specified object id.
 * @author Arnau Mora
 * @since 20220101
 * @param A The children object type.
 * @param searchSession The search session where to search for the data.
 * @param namespace The namespace of [A].
 */
suspend inline fun <A : DataClassImpl, R : Comparable<R>> @receiver:ObjectId
String.getChildren(
    searchSession: AppSearchSession,
    namespace: String,
    crossinline sortBy: (A) -> R?
) =
    searchSession.getChildren(namespace, this, sortBy)
