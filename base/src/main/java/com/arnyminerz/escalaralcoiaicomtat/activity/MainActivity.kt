package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkAwareComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavItems
import com.arnyminerz.escalaralcoiaicomtat.core.ui.Screen
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.DataClassExplorer
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.DeveloperScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.DownloadsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.ExploreScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.MapScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.SettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.MainMapViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.DeveloperViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.DownloadsViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.ExploreViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.SettingsViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.settingsViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.badge.ExperimentalBadgeUtils

class MainActivity : NetworkAwareComponentActivity() {
    internal val exploreViewModel by viewModels<ExploreViewModel>(factoryProducer = {
        ExploreViewModel.Factory(application)
    })
    internal val mapViewModel by viewModels<MainMapViewModel>(factoryProducer = {
        MainMapViewModel.Factory(application, PreferencesModule.getMarkerCentering)
    })
    internal val downloadsViewModel by viewModels<DownloadsViewModel>(factoryProducer = {
        DownloadsViewModel.Factory(application)
    })
    internal val settingsViewModel by viewModels<SettingsViewModel>(factoryProducer = {
        PreferencesModule.settingsViewModel
    })
    internal val developerViewModel by viewModels<DeveloperViewModel>(factoryProducer = {
        DeveloperViewModel.Factory(application)
    })

    /**
     * The GoogleMap instance for adding and removing features to the map.
     * @author Arnau Mora
     * @since 20211230
     */
    internal var googleMap: GoogleMap? = null

    @ExperimentalBadgeUtils
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()

                NavHost(navController, startDestination = "/") {
                    composable(
                        "/Areas/{areaId}",
                        arguments = listOf(
                            navArgument("areaId") { type = NavType.StringType },
                        )
                    ) { entry ->
                        DataClassExplorer(navController, entry.arguments ?: Bundle())
                    }
                    composable(
                        "/Areas/{areaId}/Zones/{zoneId}",
                        arguments = listOf(
                            navArgument("areaId") { type = NavType.StringType },
                            navArgument("zoneId") { type = NavType.StringType },
                        )
                    ) { entry ->
                        DataClassExplorer(navController, entry.arguments ?: Bundle())
                    }
                    composable(
                        "/Areas/{areaId}/Zones/{zoneId}/Sectors/{sectorId}",
                        arguments = listOf(
                            navArgument("areaId") { type = NavType.StringType },
                            navArgument("zoneId") { type = NavType.StringType },
                            navArgument("sectorId") { type = NavType.StringType },
                        )
                    ) { entry ->
                        DataClassExplorer(navController, entry.arguments ?: Bundle())
                    }

                    composable("/") {
                        Home(navController)
                    }
                }
            }
        }
    }

    @ExperimentalBadgeUtils
    @ExperimentalMaterial3Api
    @Composable
    private fun Home(rootNavController: NavController) {
        val homeNavController = rememberNavController()
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavItems(
                        homeNavController,
                        mutableListOf(
                            Screen.Explore, Screen.Map, Screen.Downloads, Screen.Settings
                        ).apply {
                            // If in debug mode, add the developer screen
                            if (BuildConfig.DEBUG)
                                add(Screen.Developer)
                        }
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
                    ExploreScreen(rootNavController)
                }
                composable(Screen.Map.route) {
                    MapScreen()
                }
                composable(Screen.Downloads.route) {
                    DownloadsScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
                composable(Screen.Developer.route) {
                    DeveloperScreen()
                }
            }
        }
    }
}