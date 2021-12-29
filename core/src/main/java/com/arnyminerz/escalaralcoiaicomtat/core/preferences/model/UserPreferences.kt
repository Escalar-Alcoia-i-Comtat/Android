package com.arnyminerz.escalaralcoiaicomtat.core.preferences.model

/**
 * Stores the user's preferences.
 * @author Arnau Mora
 * @since 20211229
 * @param nearbyZonesEnabled If the nearby zones module should be enabled.
 * @param nearbyZonesDistance The distance at which a zone will be considered nearby.
 * @param dataCollection If analytics data should be collected.
 * @param errorCollection If error reporting data should be collected.
 * @param showAlerts If notification alerts should be displayed.
 * @param language The language key the user has chosen.
 * @param centerMarkerOnClick If when clicked a marker on the map it should be centered.
 * @param mobileDownload If downloads should be ran when using mobile data connections.
 * @param meteredDownload If downloads should be ran when using metered connections.
 * @param roamingDownload If downloads should be ran when using roaming connections.
 * @param downloadQuality The quality of compression of the downloaded images.
 */
data class UserPreferences(
    val nearbyZonesEnabled: Boolean,
    val nearbyZonesDistance: Int,
    val dataCollection: Boolean,
    val errorCollection: Boolean,
    val showAlerts: Boolean,
    val language: String,
    val centerMarkerOnClick: Boolean,
    val mobileDownload: Boolean,
    val meteredDownload: Boolean,
    val roamingDownload: Boolean,
    val downloadQuality: Int
)
