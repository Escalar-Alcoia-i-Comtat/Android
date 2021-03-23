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
    }
    return this
}

class ImageLoadParameters<T> {
    internal var requestOptions: RequestOptions? = null
    internal var transitionOptions: TransitionOptions<*, T>? = null
    internal var thumbnailSize: Float? = null
    var resultImageScale: Float? = null

    fun withRequestOptions(requestOptions: RequestOptions?): ImageLoadParameters<T> {
        this.requestOptions = requestOptions
        return this
    }

    fun withTransitionOptions(transitionOptions: TransitionOptions<*, T>?): ImageLoadParameters<T> {
        this.transitionOptions = transitionOptions
        return this
    }

    fun withThumbnailSize(thumbnailSize: Float?): ImageLoadParameters<T> {
        this.thumbnailSize = thumbnailSize
        return this
    }

    fun withResultImageScale(scale: Float?): ImageLoadParameters<T> {
        this.resultImageScale = scale
        return this
    }
}
