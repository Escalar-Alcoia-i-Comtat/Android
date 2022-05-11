package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.android.volley.Request
import com.android.volley.VolleyError
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.network.VolleySingleton
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_CHILDREN_COUNT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.REST_API_DOWNLOAD_ENDPOINT
import com.arnyminerz.escalaralcoiaicomtat.core.utils.InputStreamVolleyRequest
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.core.utils.storage.dataDir
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.parcelize.IgnoredOnParcel
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import java.io.File
import java.io.Serializable
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
    open val displayOptions: DataClassDisplayOptions,
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
        private fun imageFile(context: Context, namespace: Namespace, objectId: String): File? =
            dataDir(context)?.let { File(it, imageName(namespace, objectId, null)) }

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
    suspend fun isEmpty(context: Context): Boolean = getCount(context) <= 0

    /**
     * Checks if the data class doesn't have any children
     * @author Arnau Mora
     * @since 20210411
     */
    @WorkerThread
    suspend fun isNotEmpty(context: Context): Boolean = getCount(context) > 0

    /**
     * Returns the amount of children the [DataClass] has.
     * @author Arnau Mora
     * @since 20210724
     */
    @WorkerThread
    @Deprecated("Renamed to getCount", replaceWith = ReplaceWith("getCount(context)"))
    suspend fun getSize(context: Context): Int =
        getChildren(context) { it.objectId }.size

    /**
     * Returns the amount of children the [DataClass] has.
     * @author Arnau Mora
     * @since 20220515
     */
    @WorkerThread
    suspend fun getCount(context: Context): Int =
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
     * Returns the File that represents the image of the DataClass
     * @author Arnau Mora
     * @date 2020/09/10
     * @param context The context to run from
     * @return The path of the image file that can be downloaded
     */
    fun imageFile(context: Context): File? =
        Companion.imageFile(context, namespace, objectId)

    /**
     * Checks if the [DataClass] has a stored [downloadUrl].
     * @author Arnau Mora
     * @since 20210722
     */
    fun hasStorageUrl() = downloadUrl != null

    @Composable
    fun Image(
        modifier: Modifier = Modifier,
        imageLoadParameters: ImageLoadParameters? = null,
        isPlaceholder: Boolean = false,
        onFinishLoading: () -> Unit
    ) {
        if (isPlaceholder)
            androidx.compose.foundation.Image(
                painterResource(displayOptions.placeholderDrawable),
                contentDescription = "Placeholder image",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = modifier,
            )
        else {
            val context = LocalContext.current

            val downloadedImageFile = imageFile(context)

            val scale = imageLoadParameters?.resultImageScale ?: 1f
            val overrideSize = imageLoadParameters?.overrideSize

            // If image is downloaded, load it
            val image = when {
                downloadedImageFile?.exists() == true -> downloadedImageFile
                else -> "$REST_API_DOWNLOAD_ENDPOINT$imagePath"
            }

            GlideImage(
                imageModel = image,
                requestOptions = {
                    RequestOptions
                        .placeholderOf(displayOptions.placeholderDrawable)
                        .error(displayOptions.placeholderDrawable)
                        .downsample(DownsampleStrategy.CENTER_OUTSIDE)
                        .encodeQuality(imageQuality)
                        .sizeMultiplier(scale)
                        .let {
                            // Override size if requested
                            if (overrideSize != null)
                                it.override(overrideSize.first, overrideSize.second)
                            else
                                it
                        }
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                },
                requestListener = object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean = false

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        onFinishLoading()
                        return false
                    }
                },
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = modifier,
            )
        }
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
