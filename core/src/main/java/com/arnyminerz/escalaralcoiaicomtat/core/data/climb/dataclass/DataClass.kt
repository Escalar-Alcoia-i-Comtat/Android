package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.exception.CouldNotCreateDynamicLinkException
import com.arnyminerz.escalaralcoiaicomtat.core.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.core.exception.NotDownloadedException
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ACTIVITY_AREA_META
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ACTIVITY_SECTOR_META
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ACTIVITY_ZONE_META
import com.arnyminerz.escalaralcoiaicomtat.core.shared.APPLICATION_ID
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DATACLASS_WAIT_CHILDREN_DELAY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DYNAMIC_LINKS_DOMAIN
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR_COUNT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_STATIC
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.core.shared.cache
import com.arnyminerz.escalaralcoiaicomtat.core.utils.allTrue
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.scale
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DOWNLOAD_QUALITY_MAX
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DOWNLOAD_QUALITY_MIN
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DownloadData
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DownloadWorker
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.dynamiclinks.ktx.socialMetaTagParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// A: List type
// B: Parent Type
abstract class DataClass<A : DataClassImpl, B : DataClassImpl>(
    val displayName: String,
    timestamp: Date,
    imageReferenceUrl: String,
    val kmzReferenceUrl: String?,
    val uiMetadata: UIMetadata,
    val metadata: DataClassMetadata
) : DataClassImpl(metadata.objectId, metadata.namespace, timestamp), Iterable<A> {
    companion object {
        /**
         * Searches in [AREAS] and tries to get an intent from them.
         * @author Arnau Mora
         * @since 20210416
         * @param context The context to initialize the [Intent]
         * @param queryName What to search. May be [DataClass.displayName] or [DataClassMetadata.webURL].
         * @param firestore The [FirebaseFirestore] instance.
         * @return An [Intent] if the [DataClass] was found, or null.
         */
        suspend fun getIntent(
            context: Context,
            queryName: String,
            firestore: FirebaseFirestore
        ): Intent? {
            var result: Intent? = null

            Timber.v("Getting Activities...")
            val app = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val appBundle = app.metaData
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

            Timber.d("Trying to generate intent from \"$queryName\". Searching in ${AREAS.size} areas.")
            for (area in AREAS) {
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
                    val zones = arrayListOf<Zone>()
                    area.getChildren(firestore).toCollection(zones)
                    for (zone in zones) {
                        Timber.d("    Finding in ${zone.displayName}.")
                        // Children must be loaded so `count` is fetched correctly.
                        val sectors = arrayListOf<Sector>()
                        zone.getChildren(firestore)
                            .toCollection(sectors)

                        if (zone.displayName.equals(queryName, true) ||
                            zone.metadata.webURL.equals(queryName, true)
                        )
                            result = Intent(context, zoneActivityClass).apply {
                                Timber.d("Found Zone id ${zone.objectId}!")
                                putExtra(EXTRA_AREA, area.objectId)
                                putExtra(EXTRA_ZONE, zone.objectId)
                                putExtra(EXTRA_SECTOR_COUNT, zone.count())
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
                                            putExtra(
                                                EXTRA_SECTOR_COUNT,
                                                zone.count()
                                            )
                                            putExtra(EXTRA_SECTOR_INDEX, counter)
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
     * Stores if the children are currently being loaded.
     * @author Arnau Mora
     * @since 20210514
     */
    private var loadingChildren = false

    protected val innerChildren = arrayListOf<A>()

    var imageReferenceUrl: String = imageReferenceUrl
        private set

    private val pin = "${namespace}_$objectId"

    val transitionName = objectId + displayName.replace(" ", "_")

    /**
     * Returns the data classes' children. May fetch them from storage, or return the cached items
     * @author Arnau Mora
     * @since 20210313
     * @param firestore The Firestore instance.
     * @throws NoInternetAccessException If no Internet connection is available, and the children are
     * not stored in storage.
     * @throws IllegalStateException When the children has not been loaded and [firestore] is null.
     */
    @WorkerThread
    @Throws(NoInternetAccessException::class, IllegalStateException::class)
    suspend fun getChildren(firestore: FirebaseFirestore?): List<A> {
        if (loadingChildren) {
            Timber.v("Waiting for children to finish loading")
            while (loadingChildren) {
                delay(DATACLASS_WAIT_CHILDREN_DELAY)
            }
            Timber.v("Finished loading children!")
        }
        val dataClassId = metadata.documentPath
        if (innerChildren.isEmpty()) {
            loadingChildren = true
            when {
                cache.hasChild(dataClassId) -> {
                    // Loads children from cache
                    val children = cache.getChildren(dataClassId)!!
                    for (child in children)
                        if (coroutineContext.isActive)
                            (child as? A)?.let { data ->
                                innerChildren.add(data)
                            }
                        else break
                }
                firestore != null -> {
                    // Loads children from server
                    innerChildren.addAll(
                        loadChildren(firestore)
                    )
                    cache.storeChild(dataClassId, innerChildren)
                }
                else -> {
                    loadingChildren = false
                    throw IllegalStateException("There are no loaded children, and firestore is null.")
                }
            }
            loadingChildren = false
        }
        return innerChildren
    }

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
     * @throws StorageException When there has been an exception with the [storage] download.
     * @see kmzReferenceUrl
     */
    @Throws(StorageException::class)
    private suspend fun storeKmz(
        storage: FirebaseStorage,
        targetFile: File
    ): FileDownloadTask.TaskSnapshot? =
        if (kmzReferenceUrl != null)
            suspendCoroutine { cont ->
                storage.getReferenceFromUrl(kmzReferenceUrl)
                    .getFile(targetFile)
                    .addOnSuccessListener { cont.resume(it) }
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
     * @throws IllegalStateException When [kmzReferenceUrl] is null, so a [File] can't be retrieved.
     */
    @Throws(IllegalStateException::class)
    suspend fun kmzFile(context: Context, storage: FirebaseStorage, permanent: Boolean): File {
        val kmzFile = kmzFile(context, permanent)

        if (!kmzFile.exists()) {
            Timber.v("Storing KMZ file...")
            val kmzTaskSnapshot = storeKmz(storage, kmzFile)
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
     * Gets the children element at [index].
     * May throw [IndexOutOfBoundsException] if children have not been loaded.
     * @author Arnau Mora
     * @since 20210413
     * @throws IndexOutOfBoundsException When the specified [index] does not exist in [innerChildren]
     */
    @Throws(IndexOutOfBoundsException::class)
    @WorkerThread
    operator fun get(index: Int): A = innerChildren[index]

    @WorkerThread
    protected abstract suspend fun loadChildren(firestore: FirebaseFirestore): Collection<A>

    /**
     * Gets an object based on objectId
     * @author Arnau Mora
     * @since 20210312
     * @param objectId The id to find
     * @return The found dataclass
     * @see getChildren
     * @throws IllegalStateException If the children's list is empty
     * @throws IndexOutOfBoundsException If the objectId was not found
     */
    @Throws(IllegalStateException::class, IndexOutOfBoundsException::class)
    operator fun get(objectId: String): A {
        if (innerChildren.isEmpty())
            throw IllegalStateException("Children is empty")
        for (child in innerChildren)
            if (child.objectId == objectId)
                return child
        throw IndexOutOfBoundsException("Could not find $objectId in children.")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DataClass<*, *>)
            return super.equals(other)
        return other.namespace == namespace && other.objectId == objectId
    }

    /**
     * Gets the children [Iterator].
     * @author Arnau Mora
     * @since 20210413
     * @throws IllegalStateException When there are no loaded children.
     */
    @WorkerThread
    @Throws(IllegalStateException::class)
    override fun iterator(): Iterator<A> {
        if (innerChildren.isEmpty())
            throw IllegalStateException("Children is empty")
        return innerChildren.iterator()
    }

    override fun toString(): String = displayName

    /**
     * Generates a list of [DownloadedSection].
     * @author Arnau Mora
     * @since 20210412
     * @param activity The [Activity] where the function is being ran on.
     * @param firestore The [FirebaseFirestore] instance to load the data from.
     * @param showNonDownloaded If the non-downloaded sections should be added.
     * @param progressListener A listener for the progress of the load.
     */
    @WorkerThread
    suspend fun downloadedSectionList(
        activity: Activity,
        firestore: FirebaseFirestore,
        showNonDownloaded: Boolean,
        progressListener: (suspend (current: Int, max: Int) -> Unit)? = null
    ): Flow<DownloadedSection> = flow {
        Timber.v("Getting downloaded sections...")
        val downloadedSectionsList = arrayListOf<DownloadedSection>()
        val children = arrayListOf<DataClassImpl>()
        getChildren(firestore).toCollection(children)
        for ((c, child) in children.withIndex())
            (child as? DataClass<*, *>)?.let { dataClass -> // Paths shouldn't be included
                val downloadStatus = dataClass.downloadStatus(activity, firestore)
                progressListener?.invoke(c, children.size)
                if (showNonDownloaded ||
                    downloadStatus.isDownloaded() || downloadStatus.partialDownload()
                )
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
    fun download(
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
        return DownloadWorker.schedule(context, pin, downloadData)
    }

    /**
     * Gets the DownloadStatus of the DataClass
     * @author Arnau Mora
     * @since 20210313
     * @param activity The activity to run from
     * @param firestore The [FirebaseFirestore] instance to load children from
     * @param progressListener A progress updater
     * @return a matching DownloadStatus representing the Data Class' download status
     */
    @WorkerThread
    suspend fun downloadStatus(
        activity: Activity,
        firestore: FirebaseFirestore,
        progressListener: ((current: Int, max: Int) -> Unit)? = null
    ): DownloadStatus {
        Timber.d("$pin Checking if downloaded")

        val downloadWorkInfo = downloadWorkInfo(activity)
        val result = if (downloadWorkInfo != null)
            DownloadStatus.DOWNLOADING
        else {
            val imageFile = imageFile(activity)
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
            val children = arrayListOf<DataClassImpl>()
            getChildren(firestore).toCollection(children)

            Timber.v("$pin Finding for a downloaded children in ${children.size}...")
            var allChildrenDownloaded = true
            var atLeastOneChildrenDownloaded = false
            for ((c, child) in children.withIndex()) {
                if (child is DataClass<*, *>) {
                    progressListener?.invoke(c, children.size)
                    val childDownloadStatus = child.downloadStatus(activity, firestore)
                    if (childDownloadStatus != DownloadStatus.DOWNLOADED) {
                        Timber.d(
                            "$pin has a non-downloaded children (${child.pin}): $childDownloadStatus"
                        )
                        allChildrenDownloaded = false
                        break
                    } else atLeastOneChildrenDownloaded = true
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
     * @param activity The activity to run from
     * @param firestore A [FirebaseFirestore] instance to load new data from.
     *
     * @return If the data class has any downloaded children
     */
    @WorkerThread
    suspend fun hasAnyDownloadedChildren(
        activity: Activity,
        firestore: FirebaseFirestore
    ): Boolean {
        val children = arrayListOf<DataClassImpl>()
        getChildren(firestore).toCollection(children)
        for (child in children)
            if (child is DataClass<*, *> &&
                child.downloadStatus(activity, firestore) == DownloadStatus.DOWNLOADED
            )
                return true
        return false
    }

    /**
     * Deletes the downloaded content if downloaded
     * @author Arnau Mora
     * @date 2020/09/11
     * @param activity The activity to run from
     * @return If the content was deleted successfully. Note: returns true if not downloaded
     */
    suspend fun delete(activity: Activity): Boolean {
        Timber.v("Deleting $objectId")
        val imgFile = imageFile(activity)
        val lst = arrayListOf<Boolean>()

        val kmzFile = kmzFile(activity, true)
        if (kmzFile.exists()) {
            Timber.v("Deleting \"$kmzFile\"")
            kmzFile.deleteIfExists()
        }

        Timber.v("Deleting \"$imgFile\"...")
        lst.add(imgFile.deleteIfExists())
        val children = arrayListOf<DataClassImpl>()
        try {
            getChildren(null).toCollection(children)
        } catch (_: IllegalStateException) {
        }
        for (child in children)
            (child as? DataClass<*, *>)?.let {
                lst.add(it.delete(activity))
            }
        return lst.allTrue()
    }

    /**
     * Gets the space that is occupied by the data class' downloaded data in the system
     * @author Arnau Mora
     * @date 2020/09/11
     * @patch 2020/09/12 - Arnau Mora: Added child space computation
     * @param activity The activity to run from
     * @return The size in bytes that is used by the downloaded data
     *
     * @throws NotDownloadedException If tried to get size when not downloaded
     */
    @Throws(NotDownloadedException::class)
    suspend fun size(activity: Activity): Long {
        val imgFile = imageFile(activity)

        if (!imgFile.exists()) throw NotDownloadedException(this)

        var size = imgFile.length()

        val children = arrayListOf<DataClassImpl>()
        try {
            getChildren(null).toCollection(children)
        } catch (_: IllegalStateException) {
        }
        for (child in children)
            if (child is DataClass<*, *>)
                size += child.size(activity)

        Timber.v("\"$displayName\" storage usage: $size")

        return size
    }

    /**
     * Returns the amount of children the data class has
     * @author Arnau Mora
     * @date 20210413
     * @return The amount of children the data class has
     * @throws IllegalStateException When the children has not been loaded and [firestore] is null.
     * @see getChildren
     */
    @Throws(IllegalStateException::class)
    @WorkerThread
    suspend fun count(firestore: FirebaseFirestore?): Int {
        val children = arrayListOf<DataClassImpl>()
        getChildren(firestore).toCollection(children)
        return children.size
    }

    /**
     * Returns the amount of children the data class has, as well as all the children
     * @author Arnau Mora
     * @date 2020/09/11
     * @return The amount of children the data class has, as well as all the children
     */
    fun fullCount(): Int { // Counts all the children also
        var counter = 1 // Starts at 1 for counting self

        for (me in this)
            if (me is DataClass<*, *>)
                counter += me.fullCount()

        return counter
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
     * Checks if the data class has children.
     * Note: won't load children, will just use the already loaded ones.
     * @author Arnau Mora
     * @since 20210411
     */
    @WorkerThread
    fun isEmpty(): Boolean = innerChildren.isEmpty()

    /**
     * Checks if the data class doesn't have any children
     * @author Arnau Mora
     * @since 20210411
     */
    @WorkerThread
    fun isNotEmpty() = !isEmpty()

    /**
     * Returns the File that represents the image of the DataClass
     * @author Arnau Mora
     * @date 2020/09/10
     * @param context The context to run from
     * @return The path of the image file that can be downloaded
     */
    fun imageFile(context: Context): File = File(dataDir(context), "$namespace-$objectId.webp")

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

        @UiThread
        fun loadImage(bmp: Bitmap?, @DrawableRes resource: Int?) {
            if (bmp != null)
                imageView.setImageBitmap(bmp)
            if (resource != null)
                imageView.setImageResource(resource)
            imageView.scaleType = imageLoadParameters?.scaleType ?: ImageView.ScaleType.CENTER_CROP
        }

        val showPlaceholder = imageLoadParameters?.showPlaceholder ?: true
        if (showPlaceholder)
            loadImage(null, uiMetadata.placeholderDrawable)

        val downloadedImageFile = imageFile(activity)
        if (downloadedImageFile.exists()) {
            Timber.d("Loading image from storage: ${downloadedImageFile.path}")
            val bmp = readBitmap(downloadedImageFile)
            imageView.setImageBitmap(bmp)
        } else {
            val tempFile = File.createTempFile("images", objectId)
            storage.getReferenceFromUrl(imageReferenceUrl)
                .getFile(tempFile)
                .addOnSuccessListener {
                    Timber.v("Loaded image for $objectId. Decoding...")
                    val bitmap = BitmapFactory.decodeFile(tempFile.path)

                    Timber.v("Image decoded, scaling...")
                    val scale = imageLoadParameters?.resultImageScale ?: 1f
                    val bmp = bitmap.scale(scale)

                    Timber.v("Setting image into imageView.")
                    loadImage(bmp, null)
                }
                .addOnProgressListener { snapshot ->
                    val bytesCount = snapshot.bytesTransferred
                    val totalBytes = snapshot.totalByteCount
                    val progress = bytesCount / totalBytes
                    progressBar?.progress = (progress * 100).toInt()
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Could not load DataClass ($objectId) image.")
                    loadImage(null, uiMetadata.errorPlaceholderDrawable)
                }
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
 * Finds an [DataClass] inside a list with an specific id. If it's not found, null is returned.
 * @author Arnau Mora
 * @since 20210413
 * @param objectId The id to search
 */
operator fun <A : DataClassImpl, B : DataClassImpl, D : DataClass<A, B>> Iterable<D>.get(objectId: String): D? {
    for (o in this)
        if (o.objectId == objectId)
            return o
    return null
}
