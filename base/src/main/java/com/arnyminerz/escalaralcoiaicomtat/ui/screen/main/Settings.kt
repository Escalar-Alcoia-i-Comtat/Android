package com.arnyminerz.escalaralcoiaicomtat.ui.screen.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.core.data.SemVer
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.ui.isolated_screen.ApplicationInfoWindow
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.settings.GeneralSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.settings.MainSettingsScreen
import com.arnyminerz.escalaralcoiaicomtat.ui.screen.settings.NotificationsSettingsScreen
import com.google.accompanist.pager.ExperimentalPagerApi

/**
 * The Settings screen of the Main Activity.
 * @author Arnau Mora
 * @since 20220102
 */
@Composable
@ExperimentalPagerApi
@ExperimentalMaterial3Api
fun MainActivity.SettingsScreen(settingsNavController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        val settingsTitle = stringResource(R.string.item_settings)
        val loadingPlaceholder = stringResource(R.string.status_loading)
        var title by remember { mutableStateOf(settingsTitle) }
        val serverVersion by PreferencesModule.systemPreferencesRepository
            .getServerVersion
            .collectAsState(loadingPlaceholder)
        val serverIsProduction: Boolean by PreferencesModule.systemPreferencesRepository
            .getServerIsProduction
            .collectAsState(true)

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FloatingActionButton(
                onClick = ::backHandler,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(
                    if (title != settingsTitle)
                        Icons.Rounded.ChevronLeft
                    else
                        Icons.Rounded.Close,
                    contentDescription = if (title != settingsTitle)
                        stringResource(R.string.action_back)
                    else
                        stringResource(R.string.fab_desc_view_main_screen),
                )
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
                GeneralSettingsScreen(settingsViewModel)
            }
            composable("notifications") {
                title = stringResource(R.string.pref_noti_title)
                NotificationsSettingsScreen(this@SettingsScreen, settingsViewModel)
            }
            composable("info") {
                title = stringResource(R.string.pref_info_title)
                ApplicationInfoWindow(
                    appIconResource = R.mipmap.ic_launcher_round,
                    appName = stringResource(R.string.app_name),
                    appBuild = BuildConfig.VERSION_CODE,
                    appVersion = BuildConfig.VERSION_NAME,
                    serverVersion = SemVer.fromString(serverVersion),
                    serverProduction = serverIsProduction,
                    "https://github.com/Escalar-Alcoia-i-Comtat/Android",
                    "https://escalaralcoiaicomtat.org/",
                )
            }
        }
    }
}
