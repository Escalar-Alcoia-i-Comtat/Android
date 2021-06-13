package com.arnyminerz.escalaralcoiaicomtat.core.data

enum class NearbyZonesError(val message: String) {
    NEARBY_ZONES_NOT_INITIALIZED("MapHelper is not initialized"),
    NEARBY_ZONES_NOT_LOADED("MapHelper is not loaded"),
    NEARBY_ZONES_CONTEXT("Not showing fragment (context is null)"),
    NEARBY_ZONES_NOT_ENABLED("Nearby Zones not enabled"),
    NEARBY_ZONES_PERMISSION("Location permission not granted"),
    NEARBY_ZONES_RESUMED("Not showing fragment (not resumed)"),
    NEARBY_ZONES_EMPTY("AREAS is empty"),
    NEARBY_ZONES_GPS_DISABLED("The device's GPS is turned off")
}
