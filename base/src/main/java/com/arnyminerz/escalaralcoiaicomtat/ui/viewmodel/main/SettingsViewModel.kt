package com.arnyminerz.escalaralcoiaicomtat.ui.viewmodel.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.impl.NEARBY_DISTANCE_DEFAULT
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetAlertNotificationsEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetDataCollection
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetErrorCollection
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetLanguage
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetMarkerCentering
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetMeteredDownloadsEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetMobileDownloadsEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetNearbyZonesDistance
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetNearbyZonesEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.GetRoamingDownloadsEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetAlertNotificationsEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetDataCollection
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetErrorCollection
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetLanguage
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetMarkerCentering
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetMeteredDownloadsEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetMobileDownloadsEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetNearbyZonesDistance
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetNearbyZonesEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.user.SetRoamingDownloadsEnabled
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
    markerCentering: GetMarkerCentering,
    errorCollection: GetErrorCollection,
    dataCollection: GetDataCollection,
    alertNotificationsEnabled: GetAlertNotificationsEnabled,
    mobileDownloadsEnabled: GetMobileDownloadsEnabled,
    meteredDownloadsEnabled: GetMeteredDownloadsEnabled,
    roamingDownloadsEnabled: GetRoamingDownloadsEnabled,
    private val _setLanguage: SetLanguage,
    private val _setNearbyZonesEnabled: SetNearbyZonesEnabled,
    private val _setNearbyZonesDistance: SetNearbyZonesDistance,
    private val _setMarkerCentering: SetMarkerCentering,
    private val _setErrorCollection: SetErrorCollection,
    private val _setDataCollection: SetDataCollection,
    private val _setAlertNotificationsEnabled: SetAlertNotificationsEnabled,
    private val _setMobileDownloadsEnabled: SetMobileDownloadsEnabled,
    private val _setMeteredDownloadsEnabled: SetMeteredDownloadsEnabled,
    private val _setRoamingDownloadsEnabled: SetRoamingDownloadsEnabled
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

    val markerCentering: StateFlow<Boolean> = markerCentering().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    val errorCollection: StateFlow<Boolean> = errorCollection().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    val dataCollection: StateFlow<Boolean> = dataCollection().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    val alertNotificationsEnabled: StateFlow<Boolean> = alertNotificationsEnabled().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    val mobileDownloadsEnabled: StateFlow<Boolean> = mobileDownloadsEnabled().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    val roamingDownloadsEnabled: StateFlow<Boolean> = roamingDownloadsEnabled().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    val meteredDownloadsEnabled: StateFlow<Boolean> = meteredDownloadsEnabled().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
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

    fun setMarkerCentering(enabled: Boolean) {
        viewModelScope.launch { _setMarkerCentering(enabled) }
    }

    fun setErrorCollection(enabled: Boolean) {
        viewModelScope.launch { _setErrorCollection(enabled) }
    }

    fun setDataCollection(enabled: Boolean) {
        viewModelScope.launch { _setDataCollection(enabled) }
    }

    fun setAlertNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { _setAlertNotificationsEnabled(enabled) }
    }

    fun setMobileDownloadsEnabled(enabled: Boolean) {
        viewModelScope.launch { _setMobileDownloadsEnabled(enabled) }
    }

    fun setMeteredDownloadsEnabled(enabled: Boolean) {
        viewModelScope.launch { _setMeteredDownloadsEnabled(enabled) }
    }

    fun setRoamingDownloadsEnabled(enabled: Boolean) {
        viewModelScope.launch { _setRoamingDownloadsEnabled(enabled) }
    }

    class Factory(
        private val getLanguage: GetLanguage,
        private val getNearbyZonesEnabled: GetNearbyZonesEnabled,
        private val getNearbyZonesDistance: GetNearbyZonesDistance,
        private val getMarkerCentering: GetMarkerCentering,
        private val getErrorCollection: GetErrorCollection,
        private val getDataCollection: GetDataCollection,
        private val getAlertNotificationsEnabled: GetAlertNotificationsEnabled,
        private val getMobileDownloadsEnabled: GetMobileDownloadsEnabled,
        private val getMeteredDownloadsEnabled: GetMeteredDownloadsEnabled,
        private val getRoamingDownloadsEnabled: GetRoamingDownloadsEnabled,
        private val setLanguage: SetLanguage,
        private val setNearbyZonesEnabled: SetNearbyZonesEnabled,
        private val setNearbyZonesDistance: SetNearbyZonesDistance,
        private val setMarkerCentering: SetMarkerCentering,
        private val setErrorCollection: SetErrorCollection,
        private val setDataCollection: SetDataCollection,
        private val setAlertNotificationsEnabled: SetAlertNotificationsEnabled,
        private val setMobileDownloadsEnabled: SetMobileDownloadsEnabled,
        private val setMeteredDownloadsEnabled: SetMeteredDownloadsEnabled,
        private val setRoamingDownloadsEnabled: SetRoamingDownloadsEnabled,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java))
                return SettingsViewModel(
                    getLanguage,
                    getNearbyZonesEnabled,
                    getNearbyZonesDistance,
                    getMarkerCentering,
                    getErrorCollection,
                    getDataCollection,
                    getAlertNotificationsEnabled,
                    getMobileDownloadsEnabled,
                    getMeteredDownloadsEnabled,
                    getRoamingDownloadsEnabled,
                    setLanguage,
                    setNearbyZonesEnabled,
                    setNearbyZonesDistance,
                    setMarkerCentering,
                    setErrorCollection,
                    setDataCollection,
                    setAlertNotificationsEnabled,
                    setMobileDownloadsEnabled,
                    setMeteredDownloadsEnabled,
                    setRoamingDownloadsEnabled
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
        getLanguage,
        getNearbyZonesEnabled,
        getNearbyZonesDistance,
        getMarkerCentering,
        getErrorCollection,
        getDataCollection,
        getAlertNotificationsEnabled,
        getMobileDownloadsEnabled,
        getMeteredDownloadsEnabled,
        getRoamingDownloadsEnabled,
        setLanguage,
        setNearbyZonesEnabled,
        setNearbyZonesDistance,
        setMarkerCentering,
        setErrorCollection,
        setDataCollection,
        setAlertNotificationsEnabled,
        setMobileDownloadsEnabled,
        setMeteredDownloadsEnabled,
        setRoamingDownloadsEnabled
    )
