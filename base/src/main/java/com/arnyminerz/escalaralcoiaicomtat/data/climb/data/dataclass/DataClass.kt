package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.dataclass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.SectorActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.ZoneActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.download.DownloadedSection
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.exception.NotDownloadedException
import com.arnyminerz.escalaralcoiaicomtat.generic.allTrue
import com.arnyminerz.escalaralcoiaicomtat.generic.deleteIfExists
import com.arnyminerz.escalaralcoiaicomtat.generic.onUiThread
import com.arnyminerz.escalaralcoiaicomtat.generic.putExtra
import com.arnyminerz.escalaralcoiaicomtat.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.shared.EXTRA_AREA
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
import com.parse.ParseObject
import com.parse.ParseQuery
import timber.log.Timber
import java.io.File
import java.util.Date

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
    override val namespace: String,
    open val childrenNamespace: String
) : DataClassImpl(objectId, namespace), Iterable<A> {
    companion object {
        /**
         * Searches in AREAS and tries to get an intent from them
         */
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
                                for ((s, sector) in zone.withIndex()) {
                                    Timber.d("      Finding in ${sector.displayName}.")
                                    if (sector.displayName.equals(queryName, true))
                                        return Intent(context, SectorActivity::class.java).apply {
                                            Timber.d("Found Sector id ${sector.objectId} at $s!")
                                            putExtra(EXTRA_AREA, area.objectId)
                                            putExtra(EXTRA_ZONE, zone.objectId)
                                            putExtra(EXTRA_SECTOR_INDEX, s)
                                        }
                                }
                        }
                    else -> Timber.w("Area is empty.")
                }
            }
            Timber.w("Could not generate intent")
            return null
        }
    }

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
            throw IllegalArgumentException("Quality must be between 1 and 100")
        return DownloadWorker.schedule(context, pin, DownloadData(this, overwrite, quality))
    }

    /**
     * Gets the DownloadStatus of the DataClass
     * @author Arnau Mora
     * @since 20210313
     * @param context The context to run from
     * @return a matching DownloadStatus representing the Data Class' download status
     */
    fun downloadStatus(context: Context): DownloadStatus {
        Timber.d("$namespace:$objectId Checking if downloaded")
        var result: DownloadStatus? = null
        when {
            isDownloading(context) -> result = DownloadStatus.DOWNLOADING
            else -> {
                val imageFile = imageFile(context)
                val imageFileExists = imageFile.exists()
                if (!imageFileExists) {
                    Timber.d("$namespace:$objectId Image file ($imageFile) doesn't exist")
                    result = DownloadStatus.NOT_DOWNLOADED
                } else for (child in children)
                    if (child is DataClass<*, *>) {
                        if (!child.downloadStatus(context)) {
                            Timber.d("There's a non-downloaded children (${child.namespace}:${child.objectId})")
                            result = DownloadStatus.NOT_DOWNLOADED
                        }
                    } else Timber.d("$namespace:$objectId Child is not DataClass")
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
            if (child is DataClass<*, *> && child.downloadStatus(context) == DownloadStatus.DOWNLOADED)
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
    fun asyncLoadImage(
        context: Context,
        imageView: ImageView,
        progressBar: ProgressBar? = null,
        imageLoadParameters: ImageLoadParameters? = null
    ) {
        if (context is Activity)
            if (context.isDestroyed)
                return Timber.e("The activity is destroyed, won't load image.")

        progressBar?.show()

        val downloadedImageFile = imageFile(context)
        if (downloadedImageFile.exists()) {
            Timber.d("Loading area image from storage: ${downloadedImageFile.path}")
            context.onUiThread {
                imageView.setImageBitmap(readBitmap(downloadedImageFile))
                progressBar?.hide()
                imageView.show()
            }
        } else {
            Timber.d("Getting image from URL ($imageUrl)")

            val scale = imageLoadParameters?.resultImageScale ?: 1f
            /*context.onUiThread {
                imageView.setImageResource(placeholderDrawable)
            }*/

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
                        progressBar?.hide()
                        /*context.onUiThread {
                            imageView.setImageResource(errorPlaceholderDrawable)
                        }*/
                        Timber.e(
                            e,
                            "Could not load image for $namespace#$objectId! Url: $imageUrl"
                        )
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
                        progressBar?.hide()
                        /*context.onUiThread {
                            if (resource == null)
                                Timber.w("Bitmap is null!")
                            else
                                imageView.setImageBitmap(resource)
                        }*/

                        return false
                    }
                })
                .apply(imageLoadParameters)
                .into(imageView)
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
        return result
    }
}
