package com.arnyminerz.escalaralcoiaicomtat.view

import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.request.RequestOptions
import timber.log.Timber

fun <T> RequestBuilder<T>.apply(imageLoadParameters: ImageLoadParameters<T>?): RequestBuilder<T> {
    imageLoadParameters?.let { ilp ->
        ilp.requestOptions?.let {
            apply(it)
            Timber.v("Applied request options")
        }
        ilp.transitionOptions?.let {
            transition(it)
            Timber.v("Applied transition options")
        }
        ilp.thumbnailSize?.let {
            thumbnail(it)
            Timber.v("Applied thumbnail size options")
        }
        if (!ilp.showPlaceholder) {
            Timber.v("Disabling placeholder")
            return this.placeholder(null)
        }
    }
    return this
}

class ImageLoadParameters<T> {
    internal var requestOptions: RequestOptions? = null
    internal var transitionOptions: TransitionOptions<*, T>? = null
    internal var thumbnailSize: Float? = null
    internal var showPlaceholder: Boolean = true
    var resultImageScale: Float? = null

    /**
     * Sets request options for loading the image
     * @author Arnau Mora
     * @since 20210323
     * @param requestOptions The options
     * @return The current instance for chaining calls
     * @see RequestOptions
     */
    fun withRequestOptions(requestOptions: RequestOptions?): ImageLoadParameters<T> {
        this.requestOptions = requestOptions
        return this
    }

    /**
     * Sets transition options for loading the image
     * @author Arnau Mora
     * @since 20210323
     * @param transitionOptions The options to apply
     * @return The current instance for chaining calls
     * @see TransitionOptions
     */
    fun withTransitionOptions(transitionOptions: TransitionOptions<*, T>?): ImageLoadParameters<T> {
        this.transitionOptions = transitionOptions
        return this
    }

    /**
     * Sets the scale of the image while it's getting loaded
     * @author Arnau Mora
     * @since 20210323
     * @param thumbnailSize The size of the image while it's getting loaded
     * @return The current instance for chaining calls
     */
    fun withThumbnailSize(thumbnailSize: Float?): ImageLoadParameters<T> {
        this.thumbnailSize = thumbnailSize
        return this
    }

    /**
     * Sets the image scale once the load has been completed
     * @author Arnau Mora
     * @since 20210323
     * @param scale The image's scale
     * @return The current instance for chaining calls
     */
    fun withResultImageScale(scale: Float?): ImageLoadParameters<T> {
        this.resultImageScale = scale
        return this
    }

    /**
     * Sets if the image's loading placeholder should be shown
     * @author Arnau Mora
     * @since 20210323
     * @param showPlaceholder If the placeholder should be shown
     * @return The current instance for chaining calls
     */
    fun setShowPlaceholder(showPlaceholder: Boolean): ImageLoadParameters<T> {
        this.showPlaceholder = showPlaceholder
        return this
    }
}
