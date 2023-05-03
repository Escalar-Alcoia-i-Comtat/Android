package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingData
import java.util.Date
import java.util.Locale

private val dateFormatter: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

@Composable
fun BlockingCard(
    blockingData: BlockingData
) {
    val blockingType = blockingData.blockingType

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = Modifier
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            )
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = stringResource(blockingType.contentDescription),
                modifier = Modifier
                    .size(36.dp)
                    .padding(start = 4.dp, top = 4.dp),
                tint = MaterialTheme.colorScheme.onError,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = 4.dp,
                        vertical = 8.dp,
                    ),
            ) {
                Text(
                    text = stringResource(blockingType.explanation),
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.labelMedium
                )
                blockingData.endDate?.let {
                    Text(
                        text = stringResource(R.string.path_blocking_end_date, dateFormatter.format(it)),
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun BlockingCard_noEndDate() {
    BlockingCard(
        BlockingData(1, "1234", "bird", null)
    )
}

@Preview
@Composable
fun BlockingCard_withEndDate() {
    BlockingCard(
        BlockingData(1, "1234", "bird", Date(1683141801000))
    )
}
