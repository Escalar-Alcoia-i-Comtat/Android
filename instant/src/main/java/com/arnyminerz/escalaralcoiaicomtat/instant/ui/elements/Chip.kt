package com.arnyminerz.escalaralcoiaicomtat.instant.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Chip(label: String, contentDescription: String = "", icon: ImageVector? = null) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(50))
    ) {
        Surface(
            elevation = 1.dp,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colors.surface
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) Icon(
                    icon,
                    contentDescription,
                    tint = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                )
                Text(
                    label,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.onSurface)
                )
            }
        }
    }
}

@Composable
@Preview
fun ChipPreview() {
    Chip(label = "Demo Chip", icon = Icons.Rounded.Info)
}
