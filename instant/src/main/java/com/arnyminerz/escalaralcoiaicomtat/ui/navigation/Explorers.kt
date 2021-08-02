package com.arnyminerz.escalaralcoiaicomtat.ui.navigation

import android.app.Activity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil.annotation.ExperimentalCoilApi
import com.arnyminerz.escalaralcoiaicomtat.SectorView
import com.arnyminerz.escalaralcoiaicomtat.core.ui.animation.EnterAnimation
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.AreasExplorer
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.SectorsExplorer
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.ZonesExplorer

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalCoilApi
fun NavGraphBuilder.areas(activity: Activity, navController: NavController) {
    composable("Areas") {
        EnterAnimation {
            AreasExplorer(activity, navController)
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalCoilApi
fun NavGraphBuilder.zones(activity: Activity, navController: NavController) {
    composable("Areas/{areaId}") { backStackEntry ->
        val areaId = backStackEntry.arguments?.getString("areaId")
        if (areaId != null)
            EnterAnimation {
                ZonesExplorer(activity, navController, areaId)
            }
        else
            Text(text = "Could not navigate to area: $areaId")
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalCoilApi
fun NavGraphBuilder.sectors(activity: Activity, navController: NavController) {
    composable("Areas/{areaId}/Zones/{zoneId}") { backStackEntry ->
        val areaId = backStackEntry.arguments?.getString("areaId")
        val zoneId = backStackEntry.arguments?.getString("zoneId")
        if (areaId != null && zoneId != null)
            EnterAnimation {
                SectorsExplorer(activity, navController, areaId, zoneId)
            }
        else
            Text(text = "Could not navigate to zone Z/$zoneId in A/$areaId")
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalCoilApi
fun NavGraphBuilder.sector(activity: Activity) {
    composable("Areas/{areaId}/Zones/{zoneId}/Sectors/{sectorId}") { backStackEntry ->
        val areaId = backStackEntry.arguments?.getString("areaId")
        val zoneId = backStackEntry.arguments?.getString("zoneId")
        val sectorId = backStackEntry.arguments?.getString("sectorId")
        if (areaId != null && zoneId != null && sectorId != null)
            EnterAnimation {
                SectorView(activity, areaId, zoneId, sectorId)
            }
        else
            Text(text = "Could not navigate to sector S/$sectorId in Z/$zoneId in A/$areaId")
    }
}
