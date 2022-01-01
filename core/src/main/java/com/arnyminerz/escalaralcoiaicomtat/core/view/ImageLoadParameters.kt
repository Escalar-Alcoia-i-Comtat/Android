package com.arnyminerz.escalaralcoiaicomtat.core.view

import android.widget.ImageView

@Suppress("unused")
class ImageLoadParameters {
    internal var showPlaceholder: Boolean = true
    var resultImageScale: Float? = null
    var scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP

    /**
     * Sets the image scale once the load has been completed
     * @author Arnau Mora
     * @since 20210323
     * @param scale The image's scale
     * @return The current instance for chaining calls
     */
    fun withResultImageScale(scale: Float?): ImageLoadParameters {
        this.resultImageScale = scale
        return this
    }

    /**
     * Sets the image resize mode.
     * @author Arnau Mora
     * @since 20210612
     * @param scaleType The [ImageView.ScaleType] to set.
     * @return The current instance for chaining calls
     */
    fun withScaleType(scaleType: ImageView.ScaleType): ImageLoadParameters {
        this.scaleType = scaleType
        return this
    }

    /**
     * Sets if the image's loading placeholder should be shown
     * @author Arnau Mora
     * @since 20210323
     * @param showPlaceholder If the placeholder should be shown
     * @return The current instance for chaining calls
     */
    fun setShowPlaceholder(showPlaceholder: Boolean): ImageLoadParameters {
        this.showPlaceholder = showPlaceholder
        return this
    }
}
