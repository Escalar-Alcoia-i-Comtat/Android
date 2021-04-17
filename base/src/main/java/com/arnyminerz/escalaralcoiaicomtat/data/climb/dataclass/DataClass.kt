package com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.ZoneActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.exception.NotDownloadedException
import com.arnyminerz.escalaralcoiaicomtat.generic.allTrue
import com.arnyminerz.escalaralcoiaicomtat.generic.awaitTask
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.generic.uiContext
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.DATACLASS_WAIT_CHILDREN_DELAY
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_COUNT
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_SECTOR_INDEX
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.view.apply
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.show
import com.arnyminerz.escalaralcoiaicomtat.worker.DOWNLOAD_QUALITY_MAX
import com.arnyminerz.escalaralcoiaicomtat.worker.DOWNLOAD_QUALITY_MIN
import com.arnyminerz.escalaralcoiaicomtat.worker.DownloadData
import com.arnyminerz.escalaralcoiaicomtat.worker.DownloadWorker
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toCollection
import timber.log.Timber
import java.io.File
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// A: List type
// B: Parent Type
abstract class DataClass<A : DataClassImpl, B : DataClassImpl>(
    val displayName: String,
    timestamp: Date,
    val imageReferenceUrl: String,
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
         * @param queryName What to search
         * @param firestore The [FirebaseFirestore] instance.
         * @return An [Intent] if the [DataClass] was found, or null.
         */
        suspend fun getIntent(
            context: Context,
            queryName: String,
            firestore: FirebaseFirestore
        ): Intent? {
            var result: Intent? = null
            Timber.d("Trying to generate intent from \"$queryName\". Searching in ${AREAS.size} areas.")
            for (area in AREAS) {
                Timber.d("  Finding in ${area.displayName}.")
                if (area.displayName.equals(queryName, true))
                    result = Intent(context, AreaActivity::class.java).apply {
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
                        zone.getChildren(firestore).toCollection(sectors)

                        if (zone.displayName.equals(queryName, true))
                            result = Intent(context, ZoneActivity::class.java).apply {
                                Timber.d("Found Zone id ${zone.objectId}!")
                                putExtra(EXTRA_AREA, area.objectId)
                                putExtra(EXTRA_ZONE, zone.objectId)
                                putExtra(EXTRA_SECTOR_COUNT, zone.count())
                            }
                        else
                            for ((counter, sector) in sectors.withIndex()) {
                                Timber.d("      Finding in ${sector.displayName}.")
                                if (sector.displayName.equals(queryName, true))
                                    result = Intent(
                                        context,
                                        SectorActivity::class.java
                                    ).apply {
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
            Timber.w("Could not generate intent")
            return result
        }
    }

    protected val innerChildren = arrayListOf<A>()

    private val pin = "${namespace}_$objectId"

    val transitionName = objectId + displayName.replace(" ", "_")

    private var loadingChildren = false

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
    suspend fun getChildren(firestore: FirebaseFirestore?): Flow<A> = flow {
        if (loadingChildren) {
            Timber.v("Waiting for children to finish loading")
            while (loadingChildren) {
                delay(DATACLASS_WAIT_CHILDREN_DELAY)
            }
            Timber.v("Finished loading children!")
        }
        if (innerChildren.isEmpty())
            if (firestore == null)
                throw IllegalStateException("There are no loaded children, and firestore is null.")
            else {
                loadingChildren = true
                loadChildren(firestore).collect {
                    innerChildren.add(it)
                    emit(it)
                }
                loadingChildren = false
            }
        else
            for (a in innerChildren)
                emit(a)
    }

    /**
     * Gets the KMZ file path.
     * @author Arnau Mora
     * @since 20210416
     * @param context The context to run from.
     */
    private fun getKmzFile(context: Context): File = File(context.cacheDir, pin)

    /**
     * Gets the KMZ file of the [Area] and stores it into [targetFile].
     * @author Arnau Mora
     * @since 20210416
     * @param storage The [FirebaseStorage] instance.
     * @param targetFile The [File] to store the KMZ at.
     * @see kmzReferenceUrl
     */
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
     * If it has never been loaded, it gets loaded from [storage]. Otherwise, it gets loaded from cache.
     * @author Arnau Mora
     * @since 20210416
     * @param context The context to run from.
     * @param storage The [FirebaseStorage] instance.
     */
    suspend fun getKmzFile(context: Context, storage: FirebaseStorage): File {
        val kmzFile = getKmzFile(context)

        if (!kmzFile.exists())
            storeKmz(storage, kmzFile)

        return kmzFile
    }

    /**
     * Checks if the DataClass is being downloaded
     * @author Arnau Mora
     * @since 20210313
     * @param context The context to check from
     * @return If the DataClass is being downloaded
     */
    @WorkerThread
    fun isDownloading(context: Context): Boolean {
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosByTag(pin).get()
        if (workInfos.isEmpty())
            return false
        var anyRunning = false
        for (workInfo in workInfos)
            when (workInfo.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> anyRunning = true
                else -> continue
            }
        return anyRunning
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
    protected abstract suspend fun loadChildren(firestore: FirebaseFirestore): Flow<A>

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
     * @param context The [Context] where the function is being ran on.
     * @param firestore The [FirebaseFirestore] instance to load the data from.
     * @param showNonDownloaded If the non-downloaded sections should be added.
     * @param progressListener A listener for the progress of the load.
     */
    @WorkerThread
    suspend fun downloadedSectionList(
        context: Context,
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
                val downloadStatus = dataClass.downloadStatus(context, firestore)
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
     * @param styleUri The Mapbox Map's [Style] uri ([Style.getUri]).
     * @param overwrite If the new data should overwrite the old one
     * @param quality The quality in which do the codification
     * @return A LiveData object with the download work info
     *
     * @throws IllegalArgumentException If the specified quality is out of bounds
     */
    @Throws(IllegalArgumentException::class)
    fun download(
        context: Context,
        styleUri: String?,
        overwrite: Boolean = true,
        quality: Int = 100
    ): LiveData<WorkInfo> {
        if (quality < DOWNLOAD_QUALITY_MIN || quality > DOWNLOAD_QUALITY_MAX)
            throw IllegalArgumentException(
                "Quality must be between $DOWNLOAD_QUALITY_MIN and $DOWNLOAD_QUALITY_MAX"
            )
        Timber.v("Downloading $namespace \"$displayName\"...")
        Timber.v("Preparing DownloadData...")
        val downloadData = DownloadData(this, styleUri, overwrite, quality)
        Timber.v("Scheduling download...")
        return DownloadWorker.schedule(context, pin, downloadData)
    }

    /**
     * Gets the DownloadStatus of the DataClass
     * @author Arnau Mora
     * @since 20210313
     * @param context The context to run from
     * @param firestore The [FirebaseFirestore] instance to load children from
     * @param progressListener A progress updater
     * @return a matching DownloadStatus representing the Data Class' download status
     */
    @WorkerThread
    suspend fun downloadStatus(
        context: Context,
        firestore: FirebaseFirestore,
        progressListener: ((current: Int, max: Int) -> Unit)? = null
    ): DownloadStatus {
        Timber.d("$namespace:$objectId Checking if downloaded")
        var result: DownloadStatus? = null
        when {
            isDownloading(context) -> result = DownloadStatus.DOWNLOADING
            else -> {
                val imageFile = imageFile(context)
                Timber.v("Checking if image file exists...")
                val imageFileExists = imageFile.exists()
                if (!imageFileExists) {
                    Timber.d("$namespace:$objectId Image file ($imageFile) doesn't exist")
                    result = DownloadStatus.NOT_DOWNLOADED
                }

                Timber.v("Getting children elements download status...")
                val children = arrayListOf<DataClassImpl>()
                getChildren(firestore).toCollection(children)
                for ((c, child) in children.withIndex()) {
                    if (child is DataClass<*, *>) {
                        progressListener?.invoke(c, children.size)
                        val childDownloadStatus = child.downloadStatus(context, firestore)
                        // If the result has not been set yet, which means the image is downloaded
                        if (result == null) {
                            // But there's a non-downloaded children
                            if (!childDownloadStatus.isDownloaded()) {
                                Timber.d("There's a non-downloaded children (${child.namespace}:${child.objectId})")
                                result = DownloadStatus.PARTIALLY
                            }
                        }
                    } else Timber.d("$namespace:$objectId Child is not DataClass")
                    // If a result has been obtained, exit the for
                    if (result != DownloadStatus.NOT_DOWNLOADED && result != null)
                        break
                }
            }
        }
        Timber.d("Finished checking download status. Result: $result")
        return result ?: DownloadStatus.DOWNLOADED
    }

    /**
     * Checks if the data class has any children that has been downloaded
     * @author Arnau Mora
     * @date 2020/09/14
     * @param context The context to run from
     * @param firestore A [FirebaseFirestore] instance to load new data from.
     *
     * @return If the data class has any downloaded children
     */
    @WorkerThread
    suspend fun hasAnyDownloadedChildren(context: Context, firestore: FirebaseFirestore): Boolean {
        val children = arrayListOf<DataClassImpl>()
        getChildren(firestore).toCollection(children)
        for (child in children)
            if (child is DataClass<*, *> &&
                child.downloadStatus(context, firestore) == DownloadStatus.DOWNLOADED
            )
                return true
        return false
    }

    /**
     * Deletes the downloaded content if downloaded
     * @author Arnau Mora
     * @date 2020/09/11
     * @param context The context to run from
     * @return If the content was deleted successfully. Note: returns true if not downloaded
     */
    suspend fun delete(context: Context): Boolean {
        Timber.v("Deleting $objectId")
        val imgFile = imageFile(context)
        val lst = arrayListOf<Boolean>()

        Timber.v("Deleting \"$imgFile\"...")
        lst.add(imgFile.deleteIfExists())
        val children = arrayListOf<DataClassImpl>()
        try {
            getChildren(null).toCollection(children)
        } catch (_: IllegalStateException) {
        }
        for (child in children)
            (child as? DataClass<*, *>)?.let {
                lst.add(it.delete(context))
            }
        return lst.allTrue()
    }

    /**
     * Gets the space that is occupied by the data class' downloaded data in the system
     * @author Arnau Mora
     * @date 2020/09/11
     * @patch 2020/09/12 - Arnau Mora: Added child space computation
     * @param context The context to run from
     * @return The size in bytes that is used by the downloaded data
     *
     * @throws NotDownloadedException If tried to get size when not downloaded
     */
    @Throws(NotDownloadedException::class)
    suspend fun size(context: Context): Long {
        val imgFile = imageFile(context)

        if (!imgFile.exists()) throw NotDownloadedException(this)

        var size = imgFile.length()

        val children = arrayListOf<DataClassImpl>()
        try {
            getChildren(null).toCollection(children)
        } catch (_: IllegalStateException) {
        }
        for (child in children)
            if (child is DataClass<*, *>)
                size += child.size(context)

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
     * Loads the image of the Data Class
     * @author Arnau Mora
     * @date 2020/09/11
     * @patch 2020/09/12 - Arnau Mora: Added function loadImage into this
     * @param context The context to run from
     * @param imageView The Image View for loading the image into
     * @param progressBar The loading progress bar
     * @param imageLoadParameters The parameters to use for loading the image
     */
    @UiThread
    suspend fun loadImage(
        context: Context,
        storage: FirebaseStorage,
        imageView: ImageView,
        progressBar: ProgressBar? = null,
        imageLoadParameters: ImageLoadParameters<Bitmap>? = null
    ) {
        if (context is Activity)
            if (context.isDestroyed)
                return Timber.e("The activity is destroyed, won't load image.")

        uiContext {
            progressBar?.show()
        }
        val scale = imageLoadParameters?.resultImageScale ?: 1f

        var imageLoadRequest = Glide.with(context)
            .asBitmap()
        val downloadedImageFile = imageFile(context)
        imageLoadRequest = if (downloadedImageFile.exists()) {
            Timber.d("Loading image from storage: ${downloadedImageFile.path}")
            imageLoadRequest
                .load(readBitmap(downloadedImageFile))
        } else {
            Timber.d("Getting image from Firebase: $imageReferenceUrl")
            val ref = storage.getReferenceFromUrl(imageReferenceUrl)
            val url = ref.downloadUrl.awaitTask()
            imageLoadRequest
                .load(url)
        }
        uiContext {
            imageLoadRequest.placeholder(uiMetadata.placeholderDrawable)
                .error(uiMetadata.errorPlaceholderDrawable)
                .fallback(uiMetadata.errorPlaceholderDrawable)
                .thumbnail(scale)
                .apply(imageLoadParameters)
                .addListener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar?.hide()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar?.hide()
                        return false
                    }
                })
                .into(imageView)
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
