package com.arnyminerz.escalaralcoiaicomtat.core.ui.element

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun IconButtonWithText(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    contentDescription: String = text,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(),
    ) {
        Column {
            Image(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.CenterHorizontally),
                colorFilter = ColorFilter.tint(color)
            )
            Text(
                text = text,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}
