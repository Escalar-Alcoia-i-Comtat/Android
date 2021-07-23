package com.arnyminerz.escalaralcoiaicomtat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.arnyminerz.escalaralcoiaicomtat.core.ui.animation.EnterAnimation
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.Explorer
import com.arnyminerz.escalaralcoiaicomtat.ui.elements.Chip
import com.arnyminerz.escalaralcoiaicomtat.ui.elements.ZoomableImage
import com.arnyminerz.escalaralcoiaicomtat.ui.theme.EscalarAlcoiaIComtatTheme
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.AreasViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.SectorViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.SectorsViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.ZonesViewModel
import com.google.android.gms.instantapps.InstantApps
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

const val STATUS_INSTALLED = "installed"
const val STATUS_INSTANT = "instant"
const val ANALYTICS_USER_PROP = "app_type"

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

        setContent {
            EscalarAlcoiaIComtatTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    MainView(this, data?.path)
                }
            }
        }
    }

    /**
     * Sets an user property in Firebase Analytics for telling if the user is using instant or not.
     * @author Arnau Mora
     * @since 20210721
     */
    private fun instantInfoSetup() {
        val analytics = Firebase.analytics

        analytics.setUserProperty(
            ANALYTICS_USER_PROP,
            if (InstantApps.getPackageManagerCompat(this).isInstantApp)
                STATUS_INSTANT
            else
                STATUS_INSTALLED
        )
    }
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun MainView(activity: Activity, path: String? = null) {
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
            Column {
                // Top Menu
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val postInstall = Intent(Intent.ACTION_MAIN)
                                    .addCategory(Intent.CATEGORY_DEFAULT)
                                    .setPackage("com.arnyminerz.escalaralcoiaicomtat")
                                InstantApps.showInstallPrompt(activity, postInstall, 0, null)
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.textButtonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = MaterialTheme.colors.onSecondary
                            ),
                        ) {
                            Text("Install App")
                        }
                    }
                }

                // Content card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .animateContentSize(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "Areas"
                    ) {
                        composable("Areas") {
                            EnterAnimation {
                                AreasExplorer(activity, navController)
                            }
                        }
                        composable("Areas/{areaId}") { backStackEntry ->
                            val areaId = backStackEntry.arguments?.getString("areaId")
                            if (areaId != null)
                                EnterAnimation {
                                    ZonesExplorer(activity, navController, areaId)
                                }
                            else
                                Text(text = "Could not navigate to area: $areaId")
                        }
                        composable("Areas/{areaId}/Zones/{zoneId}") { backStackEntry ->
                            val areaId = backStackEntry.arguments?.getString("areaId")
                            val zoneId = backStackEntry.arguments?.getString("zoneId")
                            if (areaId != null && zoneId != null)
                                EnterAnimation {
                                    SectorsExplorer(activity, navController, areaId, zoneId)
                                }
                            else
                                Text(text = "Could not navigate to zone Z/$zoneId in A/$areaId")
                        }
                        composable("Areas/{areaId}/Zones/{zoneId}/Sectors/{sectorId}") { backStackEntry ->
                            val areaId = backStackEntry.arguments?.getString("areaId")
                            val zoneId = backStackEntry.arguments?.getString("zoneId")
                            val sectorId = backStackEntry.arguments?.getString("sectorId")
                            if (areaId != null && zoneId != null && sectorId != null)
                                EnterAnimation {
                                    SectorView(activity, areaId, zoneId, sectorId)
                                }
                            else
                                Text(text = "Could not navigate to sector S/$sectorId in Z/$zoneId in A/$areaId")
                        }

                        if (path != null && path.isNotEmpty())
                            try {
                                navController.navigate(path)
                            } catch (e: IllegalArgumentException) {
                                Timber.e(e, "Could not navigate to $path")
                            }
                    }
                }
            }
        },
    )
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun AreasExplorer(activity: Activity, navController: NavController) {
    Explorer(activity, navController, 1, dataClassViewModel = AreasViewModel::class.java)
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun ZonesExplorer(activity: Activity, navController: NavController, areaId: String) {
    Explorer(
        activity,
        navController,
        2,
        dataClassViewModel = ZonesViewModel::class.java,
        viewModelArguments = listOf(areaId)
    )
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun SectorsExplorer(
    activity: Activity,
    navController: NavController,
    areaId: String,
    zoneId: String
) {
    Explorer(
        activity,
        navController,
        1,
        dataClassViewModel = SectorsViewModel::class.java,
        viewModelArguments = listOf(areaId, zoneId)
    )
}

@Composable
@ExperimentalAnimationApi
fun SectorView(activity: Activity, areaId: String, zoneId: String, sectorId: String) {
    val viewModel = SectorViewModel(areaId, zoneId, sectorId)
    val liveItems = viewModel.items
    val liveSector = viewModel.sector
    val sector: Sector? by liveSector.observeAsState(null)
    val paths: List<Path> by liveItems.observeAsState(listOf())

    var isLoading by remember { mutableStateOf(true) }
    var isMaximized by remember { mutableStateOf(false) }

    liveItems.observe(activity as LifecycleOwner) { isLoading = it.isEmpty() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        AnimatedVisibility(visible = isLoading, modifier = Modifier.size(52.dp)) {
            CircularProgressIndicator()
        }
    }

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
