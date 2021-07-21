package com.arnyminerz.escalaralcoiaicomtat.instant.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun ZoomableImage(
    painter: Painter,
    modifier: Modifier = Modifier,
    enableScale: Boolean = true,
    enableRotation: Boolean = true,
    minZoom: Float = .5f,
    maxZoom: Float = 2f
) {
    val scale = remember { mutableStateOf(1f) }
    val rotationState = remember { mutableStateOf(0f) }
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, rotation ->
                    if (enableScale)
                        scale.value *= zoom
                    if (enableRotation)
                        rotationState.value += rotation
                }
                detectTapGestures(onDoubleTap = {
                    if (enableScale)
                        scale.value *= 2
                })
            }
    ) {
        Image(
            modifier = Modifier
                .align(Alignment.Center) // keep the image centralized into the Box
                .graphicsLayer(
                    // adding some zoom limits (min 50%, max 200%)
                    scaleX = maxOf(minZoom, minOf(maxZoom, scale.value)),
                    scaleY = maxOf(minZoom, minOf(maxZoom, scale.value)),
                    rotationZ = rotationState.value
                ),
            contentDescription = null,
            painter = painter
        )
    }
}
