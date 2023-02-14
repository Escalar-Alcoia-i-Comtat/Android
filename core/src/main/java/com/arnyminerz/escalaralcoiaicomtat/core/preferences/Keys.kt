package com.arnyminerz.escalaralcoiaicomtat.core.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * The default value of the distance to consider as nearby for nearby zones.
 * @author Arnau Mora
 * @since 20211229
 */
const val NEARBY_DISTANCE_DEFAULT = 1000

object Keys {
    val nearbyZonesEnabled = booleanPreferencesKey("nearby_enabled")
    val nearbyZonesDistance = intPreferencesKey("nearby_distance")
    val showAlerts = booleanPreferencesKey("alerts_enabled")
    val centerMarkerOnClick = booleanPreferencesKey("center_marker")
    val mobileDownload = booleanPreferencesKey("mobile_download")
    val meteredDownload = booleanPreferencesKey("metered_download")
    val roamingDownload = booleanPreferencesKey("roaming_download")
    val downloadQuality = intPreferencesKey("download_quality")
    val enableDeveloperTab = booleanPreferencesKey("developer_tab")

    //<editor-fold desc="System preferences">
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
     * Stores the version of the software installed in the server.
     * @author Arnau Mora
     * @since 20220627
     */
    val serverVersion = stringPreferencesKey("server_version")

    /**
     * Stores whether or not the server that provided the data is marked as production.
     * @author Arnau Mora
     * @since 20220627
     */
    val serverIsProduction = booleanPreferencesKey("server_production")

    /**
     * Whether or not the incompatible MD5 encryption warning has been shown.
     * @author Arnau Mora
     * @since 20211229
     */
    val shownMd5Warning = booleanPreferencesKey("md5_warning")

    /**
     * Whether or not the SharedPreferences migration warning has been shown.
     * @author Arnau Mora
     * @since 20220316
     */
    val shownPreferencesWarning = booleanPreferencesKey("preferences_warning")
    //</editor-fold>
}
