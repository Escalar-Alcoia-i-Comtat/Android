package com.arnyminerz.escalaralcoiaicomtat.core.ui.element

import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

/**
 * Creates an Image composable that can be zoomed in and out.
 * @author Arnau Mora
 * @since 20220118
 * @param imageModel The data to load into the image through Glide.
 * @param contentDescription The description of the image for accessibility.
 * @param modifier Modifiers to apply to the image component.
 * @param minScale The minimum scale that can be applied to the image.
 * @param maxScale The maximum scale that can be applied to the image.
 * @param placeholderDrawable The drawable that will be displayed when the image is being loaded.
 * @param errorDrawable The drawable that will be displayed when the image could not be loaded.
 * @param encodeQuality The quality in which the image will be encoded.
 * @param sizeMultiplier The size multiplier for scaling the image.
 * @see <a href="https://stackoverflow.com/a/69782530/5717211">StackOverflow</a>
 */
@Composable
fun ZoomableImage(
    imageModel: Any?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    minScale: Float = .5f,
    maxScale: Float = 3f,
    @DrawableRes placeholderDrawable: Int = R.drawable.ic_tall_placeholder,
    @DrawableRes errorDrawable: Int = placeholderDrawable,
    @IntRange(from = 0L, to = 100L) encodeQuality: Int = 85,
    @FloatRange(from = 0.0, to = 1.0) sizeMultiplier: Float = 1f,
) {
    var loadingImage by remember { mutableStateOf(true) }

    ZoomableBox(
        minScale = minScale,
        maxScale = maxScale,
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) { (scale, offsetX, offsetY) ->
        GlideImage(
            imageModel = { imageModel },
            imageOptions = ImageOptions(
                contentDescription = contentDescription,
                contentScale = contentScale,
            ),
            requestOptions = {
                RequestOptions
                    .placeholderOf(placeholderDrawable)
                    .error(errorDrawable)
                    .downsample(DownsampleStrategy.CENTER_OUTSIDE)
                    .encodeQuality(encodeQuality)
                    .sizeMultiplier(sizeMultiplier)
            },
            requestListener = {
                object : RequestListener<Any> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Any>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingImage = false
                        return false
                    }

                    override fun onResourceReady(
                        resource: Any?,
                        model: Any?,
                        target: Target<Any>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingImage = false
                        return false
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .placeholder(
                    loadingImage,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    highlight = PlaceholderHighlight.fade(
                        MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = .8f),
                    ),
                ),
        )
    }
}
