package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import androidx.appsearch.app.AppSearchSession
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.getChildren
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.Chip
import com.arnyminerz.escalaralcoiaicomtat.core.utils.MEGABYTE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.humanReadableByteCountBin
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.then
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import kotlinx.coroutines.launch

/**
 * Used for displaying the contents of [DownloadedData].
 * @author Arnau Mora
 * @since 20220101
 * @param data The [DownloadedData] to display.
 * @param searchSession For requesting deletions.
 * @param onDelete Will get called when the data is deleted.
 * @throws IllegalArgumentException When the [data] is not a [Zone] or [Sector].
 */
@Composable
@Throws(IllegalArgumentException::class)
fun DownloadedDataItem(
    data: DownloadedData,
    searchSession: AppSearchSession,
    dataClassActivity: Class<*>,
    onDelete: (() -> Unit)?
) = when (data.namespace) {
    // Area is not downloadable
    Zone.NAMESPACE -> DownloadedDataItemRaw<Zone, ZoneData>(
        data.displayName,
        data.objectId,
        data.sizeBytes,
        data.namespace,
        data.childrenCount,
        searchSession,
        dataClassActivity,
        onDelete
    )
    Sector.NAMESPACE -> DownloadedDataItemRaw<Sector, SectorData>(
        data.displayName,
        data.objectId,
        data.sizeBytes,
        data.namespace,
        data.childrenCount,
        searchSession,
        dataClassActivity,
        onDelete
    )
    else -> throw IllegalArgumentException("Only Zones and Sectors can be displayed as downloaded.")
}

@Composable
private inline fun <A : DataClass<*, *, *>, reified B : DataRoot<A>> DownloadedDataItemRaw(
    displayName: String,
    @ObjectId objectId: String,
    size: Long,
    @Namespace namespace: String,
    childrenCount: Long,
    searchSession: AppSearchSession?,
    dataClassActivity: Class<*>,
    noinline onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    var viewButtonEnabled by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChildrenDialog by remember { mutableStateOf(false) }
    var childrenSectors by remember { mutableStateOf(listOf<Sector>()) }
    var loadingChildren by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp)
            ) {
                Image(
                    Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.image_desc_downloaded_marker),
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                )
                Text(
                    text = displayName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Chip(
                    text = when (namespace) {
                        Zone.NAMESPACE -> stringResource(R.string.zone_title)
                        Sector.NAMESPACE -> stringResource(R.string.sector_title)
                        else -> "N/A"
                    },
                    modifier = Modifier.padding(4.dp),
                )
                Chip(
                    text = humanReadableByteCountBin(size),
                    modifier = Modifier.padding(4.dp),
                )
                // Children chip
                if (childrenCount > 0 && searchSession != null)
                    Chip(
                        text = stringResource(R.string.downloads_sectors_title, childrenCount),
                        enabled = !loadingChildren,
                        modifier = Modifier.padding(4.dp),
                    ) {
                        loadingChildren = true
                        doAsync {
                            childrenSectors =
                                objectId.getChildren(searchSession, Sector.NAMESPACE) {
                                    it.displayName
                                }
                            loadingChildren = false
                            showChildrenDialog = true
                        }
                    }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp),
            ) {
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.textButtonColors(),
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                    )
                }
                if (searchSession != null)
                    Button(
                        enabled = viewButtonEnabled,
                        onClick = {
                            viewButtonEnabled = false
                            doAsync {
                                val intent = DataClass.getIntent<A, B>(
                                    context,
                                    dataClassActivity,
                                    searchSession,
                                    namespace,
                                    objectId
                                )
                                uiScope.launch {
                                    viewButtonEnabled = true
                                    if (intent != null)
                                        context.launch(intent)
                                    else
                                        toast(context, R.string.toast_error_internal)
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(
                            text = stringResource(R.string.action_view),
                        )
                    }
            }
        }
    }

    val delete: suspend () -> Unit = {
        val deleted = if (namespace == Zone.NAMESPACE)
            DataClass
                .delete<Sector>(context, searchSession!!, namespace, objectId)
        else
            DataClass
                .delete<Path>(context, searchSession!!, namespace, objectId)
        deleted.then {
            // DataClass deleted correctly
            uiScope.launch {
                toast(context, R.string.toast_deleted)
            }
            onDelete?.let { it() }
        } ?: run {
            // Error while deleting
            uiScope.launch {
                toast(context, R.string.toast_error_internal)
            }
        }
    }

    if (showDeleteDialog)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(text = stringResource(R.string.downloads_delete_dialog_title))
            },
            text = {
                Text(text = stringResource(R.string.downloads_delete_dialog_msg, displayName))
            },
            confirmButton = {
                Button(
                    enabled = searchSession != null,
                    onClick = {
                        if (searchSession == null)
                            return@Button
                        doAsync {
                            // Delete the DataClass
                            delete()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(
                        text = stringResource(R.string.action_delete)
                    )
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = stringResource(R.string.action_cancel)
                    )
                }
            }
        )

    if (showChildrenDialog && searchSession != null)
        AlertDialog(
            onDismissRequest = {
                showChildrenDialog = false
            },
            confirmButton = {
                Button(
                    onClick = { showChildrenDialog = false },
                    colors = ButtonDefaults.textButtonColors(),
                ) {
                    Text(text = stringResource(R.string.action_close))
                }
            },
            title = {
                Text(stringResource(R.string.downloads_sector_dialog_title))
            },
            text = {
                LazyColumn {
                    items(childrenSectors) { item ->
                        CompressedDownloadedDataItem(
                            item.displayName,
                            item.objectId,
                            searchSession,
                            dataClassActivity,
                        )
                    }
                }
            }
        )
}

@Preview(name = "DownloadedDataItem Preview")
@Composable
fun DownloadedDataItemPreview() {
    DownloadedDataItemRaw<Zone, ZoneData>(
        "Zone Placeholder",
        "object",
        12 * MEGABYTE,
        Zone.NAMESPACE,
        7,
        null,
        Void::class.java,
        null
    )
}
