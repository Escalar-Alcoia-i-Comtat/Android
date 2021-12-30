package com.arnyminerz.escalaralcoiaicomtat.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.isolated.FeedbackActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.ListDialogOptions
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsCategory
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsDataDialog
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsItem
import com.arnyminerz.escalaralcoiaicomtat.core.utils.context.LocaleHelper
import com.arnyminerz.escalaralcoiaicomtat.core.utils.launch
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterialApi::class)
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
            title = stringResource(R.string.pref_down_title),
            subtitle = stringResource(R.string.pref_down_sum),
            onClick = {
                settingsNavController.navigate("storage")
            },
            icon = Icons.Default.Storage
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
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GeneralSettingsScreen(activity: LanguageComponentActivity, viewModel: SettingsViewModel) {
    Column {
        val language by viewModel.language.collectAsState()
        val nearbyZonesEnabled by viewModel.nearbyZonesEnabled.collectAsState()
        val nearbyZonesDistance by viewModel.nearbyZonesDistance.collectAsState()
        val markerClickCenteringEnabled by viewModel.markerCentering.collectAsState()
        val errorCollectionEnabled by viewModel.errorCollection.collectAsState()
        // TODO: Add individual data collection enable switch
        // val dataCollectionEnabled by viewModel.dataCollection.collectAsState()

        SettingsItem(
            title = stringResource(R.string.pref_gene_language_title),
            subtitle = stringResource(R.string.pref_gene_language_sum),
            stateString = language,
            setString = { lang ->
                viewModel.setLanguage(lang)
                LocaleHelper.setLocale(activity, lang)
                activity.recreate()
            },
            dialog = SettingsDataDialog(
                title = stringResource(R.string.pref_gene_language_title),
                list = ListDialogOptions(
                    items = mapOf(
                        "en" to "English",
                        "ca" to "Català",
                        "es" to "Castellano"
                    )
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
                viewModel.setNearbyZonesEnabled(value)
            },
            switch = true
        )
        SettingsItem(
            title = stringResource(R.string.pref_gene_nearby_distance_title),
            subtitle = stringResource(R.string.pref_gene_nearby_distance_sum),
            enabled = nearbyZonesEnabled,
            stateInt = nearbyZonesDistance,
            setInt = { value ->
                viewModel.setNearbyZonesDistance(value)
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
                viewModel.setMarkerCentering(value)
            },
            switch = true
        )

        Divider()
        SettingsCategory(stringResource(R.string.pref_gene_section_advanced))
        SettingsItem(
            title = stringResource(R.string.pref_gene_error_reporting_title),
            subtitle = stringResource(R.string.pref_gene_error_reporting_sum),
            stateBoolean = errorCollectionEnabled,
            setBoolean = { value ->
                viewModel.setErrorCollection(value)
                viewModel.setDataCollection(value)
            },
            switch = true
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NotificationsSettingsScreen(context: Context, viewModel: SettingsViewModel) {
    Column {
        val alertsEnabled by viewModel.alertNotificationsEnabled.collectAsState()

        SettingsItem(
            title = stringResource(R.string.pref_noti_alert_title),
            subtitle = stringResource(R.string.pref_noti_alert_sum),
            stateBoolean = alertsEnabled,
            setBoolean = { value ->
                viewModel.setAlertNotificationsEnabled(value)
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StorageSettingsScreen(viewModel: SettingsViewModel) {
    Column {
        val mobileDataDownload by viewModel.mobileDownloadsEnabled.collectAsState()
        val roamingDownload by viewModel.roamingDownloadsEnabled.collectAsState()
        // TODO: Add option for metered networks
        // val meteredDownload by viewModel.nearbyZonesEnabled.collectAsState()

        SettingsCategory(
            stringResource(R.string.pref_down_cat_downloads)
        )
        SettingsItem(
            title = stringResource(R.string.pref_down_mobile_title),
            subtitle = stringResource(R.string.pref_down_mobile_sum),
            stateBoolean = mobileDataDownload,
            setBoolean = { value ->
                viewModel.setMobileDownloadsEnabled(value)
            },
            switch = true
        )
        SettingsItem(
            title = stringResource(R.string.pref_down_roaming_title),
            subtitle = stringResource(R.string.pref_down_roaming_sum),
            stateBoolean = roamingDownload,
            setBoolean = { value ->
                viewModel.setRoamingDownloadsEnabled(value)
            },
            switch = true
        )
    }
}
