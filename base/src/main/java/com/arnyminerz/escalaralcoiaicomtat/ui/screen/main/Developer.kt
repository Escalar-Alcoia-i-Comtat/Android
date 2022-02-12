package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.arnyminerz.escalaralcoiaicomtat.activity.IntroActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate

@Composable
@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
fun MainActivity.DeveloperScreen() {
    val indexedDownloads by developerViewModel.indexedDownloads.observeAsState()
    val indexTree by developerViewModel.indexTree.observeAsState()
    Column {
        Row {
            Button(
                onClick = {
                    // This should be moved somewhere else
                    toast("Loading downloads...")
                    developerViewModel.loadIndexedDownloads()
                },
                modifier = Modifier.combinedClickable(
                    onClick = {

                    },
                    onLongClick = {
                        vibrate(this@DeveloperScreen, 50)
                        developerViewModel.indexedDownloads.postValue(emptyList())
                    }
                )
            ) {
                Text(text = "Load downloads")
            }
            Button(
                onClick = {
                    // This should be moved somewhere else
                    developerViewModel.loadIndexTree()
                },
                modifier = Modifier.combinedClickable(
                    onClick = {

                    },
                    onLongClick = {
                        vibrate(this@DeveloperScreen, 50)
                        developerViewModel.indexTree.postValue("")
                    }
                )
            ) {
                Text(text = "Index tree")
            }
            Button(
                onClick = {
                    doAsync {
                        PreferencesModule.systemPreferencesRepository
                            .markIntroAsShown(false)
                        uiContext {
                            launch(IntroActivity::class.java)
                        }
                    }
                }
            ) {
                Text(text = "Show Intro")
            }
        }
        LazyColumn {
            items(indexedDownloads ?: emptyList()) { item ->
                ListItem {
                    Text(text = item)
                }
                Divider()
            }
        }
        val verticalScrollState = rememberScrollState()
        Text(
            text = indexTree ?: "Index tree not generated",
            modifier = Modifier
                .clickable {
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
                .verticalScroll(verticalScrollState)
        )
    }
}
