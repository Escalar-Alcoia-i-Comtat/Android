package com.arnyminerz.escalaralcoiaicomtat.instant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.UiThread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SETTINGS_ERROR_REPORTING_PREF
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.climb.Explorer
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.theme.EscalarAlcoiaIComtatTheme
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel.AreasViewModel
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel.SectorsViewModel
import com.arnyminerz.escalaralcoiaicomtat.instant.ui.viewmodel.ZonesViewModel
import com.google.android.gms.instantapps.InstantApps
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import timber.log.Timber

const val STATUS_INSTALLED = "installed"
const val STATUS_INSTANT = "instant"
const val ANALYTICS_USER_PROP = "app_type"

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instantInfoSetup()
        dataCollectionSetUp()

        setContent {
            EscalarAlcoiaIComtatTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    MainView(this)
                }
            }
        }
    }

    /**
     * Initializes the user-set data collection policy.
     * If debugging, data collection will always be disabled.
     * @author Arnau Mora
     * @since 20210617
     * @see SETTINGS_ERROR_REPORTING_PREF
     */
    @UiThread
    private fun dataCollectionSetUp() {
        val enableErrorReporting = SETTINGS_ERROR_REPORTING_PREF.get()

        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
        Timber.v("Set Crashlytics collection enabled to $enableErrorReporting")

        Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG && enableErrorReporting)
        Timber.v("Set Analytics collection enabled to $enableErrorReporting")

        Firebase.performance.isPerformanceCollectionEnabled = enableErrorReporting
        Timber.v("Set Performance collection enabled to $enableErrorReporting")
    }

    /**
     * Sets an user property in Firebase Analytics for telling if the user is using instant or not.
     * @author Arnau Mora
     * @since 20210721
     */
    private fun instantInfoSetup() {
        val analytics = Firebase.analytics

        analytics.setUserProperty(
            ANALYTICS_USER_PROP,
            if (InstantApps.getPackageManagerCompat(this).isInstantApp)
                STATUS_INSTANT
            else
                STATUS_INSTALLED
        )
    }
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun MainView(activity: Activity) {
    val navController = rememberNavController()

    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escalar Alcoià i Comtat") },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        backgroundColor = MaterialTheme.colors.primary,
        content = {
            Column {
                // Top Menu
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val postInstall = Intent(Intent.ACTION_MAIN)
                                    .addCategory(Intent.CATEGORY_DEFAULT)
                                    .setPackage("com.arnyminerz.escalaralcoiaicomtat")
                                InstantApps.showInstallPrompt(activity, postInstall, 0, null)
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.textButtonColors(
                                backgroundColor = MaterialTheme.colors.secondary,
                                contentColor = MaterialTheme.colors.onSecondary
                            ),
                        ) {
                            Text("Install App")
                        }
                    }
                }

                // Content card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .animateContentSize(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    NavHost(navController = navController, startDestination = "Areas") {
                        composable("Areas") { AreasExplorer(activity, navController) }
                        composable("Areas/{areaId}") { backStackEntry ->
                            val areaId = backStackEntry.arguments?.getString("areaId")
                            if (areaId != null)
                                ZonesExplorer(activity, navController, areaId)
                            else
                                Text(text = "Could not navigate to area: $areaId")
                        }
                        composable("Areas/{areaId}/Zones/{zoneId}") { backStackEntry ->
                            val areaId = backStackEntry.arguments?.getString("areaId")
                            val zoneId = backStackEntry.arguments?.getString("zoneId")
                            if (areaId != null && zoneId != null)
                                SectorsExplorer(activity, navController, areaId, zoneId)
                            else
                                Text(text = "Could not navigate to zone Z/$zoneId in A/$areaId")
                        }
                    }
                }
            }
        },
    )
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun AreasExplorer(activity: Activity, navController: NavController) {
    Explorer(activity, navController, 1, AreasViewModel::class.java)
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun ZonesExplorer(activity: Activity, navController: NavController, areaId: String) {
    Explorer(activity, navController, 2, ZonesViewModel::class.java, listOf(areaId))
}

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalCoilApi
@ExperimentalFoundationApi
fun SectorsExplorer(
    activity: Activity,
    navController: NavController,
    areaId: String,
    zoneId: String
) {
    Explorer(activity, navController, 1, SectorsViewModel::class.java, listOf(areaId, zoneId))
}
