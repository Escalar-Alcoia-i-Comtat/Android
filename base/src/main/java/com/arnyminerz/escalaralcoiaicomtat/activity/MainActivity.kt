package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.NavItems
import com.arnyminerz.escalaralcoiaicomtat.core.ui.Screen
import com.arnyminerz.escalaralcoiaicomtat.core.ui.theme.AppTheme
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.GeneralSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.MainSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.NotificationsSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.settings.StorageSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.SettingsViewModel
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.settingsViewModel

class MainActivity : LanguageComponentActivity() {
    private val settingsViewModel by viewModels<SettingsViewModel>(factoryProducer = { PreferencesModule.settingsViewModel })

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
    fun SettingsScreen() {
        val settingsNavController = rememberNavController()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            NavHost(settingsNavController, "default") {
                composable("default") {
                    MainSettingsScreen(settingsNavController)
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
                    // TODO: Create info screen
                }
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun Home() {
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
                    Text("Explore")
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
}