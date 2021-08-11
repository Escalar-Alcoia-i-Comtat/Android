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
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.exception.CouldNotCreateDynamicLinkException
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
import com.arnyminerz.escalaralcoiaicomtat.core.utils.ValueMax
import com.arnyminerz.escalaralcoiaicomtat.core.utils.WEBP_LOSSY_LEGACY
import com.arnyminerz.escalaralcoiaicomtat.core.utils.allTrue
import com.arnyminerz.escalaralcoiaicomtat.core.utils.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.scale
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DOWNLOAD_QUALITY_MAX
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DOWNLOAD_QUALITY_MIN
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DownloadData
import com.arnyminerz.escalaralcoiaicomtat.core.worker.DownloadWorker
import com.google.android.gms.tasks.OnSuccessListener
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    open val metadata: DataClassMetadata
) : DataClassImpl(
    metadata.objectId,
    metadata.namespace,
    timestampMillis,
    displayName,
    metadata.documentPath
),
    Iterable<A> {
    companion object {
        /**
         * Searches in [AREAS] and tries to get an intent from them.
         * @author Arnau Mora
         * @since 20210416
         * @param context The context to initialize the [Intent]
         * @param queryName What to search. May be [DataClass.displayName] or [DataClassMetadata.webURL].
         * @param storage The [FirebaseStorage] reference to download the files required from the server.
         * @return An [Intent] if the [DataClass] was found, or null.
         */
        suspend fun getIntent(
            context: Context,
            queryName: String,
            storage: FirebaseStorage
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
                    area.getChildren(context, storage).toCollection(zones)
                    for (zone in zones) {
                        Timber.d("    Finding in ${zone.displayName}.")
                        // Children must be loaded so `count` is fetched correctly.
                        val sectors = arrayListOf<Sector>()
                        zone.getChildren(context, storage)
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
     * Sets the quality which the images should be loaded in.
     * @author Arnau Mora
     * @since 20210724
     */
    protected abstract val imageQuality: Int

    /**
     * Stores if the children are currently being loaded.
     * @author Arnau Mora
     * @since 20210514
     */
    private var loadingChildren = false

    /**
     * Stores the currently loaded children of the [DataClass].
     * @author Arnau Mora
     * @since 20210724
     */
    protected val children = arrayListOf<A>()

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
     * Returns the children of the [DataClass]. Also downloads their images if not downloaded.
     * @author Arnau Mora
     * @since 20210313
     * @param context The context where the content is being loaded from.
     * @param storage The [FirebaseStorage] instance for loading the images from.
     */
    @WorkerThread
    fun getChildren(
        context: Context,
        storage: FirebaseStorage
    ): List<A> {
        if (loadingChildren) {
            Timber.v("Waiting for children to finish loading")
            while (loadingChildren) {
                Thread.sleep(DATACLASS_WAIT_CHILDREN_DELAY)
            }
            Timber.v("Finished loading children!")
        }

        loadingChildren = true

        for (child in children)
            if (child is DataClass<*, *> && !child.cacheImageFile(context).exists())
                child.image(context, storage, null)

        loadingChildren = false

        return children
    }

    /**
     * Adds a child to the data class.
     * @author Arnau Mora
     * @since 20210719
     */
    fun add(child: A) {
        children.add(child)
    }

    /**
     * Gets the children element at [index].
     * May throw [IndexOutOfBoundsException] if children have not been loaded.
     * @author Arnau Mora
     * @since 20210413
     * @throws IndexOutOfBoundsException When the specified [index] does not exist in [children]
     */
    @Throws(IndexOutOfBoundsException::class)
    @WorkerThread
    operator fun get(index: Int): A = children[index]

    /**
     * Finds an [DataClass] inside a list with an specific id. If it's not found, null is returned.
     * @author Arnau Mora
     * @since 20210413
     * @param objectId The id to search
     */
    operator fun get(objectId: String): A? {
        for (o in this)
            if (o.objectId == objectId)
                return o
        return null
    }

    fun has(objectId: String): Boolean {
        for (o in this)
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
    fun isEmpty(): Boolean = children.isEmpty()

    /**
     * Checks if the data class doesn't have any children
     * @author Arnau Mora
     * @since 20210411
     */
    fun isNotEmpty() = children.isNotEmpty()

    /**
     * Returns the amount of children the [DataClass] has.
     * @author Arnau Mora
     * @since 20210724
     */
    val size: Int
        get() = children.size

    /**
     * Gets the children [Iterator].
     * @author Arnau Mora
     * @since 20210413
     * @throws IllegalStateException When there are no loaded children.
     */
    @WorkerThread
    @Throws(IllegalStateException::class)
    override fun iterator(): Iterator<A> {
        if (children.isEmpty())
            throw IllegalStateException("Children is empty")
        return children.iterator()
    }

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
                storage.getReferenceFromUrl(kmzReferenceUrl!!)
                    .getFile(targetFile)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnProgressListener { Timber.v("Loading progress: ${it.bytesTransferred}/${it.totalByteCount}") }
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
     * Generates a list of [DownloadedSection].
     * @author Arnau Mora
     * @since 20210412
     * @param activity The [Activity] where the function is being ran on.
     * @param storage The [FirebaseStorage] instance to load the files from the server.
     * @param showNonDownloaded If the non-downloaded sections should be added.
     * @param progressListener A listener for the progress of the load.
     */
    @WorkerThread
    suspend fun downloadedSectionList(
        activity: Activity,
        storage: FirebaseStorage,
        showNonDownloaded: Boolean,
        progressListener: (suspend (current: Int, max: Int) -> Unit)? = null
    ): Flow<DownloadedSection> = flow {
        Timber.v("Getting downloaded sections...")
        val downloadedSectionsList = arrayListOf<DownloadedSection>()
        val children = arrayListOf<DataClassImpl>()
        getChildren(activity, storage).toCollection(children)
        for ((c, child) in children.withIndex())
            (child as? DataClass<*, *>)?.let { dataClass -> // Paths shouldn't be included
                val downloadStatus = dataClass.downloadStatus(activity, storage)
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
     * @param context The [Context] where the request is running from.
     * @param storage The [FirebaseStorage] instance to load children from the server.
     * @param progressListener A progress updater.
     * @return a matching DownloadStatus representing the Data Class' download status
     */
    @WorkerThread
    fun downloadStatus(
        context: Context,
        storage: FirebaseStorage,
        progressListener: ((current: Int, max: Int) -> Unit)? = null
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
            val children = arrayListOf<DataClassImpl>()
            getChildren(context, storage).toCollection(children)

            Timber.v("$pin Finding for a downloaded children in ${children.size}...")
            var allChildrenDownloaded = true
            var atLeastOneChildrenDownloaded = false
            for ((c, child) in children.withIndex()) {
                if (child is DataClass<*, *>) {
                    progressListener?.invoke(c, children.size)
                    val childDownloadStatus = child.downloadStatus(context, storage)
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
     * @param context The [Context] where the request is running from.
     * @param storage The [FirebaseStorage] instance to load children from the server.
     * @return If the data class has any downloaded children
     */
    @WorkerThread
    suspend fun hasAnyDownloadedChildren(
        context: Context,
        storage: FirebaseStorage
    ): Boolean {
        val children = arrayListOf<DataClassImpl>()
        getChildren(context, storage).toCollection(children)
        for (child in children)
            if (child is DataClass<*, *> &&
                child.downloadStatus(context, storage) == DownloadStatus.DOWNLOADED
            )
                return true
        return false
    }

    /**
     * Deletes the downloaded content if downloaded
     * @author Arnau Mora
     * @date 20210724
     * @param context The [Context] where the request is running from.
     * @return If the content was deleted successfully. Note: returns true if not downloaded
     */
    @WorkerThread
    fun delete(context: Context): Boolean {
        Timber.v("Deleting $objectId")
        val lst = arrayListOf<Boolean>() // Stores all the delection success statuses

        val kmzFile = kmzFile(context, true)
        if (kmzFile.exists()) {
            Timber.v("$this > Deleting \"$kmzFile\"")
            lst.add(kmzFile.deleteIfExists())
        }

        val imgFile = imageFile(context)
        if (imgFile.exists()) {
            Timber.v("$this > Deleting \"$imgFile\"")
            lst.add(imgFile.deleteIfExists())
        }

        for (child in children)
            if (child is DataClass<*, *>)
                lst.add(child.delete(context))

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
    fun size(activity: Activity): Long {
        val imgFile = imageFile(activity)

        if (!imgFile.exists()) throw NotDownloadedException(this)

        var size = imgFile.length()

        for (child in children)
            if (child is DataClass<*, *>)
                size += child.size(activity)

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
    fun imageFile(context: Context): File = File(dataDir(context), "$namespace-$objectId")

    /**
     * Returns the File that represents the image of the DataClass in cache.
     * @author Arnau Mora
     * @date 20210724
     * @param context The context to run from
     * @return The path of the image file that can be downloaded
     */
    fun cacheImageFile(context: Context): File = File(context.cacheDir, "$namespace-$objectId")

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
     * @param image This will get called once the image has been loaded.
     * @see ValueMax
     * @see ImageLoadParameters
     */
    fun image(
        context: Context,
        storage: FirebaseStorage,
        imageLoadParameters: ImageLoadParameters? = null,
        progress: ((progress: ValueMax<Long>) -> Unit)? = null,
        image: ((bitmap: Bitmap?) -> Unit)? = null
    ) {
        val downloadedImageFile = imageFile(context)
        if (downloadedImageFile.exists() && image != null) {
            Timber.d("Loading image from storage: ${downloadedImageFile.path}")
            val bmp = readBitmap(downloadedImageFile)
            image(bmp)
        } else {
            val cacheImage = cacheImageFile(context)
            var tempCacheImage: File? = null

            val successListener = OnSuccessListener<FileDownloadTask.TaskSnapshot> {
                if (image == null)
                    return@OnSuccessListener

                if (tempCacheImage != null && !cacheImage.exists()) {
                    Timber.v("$this > Compressing $tempCacheImage to $cacheImage...")
                    val bmp: Bitmap? = BitmapFactory.decodeFile(tempCacheImage!!.path)
                    val baos = ByteArrayOutputStream()
                    val compressedBitmap = bmp?.compress(WEBP_LOSSY_LEGACY, imageQuality, baos)
                    if (compressedBitmap != true)
                        Timber.e("$this > Could not compress image!")
                    else {
                        Timber.v("$this > Finished compressing image.")
                        try {
                            Timber.v("$this > Storing image to file...")
                            baos.writeTo(cacheImage.outputStream())
                            Timber.v("$this > Finished writing image.")
                            Timber.v("$this > Deleting temp cache image file...")
                            if (tempCacheImage?.deleteIfExists() == true)
                                Timber.v("$this > File deleted successfully!")
                            else
                                Timber.e("$this > Could not delete!")
                        } catch (e: IOException) {
                            Timber.e(e, "$this > Could not write compressed image.")
                        }
                    }
                }

                Timber.v("$this > Loaded image for $objectId. Decoding...")
                val bitmap = readBitmap(cacheImage)

                Timber.v("$this > Image decoded, scaling...")
                val scale = imageLoadParameters?.resultImageScale ?: 1f
                val bmp = if (scale == 1f) bitmap?.scale(scale) else bitmap

                if (bmp != null) {
                    Timber.v("$this > Setting image into imageView.")
                    image(bmp)
                } else {
                    Timber.e("$this > Could not decode image")
                    toast(context, R.string.toast_error_load_image)
                    image(null)
                }
            }

            if (cacheImage.exists()) {
                Timber.v("$this > The image file has already been cached ($cacheImage).")
                successListener.onSuccess(null)
            } else {
                tempCacheImage = File(context.cacheDir, cacheImage.nameWithoutExtension + "-temp")
                storage.getReferenceFromUrl(imageReferenceUrl)
                    .getFile(tempCacheImage)
                    .addOnSuccessListener(successListener)
                    .addOnProgressListener { snapshot ->
                        val bytesCount = snapshot.bytesTransferred
                        val totalBytes = snapshot.totalByteCount
                        progress?.invoke(ValueMax(bytesCount, totalBytes))
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "$this > Could not load DataClass ($objectId) image.")

                        image?.invoke(null)
                    }
            }
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

        image(activity, storage, imageLoadParameters, { progress ->
            progressBar?.progress = progress.percentage()
        }, { bmp -> imageView.setImageBitmap(bmp) })
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
 * @param context The [Context] where the content is being loaded from.
 * @param storage The [FirebaseStorage] reference to load files from.
 */
fun <A : DataClassImpl, B : DataClassImpl, D : DataClass<A, B>> Iterable<D>.getChildren(
    context: Context,
    storage: FirebaseStorage
): List<A> {
    val items = arrayListOf<A>()
    for (i in this)
        items.addAll(i.getChildren(context, storage))
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
