package com.arnyminerz.escalaralcoiaicomtat.core.preferences

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.impl.SystemPreferencesRepositoryImpl
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.impl.UserPreferencesRepositoryImpl
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.GetIntroShown
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.GetNearbyZonesDistance
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.GetNearbyZonesEnabled
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.SetNearbyZonesDistance
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.usecase.system.SetNearbyZonesEnabled
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
     * Clears all the preferences from the system.
     * @author Arnau Mora
     * @since 20211229
     */
    suspend fun clear() {
        requireApplication.dataStore.edit { it.clear() }
    }
}
