package com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.ui.CabinFamily
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DataClassItem
import timber.log.Timber

@Composable
@OptIn(ExperimentalCoilApi::class)
@ExperimentalMaterial3Api
fun MainActivity.DataClassExplorer(
    rootNavigator: NavController,
    arguments: Bundle,
) {
    val loading = exploreViewModel.loadingDataClasses
    val parentDataClass = exploreViewModel.parentDataClass.observeAsState()
    val hasInternet = this.hasInternet.observeAsState()

    // Sector:
    //   /Areas/{areaId}/Zones/{zoneId}/Sectors/{sectorId}
    // Zone:
    //   /Areas/{areaId}/Zones/{zoneId}
    // Area:
    //   /Areas/{areaId}
    if (arguments.containsKey("areaId"))
        if (arguments.containsKey("zoneId"))
            if (arguments.containsKey("sectorId"))
            // It's a sector
                exploreViewModel.loadChildren(Sector.NAMESPACE, arguments.getString("sectorId")!!)
            else
            // It's a zone
                exploreViewModel.loadChildren(Zone.NAMESPACE, arguments.getString("zoneId")!!)
        else
        // It's an area
            exploreViewModel.loadChildren(Area.NAMESPACE, arguments.getString("areaId")!!)
    else {
        // Throw an error, invalid arguments passed
        Timber.e("Invalid arguments passed: $arguments")
        Text(stringResource(R.string.toast_error_internal))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // TODO: Check if this works
                            rootNavigator.popBackStack()
                        }
                    ) {
                        Image(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = stringResource(R.string.fab_desc_back),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                },
                actions = {
                    if (hasInternet.value == false)
                        IconButton(
                            onClick = { /* TODO */ }
                        ) {
                            Image(
                                Icons.Rounded.CloudOff,
                                contentDescription = stringResource(R.string.image_desc_no_internet),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                            )
                        }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = {
                    Text(
                        text = parentDataClass.value?.displayName
                            ?: stringResource(R.string.status_loading),
                        fontFamily = CabinFamily
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
            if (loading.value)
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
            else
                LazyColumn {
                    items(exploreViewModel.dataClasses) { dataClass ->
                        DataClassItem(dataClass)
                    }
                }
        }
    }
}
