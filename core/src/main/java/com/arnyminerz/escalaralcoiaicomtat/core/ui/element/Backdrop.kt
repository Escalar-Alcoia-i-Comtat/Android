package com.arnyminerz.escalaralcoiaicomtat.core.ui.element

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * The UI element for showing a backdrop
 * @author Arnau Mora
 * @since 20210723
 * @see <a href="https://material.io/components/backdrop">material.io</a>
 */
@Composable
@ExperimentalAnimationApi
fun Backdrop(
    expanded: Boolean,
    items: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colors.primary),
    ) {
        // Top Menu
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) { items(this) }
        }

        // Content card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) { content() }
    }
}