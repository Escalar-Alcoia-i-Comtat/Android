package com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.get
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AREAS
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.ZoomableImage
import timber.log.Timber

@Composable
@ExperimentalCoilApi
fun SectorExplorer(
    activity: Activity,
    navController: NavController,
    areaId: String,
    zoneId: String,
    sectorId: String
) {
    val sector = AREAS[areaId]?.get(zoneId)?.get(sectorId)
    if (sector == null)
        Text(text = "Could not load sector $sectorId")
    else {
        ZoomableImage(
            painter = rememberImagePainter(
                data = sector.cacheImageFile(activity),
                onExecute = { previous, current ->
                    Timber.v("Loaded image (${current.request.data})!")
                    true
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(.5f)
        )
    }
}
