package com.arnyminerz.escalaralcoiaicomtat.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.FeedbackActivity
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.Keys
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.NEARBY_DISTANCE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.collectAsState
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.setAsync
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.ListDialogOptions
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsCategory
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsDataDialog
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsItem
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import timber.log.Timber

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(context: Context, settingsNavController: NavController) {
    Column {
        SettingsItem(
            title = stringResource(R.string.pref_main_title),
            subtitle = stringResource(R.string.pref_main_sum),
            onClick = {
                settingsNavController.navigate("general")
            },
            icon = Icons.Default.Star
        )
        SettingsItem(
            title = stringResource(R.string.pref_noti_title),
            subtitle = stringResource(R.string.pref_noti_sum),
            onClick = {
                settingsNavController.navigate("notifications")
            },
            icon = Icons.Default.Notifications
        )
        SettingsItem(
            title = stringResource(R.string.pref_info_title),
            subtitle = stringResource(R.string.pref_info_sum),
            onClick = {
                settingsNavController.navigate("info")
            },
            icon = Icons.Default.Info
        )
        SettingsItem(
            title = stringResource(R.string.pref_feedback_title),
            subtitle = stringResource(R.string.pref_feedback_sum),
            onClick = {
                context.launch(FeedbackActivity::class.java)
            },
            icon = Icons.Default.BugReport
        )
        if (BuildConfig.DEBUG) {
            val showDeveloperTab by collectAsState(Keys.enableDeveloperTab, true)
            SettingsItem(
                title = stringResource(R.string.pref_main_show_developer_tab_title),
                subtitle = stringResource(R.string.pref_main_show_developer_tab_sum),
                stateBoolean = showDeveloperTab,
                switch = true,
                setBoolean = {
                    context.setAsync(Keys.enableDeveloperTab, !showDeveloperTab)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen() {
    val context = LocalContext.current

    Column {
        val nearbyZonesEnabled by collectAsState(Keys.nearbyZonesEnabled, true)
        val nearbyZonesDistance by collectAsState(Keys.nearbyZonesDistance, NEARBY_DISTANCE_DEFAULT)
        val markerClickCenteringEnabled by collectAsState(Keys.centerMarkerOnClick, true)

        SettingsItem(
            title = stringResource(R.string.pref_gene_language_title),
            subtitle = stringResource(R.string.pref_gene_language_sum),
            setString = { lang ->
                Timber.i("Setting app locale to $lang...")
                val appLocale = LocaleListCompat.forLanguageTags(lang)
                AppCompatDelegate.setApplicationLocales(appLocale)
            },
            dialog = SettingsDataDialog(
                title = stringResource(R.string.pref_gene_language_title),
                list = ListDialogOptions(
                    items = mapOf(
                        "en" to "English",
                        "ca" to "Català",
                        "es" to "Castellano"
                    ),
                    showSelectedItem = false,
                )
            )
        )

        Divider()
        SettingsCategory(
            stringResource(R.string.pref_gene_section_nearby)
        )
        SettingsItem(
            title = stringResource(R.string.pref_gene_nearby_title),
            subtitle = stringResource(R.string.pref_gene_nearby_sum),
            stateBoolean = nearbyZonesEnabled,
            setBoolean = { value ->
                context.setAsync(Keys.nearbyZonesEnabled, value)
            },
            switch = true
        )
        SettingsItem(
            title = stringResource(R.string.pref_gene_nearby_distance_title),
            subtitle = stringResource(R.string.pref_gene_nearby_distance_sum),
            enabled = nearbyZonesEnabled,
            stateInt = nearbyZonesDistance,
            setInt = { value ->
                context.setAsync(Keys.nearbyZonesDistance, value)
            },
            dialog = SettingsDataDialog(
                title = stringResource(R.string.pref_gene_nearby_distance_dialog_title),
                integer = true,
                positiveButton = stringResource(R.string.action_ok),
                negativeButton = stringResource(R.string.action_close)
            )
        )

        Divider()
        SettingsCategory(stringResource(R.string.pref_gene_section_map))
        SettingsItem(
            title = stringResource(R.string.pref_gene_map_move_marker_title),
            subtitle = stringResource(R.string.pref_gene_map_move_marker_sum),
            stateBoolean = markerClickCenteringEnabled,
            setBoolean = { value ->
                context.setAsync(Keys.centerMarkerOnClick, value)
            },
            switch = true
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen() {
    val context = LocalContext.current

    Column {
        val alertsEnabled by collectAsState(Keys.showAlerts, true)

        SettingsItem(
            title = stringResource(R.string.pref_noti_alert_title),
            subtitle = stringResource(R.string.pref_noti_alert_sum),
            stateBoolean = alertsEnabled,
            setBoolean = { value ->
                context.setAsync(Keys.showAlerts, value)
            },
            switch = true
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            SettingsItem(
                title = stringResource(R.string.pref_noti_device_title),
                subtitle = stringResource(R.string.pref_noti_device_sum),
                onClick = {
                    context.launch(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    ) {
                        putExtra(
                            Settings.EXTRA_APP_PACKAGE,
                            context.packageName
                        )
                    }
                }
            )
    }
}
