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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.preference.PreferenceManager
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UpdaterSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavItem
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavItems
import com.arnyminerz.escalaralcoiaicomtat.core.ui.Screen
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.DeveloperScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.ExploreScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.MapScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.SettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.main.StorageScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.MainMapViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.DeveloperViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.ExploreViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.SettingsViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.StorageViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main.settingsViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.material.badge.ExperimentalBadgeUtils
import org.osmdroid.config.Configuration

class MainActivity : LanguageComponentActivity() {
    internal val exploreViewModel by viewModels<ExploreViewModel>(factoryProducer = {
        ExploreViewModel.Factory(application)
    })
    internal val mapViewModel by viewModels<MainMapViewModel>(factoryProducer = {
        MainMapViewModel.Factory(application, PreferencesModule.getMarkerCentering)
    })
    internal val storageViewModel by viewModels<StorageViewModel>(factoryProducer = {
        StorageViewModel.Factory(application)
    })
    internal val settingsViewModel by viewModels<SettingsViewModel>(factoryProducer = {
        PreferencesModule.settingsViewModel
    })
    internal val developerViewModel by viewModels<DeveloperViewModel>(factoryProducer = {
        DeveloperViewModel.Factory(application)
    })

    @ExperimentalBadgeUtils
    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalPagerApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance()
            .apply {
                load(
                    this@MainActivity,
                    PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                )
                userAgentValue = BuildConfig.APPLICATION_ID
            }

        setContent {
            AppTheme {
                Home()
            }
            storageViewModel.checkForUpdates()
        }
    }

    @ExperimentalBadgeUtils
    @ExperimentalMaterial3Api
    @ExperimentalMaterialApi
    @ExperimentalPagerApi
    @Composable
    private fun Home() {
        val pagerState = rememberPagerState()
        var userScrollEnabled by remember { mutableStateOf(true) }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                userScrollEnabled = page != 1
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val updatesAvailable by UpdaterSingleton.getInstance()
                        .updateAvailableObjects
                        .observeAsState(emptyList())

                    NavItems(
                        pagerState,
                        mutableListOf(
                            NavItem(Screen.Explore),
                            NavItem(Screen.Map),
                            NavItem(Screen.Storage, updatesAvailable.size),
                            NavItem(Screen.Settings)
                        ).apply {
                            // If in debug mode, add the developer screen
                            if (BuildConfig.DEBUG)
                                add(
                                    NavItem(
                                        Screen.Developer,
                                        visible = PreferencesModule
                                            .userPreferencesRepository
                                            .developerTabEnabled
                                            .collectAsState(true)
                                    )
                                )
                        }
                    )
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                count = 5,
                state = pagerState,
                modifier = Modifier.padding(innerPadding),
                userScrollEnabled = userScrollEnabled,
            ) { index ->
                when (index) {
                    0 -> ExploreScreen()
                    1 -> MapScreen()
                    2 -> StorageScreen()
                    3 -> SettingsScreen()
                    4 -> if (BuildConfig.DEBUG) DeveloperScreen()
                }
            }
        }
    }
}