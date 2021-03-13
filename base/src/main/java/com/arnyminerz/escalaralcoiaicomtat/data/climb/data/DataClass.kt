package com.arnyminerz.escalaralcoiaicomtat.data.climb.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import androidx.annotation.WorkerThread
import com.arnyminerz.escalaralcoiaicomtat.activity.AREAS
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_SECTOR
import com.arnyminerz.escalaralcoiaicomtat.activity.EXTRA_ZONE
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.ZoneActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.download.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.data.climb.types.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.exception.AlreadyLoadingException
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.exception.NotDownloadedException
import com.arnyminerz.escalaralcoiaicomtat.generic.*
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.storage.readBitmap
import com.arnyminerz.escalaralcoiaicomtat.view.ImageLoadParameters
import com.arnyminerz.escalaralcoiaicomtat.view.apply
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.boltsinternal.Task
import timber.log.Timber
import java.io.File
import java.util.*

/**
 * Searches in AREAS and tries to get an intent from them
 */
@ExperimentalUnsignedTypes
fun getIntent(context: Context, queryName: String): Intent? {
    Timber.d("Trying to generate intent from \"$queryName\". Searching in ${AREAS.size} areas.")
    for (area in AREAS.values) {
        Timber.d("  Finding in ${area.displayName}. It has ${area.count()} zones.")
        when {
            area.displayName.equals(queryName, true) ->
                return Intent(context, AreaActivity::class.java).apply {
                    Timber.d("Found Area id ${area.objectId}!")
                    putExtra(EXTRA_AREA, area.objectId)
                }
            area.isNotEmpty() ->
                for (zone in area) {
                    Timber.d("    Finding in ${zone.displayName}. It has ${zone.count()} sectors.")
                    if (zone.displayName.equals(queryName, true))
                        return Intent(context, ZoneActivity::class.java).apply {
                            Timber.d("Found Zone id ${zone.objectId}!")
                            putExtra(EXTRA_AREA, area.objectId)
                            putExtra(EXTRA_ZONE, zone.objectId)
                        }
                    else if (zone.isNotEmpty())
                        for (sector in zone) {
                            Timber.d("      Finding in ${sector.displayName}.")
                            if (sector.displayName.equals(queryName, true))
                                return Intent(context, SectorActivity::class.java).apply {
                                    Timber.d("Found Sector id ${sector.objectId}!")
                                    putExtra(EXTRA_AREA, area.objectId)
                                    putExtra(EXTRA_ZONE, zone.objectId)
                                    putExtra(EXTRA_SECTOR, sector.objectId)
                                }
                        }
                }
            else -> Timber.w("Area is empty.")
        }
    }
    Timber.w("Could not generate intent")
    return null
}

fun <A : ParseObject> ParseQuery<A>.fetchPinOrNetwork(
    label: String,
    shouldPin: Boolean = false,
    callback: ((objects: List<A>, error: Exception?) -> Unit)? = null
): Task<List<A>> = fromPin(label).findInBackground().continueWithTask { task ->
    val error = task.error
    val objects = task.result
    if (error != null) {
        if (error is ParseException && error.code == ParseException.CACHE_MISS) {
            Timber.w("No stored data found. Fetching from network.")
            return@continueWithTask fromNetwork().findInBackground()
        } else Timber.e(error, "Could not fetch data.")
    }
    if (objects.size <= 0) {
        Timber.w("No stored data found. Fetching from network.")
        return@continueWithTask fromNetwork().findInBackground()
    }
    Timber.d("Loading from pin...")
    return@continueWithTask task
}.continueWithTask { task ->
    callback?.invoke(task.result, task.error)
    if (shouldPin) {
        Timber.d("Pinning...")
        ParseObject.pinAll(label, task.result)
    }
    return@continueWithTask task
}

@WorkerThread
fun <A : ParseObject> ParseQuery<A>.fetchPinOrNetworkSync(
    label: String,
    shouldPin: Boolean = false
): List<A> {
    val list = arrayListOf<A>()
    fetchPinOrNetwork(label, shouldPin) { result, error ->
        error?.let { throw it }
        list.addAll(result)
    }.waitForCompletion()
    return list
}

/**
 * Fetches a pin from the Datastore, or if it's not stored, from the network.
 * @param state The current network state
 * @param label The label to fetch
 */
@Throws(NoInternetAccessException::class)
fun <A : ParseObject> ParseQuery<A>.fetchPinOrNetwork(
    state: ConnectivityProvider.NetworkState,
    label: String,
    shouldPin: Boolean = false
): List<ParseObject> {
    val result = arrayListOf<ParseObject>()
    limit = PATHS_BATCH_SIZE
    fromPin(label).findInBackground().continueWithTask { task ->
        var resultTask = task
        val error = task.error
        val objects = task.result
        if (error != null) {
            if (error is ParseException && error.code == ParseException.CACHE_MISS) {
                Timber.w("No stored data found. Fetching from network.")
                resultTask = fromNetwork().findInBackground()
            } else Timber.e(error, "Could not fetch data.")
        }
        if (objects.size <= 0) {
            Timber.w("The stored data's size is not greater than 0. Fetching from network.")
            resultTask = fromNetwork().findInBackground()
        }
        if (resultTask != task && !state.hasInternet) {
            Timber.w("Device doesn't have an Internet connection, and there's no cached data")
            throw NoInternetAccessException(
                "Device doesn't have an Internet connection, and there's no cached data"
            )
        }
        return@continueWithTask resultTask
    }.continueWithTask { task ->
        if (task.error != null) throw task.error
        result.addAll(task.result)
        if (shouldPin) {
            Timber.d("Pinning...")
            ParseObject.pinAll(label, task.result)
        }
        task
    }.waitForCompletion()
    return result
}

enum class DataClasses(val namespace: String) {
    AREA(Area.NAMESPACE),
    ZONE(Zone.NAMESPACE),
    SECTOR(Sector.NAMESPACE)
}

abstract class DataClassImpl(open val objectId: String) : Parcelable

// A: List type
// B: Parent Type
abstract class DataClass<A : DataClassImpl, B : DataClassImpl>(
    override val objectId: String,
    open val displayName: String,
    open val timestamp: Date?,
    open val imageUrl: String,
    open val kmlAddress: String?,
    @DrawableRes val placeholderDrawable: Int,
    @DrawableRes val errorPlaceholderDrawable: Int,
    open val namespace: String,
    open val childrenNamespace: String
) : DataClassImpl(objectId), Iterable<A> {
    protected val pin = "${DATA_FIX_LABEL}_${Area.NAMESPACE}_${objectId}"

    protected val innerChildren = arrayListOf<A>()

    /**
     * Returns the data classes' children. May fetch them from storage, or return the cached items
     * @author Arnau Mora
     * @since 20210313
     * @throws NoInternetAccessException If no Internet connection is available, and the children are
     * not stored in storage.
     */
    val children: List<A>
        @WorkerThread
        @Throws(NoInternetAccessException::class)
        get() {
            if (innerChildren.isEmpty())
                innerChildren.addAll(loadChildren())
            return innerChildren
        }

    fun add(item: A) {
        innerChildren.add(item)
    }

    fun addAll(vararg items: A) {
        for (item in items)
            innerChildren.add(item)
    }

    fun addAll(items: Iterable<A>) {
        for (item in items)
            innerChildren.add(item)
    }

    /**
     * Stores when a download has been started
     */
    var isDownloading = false
        private set

    operator fun get(index: Int): A = children[index]

    @WorkerThread
    @Throws(NoInternetAccessException::class)
    protected abstract fun loadChildren(): List<A>

    /**
     * Gets an object based on objectId
     * @author Arnau Mora
     * @since 20210312
     * @param objectId The id to find
     * @return The found dataclass
     * @see children
     * @throws IllegalStateException If the children's list is empty
     * @throws IndexOutOfBoundsException If the objectId was not found
     */
    @Throws(IllegalStateException::class, IndexOutOfBoundsException::class)
    operator fun get(objectId: String): A {
        if (children.isEmpty())
            throw IllegalStateException("Children is empty")
        for (child in children)
            if (child.objectId == objectId)
                return child
        throw IndexOutOfBoundsException("Could not find $objectId in children.")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DataClass<*, *>)
            return super.equals(other)
        return other.namespace == namespace && other.objectId == objectId
    }

    override fun iterator(): Iterator<A> = children.iterator()

    override fun toString(): String = displayName

    fun downloadedSectionList(): ArrayList<DownloadedSection> {
        val downloadedSectionsList = arrayListOf<DownloadedSection>()
        for (child in children)
            (child as? DataClass<*, *>)?.let { // Paths shouldn't be included
                downloadedSectionsList.add(DownloadedSection(it))
            }
        return downloadedSectionsList
    }

    /**
     * Downloads the image data of the DataClass.
     * @author Arnau Mora
     * @date 2020/09/10
     * @param context The context to run from.
     * @param overwrite If the new data should overwrite the old one
     * @param startListener This will be called when the download starts.
     * @param finishListener This will be called when the download finishes.
     * @param loadFailedListener This will be called when an error occurs during the download.
     *
     * @throws FileAlreadyExistsException If the data has already been downloaded and overwrite is false
     * @throws AlreadyLoadingException If the content is already being downloaded
     */
    @WorkerThread
    @Throws(FileAlreadyExistsException::class, AlreadyLoadingException::class)
    fun download(
        context: Context,
        overwrite: Boolean = false,
        startListener: (() -> Unit)?,
        finishListener: ((imageFile: File) -> Unit)?,
        progressUpdater: ((progress: Int, max: Int) -> Unit)?,
        loadFailedListener: (() -> Unit)?
    ) {
        // TODO: A worker should be used
        val imageFile = imageFile(context)
        if (imageFile.exists() && !overwrite)
            throw FileAlreadyExistsException(imageFile)

        Timber.v("Downloading $displayName...")
        Timber.d("Pinning data...")
        val query = ParseQuery<ParseObject>(namespace)
        val obj = query.get(objectId)
        obj.pin()

        Timber.d("Downloading image...")
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadStarted(placeholder: Drawable?) {
                    super.onLoadStarted(placeholder)
                    startListener?.invoke()
                }

                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                    Timber.v("Downloaded \"$displayName\"!")
                    imageFile.storeBitmap(bitmap)

                    var counter = 1 // Starts at 1 for representing self, that just downloaded
                    val targetCounter = fullCount()
                    if (children.isNotEmpty() && children.first() is DataClass<*, *>)
                        for (child in children)
                            (child as? DataClass<*, *>)?.download(context, overwrite, null, {
                                counter++
                                if (counter >= targetCounter) {
                                    Timber.v("Completely finished downloading \"$displayName\"")
                                    finishListener?.invoke(imageFile)
                                } else {
                                    Timber.d(
                                        "  Won't call finish listener since counter is still $counter/$targetCounter"
                                    )
                                    progressUpdater?.invoke(counter, children.size)
                                }
                            }, { _, _ -> counter++ }, null)
                    else {
                        Timber.v("Completely finished downloading \"$displayName\"")
                        finishListener?.invoke(imageFile)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    Timber.v("Completely finished downloading \"$displayName\"")
                    loadFailedListener?.invoke()
                }
            })
    }

    /**
     * Checks if the Data Class is downloaded
     * @author Arnau Mora
     * @date 2020/09/11
     * @param context The context to run from
     * @return a matching DownloadStatus representing the Data Class' download status
     */
    fun isDownloaded(context: Context): DownloadStatus {
        var result: DownloadStatus? = null
        when {
            isDownloading -> result = DownloadStatus.DOWNLOADING
            else -> {
                val imageFileExists = imageFile(context).exists()
                if (!imageFileExists)
                    result = DownloadStatus.NOT_DOWNLOADED
                else for (child in children)
                    if (child is DataClass<*, *>)
                        if (!child.isDownloaded(context))
                            result = DownloadStatus.NOT_DOWNLOADED
            }
        }
        return result ?: DownloadStatus.DOWNLOADED
    }

    /**
     * Checks if the data class has any children that has been downloaded
     * @author Arnau Mora
     * @date 2020/09/14
     * @param context The context to run from
     *
     * @return If the data class has any downloaded children
     */
    fun hasAnyDownloadedChildren(context: Context): Boolean {
        for (child in children)
            if (child is DataClass<*, *> && child.isDownloaded(context) == DownloadStatus.DOWNLOADED)
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
    fun delete(context: Context): Boolean {
        Timber.v("Deleting $objectId")
        val imgFile = imageFile(context)
        val lst = arrayListOf<Boolean>()

        Timber.d("Unpinning...")
        val query = ParseQuery<ParseObject>(namespace)
        val obj = query.get(objectId)
        obj.unpin()

        Timber.v("Deleting \"$imgFile\"...")
        lst.add(imgFile.deleteIfExists())
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
    fun size(context: Context): Long {
        val imgFile = imageFile(context)

        if (!imgFile.exists()) throw NotDownloadedException(this)

        var size = imgFile.length()

        for (child in children)
            if (child is DataClass<*, *>)
                size += child.size(context)

        Timber.v("\"$displayName\" storage usage: $size")

        return size
    }

    /**
     * Returns the amount of children the data class has
     * @author Arnau Mora
     * @date 2020/09/11
     * @return The amount of children the data class has
     */
    fun count(): Int = children.size

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
     * Checks if the data class has children
     */
    fun isEmpty(): Boolean = children.isEmpty()

    /**
     * Checks if the data class doesn't have any children
     */
    fun isNotEmpty() = !isEmpty()

    /**
     * Returns the File that represents the image of the DataClass
     * @author Arnau Mora
     * @date 2020/09/10
     * @param context The context to run from
     * @return The path of the image file that can be downloaded
     */
    fun imageFile(context: Context): File = File(dataDir(context), "$namespace-$objectId.jpg")

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
    fun asyncLoadImage(
        context: Context,
        imageView: ImageView,
        progressBar: ProgressBar? = null,
        imageLoadParameters: ImageLoadParameters? = null
    ) {
        if (context is Activity)
            if (context.isDestroyed)
                return Timber.e("The activity is destroyed, won't load image.")

        progressBar?.let { context.visibility(it, true) }

        val downloadedImageFile = imageFile(context)
        if (downloadedImageFile.exists()) {
            Timber.d("Loading area image from storage: ${downloadedImageFile.path}")
            context.onUiThread {
                imageView.setImageBitmap(readBitmap(downloadedImageFile))
                progressBar?.let { context.visibility(it, false) }
                visibility(imageView, true)
            }
        } else {
            Timber.d("Getting image from URL ($imageUrl)")

            val scale = imageLoadParameters?.resultImageScale ?: 1f
            context.onUiThread {
                imageView.setImageResource(placeholderDrawable)
            }

            Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .placeholder(placeholderDrawable)
                .error(errorPlaceholderDrawable)
                .fallback(errorPlaceholderDrawable)
                .fitCenter()
                .thumbnail(scale)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        context.onUiThread {
                            imageView.setImageResource(errorPlaceholderDrawable)
                            visibility(progressBar, false)
                        }
                        Timber.e(e, "Could not load image!")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Timber.d("Got bitmap, loading on imageView. Namespace: $namespace")
                        context.onUiThread {
                            visibility(progressBar, false)

                            if (resource == null)
                                Timber.e("Bitmap is null!")
                            else
                                imageView.setImageBitmap(resource)
                        }

                        return true
                    }
                })
                .apply(imageLoadParameters)
                .submit()
        }
    }

    override fun hashCode(): Int {
        var result = objectId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        result = 31 * result + imageUrl.hashCode()
        result = 31 * result + placeholderDrawable
        result = 31 * result + errorPlaceholderDrawable
        result = 31 * result + namespace.hashCode()
        result = 31 * result + children.hashCode()
        result = 31 * result + isDownloading.hashCode()
        return result
    }
}
