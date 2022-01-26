package com.arnyminerz.escalaralcoiaicomtat.core.ui.element

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An indicator to show the user when the app is loading content. Automatically shows and hides
 * depending on [isLoading] with an animation.
 * @author Arnau Mora
 * @since 20210730
 * @param isLoading Whether or not the content is being loaded, this is, whether or not to show the
 * indicator.
 * @param size The size of the indicator.
 */
@ExperimentalAnimationApi
@Composable
fun LoadingIndicator(isLoading: Boolean, size: Dp = 52.dp) {
    AnimatedVisibility(visible = isLoading) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(size))
        }
    }
}