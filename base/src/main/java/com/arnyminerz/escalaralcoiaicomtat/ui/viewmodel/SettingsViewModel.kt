package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.impl.NEARBY_DISTANCE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.GetNearbyZonesDistance
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.GetNearbyZonesEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.SetNearbyZonesDistance
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.SetNearbyZonesEnabled
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsViewModel(
    nearbyZonesEnabled: GetNearbyZonesEnabled,
    nearbyZonesDistance: GetNearbyZonesDistance,
    private val _setNearbyZonesEnabled: SetNearbyZonesEnabled,
    private val _setNearbyZonesDistance: SetNearbyZonesDistance,
) : ViewModel() {
    init {
        Timber.d("$this::init")
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("$this::onCleared")
    }

    val nearbyZonesEnabled: StateFlow<Boolean> = nearbyZonesEnabled().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    val nearbyZonesDistance: StateFlow<Int> = nearbyZonesDistance().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        NEARBY_DISTANCE_DEFAULT
    )

    fun setNearbyZonesEnabled(enabled: Boolean) {
        viewModelScope.launch { _setNearbyZonesEnabled(enabled) }
    }

    fun setNearbyZonesDistance(distance: Int) {
        viewModelScope.launch { _setNearbyZonesDistance(distance) }
    }

    class Factory(
        private val getNearbyZonesEnabled: GetNearbyZonesEnabled,
        private val getNearbyZonesDistance: GetNearbyZonesDistance,
        private val setNearbyZonesEnabled: SetNearbyZonesEnabled,
        private val setNearbyZonesDistance: SetNearbyZonesDistance,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java))
                return SettingsViewModel(
                    getNearbyZonesEnabled,
                    getNearbyZonesDistance,
                    setNearbyZonesEnabled,
                    setNearbyZonesDistance
                ) as T
            error("Unknown view model class: $modelClass")
        }
    }
}

/**
 * Returns the factory for the view model for the settings screen.
 * @author Arnau Mora
 * @since 20211229
 */
val PreferencesModule.settingsViewModel
    get() = SettingsViewModel.Factory(
        this.getNearbyZonesEnabled,
        this.getNearbyZonesDistance,
        this.setNearbyZonesEnabled,
        this.setNearbyZonesDistance,
    )
