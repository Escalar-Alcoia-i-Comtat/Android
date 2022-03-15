package com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore

import android.app.Activity
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.MutableLiveData
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.ui.CabinFamily
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DataClassItem
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.tooltip.Tooltip
import com.arnyminerz.escalaralcoiaicomtat.core.utils.vibrate
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.ExploreViewModel
import com.google.android.material.badge.ExperimentalBadgeUtils

@Composable
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@ExperimentalBadgeUtils
fun Activity.DataClassExplorer(
    exploreViewModel: ExploreViewModel,
    dataClass: DataClass<*, *, *>,
    hasInternetLiveData: MutableLiveData<Boolean>,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            val showNetworkTooltip = remember { mutableStateOf(false) }

            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { onBackPressed() }
                    ) {
                        Image(
                            Icons.Rounded.ChevronLeft,
                            contentDescription = stringResource(R.string.fab_desc_back),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                },
                actions = {
                    val hasInternet by hasInternetLiveData.observeAsState()
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
                                    context.vibrate(50)
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
                    Text(
                        text = dataClass.displayName,
                        fontFamily = CabinFamily
                    )
                }
            )
        }
    ) { padding ->
        val items = exploreViewModel.children
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
        ) {
            // The loading indicator
            item {
                AnimatedVisibility(visible = items.isEmpty()) {
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
            // The items
            items(items) { item ->
                DataClassItem(item)
            }
        }
    }

    // Load the children data from dataClass
    if (dataClass is Area)
        exploreViewModel.childrenLoader(dataClass) { it.displayName }
    else
        exploreViewModel.childrenLoader(dataClass as Zone) { it.weight }
}
