package com.arnyminerz.escalaralcoiaicomtat.activity

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.climb.AreaActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_AREA
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavItems
import com.arnyminerz.escalaralcoiaicomtat.core.ui.Screen
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.climb.DataClassItem
import com.arnyminerz.escalaralcoiaicomtat.core.ui.isolated_screen.ApplicationInfoWindow
import com.arnyminerz.escalaralcoiaicomtat.core.ui.map.GoogleMap
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.includeAll
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.GeneralSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.MainSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.NotificationsSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.StorageSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.ExploreViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.MainMapViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.SettingsViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.settingsViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.badge.ExperimentalBadgeUtils
import timber.log.Timber

class MainActivity : LanguageComponentActivity() {
    private val settingsViewModel by viewModels<SettingsViewModel>(factoryProducer = {
        PreferencesModule.settingsViewModel
    })
    private val exploreViewModel by viewModels<ExploreViewModel>(factoryProducer = {
        ExploreViewModel.Factory(application)
    })
    private val mapViewModel by viewModels<MainMapViewModel>(factoryProducer = {
        MainMapViewModel.Factory(application, PreferencesModule.getMarkerCentering)
    })

    /**
     * The GoogleMap instance for adding and removing features to the map.
     * @author Arnau Mora
     * @since 20211230
     */
    private lateinit var googleMap: GoogleMap

    @ExperimentalBadgeUtils
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()

                NavHost(navController, startDestination = "/") {
                    /*composable(
                        "/Areas/{areaId}/Zones/{zoneId}/Sectors/{sectorId}",
                        arguments = listOf(
                            navArgument("areaId") { type = NavType.StringType },
                            navArgument("zoneId") { type = NavType.StringType },
                            navArgument("sectorId") { type = NavType.StringType }
                        )
                    ) {*/
                    composable("/") {
                        Home()
                    }
                }
            }
        }
    }

    @Preview(name = "Settings screen")
    @Composable
    private fun SettingsScreen() {
        val settingsNavController = rememberNavController()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            NavHost(settingsNavController, "default") {
                composable("default") {
                    MainSettingsScreen(this@MainActivity, settingsNavController)
                }
                composable("general") {
                    GeneralSettingsScreen(this@MainActivity, settingsViewModel)
                }
                composable("notifications") {
                    NotificationsSettingsScreen(this@MainActivity, settingsViewModel)
                }
                composable("storage") {
                    StorageSettingsScreen(settingsViewModel)
                }
                composable("info") {
                    ApplicationInfoWindow(
                        appIconResource = R.mipmap.ic_launcher_round,
                        appName = stringResource(R.string.app_name),
                        appBuild = BuildConfig.VERSION_CODE,
                        appVersion = BuildConfig.VERSION_NAME,
                        "https://github.com/Escalar-Alcoia-i-Comtat/Android"
                    )
                }
            }
        }
    }

    @ExperimentalBadgeUtils
    @ExperimentalMaterial3Api
    @Composable
    private fun Home() {
        val homeNavController = rememberNavController()
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavItems(
                        homeNavController,
                        listOf(
                            Screen.Explore, Screen.Map, Screen.Downloads, Screen.Settings
                        )
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                homeNavController,
                Screen.Explore.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Explore.route) {
                    ExploreScreen()
                }
                composable(Screen.Map.route) {
                    MapScreen()
                }
                composable(Screen.Downloads.route) {
                    Text("Downloads")
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
    @Composable
    @ExperimentalBadgeUtils
    private fun ExploreScreen() {
        val loadedAreas = exploreViewModel.loadedAreas

        LazyColumn {
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = !loadedAreas.value,
                        modifier = Modifier
                            .align(Alignment.Center)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            items(exploreViewModel.areas) { area ->
                Timber.d("Displaying $area...")
                DataClassItem(area) {
                    launch(AreaActivity::class.java) {
                        putExtra(EXTRA_AREA, area.objectId)
                    }
                }
            }
        }
        exploreViewModel.loadAreas()
    }

    @Composable
    @SuppressLint("PotentialBehaviorOverride")
    private fun MapScreen() {
        Box(modifier = Modifier.fillMaxSize()) {
            var bottomDialogVisible by remember { mutableStateOf(false) }
            var bottomDialogTitle by remember { mutableStateOf("") }
            var bottomDialogImage by remember { mutableStateOf<Uri?>(null) }

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) { googleMap ->
                this@MainActivity.googleMap = googleMap
                googleMap.setOnMapClickListener { bottomDialogVisible = false }
                googleMap.setOnMarkerClickListener { marker ->
                    val markerCenteringEnabled = mapViewModel.centerMarkerOnClick
                    if (markerCenteringEnabled.value) {
                        googleMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(
                                    marker.position,
                                    googleMap.cameraPosition.zoom
                                )
                            )
                        )
                    }
                    val markerTitle = marker.title
                    if (markerTitle != null) {
                        bottomDialogTitle = markerTitle
                        bottomDialogVisible = true
                        val loadedImageUrl = marker.snippet?.let { snippet ->
                            val srcPos = snippet.indexOf("src=\"")
                                .takeIf { it >= 0 } ?: return@let false
                            val cutUrl = snippet.substring(srcPos + 5)
                            val endPos = cutUrl.indexOf('"')
                                .takeIf { it >= 0 } ?: return@let false
                            val imageUrl = cutUrl.substring(0, endPos)
                            bottomDialogImage = Uri.parse(imageUrl)
                            true
                        } ?: false
                        if (!loadedImageUrl)
                            bottomDialogImage = null
                        Timber.i("Marker snippet: ${marker.snippet}")
                    }
                    true
                }
            }

            val density = LocalDensity.current
            AnimatedVisibility(
                visible = bottomDialogVisible,
                enter = slideInVertically { with(density) { 40.dp.roundToPx() } } +
                        fadeIn(initialAlpha = .3f),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
            ) {
                Card(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        Image(
                            painter = rememberImagePainter(
                                request = ImageRequest.Builder(this@MainActivity)
                                    .data(bottomDialogImage)
                                    .placeholder(R.drawable.ic_wide_placeholder)
                                    .error(R.drawable.ic_wide_placeholder)
                                    .build()
                            ),
                            contentDescription = "", // TODO: Set content description
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        Row {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = bottomDialogTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.fillMaxWidth(),
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Button(
                                    onClick = { /*TODO*/ }, // TODO: Click action
                                    colors = ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Row {
                                        Image(
                                            Icons.Rounded.Login,
                                            contentDescription = "", // TODO: Set content description
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.CenterVertically)
                                        )
                                        Text(
                                            text = stringResource(R.string.action_view),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (this@MainActivity::googleMap.isInitialized && exploreViewModel.loadedAreas.value) {
                for (area in exploreViewModel.areas)
                    mapViewModel.loadGoogleMap(googleMap, area)
            }
        }
        mapViewModel.locations.observe(this as LifecycleOwner) { locations ->
            try {
                val bounds = LatLngBounds.builder()
                    .includeAll(locations)
                    .build()

                Timber.i("Detected ${locations.size} locations in map.")
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 45))
            } catch (e: IllegalStateException) {
                Timber.e("No markers were loaded.")
            }
        }
        exploreViewModel.loadAreas()
    }
}