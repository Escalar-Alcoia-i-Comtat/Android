package com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.model.SystemPreferences
import kotlinx.coroutines.flow.Flow

/**
 * For interfacing the system preferences with some more usable functions.
 * @author Arnau Mora
 * @since 20211229
 */
interface SystemPreferencesRepository {
    /**
     * Should return the system's preferences mapped out to a [SystemPreferences] instance.
     * @author Arnau Mora
     * @since 20211229
     */
    val systemPreferences: Flow<SystemPreferences>

    /**
     * Sets the intro shown preference as true.
     * @author Arnau Mora
     * @since 20211229
     * @param shown Whether or not the intro has been shown. Usually will be true, but can be false
     * for showing intro again.
     */
    suspend fun markIntroAsShown(shown: Boolean = true)

    /**
     * Tells the system the battery optimization warning has been shown and should not be displayed
     * again.
     * @author Arnau Mora
     * @since 20211229
     */
    suspend fun markBatteryOptimizationWarningShown()

    /**
     * Tells the system that the data has already been indexed.
     * @author Arnau Mora
     * @since 20211229
     */
    suspend fun markDataIndexed(indexed: Boolean = true)

    /**
     * Sets the data indexed preference to false, so the system should index it again.
     * @author Arnau Mora
     * @since 20211229
     */
    suspend fun voidData()

    /**
     * Sets the version of the stored data, this way updates can be detected.
     * @author Arnau Mora
     * @since 20211229
     */
    suspend fun setDataVersion(version: Long)

    /**
     * Tells the system that the incompatible MD5 warning has already been shown, and should not
     * be displayed again.
     * @author Arnau Mora
     * @since 20211229
     */
    suspend fun markMd5WarningShown()

    /**
     * Should return the value of the shown intro preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val shownIntro: Flow<Boolean>

    /**
     * Should return the value of the shown MD5 warning preference.
     * @author Arnau Mora
     * @since 20220118
     */
    val shownMd5Warning: Flow<Boolean>

    /**
     * Should return the value of the indexed data preference.
     * @author Arnau Mora
     * @since 20220118
     */
    val indexedData: Flow<Boolean>

    /**
     * Should return the value of the stored data version preference.
     * @author Arnau Mora
     * @since 20220118
     */
    val dataVersion: Flow<Long>
}