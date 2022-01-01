package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arnyminerz.escalaralcoiaicomtat.core.R

@Composable
fun CompressedDownloadedDataItem(
    displayName: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
            ) {
                Text(
                    text = displayName,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Column {
                Button(
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.textButtonColors()
                ) {
                    Image(
                        Icons.Rounded.RemoveRedEye,
                        contentDescription = stringResource(R.string.action_view),
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CompressedDownloadedDataItemPreview() {
    CompressedDownloadedDataItem(
        "Preview Sector"
    )
}
