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
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.getChildren
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads.DownloadedData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.Chip
import com.arnyminerz.escalaralcoiaicomtat.core.utils.MEGABYTE
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.humanReadableByteCountBin
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
 */
@Composable
fun DownloadedDataItem(
    data: DownloadedData,
    searchSession: AppSearchSession,
    onDelete: (() -> Unit)?
) = DownloadedDataItemRaw(
    data.displayName,
    data.objectId,
    data.sizeBytes,
    data.namespace,
    data.childrenCount,
    searchSession,
    onDelete
)

@Composable
private fun DownloadedDataItemRaw(
    displayName: String,
    @ObjectId objectId: String,
    size: Long,
    namespace: String,
    childrenCount: Long,
    searchSession: AppSearchSession?,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
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
                    contentDescription = "", // TODO: Content description
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
                            childrenSectors = objectId.getChildren(searchSession, Sector.NAMESPACE)
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
                Button(
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.outlinedButtonColors(),
                ) {
                    Text(
                        text = stringResource(R.string.action_view),
                    )
                }
            }
        }
    }

    suspend fun delete() {
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

    if (showChildrenDialog)
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
                        CompressedDownloadedDataItem(item.displayName)
                    }
                }
            }
        )
}

@Preview(name = "DownloadedDataItem Preview")
@Composable
fun DownloadedDataItemPreview() {
    DownloadedDataItemRaw(
        "Zone Placeholder",
        "object",
        12 * MEGABYTE,
        Zone.NAMESPACE,
        7,
        null,
        null
    )
}
