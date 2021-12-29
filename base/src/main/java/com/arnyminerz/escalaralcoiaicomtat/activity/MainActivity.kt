package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
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
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.GeneralSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.MainSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.NotificationsSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.StorageSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.ExploreViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.SettingsViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.settingsViewModel
import com.google.android.material.badge.ExperimentalBadgeUtils
import timber.log.Timber

class MainActivity : LanguageComponentActivity() {
    private val settingsViewModel by viewModels<SettingsViewModel>(factoryProducer = { PreferencesModule.settingsViewModel })
    private val exploreViewModel by viewModels<ExploreViewModel>(factoryProducer = {
        ExploreViewModel.Factory(
            application
        )
    })

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
        ) {
            NavHost(homeNavController, Screen.Explore.route) {
                composable(Screen.Explore.route) {
                    ExploreScreen()
                }
                composable(Screen.Map.route) {
                    Text("Map")
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
}