package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.app.Activity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.ZonesViewModel

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun ZonesExplorer(activity: Activity, navController: NavController, areaId: String) {
    Explorer(
        activity,
        navController,
        dataClassViewModel = ZonesViewModel::class.java,
        viewModelArguments = listOf(activity.application, areaId)
    )
}
