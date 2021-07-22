package com.arnyminerz.escalaralcoiaicomtat.instant.ui.elements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun ZoomableImage(
    painter: Painter,
    modifier: Modifier = Modifier,
    enableScale: Boolean = true,
    enableRotation: Boolean = true,
    minZoom: Float = .5f,
    maxZoom: Float = 2f
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    var rotation by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    val scaleState: Float by animateFloatAsState(scale)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, rot ->
                    if (enableScale)
                        scale *= zoom
                    if (enableRotation)
                        rotation += rot
                }
                detectDragGestures(onDrag = { change, amount ->
                    Timber.v("Drag amount: $amount. Drag change: ${change.position}")
                    change.consumeAllChanges()
                    offsetX = amount.x
                    offsetY = amount.y
                })
                detectTapGestures(onDoubleTap = {
                    if (enableScale)
                        scale *= 2
                })
            }
    ) {
        Image(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .graphicsLayer(
                    // adding some zoom limits (min 50%, max 200%)
                    scaleX = maxOf(minZoom, minOf(maxZoom, scaleState)),
                    scaleY = maxOf(minZoom, minOf(maxZoom, scaleState)),
                    rotationZ = rotation
                ),
            contentDescription = null,
            painter = painter
        )
    }
}
