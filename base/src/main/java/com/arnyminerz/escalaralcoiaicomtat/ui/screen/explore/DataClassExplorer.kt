package com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.ui.CabinFamily
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DataClassItem
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.tooltip.Tooltip
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.google.firebase.storage.FirebaseStorage
import timber.log.Timber

@ExperimentalFoundationApi
@Composable
@ExperimentalMaterial3Api
fun MainActivity.DataClassExplorer(
    rootNavigator: NavController,
    storage: FirebaseStorage,
    arguments: Bundle,
) {
    val context = LocalContext.current
    var showErrorCard by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            val showNetworkTooltip = remember { mutableStateOf(false) }

            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // TODO: Check if this works
                            exploreViewModel.notifyNavigation()
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
                    val hasInternet by hasInternet.observeAsState()
                    if (hasInternet == false)
                        IconButton(
                            onClick = { showNetworkTooltip.value = true },
                            modifier = Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(),
                                onClickLabel = "Button action description",
                                role = Role.Button,
                                onClick = { showNetworkTooltip.value = true },
                                onLongClick = {
                                    vibrate(context, 50)
                                    showNetworkTooltip.value = true
                                },
                            ),
                        ) {
                            Image(
                                Icons.Rounded.CloudOff,
                                contentDescription = stringResource(R.string.image_desc_no_internet),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                            )
                        }

                    Tooltip(showNetworkTooltip) {
                        Text(getString(R.string.status_no_internet))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = {
                    val parentDataClass by exploreViewModel.parentDataClass.observeAsState()
                    Text(
                        text = parentDataClass?.displayName
                            ?: stringResource(R.string.status_loading),
                        fontFamily = CabinFamily
                    )
                }
            )
        }
    ) { padding ->
        val items by exploreViewModel.dataClasses.observeAsState()
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
        ) {
            // The loading indicator
            item {
                AnimatedVisibility(visible = items?.isNotEmpty() != true) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
            // The error card, just in case something goes wrong
            item {
                AnimatedVisibility(visible = showErrorCard) {
                    Card(
                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.toast_error_internal),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
            // The items
            items(items ?: emptyList()) { dataClass ->
                DataClassItem(dataClass, storage) {
                    exploreViewModel.notifyNavigation()
                    rootNavigator.navigate(dataClass.documentPath)
                }
            }
        }
    }

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
        showErrorCard = true
    }
}
