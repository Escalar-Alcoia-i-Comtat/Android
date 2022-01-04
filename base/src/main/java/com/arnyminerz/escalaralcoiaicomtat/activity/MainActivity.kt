package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkAwareComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavItems
import com.arnyminerz.escalaralcoiaicomtat.core.ui.Screen
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.explore.DataClassExplorer
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
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

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

    /**
     * Tells whether or not the device is connected to the Internet.
     * @author Arnau Mora
     * @since 20220102
     */
    internal val hasInternet = MutableLiveData<Boolean>()

    val storage: FirebaseStorage = Firebase.storage

    @ExperimentalBadgeUtils
    @OptIn(
        ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
        ExperimentalPagerApi::class,
    )
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
                        DataClassExplorer(navController, storage, entry.arguments ?: Bundle())
                    }
                    composable(
                        "/Areas/{areaId}/Zones/{zoneId}",
                        arguments = listOf(
                            navArgument("areaId") { type = NavType.StringType },
                            navArgument("zoneId") { type = NavType.StringType },
                        )
                    ) { entry ->
                        DataClassExplorer(navController, storage, entry.arguments ?: Bundle())
                    }
                    composable(
                        "/Areas/{areaId}/Zones/{zoneId}/Sectors/{sectorId}",
                        arguments = listOf(
                            navArgument("areaId") { type = NavType.StringType },
                            navArgument("zoneId") { type = NavType.StringType },
                            navArgument("sectorId") { type = NavType.StringType },
                        )
                    ) { entry ->
                        DataClassExplorer(navController, storage, entry.arguments ?: Bundle())
                    }

                    composable("/") {
                        Home(navController)
                    }
                }
            }
        }
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        super.onStateChange(state)
        Timber.i("Updated network state. Internet: ${state.hasInternet}")
        hasInternet.postValue(state.hasInternet)
    }

    @ExperimentalBadgeUtils
    @ExperimentalMaterial3Api
    @ExperimentalPagerApi
    @Composable
    private fun Home(rootNavController: NavController) {
        val pagerState = rememberPagerState()
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavItems(
                        pagerState,
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
            HorizontalPager(
                count = 5,
                state = pagerState,
                modifier = Modifier.padding(innerPadding),
            ) { index ->
                when (index) {
                    0 -> ExploreScreen(rootNavController, storage)
                    1 -> MapScreen()
                    2 -> DownloadsScreen()
                    3 -> SettingsScreen()
                }
            }
        }
    }
}