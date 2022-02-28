package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
        val settingsTitle = stringResource(R.string.item_settings)
        var title by remember { mutableStateOf(settingsTitle) }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(visible = title != settingsTitle) {
                FloatingActionButton(
                    onClick = { onBackPressed() },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }
        NavHost(settingsNavController, "default") {
            composable("default") {
                title = settingsTitle
                MainSettingsScreen(this@SettingsScreen, settingsNavController)
            }
            composable("general") {
                title = stringResource(R.string.pref_main_title)
                GeneralSettingsScreen(this@SettingsScreen, settingsViewModel)
            }
            composable("notifications") {
                title = stringResource(R.string.pref_noti_title)
                NotificationsSettingsScreen(this@SettingsScreen, settingsViewModel)
            }
            composable("storage") {
                title = stringResource(R.string.pref_down_title)
                StorageSettingsScreen(settingsViewModel)
            }
            composable("info") {
                title = stringResource(R.string.pref_info_title)
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
