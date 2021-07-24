package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.app.Activity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.AreasViewModel

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun AreasExplorer(activity: Activity, navController: NavController) {
    Explorer(
        activity,
        navController,
        dataClassViewModel = AreasViewModel::class.java,
        listOf(activity.application)
    )
}
