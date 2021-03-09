package com.arnyminerz.escalaralcoiaicomtat.view

import android.graphics.Bitmap
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.request.RequestOptions
import timber.log.Timber

fun RequestBuilder<Bitmap>.apply(imageLoadParameters: ImageLoadParameters?): RequestBuilder<Bitmap> {
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

class ImageLoadParameters {
    internal var requestOptions: RequestOptions? = null
    internal var transitionOptions: TransitionOptions<*, in Bitmap>? = null
    internal var thumbnailSize: Float? = null
    var resultImageScale: Float? = null

    fun withRequestOptions(requestOptions: RequestOptions?): ImageLoadParameters {
        this.requestOptions = requestOptions
        return this
    }

    fun withTransitionOptions(transitionOptions: TransitionOptions<*, in Bitmap>?): ImageLoadParameters {
        this.transitionOptions = transitionOptions
        return this
    }

    fun withThumbnailSize(thumbnailSize: Float?): ImageLoadParameters {
        this.thumbnailSize = thumbnailSize
        return this
    }

    fun withResultImageScale(scale: Float?): ImageLoadParameters {
        this.resultImageScale = scale
        return this
    }
}
