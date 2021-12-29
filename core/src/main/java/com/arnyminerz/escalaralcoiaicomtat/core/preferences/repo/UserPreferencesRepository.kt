package com.arnyminerz.escalaralcoiaicomtat.core.preferences.repo

import com.arnyminerz.escalaralcoiaicomtat.core.preferences.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * For interfacing the user preferences with some more usable functions.
 * @author Arnau Mora
 * @since 20211229
 */
interface UserPreferencesRepository {
    /**
     * Should return the user's preferences mapped out to the [UserPreferences] data class.
     * @author Arnau Mora
     * @since 20211229
     */
    val userPreferences: Flow<UserPreferences>

    /**
     * Enables or disables the nearby zones.
     * @author Arnau Mora
     * @since 20211229
     * @param enabled If true, nearby zones will be enabled, false will disable.
     */
    suspend fun setNearbyZonesEnabled(enabled: Boolean)

    /**
     * Sets the distance until which a zone will be considered as nearby.
     * @author Arnau Mora
     * @since 20211229
     * @param distance The distance in meters to consider as nearby.
     */
    suspend fun setNearbyZonesDistance(distance: Int)

    /**
     * Enables or disables the user data collection.
     * @author Arnau Mora
     * @since 20211229
     * @param enabled If true, user data collection will be enabled, false will disable.
     */
    suspend fun setDataCollectionEnabled(enabled: Boolean)

    /**
     * Enables or disables the error data collection.
     * @author Arnau Mora
     * @since 20211229
     * @param enabled If true, error collection will be enabled, false will disable.
     */
    suspend fun setErrorCollectionEnabled(enabled: Boolean)

    /**
     * Enables or disables the alert notifications.
     * @author Arnau Mora
     * @since 20211229
     * @param enabled If true, alert notifications will be enabled, false will disable.
     */
    suspend fun setDisplayAlertsEnabled(enabled: Boolean)

    /**
     * Sets the language to use in the app.
     * @author Arnau Mora
     * @since 20211229
     * @param language The language in ISO format to use in the app.
     */
    suspend fun setLanguage(language: String)

    /**
     * Enables or disables centering the map when clicked a marker.
     * @author Arnau Mora
     * @since 20211229
     * @param enabled If true, marker centering will be enabled, false will disable.
     */
    suspend fun setMarkerClickCenteringEnabled(enabled: Boolean)

    /**
     * Enables or disables downloading when connected to a mobile data network.
     * @author Arnau Mora
     * @since 20211229
     * @param enabled If true, mobile data downloads will be enabled, false will disable.
     */
    suspend fun setDownloadMobileEnabled(enabled: Boolean)

    /**
     * Enables or disables downloading when connected to a metered data network.
     * @author Arnau Mora
     * @since 20211229
     * @param enabled If true, metered downloads will be enabled, false will disable.
     */
    suspend fun setDownloadMeteredEnabled(enabled: Boolean)

    /**
     * Enables or disables downloading when connected to a roaming network.
     * @author Arnau Mora
     * @since 20211229
     * @param enabled If true, roaming downloads will be enabled, false will disable.
     */
    suspend fun setDownloadRoamingEnabled(enabled: Boolean)

    /**
     * Sets the quality of the downloaded images.
     * @author Arnau Mora
     * @since 20211229
     * @param quality The quality of the images downloaded in percentage.
     */
    suspend fun setDownloadQuality(quality: Int)

    /**
     * Should return the value of the language preference of the user.
     * @author Arnau Mora
     * @since 20211229
     */
    val language: Flow<String>

    /**
     * Should return the value of the nearby zones enabled preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val nearbyZonesEnabled: Flow<Boolean>

    /**
     * Should return the value of the nearby zones distance preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val nearbyZonesDistance: Flow<Int>

    /**
     * Should return the value of the center map marker on click preference.
     * @author Arnau Mora
     * @since 20211229
     */
    val markerClickCenteringEnabled: Flow<Boolean>

    /**
     * Should return whether or not error collection is enabled.
     * @author Arnau Mora
     * @since 20211229
     */
    val errorCollectionEnabled: Flow<Boolean>

    /**
     * Should return whether or not user data collection is enabled.
     * @author Arnau Mora
     * @since 20211229
     */
    val dataCollectionEnabled: Flow<Boolean>

    /**
     * Should return whether or not the user has enabled alert notifications.
     * @author Arnau Mora
     * @since 20211229
     */
    val alertNotificationsEnabled: Flow<Boolean>

    /**
     * Should return whether or not the user has chosen to run downloads while on mobile network.
     * @author Arnau Mora
     * @since 20211229
     */
    val mobileDownloadsEnabled: Flow<Boolean>

    /**
     * Should return whether or not the user has chosen to run downloads while on roaming network.
     * @author Arnau Mora
     * @since 20211229
     */
    val roamingDownloadsEnabled: Flow<Boolean>

    /**
     * Should return whether or not the user has chosen to run downloads while on metered network.
     * @author Arnau Mora
     * @since 20211229
     */
    val meteredDownloadsEnabled: Flow<Boolean>
}