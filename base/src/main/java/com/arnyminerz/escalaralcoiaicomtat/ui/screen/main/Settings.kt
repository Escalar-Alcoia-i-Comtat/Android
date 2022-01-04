package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.ui.isolated_screen.ApplicationInfoWindow
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.settings.GeneralSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.settings.MainSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.settings.NotificationsSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.settings.StorageSettingsScreen

/**
 * The Settings screen of the Main Activity.
 * @author Arnau Mora
 * @since 20220102
 */
@Preview(name = "Settings screen")
@Composable
fun MainActivity.SettingsScreen() {
    val settingsNavController = rememberNavController()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        NavHost(settingsNavController, "default") {
            composable("default") {
                MainSettingsScreen(this@SettingsScreen, settingsNavController)
            }
            composable("general") {
                GeneralSettingsScreen(this@SettingsScreen, settingsViewModel)
            }
            composable("notifications") {
                NotificationsSettingsScreen(this@SettingsScreen, settingsViewModel)
            }
            composable("storage") {
                StorageSettingsScreen(settingsViewModel)
            }
            composable("info") {
                ApplicationInfoWindow(
                    appIconResource = R.mipmap.ic_launcher_round,
                    appName = stringResource(R.string.app_name),
                    appBuild = BuildConfig.VERSION_CODE,
                    appVersion = BuildConfig.VERSION_NAME,
                    "https://github.com/Escalar-Alcoia-i-Comtat/Android"
                )
            }
        }
    }
}
