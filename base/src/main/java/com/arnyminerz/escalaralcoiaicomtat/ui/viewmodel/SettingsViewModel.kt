package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.impl.NEARBY_DISTANCE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetLanguage
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetNearbyZonesDistance
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetNearbyZonesEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetLanguage
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetNearbyZonesDistance
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetNearbyZonesEnabled
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class SettingsViewModel(
    language: GetLanguage,
    nearbyZonesEnabled: GetNearbyZonesEnabled,
    nearbyZonesDistance: GetNearbyZonesDistance,
    private val _setLanguage: SetLanguage,
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

    val language: StateFlow<String> = language().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Locale.getDefault().language
    )

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

    fun setLanguage(language: String) {
        viewModelScope.launch { _setLanguage(language) }
    }

    fun setNearbyZonesEnabled(enabled: Boolean) {
        viewModelScope.launch { _setNearbyZonesEnabled(enabled) }
    }

    fun setNearbyZonesDistance(distance: Int) {
        viewModelScope.launch { _setNearbyZonesDistance(distance) }
    }

    class Factory(
        private val getLanguage: GetLanguage,
        private val getNearbyZonesEnabled: GetNearbyZonesEnabled,
        private val getNearbyZonesDistance: GetNearbyZonesDistance,
        private val setLanguage: SetLanguage,
        private val setNearbyZonesEnabled: SetNearbyZonesEnabled,
        private val setNearbyZonesDistance: SetNearbyZonesDistance,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java))
                return SettingsViewModel(
                    getLanguage,
                    getNearbyZonesEnabled,
                    getNearbyZonesDistance,
                    setLanguage,
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
        this.getLanguage,
        this.getNearbyZonesEnabled,
        this.getNearbyZonesDistance,
        this.setLanguage,
        this.setNearbyZonesEnabled,
        this.setNearbyZonesDistance,
    )
