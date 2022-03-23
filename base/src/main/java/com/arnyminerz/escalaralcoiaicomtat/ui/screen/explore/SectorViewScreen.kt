package com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.ui.CabinFamily
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.SectorPage
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorPageViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Displays the contents of Sectors.
 * @author Arnau Mora
 * @since 20220106
 * @param sectorPageViewModel An initialized [SectorPageViewModel] for doing heavy tasks and loading
 * contents asynchronously.
 * @param zone The [Zone] that contains the children to be displayed.
 * @param childrenCount The amount of children the [zone] has.
 * @param maximized Stores whether or not the image is maximized.
 * @param index The index to currently display.
 * @param indexInterface For updating the activity's stored index value.
 */
@Composable
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalPagerApi
fun Activity.SectorViewScreen(
    sectorPageViewModel: SectorPageViewModel,
    zone: Zone,
    childrenCount: Int,
    maximized: MutableState<Boolean>,
    index: Int?,
    indexInterface: (index: Int) -> Unit,
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(index!!)

    var sectors by remember { mutableStateOf(emptyList<Sector>()) }
    var currentSector by remember { mutableStateOf<Sector?>(null) }
    LaunchedEffect(this) {
        sectors = zone.getChildren(context) { it.weight }
        currentSector = sectors[index]
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            indexInterface(page)
            if (sectors.isNotEmpty())
                currentSector = sectors[page]
        }
    }

    var showSectorsDialog by remember { mutableStateOf(false) }

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
                        onClick = { showSectorsDialog = true },
                    ) {
                        Icon(
                            Icons.Rounded.List,
                            contentDescription = stringResource(R.string.action_view_sectors),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        text = currentSector?.displayName
                            ?: stringResource(R.string.status_loading),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = CabinFamily,
                    )
                }
            )
        }
    ) { padding ->
        if (showSectorsDialog)
            AlertDialog(
                onDismissRequest = { showSectorsDialog = false },
                confirmButton = {
                    TextButton(onClick = { showSectorsDialog = false }) {
                        Text(text = stringResource(R.string.action_close))
                    }
                },
                title = {
                    Text(stringResource(R.string.sector_title))
                },
                text = {
                    LazyColumn {
                        itemsIndexed(sectors) { listIndex, sector ->
                            ListItem(
                                modifier = Modifier
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(listIndex)
                                        }
                                        showSectorsDialog = false
                                    },
                                icon = {
                                    Icon(
                                        if (index == listIndex)
                                            Icons.Rounded.RadioButtonChecked
                                        else
                                            Icons.Rounded.RadioButtonUnchecked,
                                        sector.displayName
                                    )
                                }
                            ) {
                                Text(sector.displayName)
                            }
                        }
                    }
                },
            )

        if (currentSector != null)
            HorizontalPager(
                count = childrenCount,
                state = pagerState,
                modifier = Modifier.padding(padding),
                userScrollEnabled = !maximized.value,
            ) {
                Timber.d("Rendering Sector...")
                SectorPage(
                    sectorPageViewModel,
                    currentSector!!,
                    maximized,
                )
            }
        else
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
    }
}
