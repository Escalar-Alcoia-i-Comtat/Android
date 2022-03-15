package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UpdaterSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.Chip
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DownloadedDataItem
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.StorageViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = text,
        fontWeight = fontWeight,
        modifier = modifier
            .border(1.dp, Color.Black)
            .weight(weight)
            .padding(8.dp)
    )
}

/**
 * Shows the updates available card.
 * @author Arnau Mora
 * @since 20220315
 * @param viewModel The [StorageViewModel] instance for loading data.
 */
@Composable
@ExperimentalMaterialApi
private fun UpdatesCard(viewModel: StorageViewModel) {
    val context = LocalContext.current
    val updatesAvailable by UpdaterSingleton.getInstance()
        .updateAvailableObjects
        .observeAsState(emptyList())

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.updates_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                .weight(1f)
        )
        if (updatesAvailable.isNotEmpty())
            Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.textButtonColors()
            ) {
                Text(text = stringResource(R.string.action_update_all))
            }
    }
    if (updatesAvailable.isNotEmpty()) {
        LazyColumn {
            items(updatesAvailable) { updateData ->
                var showInfoDialog by remember { mutableStateOf(false) }

                if (showInfoDialog)
                    AlertDialog(
                        onDismissRequest = { showInfoDialog = false },
                        confirmButton = {
                            Button(onClick = { showInfoDialog = false }) {
                                Text(text = "Hide")
                            }
                        },
                        text = {
                            Box(modifier = Modifier.fillMaxSize()) {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                    ) {
                                        // header
                                        item {
                                            Row(Modifier.background(Color.Gray)) {
                                                TableCell(
                                                    text = "Key",
                                                    weight = 1f,
                                                    modifier = Modifier
                                                        .width(150.dp),
                                                )
                                                TableCell(
                                                    text = "Local",
                                                    weight = 1f,
                                                    modifier = Modifier
                                                        .width(150.dp),
                                                )
                                                TableCell(
                                                    text = "Server",
                                                    weight = 1f,
                                                    modifier = Modifier
                                                        .width(150.dp),
                                                )
                                            }
                                        }
                                        // rows
                                        val dialogItems = updateData.localDisplayMap
                                        items(dialogItems.toList()) { (key, value) ->
                                            Row(Modifier.fillMaxWidth()) {
                                                val local = value.toString()
                                                    .let {
                                                        if (it.isDigitsOnly() && it.length == 13)
                                                            SimpleDateFormat(
                                                                "yyyy-MM-dd HH:mm:ss",
                                                                Locale.getDefault()
                                                            ).format(Date(it.toLong()))
                                                        else
                                                            it
                                                    }
                                                val server = updateData.serverDisplayMap
                                                    .getOrDefault(key, "<null>")
                                                    .toString()
                                                    .let {
                                                        if (it.isDigitsOnly() && it.length == 13)
                                                            SimpleDateFormat(
                                                                "yyyy-MM-dd HH:mm:ss",
                                                                Locale.getDefault()
                                                            ).format(Date(it.toLong()))
                                                        else
                                                            it
                                                    }
                                                val fontWeight = if (local == server)
                                                    FontWeight.Normal
                                                else FontWeight.Bold

                                                TableCell(
                                                    text = key,
                                                    weight = 1f,
                                                    modifier = Modifier
                                                        .width(150.dp),
                                                )
                                                TableCell(
                                                    text = local,
                                                    weight = 1f,
                                                    fontWeight = fontWeight,
                                                    modifier = Modifier
                                                        .width(150.dp),
                                                )
                                                TableCell(
                                                    text = server,
                                                    weight = 1f,
                                                    fontWeight = fontWeight,
                                                    modifier = Modifier
                                                        .width(150.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = updateData.localHash.toString(),
                                    modifier = Modifier.fillMaxWidth(.5f),
                                )
                                Text(
                                    text = updateData.serverHash.toString(),
                                    modifier = Modifier.fillMaxWidth(.5f),
                                )
                            }
                        },
                    )

                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    trailing = {
                        var buttonEnabled by remember { mutableStateOf(true) }

                        Row {
                            if (BuildConfig.DEBUG)
                                IconButton(onClick = { showInfoDialog = true }) {
                                    Icon(Icons.Rounded.Info, "Info")
                                }

                            IconButton(
                                onClick = {
                                    buttonEnabled = false
                                    viewModel.update(updateData)
                                },
                                enabled = buttonEnabled,
                            ) {
                                Icon(
                                    Icons.Rounded.Download,
                                    contentDescription = stringResource(R.string.action_download)
                                )
                            }
                        }
                    }
                ) {
                    BadgedBox(
                        badge = {
                            if (updateData.localHash == 0)
                                Badge { Text(text = stringResource(R.string.badge_new)) }
                        }
                    ) {
                        Text(
                            text = updateData.displayName,
                            modifier = Modifier
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    } else
        Text(
            text = stringResource(R.string.updates_no_update_available),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(8.dp)
        )
}

@Composable
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
fun MainActivity.StorageScreen() {
    val downloads by storageViewModel.downloads.observeAsState()
    val sizeString by remember { storageViewModel.sizeString }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                UpdatesCard(storageViewModel)
            }
        }

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
                    DownloadedDataItem(
                        data.first,
                        app.searchSession,
                        DataClassActivity::class.java
                    ) {
                        // This gets called when data gets deleted
                        storageViewModel.loadDownloads()
                    }
            }
        }
    }

    storageViewModel.loadDownloads()
}
