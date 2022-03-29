package com.arnyminerz.escalaralcoiaicomtat.core.view

import android.widget.ImageView
import androidx.compose.ui.unit.Dp
import com.arnyminerz.escalaralcoiaicomtat.core.utils.px

@Suppress("unused")
class ImageLoadParameters {
    var showPlaceholder: Boolean = true
    var resultImageScale: Float? = null
    var scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP

    /**
     * Can be set to any value to override the set image size.
     * @author Arnau Mora
     * @since 20220329
     */
    var overrideSize: Pair<Int, Int>? = null

    /**
     * Returns [resultImageScale] as an int, this is, for example, if [resultImageScale] is 0.5,
     * which means the image should be half the resolution, this will return 2.
     * @author Arnau Mora
     * @since 20220104
     */
    val resultImageSampleSize: Int
        get() = resultImageScale?.let {
            (1 / it).toInt()
        } ?: 1

    /**
     * Sets the image scale once the load has been completed
     * @author Arnau Mora
     * @since 20210323
     * @param scale The image's scale
     * @return The current instance for chaining calls
     */
    fun withResultImageScale(scale: Float?): ImageLoadParameters = also {
        it.resultImageScale = scale
    }

    /**
     * Sets the image resize mode.
     * @author Arnau Mora
     * @since 20210612
     * @param scaleType The [ImageView.ScaleType] to set.
     * @return The current instance for chaining calls
     */
    fun withScaleType(scaleType: ImageView.ScaleType): ImageLoadParameters = also {
        it.scaleType = scaleType
    }

    /**
     * Sets the value of [overrideSize] and returns itself.
     * @author Arnau Mora
     * @since 20220329
     * @param width The new target width to override.
     * @param height The new target height to override.
     */
    fun withSize(width: Int, height: Int): ImageLoadParameters = also {
        it.overrideSize = width to height
    }

    /**
     * Sets the value of [overrideSize] and returns itself.
     * @author Arnau Mora
     * @since 20220329
     * @param width The new target width to override.
     * @param height The new target height to override.
     */
    fun withSize(width: Dp, height: Dp): ImageLoadParameters = also {
        it.overrideSize = width.px.toInt() to height.px.toInt()
    }

    /**
     * Sets if the image's loading placeholder should be shown
     * @author Arnau Mora
     * @since 20210323
     * @param showPlaceholder If the placeholder should be shown
     * @return The current instance for chaining calls
     */
    fun setShowPlaceholder(showPlaceholder: Boolean): ImageLoadParameters = also {
        it.showPlaceholder = showPlaceholder
    }
}
