package com.arnyminerz.escalaralcoiaicomtat.core.preferences.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
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
         * Whether or not the device is waiting for the user to confirm their email address.
         * @author Arnau Mora
         * @since 20211229
         */
        val waitingForEmailConfirmation = booleanPreferencesKey("waiting_email")

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
        val dataVersion = intPreferencesKey("data_version")

        /**
         * Whether or not the incompatible MD5 encryption warning has been shown.
         * @author Arnau Mora
         * @since 20211229
         */
        val shownMd5Warning = booleanPreferencesKey("md5_warning")
    }

    private inline val Preferences.shownIntro
        get() = this[Keys.shownIntro] ?: false

    private inline val Preferences.waitingForEmailConfirmation
        get() = this[Keys.waitingForEmailConfirmation] ?: false

    private inline val Preferences.shownBatteryOptimizationWarning
        get() = this[Keys.shownBatteryOptimizationWarning] ?: false

    private inline val Preferences.indexedData
        get() = this[Keys.indexedData] ?: false

    private inline val Preferences.dataVersion
        get() = this[Keys.dataVersion] ?: -1

    private inline val Preferences.shownMd5Warning
        get() = this[Keys.shownMd5Warning] ?: false

    override val systemPreferences: Flow<SystemPreferences> = dataStore.data
        .catch {
            if (it is IOException)
                emit(emptyPreferences())
            else throw it
        }
        .map { preferences ->
            SystemPreferences(
                preferences.shownIntro,
                preferences.waitingForEmailConfirmation,
                preferences.shownBatteryOptimizationWarning,
                preferences.indexedData,
                preferences.dataVersion,
                preferences.shownMd5Warning
            )
        }
        .distinctUntilChanged()

    override suspend fun markIntroAsShown() {
        dataStore.edit {
            it[Keys.shownIntro] = true
        }
    }

    override suspend fun setWaitingForEmailConfirmation(waiting: Boolean) {
        dataStore.edit {
            it[Keys.waitingForEmailConfirmation] = waiting
        }
    }

    override suspend fun markBatteryOptimizationWarningShown() {
        dataStore.edit {
            it[Keys.shownBatteryOptimizationWarning] = true
        }
    }

    override suspend fun markDataIndexed() {
        dataStore.edit {
            it[Keys.indexedData] = true
        }
    }

    override suspend fun voidData() {
        dataStore.edit {
            it[Keys.indexedData] = false
        }
    }

    override suspend fun setDataVersion(version: Int) {
        dataStore.edit {
            it[Keys.dataVersion] = version
        }
    }

    override suspend fun markMd5WarningShown() {
        dataStore.edit {
            it[Keys.shownMd5Warning] = true
        }
    }

    /**
     * Returns whether or not the intro has been shown.
     * @author Arnau Mora
     * @since 20211229
     */
    override val shownIntro: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map {
            it[Keys.shownIntro] ?: false
        }
        .distinctUntilChanged()

    /**
     * Returns whether or not the MD5 warning has been shown.
     * @author Arnau Mora
     * @since 20211229
     */
    override val shownMd5Warning: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map {
            it[Keys.shownMd5Warning] ?: false
        }
        .distinctUntilChanged()
}