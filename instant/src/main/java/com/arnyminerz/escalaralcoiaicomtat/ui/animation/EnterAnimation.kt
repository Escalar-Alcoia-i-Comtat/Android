package com.arnyminerz.escalaralcoiaicomtat.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
@ExperimentalAnimationApi
fun EnterAnimation(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { -100 }
        ) + fadeIn(initialAlpha = 0.7f),
        exit = slideOutVertically() + fadeOut(),
        content = content
    )
}