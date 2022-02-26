package com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.ui.CabinFamily
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.SectorPage
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorPageViewModel
import com.google.accompanist.pager.ExperimentalPagerApi

/**
 * Displays the contents of Sectors.
 * @author Arnau Mora
 * @since 20220106
 * @param sectorPageViewModel An initialized [SectorPageViewModel] for doing heavy tasks and loading
 * contents asynchronously.
 * @param zone The parent [Zone] of [sector]. Will be used to get the rest of the children sectors.
 * @param sector The sector to load. Will be used to get the parent zone, and load the rest of the
 * Sectors.
 * @param childrenCount The amount of [Sector]s [zone] has.
 * @param index The currently selected item. If null, [sector] will be selected, otherwise it will
 * be the index of the sectors that are inside the Zone which contains [sector].
 */
@Composable
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalPagerApi
fun Activity.SectorViewScreen(
    sectorPageViewModel: SectorPageViewModel,
    zone: Zone,
    sector: Sector,
    childrenCount: Int,
    index: Int? = null,
) {
    val currentSector by remember { mutableStateOf(sector) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { onBackPressed() },
                    ) {
                        Icon(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* TODO */ },
                    ) {
                        Icon(
                            Icons.Rounded.List,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        text = currentSector.displayName,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = CabinFamily,
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
        ) {
            SectorPage(
                sectorPageViewModel,
                currentSector,
            )
        }
    }
}
