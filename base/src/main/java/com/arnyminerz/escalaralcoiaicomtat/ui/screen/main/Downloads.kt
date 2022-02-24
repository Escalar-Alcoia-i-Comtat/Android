package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.Chip
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DownloadedDataItem
import timber.log.Timber

@Composable
@ExperimentalMaterial3Api
fun MainActivity.DownloadsScreen() {
    val downloads by downloadsViewModel.downloads.observeAsState()
    val sizeString by remember { downloadsViewModel.sizeString }
    LazyColumn {
        item {
            Card(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.downloads_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp)
                            .fillMaxWidth()
                    ) {
                        Timber.i("Size: $sizeString")
                        Chip(stringResource(R.string.downloads_size, sizeString))
                    }
                }
            }
        }
        items(downloads ?: emptyList()) { data ->
            val isParentDownloaded = data.second
            if (!isParentDownloaded)
                DownloadedDataItem(data.first, app.searchSession, DataClassActivity::class.java) {
                    // This gets called when data gets deleted
                    downloadsViewModel.loadDownloads()
                }
        }
    }
    downloadsViewModel.loadDownloads()
}
