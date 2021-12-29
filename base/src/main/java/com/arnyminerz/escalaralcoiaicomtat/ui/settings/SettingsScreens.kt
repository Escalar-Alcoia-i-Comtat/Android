package com.arnyminerz.escalaralcoiaicomtat.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsCategory
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsDataDialog
import com.arnyminerz.escalaralcoiaicomtat.core.ui.element.settings.SettingsItem
import com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.SettingsViewModel

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

@Composable
fun GeneralSettingsScreen(viewModel: SettingsViewModel) {
    Column {
        val nearbyZonesEnabled by viewModel.nearbyZonesEnabled.collectAsState()
        val nearbyZonesDistance by viewModel.nearbyZonesDistance.collectAsState()

        // TODO: Language
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
            stateInt = nearbyZonesDistance,
            setInt = { value ->
                viewModel.setNearbyZonesDistance(value)
            },
            dialog = SettingsDataDialog(
                title = stringResource(R.string.pref_gene_nearby_distance_dialog_title),
                float = true,
                positiveButton = stringResource(R.string.action_ok),
                negativeButton = stringResource(R.string.action_close)
            )
        )
    }
}
