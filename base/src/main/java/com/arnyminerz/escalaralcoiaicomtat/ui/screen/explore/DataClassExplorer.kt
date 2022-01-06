package com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore

import android.app.Activity
import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.MutableLiveData
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.DataClassActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_CHILDREN_COUNT
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_INDEX
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_PARENT
import com.arnyminerz.escalaralcoiaicomtat.core.ui.CabinFamily
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DataClassItem
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.tooltip.Tooltip
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.device.vibrate
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.ExploreViewModel
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.storage.FirebaseStorage

@Composable
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@ExperimentalBadgeUtils
fun Activity.DataClassExplorer(
    exploreViewModel: ExploreViewModel,
    storage: FirebaseStorage,
    dataClass: DataClass<*, *>,
    hasInternet: MutableLiveData<Boolean>,
) {
    val context = LocalContext.current
    val childrenLoader = exploreViewModel.childrenLoader(dataClass)

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
                    Text(
                        text = dataClass.displayName,
                        fontFamily = CabinFamily
                    )
                }
            )
        }
    ) { padding ->
        val items by remember { childrenLoader }
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
            itemsIndexed(items) { i, item ->
                DataClassItem(item, storage) {
                    launch(DataClassActivity::class.java) {
                        putExtra(EXTRA_DATACLASS, item as Parcelable)
                        putExtra(EXTRA_PARENT, dataClass as Parcelable)
                        putExtra(EXTRA_CHILDREN_COUNT, items.size)
                        putExtra(EXTRA_INDEX, i)
                    }
                }
            }
        }
    }
}
