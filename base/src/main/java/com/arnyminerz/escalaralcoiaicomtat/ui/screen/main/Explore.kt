package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DataClassItem
import com.google.android.material.badge.ExperimentalBadgeUtils
import timber.log.Timber

@Composable
@ExperimentalBadgeUtils
@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
fun MainActivity.ExploreScreen(rootNavController: NavController) {
    // TODO: Map and search bar
    val loadedAreas = exploreViewModel.loadedAreas

    LazyColumn {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = !loadedAreas.value,
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
        items(exploreViewModel.areas) { area ->
            Timber.d("Displaying $area...")
            DataClassItem(area) {
                rootNavController.navigate(area.documentPath)
            }
        }
    }
    exploreViewModel.loadAreas()
}
