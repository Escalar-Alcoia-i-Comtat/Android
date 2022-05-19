package com.arnyminerz.escalaralcoiaicomtat.activity

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arnyminerz.escalaralcoiaicomtat.core.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.firebase.Event.FINISH_INTRO
import com.arnyminerz.escalaralcoiaicomtat.core.firebase.Param.GPS_SUPPORTED
import com.arnyminerz.escalaralcoiaicomtat.core.firebase.Param.IS_BETA
import com.arnyminerz.escalaralcoiaicomtat.core.firebase.Param.NEARBY_ZONES_ENABLED
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync
import com.arnyminerz.escalaralcoiaicomtat.core.utils.isLocationPermissionGranted
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toast
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.IntroViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.introViewModelFactory
import com.arnyminerz.lib.app_intro.IntroPageData
import com.arnyminerz.lib.app_intro.IntroWindow
import com.arnyminerz.lib.app_intro.action.IntroAction
import com.arnyminerz.lib.app_intro.action.IntroActionType
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
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
     * Matches the enable status of the nearby zones functionality.
     * @author Arnau Mora
     * @since 20220512
     */
    var nearbyZonesEnabled: Boolean = false

    /**
     * Stores whether the device supports gps.
     * @author Arnau Mora
     * @since 20220512
     */
    private var gpsSupported: Boolean = false

    @OptIn(ExperimentalPagerApi::class, ExperimentalPermissionsApi::class)
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
                            mutableStateOf(
                                runBlocking {
                                    PreferencesModule.getNearbyZonesEnabled.invoke().first()
                                }
                            ),
                            IntroActionType.SWITCH,
                            { checked ->
                                nearbyZonesSwitchEnabled = false
                                doAsync {
                                    nearbyZonesEnabled = checked
                                    PreferencesModule.setNearbyZonesEnabled.invoke(checked)

                                    permissionCallback = { permissionGranted ->
                                        if (!permissionGranted) {
                                            nearbyZonesEnabled = false
                                            setState(false)
                                            doAsync {
                                                PreferencesModule.setNearbyZonesEnabled(false)
                                            }
                                        } else setState(true)
                                    }

                                    uiContext {
                                        nearbyZonesSwitchEnabled = true
                                        toast(R.string.toast_nearby_zones_enabled)
                                    }
                                }
                            },
                            nearbyZonesSwitchEnabled
                        ),
                        permissions = arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                    )

                    // Check if GPS is available, and Nearby Zones card
                    val locationManager =
                        getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    gpsSupported =
                        locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)

                    if (gpsSupported) {
                        nearbyZonesEnabled = isLocationPermissionGranted()
                        add(locationPage)
                    } else {
                        nearbyZonesSwitchEnabled = false
                        nearbyZonesEnabled = false
                        doAsync { PreferencesModule.setNearbyZonesEnabled(false) }
                    }

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
                ) {
                    Timber.v("Finished showing intro pages. Loading LoadingActivity")
                    Firebase.analytics
                        .logEvent(
                            FINISH_INTRO,
                            Bundle().apply {
                                putBoolean(
                                    IS_BETA,
                                    BuildConfig.DEBUG,
                                )
                                putBoolean(
                                    GPS_SUPPORTED,
                                    gpsSupported,
                                )
                                putBoolean(
                                    NEARBY_ZONES_ENABLED,
                                    nearbyZonesEnabled,
                                )
                            }
                        )
                    viewModel.markIntroAsShown()
                }
            }
        }
    }
}
