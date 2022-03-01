package com.arnyminerz.escalaralcoiaicomtat.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arnyminerz.escalaralcoiaicomtat.core.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.IntroPageData
import com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.IntroWindow
import com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.action.IntroAction
import com.arnyminerz.escalaralcoiaicomtat.core.ui.intro.action.IntroActionType
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.IntroViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.introViewModelFactory
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@ExperimentalMaterial3Api
class IntroActivity : ComponentActivity() {
    /**
     * The view model for updating the preference.
     * @author Arnau Mora
     * @since 20211229
     */
    private val viewModel by viewModels<IntroViewModel>(factoryProducer = { PreferencesModule.introViewModelFactory })

    var permissionCallback: (granted: Boolean) -> Unit = { }

    /**
     * The permission request for asking for location access.
     * @author Arnau Mora
     * @since 20220130
     */
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ->
                // Fine location granted
                permissionCallback(true)
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) ->
                // Coarse location granted
                permissionCallback(true)
            else -> {
                // The permission was not granted
                toast(R.string.toast_error_permission)
                permissionCallback(false)
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Listen for changes on introShown
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.introShown.collect { shownIntro ->
                    if (shownIntro)
                        launch(LoadingActivity::class.java)
                }
            }
        }

        setContent {
            AppTheme {
                var mayRequestLocationPermissions by remember { mutableStateOf(false) }
                var nearbyZonesSwitchEnabled by remember { mutableStateOf(true) }

                val introPages = mutableListOf<IntroPageData<*>>(
                    IntroPageData<Any?>(
                        stringResource(R.string.intro_main_title, "Escalar Alcoià i Comtat"),
                        stringResource(R.string.intro_main_message)
                    ),
                    IntroPageData<Any?>(
                        stringResource(R.string.intro_warning_title),
                        stringResource(R.string.intro_warning_message)
                    ),
                ).apply {
                    val locationPage = IntroPageData(
                        stringResource(R.string.intro_nearbyzones_title),
                        stringResource(R.string.intro_nearbyzones_message),
                        IntroAction(
                            stringResource(R.string.intro_nearbyzones_enable),
                            runBlocking {
                                PreferencesModule.getNearbyZonesEnabled.invoke().first()
                            },
                            IntroActionType.SWITCH,
                            { checked ->
                                nearbyZonesSwitchEnabled = false
                                doAsync {
                                    PreferencesModule.setNearbyZonesEnabled.invoke(checked)

                                    permissionCallback = { permissionGranted ->
                                        if (!permissionGranted) {
                                            setState(false)
                                            doAsync {
                                                PreferencesModule.setNearbyZonesEnabled(false)
                                            }
                                        }
                                        mayRequestLocationPermissions = false
                                    }

                                    mayRequestLocationPermissions = if (checked)
                                        ContextCompat.checkSelfPermission(
                                            this@IntroActivity,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_DENIED
                                                && ContextCompat.checkSelfPermission(
                                            this@IntroActivity,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_DENIED
                                    else false

                                    uiContext {
                                        nearbyZonesSwitchEnabled = true
                                        toast(R.string.toast_nearby_zones_enabled)
                                    }
                                }
                            },
                            nearbyZonesSwitchEnabled
                        )
                    )

                    // Check if GPS is available, and Nearby Zones card
                    val locationManager =
                        getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (locationManager.allProviders.contains(LocationManager.GPS_PROVIDER))
                        add(locationPage)
                    else
                        runBlocking { PreferencesModule.setNearbyZonesEnabled(false) }

                    if (BuildConfig.DEBUG)
                    // If debug build, warn user
                        add(
                            IntroPageData<Any?>(
                                stringResource(R.string.intro_beta_title),
                                stringResource(R.string.intro_beta_message)
                            )
                        )
                }

                IntroWindow(
                    introPages.toList(),
                    fabPermissions = if (mayRequestLocationPermissions)
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    else emptyArray(),
                    requestPermissionLauncher = locationPermissionRequest,
                ) {
                    Timber.v("Finished showing intro pages. Loading LoadingActivity")
                    viewModel.markIntroAsShown()
                }
            }
        }
    }
}
