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
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.exceptions.AppSearchException
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
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
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_STATIC
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.core.utils.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.allTrue
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadData
import com.arnyminerz.escalaralcoiaicomtat.core.worker.download.DownloadWorkerModel
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
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// A: List type
// B: Parent Type
abstract class DataClass<A : DataClassImpl, B : DataClassImpl>(
    override val displayName: String,
    override val timestampMillis: Long,
    open val imageReferenceUrl: String,
    open val kmzReferenceUrl: String?,
    open val uiMetadata: UIMetadata,
    open val metadata: DataClassMetadata,
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
         * Returns the correct image name for the desired [objectId] and [namespace].
         * @author Arnau Mora
         * @since 20210822
         */
        fun imageName(namespace: String, objectId: String, suffix: String?) =
            "$namespace-$objectId${suffix ?: ""}"

        /**
         * Searches in the [app] search instance and tries to get an intent from them.
         * @author Arnau Mora
         * @since 20210416
         * @param app The [App] instance.
         * @param queryName What to search. May be [DataClass.displayName] or [DataClassMetadata.webURL].
         * @return An [Intent] if the [DataClass] was found, or null.
         */
        @WorkerThread
        suspend fun List<Area>.getIntent(
            context: Context,
            searchSession: AppSearchSession,
            queryName: String
        ): Intent? {
            var result: Intent? = null

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

            Timber.d("Trying to generate intent from \"$queryName\". Searching in $size areas.")
            for (area in this) {
                Timber.d("  Finding in ${area.displayName}.")
                if (area.displayName.equals(queryName, true) ||
                    area.metadata.webURL.equals(queryName, true)
                )
                    result = Intent(context, areaActivityClass).apply {
                        Timber.d("Found Area id ${area.objectId}!")
                        putExtra(EXTRA_AREA, area.objectId)
                    }
                else {
                    Timber.d("  Iterating area's children...")
                    val zones = area.getChildren(searchSession)
                    for (zone in zones) {
                        Timber.d("    Finding in ${zone.displayName}.")
                        // Children must be loaded so `count` is fetched correctly.
                        val sectors = zone.getChildren(searchSession)

                        if (zone.displayName.equals(queryName, true) ||
                            zone.metadata.webURL.equals(queryName, true)
                        )
                            result = Intent(context, zoneActivityClass).apply {
                                putExtra(EXTRA_ZONE, zone.objectId)
                            }
                        else
                            for ((counter, sector) in sectors.withIndex()) {
                                Timber.d("      Finding in ${sector.displayName}.")
                                if (sector.displayName.equals(queryName, true) ||
                                    sector.metadata.webURL.equals(queryName, true)
                                )
                                    result = Intent(context, sectorActivityClass)
                                        .apply {
                                            Timber.d("Found Sector id ${sector.objectId} at $counter!")
                                            putExtra(EXTRA_AREA, area.objectId)
                                            putExtra(EXTRA_ZONE, zone.objectId)
                                        }

                                // If a result has been found, exit loop
                                if (result != null) break
                            }
                        // If a result has been found, exit loop
                        if (result != null) break
                    }
                }
                if (result != null) break
            }
            return result?.also {
                it.putExtra(EXTRA_STATIC, true)
            }
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
        get() = "${namespace}_$objectId"

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
     */
    @WorkerThread
    suspend fun getChildren(searchSession: AppSearchSession): List<A> {
        val childNamespace = metadata.childNamespace
        Timber.v("$this > Building search spec...")
        val searchSpec = SearchSpec.Builder()
            .addFilterNamespaces(childNamespace)
            .setResultCountPerPage(100)
            .setOrder(SearchSpec.ORDER_ASCENDING)
            .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
            .build()
        Timber.v("$this > Performing search for \"$objectId\" with namespace \"$childNamespace\"...")
        val searchResults = searchSession.search(objectId, searchSpec)
        Timber.v("$this > Awaiting for results...")
        val nextPage = searchResults.nextPage.await()
        val list = arrayListOf<A>()
        Timber.v("$this > Building results list...")
        for ((p, page) in nextPage.withIndex()) {
            val genericDocument = page.genericDocument
            val schemaType = genericDocument.schemaType
            Timber.v("$this > [$p] Schema type: $schemaType")
            val data = try {
                when (schemaType) {
                    "AreaData" -> genericDocument.toDocumentClass(AreaData::class.java).data()
                    "ZoneData" -> genericDocument.toDocumentClass(ZoneData::class.java).data()
                    "SectorData" -> genericDocument.toDocumentClass(SectorData::class.java).data()
                    "PathData" -> genericDocument.toDocumentClass(PathData::class.java).data()
                    else -> {
                        Timber.w("$this > [$p] Got unknown schema type.")
                        continue
                    }
                }
            } catch (e: AppSearchException) {
                Timber.e(e, "$this > [$p] Could not convert document class!")
                continue
            }
            val a = data as? A ?: continue
            Timber.v("$this > [$p] Adding to result list...")
            list.add(a)
        }
        return list
    }

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
        File(
            if (permanent) dataDir(context) else context.cacheDir,
            pin
        )

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
     */
    @Throws(IllegalStateException::class)
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
     * @param storage The [FirebaseStorage] instance to load the files from the server.
     * @param showNonDownloaded If the non-downloaded sections should be added.
     * @param progressListener A listener for the progress of the load.
     */
    @WorkerThread
    suspend fun downloadedSectionList(
        context: Context,
        searchSession: AppSearchSession,
        storage: FirebaseStorage,
        showNonDownloaded: Boolean,
        progressListener: (suspend (current: Int, max: Int) -> Unit)? = null
    ): Flow<DownloadedSection> = flow {
        Timber.v("Getting downloaded sections...")
        val downloadedSectionsList = arrayListOf<DownloadedSection>()
        val children = getChildren(searchSession)
        for ((c, child) in children.withIndex())
            (child as? DataClass<*, *>)?.let { dataClass -> // Paths shouldn't be included
                val downloadStatus = dataClass.downloadStatus(context, searchSession, storage)
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
        return schedule.invoke(null, context, pin, downloadData) as LiveData<WorkInfo>
    }

    /**
     * Gets the DownloadStatus of the DataClass
     * @author Arnau Mora
     * @since 20210313
     * @param context The currently calling [Context].
     * @param storage The [FirebaseStorage] instance to load children from the server.
     * @param progressListener A progress updater.
     * @return a matching DownloadStatus representing the Data Class' download status
     */
    @WorkerThread
    suspend fun downloadStatus(
        context: Context,
        searchSession: AppSearchSession,
        storage: FirebaseStorage,
        progressListener: (@UiThread (progress: ValueMax<Int>) -> Unit)? = null
    ): DownloadStatus {
        Timber.d("$pin Checking if downloaded")

        val downloadWorkInfo = downloadWorkInfo(context)
        val result = if (downloadWorkInfo != null)
            DownloadStatus.DOWNLOADING
        else {
            val imageFile = imageFile(context)
            Timber.v("Checking if image file exists...")
            val imageFileExists = imageFile.exists()
            if (!imageFileExists)
                Timber.d("$pin Image file ($imageFile) doesn't exist")

            // If image file exists:
            // - If all the children are downloaded: DOWNLOADED
            // - If there's a non-downloaded children: PARTIALLY
            // If image file doesn't exist:
            // - If there are not any downloaded children: NOT_DOWNLOADED
            // - If there's at least one downloaded children: PARTIALLY

            Timber.v("$pin Getting children elements download status...")
            val children = getChildren(searchSession)

            Timber.v("$pin Finding for a downloaded children in ${children.size}...")
            var allChildrenDownloaded = true
            var atLeastOneChildrenDownloaded = false
            for ((c, child) in children.withIndex()) {
                if (child is DataClass<*, *>) {
                    uiContext { progressListener?.invoke(ValueMax(c, children.size)) }
                    val childDownloadStatus = child.downloadStatus(context, searchSession, storage)
                    if (!childDownloadStatus.downloaded) {
                        // If at least one non-downloaded, that means that not all children are
                        Timber.d("$pin has a non-downloaded children (${child.pin}): $childDownloadStatus")
                        allChildrenDownloaded = false
                    } else {
                        // This means there's at least one children downloaded (or downloading?)
                        Timber.v("$pin has a downloaded/downloading children (${child.pin}): $childDownloadStatus")
                        atLeastOneChildrenDownloaded = true
                        break
                    }
                } else Timber.d("$pin Child is not DataClass")
            }

            if (imageFileExists && allChildrenDownloaded)
                DownloadStatus.DOWNLOADED
            else if (!imageFileExists && !atLeastOneChildrenDownloaded)
                DownloadStatus.NOT_DOWNLOADED
            else DownloadStatus.PARTIALLY
        }

        Timber.d("$pin Finished checking download status. Result: $result")
        return result
    }

    /**
     * Checks if the data class has any children that has been downloaded
     * @author Arnau Mora
     * @date 2020/09/14
     * @param context The currently calling [Context].
     * @param searchSession The search session for fetching data.
     * @param storage The [FirebaseStorage] instance to load children from the server.
     * @return If the data class has any downloaded children
     */
    @WorkerThread
    suspend fun hasAnyDownloadedChildren(
        context: Context,
        searchSession: AppSearchSession,
        storage: FirebaseStorage
    ): Boolean {
        val children = getChildren(searchSession)
        for (child in children)
            if (child is DataClass<*, *> &&
                child.downloadStatus(context, searchSession, storage) == DownloadStatus.DOWNLOADED
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
    suspend fun delete(context: Context, searchSession: AppSearchSession): Boolean {
        Timber.v("Deleting $objectId")
        val lst = arrayListOf<Boolean>() // Stores all the delection success statuses

        // KMZ should be deleted
        val kmzFile = kmzFile(context, true)
        if (kmzFile.exists()) {
            Timber.v("$this > Deleting \"$kmzFile\"")
            lst.add(kmzFile.deleteIfExists())
        }

        // Instead of deleting image, move to cache. System will manage it if necessary.
        val imgFile = imageFile(context)
        if (imgFile.exists()) {
            val cacheImageFile = cacheImageFile(context)
            Timber.v("$this > Copying \"$imgFile\" to \"$cacheImageFile\"...")
            imgFile.copyTo(cacheImageFile, true)
            Timber.v("$this > Deleting \"$imgFile\"")
            lst.add(imgFile.delete())
        }

        val children = getChildren(searchSession)
        for (child in children)
            if (child is DataClass<*, *>)
                lst.add(child.delete(context, searchSession))

        return lst.allTrue()
    }

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
    fun imageFile(context: Context): File =
        File(dataDir(context), imageName(namespace, objectId, null))

    /**
     * Returns the File that represents the image of the DataClass in cache.
     * @author Arnau Mora
     * @date 20210724
     * @param context The context to run from
     * @param suffix If not null, will be added to the end of the file name.
     * @return The path of the image file that can be downloaded
     */
    fun cacheImageFile(context: Context, suffix: String? = null): File =
        File(context.cacheDir, imageName(namespace, objectId, suffix))

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
     */
    @Throws(IllegalStateException::class, CouldNotCreateDynamicLinkException::class)
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
     * @see ValueMax
     * @see ImageLoadParameters
     */
    @WorkerThread
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
                            throw IOException("Could not compress image for $this.")
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
     * @see imageReferenceUrl
     */
    @UiThread
    @Throws(StorageException::class, IllegalArgumentException::class)
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
            imageView.setImageResource(uiMetadata.placeholderDrawable)

        doAsync {
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
        result = 31 * result + uiMetadata.placeholderDrawable
        result = 31 * result + uiMetadata.errorPlaceholderDrawable
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
