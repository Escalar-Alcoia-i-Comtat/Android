package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun MainActivity.DeveloperScreen() {
    val indexedDownloads by developerViewModel.indexedDownloads.observeAsState()
    val indexTree by developerViewModel.indexTree.observeAsState()
    Column {
        Row {
            Button(
                onClick = {
                    // This should be moved somewhere else
                    developerViewModel.loadIndexedDownloads()
                }
            ) {
                Text(text = "Load")
            }
            Button(
                onClick = {
                    // This should be moved somewhere else
                    developerViewModel.loadIndexTree()
                }
            ) {
                Text(text = "Index tree")
            }
        }
        LazyColumn {
            items(indexedDownloads ?: listOf()) { item ->
                ListItem {
                    Text(text = item)
                }
                Divider()
            }
        }
        Text(
            text = indexTree ?: "Index tree not generated",
            modifier = Modifier.clickable {
                launch(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Index tree")
                            putExtra(Intent.EXTRA_TEXT, indexTree)
                        },
                        "Index tree"
                    )
                )
            }
        )
    }
}
