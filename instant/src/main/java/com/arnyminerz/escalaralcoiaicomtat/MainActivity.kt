package com.arnyminerz.escalaralcoiaicomtat

import android.app.Activity
import android.app.assist.AssistContent
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChildFriendly
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.WbShade
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.firebase.dataCollectionSetUp
import com.arnyminerz.escalaralcoiaicomtat.core.shared.AFTERNOON
import com.arnyminerz.escalaralcoiaicomtat.core.shared.ALL_DAY
import com.arnyminerz.escalaralcoiaicomtat.core.shared.MORNING
import com.arnyminerz.escalaralcoiaicomtat.core.shared.NO_SUN
import com.arnyminerz.escalaralcoiaicomtat.core.shared.currentUrl
import com.arnyminerz.escalaralcoiaicomtat.core.ui.viewmodel.SectorViewModel
import com.arnyminerz.escalaralcoiaicomtat.shared.APP_TYPE_PROP
import com.arnyminerz.escalaralcoiaicomtat.shared.STATUS_INSTALLED
import com.arnyminerz.escalaralcoiaicomtat.shared.STATUS_INSTANT
import com.arnyminerz.escalaralcoiaicomtat.ui.elements.Chip
import com.arnyminerz.escalaralcoiaicomtat.ui.elements.ExpandableHeader
import com.arnyminerz.escalaralcoiaicomtat.ui.elements.InstallButton
import com.arnyminerz.escalaralcoiaicomtat.ui.elements.LoadingIndicator
import com.arnyminerz.escalaralcoiaicomtat.ui.elements.ZoomableImage
import com.arnyminerz.escalaralcoiaicomtat.ui.navigation.areas
import com.arnyminerz.escalaralcoiaicomtat.ui.navigation.sector
import com.arnyminerz.escalaralcoiaicomtat.ui.navigation.sectors
import com.arnyminerz.escalaralcoiaicomtat.ui.navigation.zones
import com.arnyminerz.escalaralcoiaicomtat.ui.theme.EscalarAlcoiaIComtatTheme
import com.arnyminerz.escalaralcoiaicomtat.utils.searchNavigation
import com.google.android.gms.instantapps.InstantApps
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data

        instantInfoSetup()
        dataCollectionSetUp()

        val auth = Firebase.auth
        if (auth.currentUser == null)
            auth.signInAnonymously()

        setContent {
            EscalarAlcoiaIComtatTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    MainView(this, data)
                }
            }
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)

        val url = currentUrl.value
        if (url != null && URLUtil.isValidUrl(url))
            outContent?.webUri = Uri.parse(url)
        else Timber.w("Cannot provide assist url since invalid url or null ($url).")
    }

    /**
     * Sets an user property in Firebase Analytics for telling if the user is using instant or not.
     * @author Arnau Mora
     * @since 20210721
     */
    private fun instantInfoSetup() {
        val analytics = Firebase.analytics
        val crashlytics = Firebase.crashlytics

        val isInstant = if (InstantApps.getPackageManagerCompat(this).isInstantApp)
            STATUS_INSTANT
        else
            STATUS_INSTALLED

        crashlytics.setCustomKey(APP_TYPE_PROP, isInstant)
        analytics.setUserProperty(APP_TYPE_PROP, isInstant)
    }
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun MainView(activity: Activity, navigateUri: Uri? = null) {
    val navController = rememberNavController()

    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escalar Alcoià i Comtat") },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.primary,
        content = {
            ExpandableHeader(expanded, headerContent = {
                InstallButton(activity)
            }) {
                NavHost(
                    navController = navController,
                    startDestination = "Areas"
                ) {
                    areas(activity, navController)
                    zones(activity, navController)
                    sectors(activity, navController)
                    sector(activity)

                    if (navigateUri != null)
                        try {
                            val path = navigateUri.path
                            if (path != null && path.isNotEmpty()) {
                                Timber.v("Navigating to: $path")
                                navController.navigate(path)
                            }
                        } catch (e: NullPointerException) {
                            try {
                                navController.searchNavigation(navigateUri)
                            } catch (e: IllegalArgumentException) {
                                Timber.e(e, "Could not navigate to $navigateUri")
                            }
                        }
                    else Timber.v("The navigateUri is null, won't navigate anywhere else.")
                }
            }
        },
    )
}

@Composable
@ExperimentalAnimationApi
fun SectorView(activity: Activity, areaId: String, zoneId: String, sectorId: String) {
    val viewModel = SectorViewModel(activity, areaId, zoneId, sectorId)
    val liveItems = viewModel.items
    val liveSector = viewModel.sector
    val sector: Sector? by liveSector.observeAsState(null)
    val paths: List<Path> by liveItems.observeAsState(listOf())

    var isLoading by remember { mutableStateOf(true) }
    var isMaximized by remember { mutableStateOf(false) }

    liveItems.observe(activity as LifecycleOwner) { isLoading = it.isEmpty() }

    LoadingIndicator(isLoading)

    Column {
        if (sector != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(if (isMaximized) 1f else .5f)
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                ZoomableImage(
                    painter = rememberImagePainter(
                        data = sector!!.downloadUrl,
                        builder = {
                            placeholder(R.drawable.ic_tall_placeholder)
                        }
                    ),
                    modifier = Modifier
                        .clip(RectangleShape)
                        .fillMaxHeight()
                        .fillMaxWidth(),
                    enableRotation = false,
                    minZoom = 1f
                )
                FloatingActionButton(
                    onClick = { isMaximized = !isMaximized },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 5.dp),
                    modifier = Modifier
                        .size(52.dp)
                        .padding(8.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(
                        if (isMaximized) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        contentDescription = if (isMaximized) "Compress" else "Expand"
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Row {
                    val sunTime = sector!!.sunTime
                    val kidsApt = sector!!.kidsApt
                    Chip(
                        label = when (sunTime) {
                            NO_SUN -> "No Sun"
                            ALL_DAY -> "All-Day Sun"
                            MORNING -> "Morning Sun"
                            AFTERNOON -> "Afternoon Sun"
                            else -> "Unknown"
                        },
                        icon = when (sunTime) {
                            NO_SUN -> Icons.Rounded.WbShade
                            ALL_DAY -> Icons.Rounded.WbSunny
                            MORNING -> Icons.Rounded.Flare
                            AFTERNOON -> Icons.Rounded.Flare
                            else -> Icons.Rounded.Close
                        }
                    )
                    if (kidsApt)
                        Chip(label = "Kids Apt", icon = Icons.Rounded.ChildFriendly)
                }
            }
        }

        val state = rememberLazyListState()
        LazyColumn(state = state) {
            items(paths) { dataClass ->

            }
        }
    }
}
