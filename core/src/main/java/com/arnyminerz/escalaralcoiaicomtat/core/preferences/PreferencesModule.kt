package com.arnyminerz.escalaralcoiaicomtat.core.preferences

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.impl.SystemPreferencesRepositoryImpl
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.impl.UserPreferencesRepositoryImpl
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.GetIntroShown
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.GetShownMd5Warning
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.GetWaitingForEmailConfirmation
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
import com.arnyminerz.escalaralcoiaicomtat.core.shared.PREFERENCES_NAME

/**
 * The module for modifying preferences.
 * @author Arnau Mora
 * @since 20211229
 */
object PreferencesModule {
    /**
     * The application instance for loading the datastore.
     * Should be initialized with [initWith].
     * @author Arnau Mora
     * @since 20211229
     */
    private var application: Application? = null

    /**
     * Returns the [application] instance or returns an error.
     * @author Arnau Mora
     * @since 20211229
     */
    private inline val requireApplication
        get() = application ?: error("Missing call: initWith(application)")

    /**
     * Initializes the [PreferencesModule] class.
     * @author Arnau Mora
     * @since 20211229
     * @param application The application instance that is initializing the module.
     */
    fun initWith(application: Application) {
        this.application = application
    }

    /**
     * Implements the datastore using [preferencesDataStore].
     * @author Arnau Mora
     * @since 20211229
     */
    private val Context.dataStore by preferencesDataStore(name = PREFERENCES_NAME)

    /**
     * The user preferences repository.
     * @author Arnau Mora
     * @since 20211229
     */
    private val userPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(requireApplication.dataStore)
    }

    /**
     * The system preferences repository.
     * @author Arnau Mora
     * @since 20211229
     */
    val systemPreferencesRepository by lazy {
        SystemPreferencesRepositoryImpl(requireApplication.dataStore)
    }

    /**
     * Returns the intro shown preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val introShown get() = GetIntroShown(systemPreferencesRepository)

    /**
     * Returns the shown MD5 warning preference.
     * @author Arnau Mora
     * @since 20220118
     */
    val shownMd5Warning get() = GetShownMd5Warning(systemPreferencesRepository)

    /**
     * Returns the waiting for email confirmation preference.
     * @author Arnau Mora
     * @since 20220118
     */
    val waitingForEmailConfirmation
        get() = GetWaitingForEmailConfirmation(
            systemPreferencesRepository
        )

    /**
     * Returns the language preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getLanguage get() = GetLanguage(userPreferencesRepository)

    /**
     * Returns the nearby zones enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getNearbyZonesEnabled get() = GetNearbyZonesEnabled(userPreferencesRepository)

    /**
     * Returns the nearby zones distance preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getNearbyZonesDistance get() = GetNearbyZonesDistance(userPreferencesRepository)

    /**
     * Returns the center marker on click enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getMarkerCentering get() = GetMarkerCentering(userPreferencesRepository)

    /**
     * Returns the error collection enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getErrorCollection get() = GetErrorCollection(userPreferencesRepository)

    /**
     * Returns the data collection enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getDataCollection get() = GetDataCollection(userPreferencesRepository)

    /**
     * Returns the alert notifications enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getAlertNotificationsEnabled get() = GetAlertNotificationsEnabled(userPreferencesRepository)

    /**
     * Returns the mobile downloads enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getMobileDownloadsEnabled get() = GetMobileDownloadsEnabled(userPreferencesRepository)

    /**
     * Returns the metered downloads enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getMeteredDownloadsEnabled get() = GetMeteredDownloadsEnabled(userPreferencesRepository)

    /**
     * Returns the roaming downloads enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val getRoamingDownloadsEnabled get() = GetRoamingDownloadsEnabled(userPreferencesRepository)

    /**
     * Used for updating the user's language preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setLanguage get() = SetLanguage(userPreferencesRepository)

    /**
     * Used for updating the nearby zones enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setNearbyZonesEnabled get() = SetNearbyZonesEnabled(userPreferencesRepository)

    /**
     * Used for updating the nearby zones distance preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setNearbyZonesDistance get() = SetNearbyZonesDistance(userPreferencesRepository)

    /**
     * Used for updating the marker centering enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setMarkerCentering get() = SetMarkerCentering(userPreferencesRepository)

    /**
     * Used for updating the error collection enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setErrorCollection get() = SetErrorCollection(userPreferencesRepository)

    /**
     * Used for updating the data collection enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setDataCollection get() = SetDataCollection(userPreferencesRepository)

    /**
     * Used for updating the alert notifications enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setAlertNotificationsEnabled get() = SetAlertNotificationsEnabled(userPreferencesRepository)

    /**
     * Used for updating the mobile data downloads enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setMobileDownloadsEnabled get() = SetMobileDownloadsEnabled(userPreferencesRepository)

    /**
     * Used for updating the metered data downloads enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setMeteredDownloadsEnabled get() = SetMeteredDownloadsEnabled(userPreferencesRepository)

    /**
     * Used for updating the roaming data downloads enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val setRoamingDownloadsEnabled get() = SetRoamingDownloadsEnabled(userPreferencesRepository)

    /**
     * Clears all the preferences from the system.
     * @author Arnau Mora
     * @since 20211229
     */
    suspend fun clear() {
        requireApplication.dataStore.edit { it.clear() }
    }
}
