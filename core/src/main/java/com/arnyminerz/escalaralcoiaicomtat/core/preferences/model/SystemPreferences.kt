package com.arnyminerz.escalaralcoiaicomtat.core.preferences.model

/**
 * A data class for passing the system's preferences.
 * @author Arnau Mora
 * @since 20211229
 * @param shownIntro Whether or not the intro has been shown to the user.
 * @param shownBatteryOptimizationWarning Whether or not the battery optimization enabled warning
 * has been shown.
 * @param indexedData Whether or not the data has been indexed.
 * @param dataVersion The version of the indexed data, for checking for updates.
 * @param shownMd5Warning Whether or not the incompatible MD5 encryption warning has been shown.
 */
data class SystemPreferences(
    val shownIntro: Boolean,
    val shownBatteryOptimizationWarning: Boolean,
    val indexedData: Boolean,
    val dataVersion: Long,
    val shownMd5Warning: Boolean
)
