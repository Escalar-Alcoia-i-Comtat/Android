package com.arnyminerz.escalaralcoiaicomtat.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.LanguageComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.ListDialogOptions
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsCategory
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsDataDialog
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsItem
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainSettingsScreen(settingsNavController: NavController) {
    SettingsItem(
        title = stringResource(R.string.pref_main_title),
        subtitle = stringResource(R.string.pref_main_sum),
        onClick = {
            settingsNavController.navigate("general")
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GeneralSettingsScreen(activity: LanguageComponentActivity, viewModel: SettingsViewModel) {
    Column {
        val language by viewModel.language.collectAsState()
        val nearbyZonesEnabled by viewModel.nearbyZonesEnabled.collectAsState()
        val nearbyZonesDistance by viewModel.nearbyZonesDistance.collectAsState()

        SettingsItem(
            title = stringResource(R.string.pref_gene_language_title),
            subtitle = stringResource(R.string.pref_gene_language_sum),
            stateString = language,
            setString = { lang ->
                viewModel.setLanguage(lang)
                activity.languageUpdate()
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
    }
}
