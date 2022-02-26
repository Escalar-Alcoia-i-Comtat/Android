package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UPDATE_AVAILABLE
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UPDATE_AVAILABLE_FAIL_CLIENT
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UPDATE_AVAILABLE_FAIL_FIELDS
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UPDATE_AVAILABLE_FAIL_SERVER
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UPDATE_AVAILABLE_FALSE
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UpdaterSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.updateAvailable
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.shared.app
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavItem
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavItems
import com.arnyminerz.escalaralcoiaicomtat.core.ui.Screen
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
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
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.badge.ExperimentalBadgeUtils
import timber.log.Timber

class MainActivity : LanguageComponentActivity() {
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
    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalPagerApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var updatesAvailable by remember { mutableStateOf<Int?>(null) }

            // Check for updates
            doAsync {
                val updateAvailable = updateAvailable(this@MainActivity, app.searchSession)
                Timber.i("Update available: $updateAvailable")
                when (updateAvailable) {
                    UPDATE_AVAILABLE_FALSE -> Timber.i("No updates available")
                    UPDATE_AVAILABLE_FAIL_SERVER, UPDATE_AVAILABLE_FAIL_FIELDS, UPDATE_AVAILABLE_FAIL_CLIENT -> {
                        Timber.w("Check for update failed.")
                    }
                    UPDATE_AVAILABLE -> {
                        val updateAvailableObjects = UpdaterSingleton
                            .getInstance()
                            .updateAvailableObjects
                        updatesAvailable = updateAvailableObjects.entries.sumOf { it.value.size }
                        uiContext { toast("Update available!") }
                    }
                }
            }

            AppTheme {
                Home(updatesAvailable)
            }
        }
    }

    @ExperimentalBadgeUtils
    @ExperimentalMaterial3Api
    @ExperimentalMaterialApi
    @ExperimentalPagerApi
    @Composable
    private fun Home(updatesAvailable: Int?) {
        val pagerState = rememberPagerState()
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavItems(
                        pagerState,
                        mutableListOf(
                            NavItem(Screen.Explore),
                            NavItem(Screen.Map),
                            NavItem(Screen.Downloads, updatesAvailable),
                            NavItem(Screen.Settings)
                        ).apply {
                            // If in debug mode, add the developer screen
                            if (BuildConfig.DEBUG)
                                add(NavItem(Screen.Developer))
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
                    0 -> ExploreScreen()
                    1 -> MapScreen()
                    2 -> DownloadsScreen(updatesAvailable?.let { it > 0 } ?: false)
                    3 -> SettingsScreen()
                    4 -> if (BuildConfig.DEBUG) DeveloperScreen()
                }
            }
        }
    }
}