package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DataClassItem
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.storage.FirebaseStorage
import timber.log.Timber

@Composable
@ExperimentalBadgeUtils
@OptIn(ExperimentalMaterial3Api::class)
fun MainActivity.ExploreScreen(rootNavController: NavController, storage: FirebaseStorage) {
    // TODO: Map and search bar
    val areas by exploreViewModel.loadAreas().observeAsState()

    LazyColumn {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = areas == null,
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
        items(areas ?: listOf()) { area ->
            Timber.d("Displaying $area...")
            DataClassItem(area, storage) {
                rootNavController.navigate(area.documentPath)
            }
        }
    }
}
