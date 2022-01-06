package com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.SectorPage
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorPageViewModel

@Preview(showSystemUi = true)
@Composable
@ExperimentalMaterial3Api
fun SectorViewScreen(
    sectorPageViewModel: SectorPageViewModel = SectorPageViewModel.composeViewModel,
    objectId: String = "sampleObjectId",
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Placeholder")
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            // TODO: Change default values
            SectorPage(
                sectorPageViewModel,
                objectId,
                displayName = "",
                sun = 0,
                kidsApt = true,
                walkingTime = 5,
                location = null
            )
        }
    }
}
