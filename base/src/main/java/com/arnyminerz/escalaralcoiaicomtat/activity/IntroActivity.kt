package com.arnyminerz.escalaralcoiaicomtat.activity

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.res.stringResource
import com.arnyminerz.escalaralcoiaicomtat.core.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.firebase.Event.FINISH_INTRO
import com.arnyminerz.escalaralcoiaicomtat.core.firebase.Param.GPS_SUPPORTED
import com.arnyminerz.escalaralcoiaicomtat.core.firebase.Param.IS_BETA
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.Keys
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferenceValue
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.observe
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.setAsync
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.core.utils.isLocationPermissionGranted
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.core.utils.uiContext
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.IntroViewModel
import com.arnyminerz.lib.app_intro.IntroPageData
import com.arnyminerz.lib.app_intro.IntroWindow
import com.arnyminerz.lib.app_intro.action.IntroAction
import com.arnyminerz.lib.app_intro.action.IntroActionType
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

@ExperimentalMaterial3Api
class IntroActivity : AppCompatActivity() {
    /**
     * The view model for updating the preference.
     * @author Arnau Mora
     * @since 20211229
     */
    private val viewModel by viewModels<IntroViewModel>(factoryProducer = {
        IntroViewModel.Factory(
            application
        )
    })

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
        observe(Keys.shownIntro, false) { shownIntro ->
            if (shownIntro)
                uiContext { launch(LoadingActivity::class.java) }
        }

        setContent {
            AppTheme {
                val introPages = mutableListOf<IntroPageData<*>>(
                    IntroPageData<Any?>(
                        stringResource(R.string.intro_main_title),
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
                            PreferenceValue(Keys.nearbyZonesEnabled, false),
                            IntroActionType.SWITCH,
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

                    if (gpsSupported && isLocationPermissionGranted())
                        add(locationPage)
                    else
                        setAsync(Keys.nearbyZonesEnabled, false)

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
                            }
                        )
                    viewModel.markIntroAsShown()
                }
            }
        }
    }
}
