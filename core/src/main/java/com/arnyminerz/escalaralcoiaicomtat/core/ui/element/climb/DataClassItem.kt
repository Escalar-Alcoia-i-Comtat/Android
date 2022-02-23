package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DownloadStatus
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.DOWNLOAD_QUALITY_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.ui.PoppinsFamily
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.DataClassItemViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.utils.humanReadableByteCountBin
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.mapsIntent
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.view.ImageLoadParameters
import java.text.SimpleDateFormat

@Composable
fun DataClassItem(
    item: DataClassImpl,
    onClick: () -> Unit
) {
    if (item is DataClass<*, *, *>) {
        val context = LocalContext.current
        val viewModel: DataClassItemViewModel = viewModel(
            factory = DataClassItemViewModel.Factory(
                context.applicationContext as Application
            )
        )

        if (item.displayOptions.downloadable)
            DownloadableDataClassItem(
                item,
                viewModel,
                onClick,
            )
        else
            NonDownloadableDataClassItem(
                item,
                viewModel,
                onClick,
            )
    } else
        PathDataClassItem(item)
}

/**
 * Displays a data class object as a path.
 * @author Arnau Mora
 * @since 20220102
 * @param dataClassImpl The data of the path
 */
@Composable
fun PathDataClassItem(dataClassImpl: DataClassImpl) {
    // TODO
    Text(
        text = "Hey! This is the contents of the path called \"${dataClassImpl.displayName}\"",
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Displays a data class object that can be downloaded. The UI is a little more complex.
 * @author Arnau Mora
 * @since 20211229
 * @param item The DataClass to display.
 * @param viewModel The View Model for doing async tasks.
 * @param onClick Will get called when the user requests to "navigate" into the DataClass.
 */
@Composable
private fun DownloadableDataClassItem(
    item: DataClass<*, *, *>,
    viewModel: DataClassItemViewModel,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    var isPartiallyDownloaded by remember { mutableStateOf(false) }

    var showDownloadInfoDialog by remember { mutableStateOf(false) }

    val downloadItem: () -> Unit = {
        viewModel.startDownloading(
            context,
            item.pin,
            item.displayName,
            quality = DOWNLOAD_QUALITY_DEFAULT
        )
    }

    Card(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(start = 8.dp, bottom = 4.dp, end = 8.dp, top = 4.dp)
            .fillMaxWidth()
    ) {
        Column {
            Row {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(.3f)
                ) {
                    item.Image(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clickable(enabled = true, role = Role.Image, onClick = onClick),
                        imageLoadParameters = ImageLoadParameters()
                            .withResultImageScale(.3f)
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1f)
                ) {
                    val childrenCount by viewModel.childrenCounter(item).observeAsState()
                    Text(
                        text = item.displayName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = PoppinsFamily,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .padding(start = 4.dp, top = 4.dp)
                            .fillMaxWidth(),
                    )
                    Text(
                        text = childrenCount?.let {
                            if (item.namespace == Zone.NAMESPACE)
                                stringResource(R.string.downloads_zones_title, it)
                            else
                                stringResource(R.string.downloads_sectors_title, it)
                        } ?: stringResource(R.string.status_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .fillMaxWidth(),
                    )
                }
                Column {
                    Button(
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier
                            .padding(end = 4.dp),
                        onClick = onClick,
                    ) {
                        Image(
                            Icons.Default.ChevronRight,
                            stringResource(R.string.action_view),
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.onTertiary
                            )
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    viewModel.addDownloadListener(item.pin) { _, _ ->
                        // TODO: Download progress should be notified
                    }

                    val downloadState by remember { viewModel.downloadStatuses[item.pin]!! }
                    isPartiallyDownloaded = downloadState.partialDownload == true
                    Button(
                        // Enable button when not downloaded, but download status is known
                        enabled = !downloadState.downloading && downloadState != DownloadStatus.UNKNOWN,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 4.dp)
                            .fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(),
                        onClick = {
                            when (downloadState) {
                                DownloadStatus.DOWNLOADED -> showDownloadInfoDialog = true
                                DownloadStatus.NOT_DOWNLOADED, DownloadStatus.PARTIALLY -> downloadItem()
                                else -> toast(context, R.string.toast_error_internal)
                            }
                        },
                    ) {
                        Icon(
                            downloadState.getActionIcon(),
                            contentDescription = stringResource(R.string.action_download),
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            text = downloadState.getText()
                        )
                    }
                }

                val location = item.location
                if (location != null)
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            modifier = Modifier
                                .padding(end = 8.dp, start = 4.dp)
                                .fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(),
                            onClick = {
                                context.launch(location.mapsIntent(markerTitle = item.displayName))
                            },
                        ) {
                            Icon(
                                Icons.Rounded.Map,
                                contentDescription = stringResource(R.string.action_view_map),
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(text = stringResource(R.string.action_view_map))
                        }
                    }
            }
        }
    }

    if (showDownloadInfoDialog)
        AlertDialog(
            onDismissRequest = { showDownloadInfoDialog = false },
            title = {
                Text(text = item.displayName)
            },
            text = {
                val downloadInfo by viewModel.downloadInfo(item).observeAsState()
                Column {
                    val format = SimpleDateFormat.getDateTimeInstance()
                    Text(
                        text = stringResource(
                            R.string.dialog_downloaded_msg,
                            downloadInfo?.let {
                                format.format(it.first)
                            } ?: stringResource(R.string.status_loading)
                        )
                    )
                    Text(
                        text = stringResource(
                            R.string.dialog_uses_storage_msg,
                            downloadInfo?.let {
                                humanReadableByteCountBin(it.second)
                            } ?: stringResource(R.string.status_loading)
                        )
                    )
                    if (isPartiallyDownloaded)
                        Text(
                            text = stringResource(
                                R.string.dialog_downloaded_partially_msg,
                                item.displayName,
                            )
                        )
                }
            },
            dismissButton = {
                if (isPartiallyDownloaded)
                    Button(onClick = { downloadItem() }) {
                        Text(text = stringResource(R.string.action_download))
                    }
                else
                    Button(onClick = { viewModel.deleteDataClass(item) }) {
                        Text(text = stringResource(R.string.action_delete))
                    }
            },
            confirmButton = {
                Button(
                    onClick = { showDownloadInfoDialog = false },
                    colors = ButtonDefaults.textButtonColors(),
                ) {
                    Text(
                        text = stringResource(R.string.action_close),
                    )
                }
            },
        )
}

/**
 * Displays a data class object that can't be downloaded. The UI is simpler, just image and name.
 * @author Arnau Mora
 * @since 20211229
 * @param item The DataClass to display.
 * @param viewModel The View Model for doing async tasks.
 * @param onClick What to do when clicked.
 */
@Composable
private fun NonDownloadableDataClassItem(
    item: DataClass<*, *, *>,
    viewModel: DataClassItemViewModel,
    onClick: (() -> Unit)? = null
) {
    val childrenCount by viewModel.childrenCounter(item).observeAsState()

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column {
            item.Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clickable {
                        onClick?.let { it() }
                    },
                imageLoadParameters = ImageLoadParameters()
                    .withResultImageScale(.3f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = item.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = childrenCount?.let {
                        stringResource(R.string.downloads_zones_title, it)
                    } ?: stringResource(R.string.status_loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
