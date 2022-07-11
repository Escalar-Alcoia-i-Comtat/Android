package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.updater.UpdaterSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavItem
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavigationItem
import com.arnyminerz.escalaralcoiaicomtat.core.ui.Screen
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launchStore
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.material.badge.ExperimentalBadgeUtils
import org.osmdroid.config.Configuration

class MainActivity : AppCompatActivity() {
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
        ExperimentalPagerApi::class,
        ExperimentalMaterialApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalFoundationApi::class,
        ExperimentalPermissionsApi::class,
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
        }
    }

    @Composable
    @ExperimentalPagerApi
    @ExperimentalBadgeUtils
    @ExperimentalMaterialApi
    @ExperimentalMaterial3Api
    @ExperimentalFoundationApi
    @ExperimentalPermissionsApi
    private fun Home() {
        val pagerState = rememberPagerState()
        var userScrollEnabled by remember { mutableStateOf(true) }
        val isServerIncompatible by storageViewModel.serverIncompatible

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                userScrollEnabled = page != 1
            }
        }

        if (isServerIncompatible)
            AlertDialog(
                onDismissRequest = { },
                confirmButton = {
                    Button(onClick = { launchStore() }) {
                        Text(
                            stringResource(R.string.action_open_store)
                        )
                    }
                },
                dismissButton = {
                    Button(onClick = { finishAndRemoveTask() }) {
                        Text(
                            stringResource(R.string.action_close_app)
                        )
                    }
                },
            )

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val updatesAvailable = UpdaterSingleton.getInstance().updateAvailableObjects

                    NavigationItem(pagerState, NavItem(Screen.Explore), 0)
                    NavigationItem(pagerState, NavItem(Screen.Map), 1)
                    NavigationItem(pagerState, NavItem(Screen.Storage, updatesAvailable), 2)
                    NavigationItem(pagerState, NavItem(Screen.Settings), 3)
                    if (BuildConfig.DEBUG)
                        NavigationItem(
                            pagerState,
                            NavItem(
                                Screen.Developer,
                                visible = PreferencesModule
                                    .userPreferencesRepository
                                    .developerTabEnabled
                                    .collectAsState(true)
                            ),
                            4
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

        storageViewModel.checkForUpdates()
    }
}