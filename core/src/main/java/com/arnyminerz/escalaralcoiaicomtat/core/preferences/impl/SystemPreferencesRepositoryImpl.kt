package com.arnyminerz.escalaralcoiaicomtat.core.preferences.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.model.SystemPreferences
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo.SystemPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * The implementation of the system preferences repository so it can be used.
 * @author Arnau Mora
 * @since 20211229
 * @param dataStore The data store to use for applying preferences.
 */
class SystemPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SystemPreferencesRepository {
    object Keys {
        /**
         * Whether or not the intro has been shown to the user.
         * @author Arnau Mora
         * @since 20211229
         */
        val shownIntro = booleanPreferencesKey("shown_intro")

        /**
         * Whether or not the battery optimization enabled warning has been shown.
         * @author Arnau Mora
         * @since 20211229
         */
        val shownBatteryOptimizationWarning = booleanPreferencesKey("battery_optimization")

        /**
         * Whether or not the data has been indexed.
         * @author Arnau Mora
         * @since 20211229
         */
        val indexedData = booleanPreferencesKey("data_indexed")

        /**
         * The version of the indexed data, for checking for updates.
         * @author Arnau Mora
         * @since 20211229
         */
        val dataVersion = longPreferencesKey("data_version")

        /**
         * Whether or not the incompatible MD5 encryption warning has been shown.
         * @author Arnau Mora
         * @since 20211229
         */
        val shownMd5Warning = booleanPreferencesKey("md5_warning")

        /**
         * Whether or not the Play Services not present warning has been shown.
         * @author Arnau Mora
         * @since 20220316
         */
        val shownPlayServicesWarning = booleanPreferencesKey("play_warning")

        /**
         * Whether or not the SharedPreferences migration warning has been shown.
         * @author Arnau Mora
         * @since 20220316
         */
        val shownPreferencesWarning = booleanPreferencesKey("preferences_warning")
    }

    private inline val Preferences.shownIntro
        get() = this[Keys.shownIntro] ?: false

    private inline val Preferences.shownBatteryOptimizationWarning
        get() = this[Keys.shownBatteryOptimizationWarning] ?: false

    private inline val Preferences.indexedData
        get() = this[Keys.indexedData] ?: false

    private inline val Preferences.dataVersion
        get() = this[Keys.dataVersion] ?: -1

    private inline val Preferences.shownMd5Warning
        get() = this[Keys.shownMd5Warning] ?: false

    private inline val Preferences.shownPlayServicesWarning
        get() = this[Keys.shownPlayServicesWarning] ?: false

    private inline val Preferences.shownPreferencesMigrationWarning
        get() = this[Keys.shownPreferencesWarning] ?: false

    override val systemPreferences: Flow<SystemPreferences> = dataStore.data
        .catch {
            if (it is IOException)
                emit(emptyPreferences())
            else throw it
        }
        .map { preferences ->
            SystemPreferences(
                preferences.shownIntro,
                preferences.shownBatteryOptimizationWarning,
                preferences.indexedData,
                preferences.dataVersion,
                preferences.shownMd5Warning,
                preferences.shownPlayServicesWarning,
                preferences.shownPreferencesMigrationWarning,
            )
        }
        .distinctUntilChanged()

    override suspend fun markIntroAsShown(shown: Boolean) {
        dataStore.edit {
            it[Keys.shownIntro] = shown
        }
    }

    override suspend fun markBatteryOptimizationWarningShown() {
        dataStore.edit {
            it[Keys.shownBatteryOptimizationWarning] = true
        }
    }

    override suspend fun markDataIndexed(indexed: Boolean) {
        dataStore.edit {
            it[Keys.indexedData] = indexed
        }
    }

    override suspend fun voidData() {
        dataStore.edit {
            it[Keys.indexedData] = false
        }
    }

    override suspend fun setDataVersion(version: Long) {
        dataStore.edit {
            it[Keys.dataVersion] = version
        }
    }

    override suspend fun markMd5WarningShown() {
        dataStore.edit {
            it[Keys.shownMd5Warning] = true
        }
    }

    override suspend fun shownPlayServicesWarning(shown: Boolean) {
        dataStore.edit {
            it[Keys.shownPlayServicesWarning] = shown
        }
    }

    override suspend fun shownPreferencesMigrationWarning(shown: Boolean) {
        dataStore.edit {
            it[Keys.shownPreferencesWarning] = shown
        }
    }

    private fun <R> getTheFlow(key: Preferences.Key<R>, default: R): Flow<R> = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map {
            it[key] ?: default
        }
        .distinctUntilChanged()

    /**
     * Returns whether or not the intro has been shown.
     * @author Arnau Mora
     * @since 20211229
     */
    override val shownIntro: Flow<Boolean> = getTheFlow(Keys.shownIntro, false)

    /**
     * Returns whether or not the MD5 warning has been shown.
     * @author Arnau Mora
     * @since 20211229
     */
    override val shownMd5Warning: Flow<Boolean> = getTheFlow(Keys.shownMd5Warning, false)

    /**
     * Returns whether or not the system has indexed all the data from the data module.
     * @author Arnau Mora
     * @since 20220118
     */
    override val indexedData: Flow<Boolean> = getTheFlow(Keys.indexedData, false)

    /**
     * Returns the version of the currently stored data.
     * @author Arnau Mora
     * @since 20220118
     */
    override val dataVersion: Flow<Long> = getTheFlow(Keys.dataVersion, -1L)

    /**
     * Returns whether or not the Play Services not present warning has been shown to the user.
     * @author Arnau Mora
     * @since 20220316
     */
    override val shownPlayServicesWarning: Flow<Boolean> =
        getTheFlow(Keys.shownPlayServicesWarning, false)

    /**
     * Returns whether or not the Preferences migration warning has been shown to the user.
     * @author Arnau Mora
     * @since 20220316
     */
    override val shownPreferencesWarning: Flow<Boolean> =
        getTheFlow(Keys.shownPreferencesWarning, false)
}