package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.app.Activity
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun SectorExplorer(
    activity: Activity,
    navController: NavController,
    areaId: String,
    zoneId: String,
    sectorId: String
) {
    Text("Exploring area $areaId, in zone $zoneId, in sector $sectorId.")
}
