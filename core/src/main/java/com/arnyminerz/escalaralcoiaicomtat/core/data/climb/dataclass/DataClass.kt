package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchResult
import androidx.appsearch.app.SearchSpec
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.exception.CouldNotCreateDynamicLinkException
import com.arnyminerz.escalaralcoiaicomtat.core.exception.NotDownloadedException
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ACTIVITY_AREA_META
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ACTIVITY_SECTOR_META
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ACTIVITY_ZONE_META
import com.arnyminerz.escalaralcoiaicomtat.core.shared.APPLICATION_ID
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_MAX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_MIN
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DYNAMIC_LINKS_DOMAIN
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.core.utils.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.allTrue
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getChildren
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getData
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadData
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadWorkerModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.dynamiclinks.ktx.socialMetaTagParameters
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
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
 * @param imageReferenceUrl The [FirebaseStorage] reference url of the image of the [DataClass].
 * @param kmzReferenceUrl The [FirebaseStorage] reference url of the KMZ file of the [DataClass].
 * May be null if not applicable or non-existing.
 * @param location The coordinates of the [DataClass] to show in a map.
 * @param metadata Some metadata of the [DataClass].
 * @param displayOptions Options for displaying in the UI.
 */
abstract class DataClass<A : DataClassImpl, B : DataClassImpl>(
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
     * The [FirebaseStorage] reference url of the image of the [DataClass].
     * @author Arnau Mora
     * @since 20210830
     */
    open val imageReferenceUrl: String,
    /**
     * The [FirebaseStorage] reference url of the KMZ file of the [DataClass].
     * May be null if not applicable or non-existing.
     * @author Arnau Mora
     * @since 20210830
     */
    open val kmzReferenceUrl: String?,
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
    metadata.documentPath
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
            searchSession: AppSearchSession,
            query: String
        ): Intent? {
            Timber.v("Getting Activities...")
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val appBundle = appInfo.metaData
            val areaActivityPackage = appBundle.getString(ACTIVITY_AREA_META)
            val zoneActivityPackage = appBundle.getString(ACTIVITY_ZONE_META)
            val sectorActivityPackage = appBundle.getString(ACTIVITY_SECTOR_META)
            if (areaActivityPackage == null)
                throw IllegalArgumentException("$ACTIVITY_AREA_META was not specified in manifest")
            if (zoneActivityPackage == null)
                throw IllegalArgumentException("$ACTIVITY_ZONE_META was not specified in manifest")
            if (sectorActivityPackage == null)
                throw IllegalArgumentException("$ACTIVITY_SECTOR_META was not specified in manifest")
            val areaActivityClass = Class.forName(areaActivityPackage)
            val zoneActivityClass = Class.forName(zoneActivityPackage)
            val sectorActivityClass = Class.forName(sectorActivityPackage)

            return searchSession.getData<Area, AreaData>(query, Area.NAMESPACE)?.let {
                Intent(context, areaActivityClass).apply {
                    putExtra(EXTRA_AREA, it.objectId)
                }
            } ?: run {
                searchSession.getData<Zone, ZoneData>(query, Zone.NAMESPACE)?.let {
                    Intent(context, zoneActivityClass).apply {
                        putExtra(EXTRA_ZONE, it.objectId)
                    }
                } ?: run {
                    searchSession.getData<Sector, SectorData>(query, Sector.NAMESPACE)?.let {
                        Intent(context, sectorActivityClass).apply {
                            val sectorPath = it.documentPath
                            val splittedPath = sectorPath.split("/")
                            putExtra(EXTRA_ZONE, splittedPath[3])
                            putExtra(EXTRA_SECTOR, it.objectId)
                        }
                    }
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
                val children = searchSession.getChildren<A>(childrenNamespace, objectId)
                for (child in children)
                    if (child is DataClass<*, *>)
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
    }

    /**
     * Sets the quality which the images should be loaded in.
     * @author Arnau Mora
     * @since 20210724
     */
    protected abstract val imageQuality: Int

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
     * Returns the parent element of the [DataClass].
     * @author Arnau Mora
     * @since 20210817
     * @param application The [App] class for fetching areas.
     * @return The correct [DataClass] that is the parent of the current one. Or null if [metadata]'s
     * [DataClassMetadata.parentId] is null.
     */
    suspend fun getParent(application: App): DataClass<*, *>? {
        // Assert non null metadata's parentId and parentNamespace.
        val parentId: String = metadata.parentId ?: return null
        val parentNamespace: String = metadata.parentNamespace ?: return null
        // Get the search session from the App.
        val searchSession = application.searchSession
        // Set up the search spec with just one result.
        val searchSpec = SearchSpec.Builder()
            .addFilterNamespaces(parentNamespace)
            .setResultCountPerPage(1)
            .build()
        // Perform the search
        val searchResults = searchSession.search(parentId, searchSpec)
        // Get the page
        val nextPage = searchResults.nextPage.await()
        // If no results found, return null
        if (nextPage.isEmpty()) return null
        // Get the only result that is available
        val searchResult = nextPage[0]
        val genericDocument = searchResult.genericDocument
        return when (genericDocument.schemaType) {
            "AreaData" -> null // Areas do not have parent
            "ZoneData" -> genericDocument.toDocumentClass(ZoneData::class.java).data()
            "SectorData" -> genericDocument.toDocumentClass(SectorData::class.java).data()
            else -> null // Just in case
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
    suspend fun getChildren(searchSession: AppSearchSession): List<A> =
        metadata.childNamespace?.let { childNamespace ->
            searchSession.getChildren(childNamespace, objectId)
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
        getChildren(searchSession)[index]

    /**
     * Finds an [DataClass] inside a list with an specific id. If it's not found, null is returned.
     * @author Arnau Mora
     * @since 20210413
     * @param objectId The id to search
     */
    @WorkerThread
    suspend fun get(searchSession: AppSearchSession, objectId: String): A? {
        for (o in getChildren(searchSession))
            if (o.objectId == objectId)
                return o
        return null
    }

    @WorkerThread
    suspend fun has(searchSession: AppSearchSession, objectId: String): Boolean {
        for (o in getChildren(searchSession))
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
    suspend fun getSize(searchSession: AppSearchSession): Int = getChildren(searchSession).size

    /**
     * Checks if the [DataClass] is the same as another one.
     * @author Arnau Mora
     * @since 20210724
     */
    override fun equals(other: Any?): Boolean {
        if (other !is DataClass<*, *>)
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
     * Gets the KMZ file path.
     * @author Arnau Mora
     * @since 20210416
     * @param context The context to run from.
     */
    private fun kmzFile(context: Context, permanent: Boolean): File =
        kmzFile(context, permanent, namespace, objectId)

    /**
     * Gets the KMZ file of the [Area] and stores it into [targetFile].
     * @author Arnau Mora
     * @since 20210416
     * @param storage The [FirebaseStorage] instance.
     * @param targetFile The [File] to store the KMZ at.
     * @param progressListener This will get called for updating the progress.
     * @throws StorageException When there has been an exception with the [storage] download.
     * @see kmzReferenceUrl
     */
    @Throws(StorageException::class)
    @WorkerThread
    private suspend fun storeKmz(
        storage: FirebaseStorage,
        targetFile: File,
        progressListener: (@UiThread (progress: ValueMax<Long>) -> Unit)? = null
    ): FileDownloadTask.TaskSnapshot? = if (kmzReferenceUrl != null)
        suspendCoroutine { cont ->
            storage.getReferenceFromUrl(kmzReferenceUrl!!)
                .getFile(targetFile)
                .addOnSuccessListener { cont.resume(it) }
                .addOnProgressListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        it.apply {
                            Timber.v("Loading progress: ${bytesTransferred}/${totalByteCount}")
                            progressListener?.invoke(ValueMax(bytesTransferred, totalByteCount))
                        }
                    }
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    else null

    /**
     * Gets the KMZ file path.
     * If it has never been loaded, it gets loaded from [storage]. Otherwise, it gets loaded from
     * cache.
     * @author Arnau Mora
     * @since 20210416
     * @param context The context to run from.
     * @param storage The [FirebaseStorage] instance.
     * @param permanent If true, the KMZ will get stored in the data directory, if false, it will
     * be cached.
     * @param progressListener This will get called for updating the progress.
     * @throws IllegalStateException When [kmzReferenceUrl] is null, so a [File] can't be retrieved.
     * @throws StorageException When there's an error while downloading the KMZ file from the server.
     */
    @Throws(IllegalStateException::class, StorageException::class)
    @WorkerThread
    suspend fun kmzFile(
        context: Context,
        storage: FirebaseStorage,
        permanent: Boolean,
        progressListener: (@UiThread (progress: ValueMax<Long>) -> Unit)? = null
    ): File {
        val kmzFile = kmzFile(context, permanent)

        if (!kmzFile.exists()) {
            Timber.v("Storing KMZ file...")
            val kmzTaskSnapshot = storeKmz(storage, kmzFile) { progress ->
                progressListener?.invoke(progress)
            }
            if (kmzTaskSnapshot == null)
                Timber.e("Could not store KMZ File ($kmzReferenceUrl).")
            else
                Timber.e(kmzTaskSnapshot.error, "Could not store KMZ File ($kmzReferenceUrl).")
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
    fun downloadWorkInfo(context: Context): WorkInfo? {
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
     * @param context The context to check from
     */
    @WorkerThread
    fun downloadWorkInfoLiveData(context: Context): LiveData<List<WorkInfo>> {
        val workManager = WorkManager.getInstance(context)
        return workManager.getWorkInfosByTagLiveData(pin)
    }

    /**
     * Generates a list of [DownloadedSection].
     * @author Arnau Mora
     * @since 20210412
     * @param context The currently calling [Context].
     * @param searchSession The search session for fetching data.
     * @param showNonDownloaded If the non-downloaded sections should be added.
     * @param progressListener A listener for the progress of the load.
     */
    @WorkerThread
    @Deprecated("Should use Jetpack Compose", level = DeprecationLevel.WARNING)
    suspend fun downloadedSectionList(
        context: Context,
        searchSession: AppSearchSession,
        showNonDownloaded: Boolean,
        progressListener: (suspend (current: Int, max: Int) -> Unit)? = null
    ): Flow<DownloadedSection> = flow {
        Timber.v("Getting downloaded sections...")
        val downloadedSectionsList = arrayListOf<DownloadedSection>()
        val children = getChildren(searchSession)
        for ((c, child) in children.withIndex())
            (child as? DataClass<*, *>)?.let { dataClass -> // Paths shouldn't be included
                val downloadStatus = dataClass.downloadStatus(context, searchSession)
                progressListener?.invoke(c, children.size)
                if (showNonDownloaded || downloadStatus.downloaded || downloadStatus.partialDownload)
                    emit(DownloadedSection(dataClass))
            }
        Timber.v("Got ${downloadedSectionsList.size} sections.")
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
    inline fun <reified W : DownloadWorkerModel> download(
        context: Context,
        overwrite: Boolean = true,
        quality: Int = 100
    ): LiveData<WorkInfo> {
        if (quality < DOWNLOAD_QUALITY_MIN || quality > DOWNLOAD_QUALITY_MAX)
            throw IllegalArgumentException(
                "Quality must be between $DOWNLOAD_QUALITY_MIN and $DOWNLOAD_QUALITY_MAX"
            )
        Timber.v("Downloading $namespace \"$displayName\"...")
        Timber.v("Preparing DownloadData...")
        val downloadData = DownloadData(this, overwrite, quality)
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
    ): DownloadStatus {
        Timber.d("$pin Checking if downloaded")

        val downloadWorkInfo = downloadWorkInfo(context)
        val result = if (downloadWorkInfo != null)
            DownloadStatus.DOWNLOADING
        else isDownloadIndexed(searchSession, objectId)

        Timber.d("$pin Finished checking download status. Result: $result")
        return result
    }

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
        val children = getChildren(searchSession)
        for (child in children)
            if (child is DataClass<*, *> &&
                child.downloadStatus(context, searchSession) == DownloadStatus.DOWNLOADED
            )
                return true
        return false
    }

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

        val children = getChildren(searchSession)
        for (child in children)
            if (child is DataClass<*, *>)
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
     * Creates a dynamic link access for the DataClass
     * @author Arnau Mora
     * @since 20210521
     * @param dynamicLinks The Firebase Dynamic Links instance to create the link from.
     * @param firebaseStorage The Firebase Storage instance to fetch the image from
     * @throws IllegalStateException When the DataClass doesn't have a [DataClassMetadata.webURL]
     * defined in [metadata].
     * @throws CouldNotCreateDynamicLinkException When there was an unknown exception while creating
     * the dynamic link.
     * @throws StorageException When there's an error while fetching the download url of the image.
     */
    @Throws(
        IllegalStateException::class,
        CouldNotCreateDynamicLinkException::class,
        StorageException::class
    )
    suspend fun getDynamicLink(
        dynamicLinks: FirebaseDynamicLinks,
        firebaseStorage: FirebaseStorage
    ): Uri = suspendCoroutine { cont ->
        Timber.v("Processing dynamic links for areas...")
        val webUrl = metadata.webURL
        if (webUrl != null) {
            Timber.i("Dynamic link not found for A/$objectId. Creating one...")
            Timber.i("Getting image URL...")
            firebaseStorage
                .getReferenceFromUrl(imageReferenceUrl)
                .downloadUrl
                .addOnSuccessListener { imageDownloadUri ->
                    Timber.i("Creating dynamic link...")
                    dynamicLinks.shortLinkAsync(ShortDynamicLink.Suffix.SHORT) {
                        link = Uri.parse("$webUrl/?area=$objectId")
                        domainUriPrefix = DYNAMIC_LINKS_DOMAIN
                        androidParameters(APPLICATION_ID) {
                            fallbackUrl =
                                Uri.parse("https://play.google.com/store/apps/details?id=${APPLICATION_ID}")
                            minimumVersion =
                                192 // This is when the dynamic link handling was introduced
                        }
                        socialMetaTagParameters {
                            title = "Escalar Alcoià i Comtat - $displayName"
                            description = displayName
                            imageUrl = imageDownloadUri
                        }
                    }.addOnSuccessListener { shortDynLink ->
                        val link = shortDynLink.shortLink
                        if (link != null)
                            cont.resume(link)
                        else
                            cont.resumeWithException(
                                CouldNotCreateDynamicLinkException(
                                    "There was an unknown error while creating the dynamic link"
                                )
                            )
                    }.addOnFailureListener { e -> cont.resumeWithException(e) }
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        } else cont.resumeWithException(IllegalStateException("The webUrl in the DataClass' metadata is null."))
    }

    /**
     * Get the [FirebaseStorage] reference for loading the [DataClass]' image.
     * @author Arnau Mora
     * @since 20210721
     * @param storage The [FirebaseStorage] reference for loading the image file.
     * @return The [StorageReference] that corresponds to the [DataClass]' image.
     */
    fun storageReference(storage: FirebaseStorage): StorageReference =
        storage.getReferenceFromUrl(imageReferenceUrl)

    /**
     * Get the [FirebaseStorage] download url for loading the [DataClass]' image.
     * @author Arnau Mora
     * @since 20210721
     * @param storage The [FirebaseStorage] reference for loading the image file.
     * @return The [DataClass]' download url.
     */
    suspend fun storageUrl(storage: FirebaseStorage): Uri {
        if (downloadUrl == null)
            downloadUrl = storageReference(storage).downloadUrl.await()
        return downloadUrl!!
    }

    /**
     * Checks if the [DataClass] has a stored [downloadUrl].
     * @author Arnau Mora
     * @since 20210722
     */
    fun hasStorageUrl() = downloadUrl != null

    /**
     * Fetches the image [Bitmap] from the DataClass.
     * @author Arnau Mora
     * @since 20210721
     * @param context The context the app is running on.
     * @param storage The [FirebaseStorage] reference for loading the image file.
     * @param imageLoadParameters The parameters desired to load the image.
     * @param progress The progress listener, for supervising loading.
     * @throws StorageException When there's an issue while downloading the image from the server.
     * @throws IOException When there's an issue while reading or writing the image from the fs.
     * @throws ArithmeticException When there's been an error while compressing the image.
     * @see ValueMax
     * @see ImageLoadParameters
     */
    @WorkerThread
    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(StorageException::class, IOException::class, ArithmeticException::class)
    suspend fun image(
        context: Context,
        storage: FirebaseStorage,
        imageLoadParameters: ImageLoadParameters? = null,
        progress: (@UiThread (progress: ValueMax<Long>) -> Unit)? = null
    ): Bitmap? {
        val downloadedImageFile = imageFile(context)
        return if (downloadedImageFile.exists()) {
            Timber.d("Loading image from storage: ${downloadedImageFile.path}")
            readBitmap(downloadedImageFile)
        } else {
            val scale = imageLoadParameters?.resultImageScale ?: 1f
            val cacheImage = cacheImageFile(context, "scale$scale")

            if (!cacheImage.exists())
                try {
                    Timber.v("$this > Getting stream...")
                    val snapshot = storage.getReferenceFromUrl(imageReferenceUrl)
                        .stream
                        .addOnProgressListener { snapshot ->
                            if (progress != null) {
                                val bytesCount = snapshot.bytesTransferred
                                val totalBytes = snapshot.totalByteCount
                                CoroutineScope(Dispatchers.Main).launch {
                                    progress(ValueMax(bytesCount, totalBytes))
                                }
                            }
                        }
                        .await()
                    Timber.v("$this > Stream loaded. Decoding...")
                    val stream = snapshot.stream
                    val bitmap: Bitmap? = BitmapFactory.decodeStream(
                        stream,
                        null,
                        BitmapFactory.Options().apply {
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
                            baos.writeTo(cacheImage.outputStream())
                            Timber.v("$this > Image stored.")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "$this > Could not load DataClass ($objectId) image.")
                    throw e
                }

            Timber.v("$this > Reading cache image ($cacheImage)...")
            readBitmap(cacheImage)
        }
    }

    /**
     * Loads the image of the Data Class
     * @author Arnau Mora
     * @date 2020/09/11
     * @patch 2020/09/12 - Arnau Mora: Added function loadImage into this
     * @param activity The [Activity] that is showing the image
     * @param imageView The Image View for loading the image into
     * @param imageLoadParameters The parameters to use for loading the image
     * @throws StorageException When there was an error while loading from [storage].
     * @throws IllegalArgumentException When the stored reference url ([imageReferenceUrl]) is not well formatted.
     * @throws IOException When there's an issue while reading or writing the image from the fs.
     * @throws ArithmeticException When there's been an error while compressing the image.
     * @see imageReferenceUrl
     */
    @UiThread
    @Throws(
        StorageException::class,
        IllegalArgumentException::class,
        IOException::class,
        ArithmeticException::class
    )
    @Suppress("BlockingMethodInNonBlockingContext")
    fun loadImage(
        activity: Activity,
        storage: FirebaseStorage,
        imageView: ImageView,
        progressBar: LinearProgressIndicator?,
        imageLoadParameters: ImageLoadParameters? = null
    ) {
        if (activity.isDestroyed) {
            Timber.e("The activity is destroyed, won't load image.")
            return
        }

        imageView.scaleType = imageLoadParameters?.scaleType ?: ImageView.ScaleType.CENTER_CROP

        val showPlaceholder = imageLoadParameters?.showPlaceholder ?: true
        if (showPlaceholder)
            imageView.setImageResource(displayOptions.placeholderDrawable)

        doAsync {
            // TODO: Add error handlers
            val bmp = image(activity, storage, imageLoadParameters) { progress ->
                progressBar?.progress = progress.percentage
            }
            uiContext { imageView.setImageBitmap(bmp) }
        }
    }

    override fun hashCode(): Int {
        var result = objectId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + imageReferenceUrl.hashCode()
        result = 31 * result + displayOptions.hashCode()
        result = 31 * result + namespace.hashCode()
        return result
    }
}

/**
 * Checks if the all the items in the iterable have a loaded [DataClass.downloadUrl].
 * @author Arnau Mora
 * @since 20210722
 * @return true if all the items have an stored download url, false otherwise.
 * @see DataClass.downloadUrl
 * @see DataClass.hasStorageUrl
 */
fun <D : DataClass<*, *>> Iterable<D>.hasStorageUrls(): Boolean {
    for (i in this)
        if (!i.hasStorageUrl())
            return false
    return true
}

/**
 * Gets the children from all the [DataClass]es in the [Iterator].
 * @author Arnau Mora
 * @since 20210724
 * @param searchSession The search session for fetching data
 */
suspend fun <A : DataClassImpl, B : DataClassImpl, D : DataClass<A, B>> Iterable<D>.getChildren(
    searchSession: AppSearchSession
): List<A> {
    val items = arrayListOf<A>()
    for (i in this)
        items.addAll(i.getChildren(searchSession))
    return items
}

/**
 * Gets an element from the [Iterable].
 * @author Arnau Mora
 * @since 20210724
 * @param objectId The id of the object to search for.
 * @return The object you are searching for, or null if not found.
 */
operator fun <D : DataClass<*, *>> Iterable<D>.get(objectId: String): D? {
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
fun <D : DataClass<*, *>> Iterable<D>.has(objectId: String): Boolean {
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
suspend fun <A : DataClassImpl> @receiver:ObjectId
String.getChildren(searchSession: AppSearchSession, namespace: String) =
    searchSession.getChildren<A>(namespace, this)
