package com.arnyminerz.escalaralcoiaicomtat.ui.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@ExperimentalAnimationApi
fun ExpandableHeader(
    expanded: Boolean,
    headerContent: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        // Top Menu
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                content = headerContent
            )
        }

        // Content card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .animateContentSize(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            content = content
        )
    }
}
