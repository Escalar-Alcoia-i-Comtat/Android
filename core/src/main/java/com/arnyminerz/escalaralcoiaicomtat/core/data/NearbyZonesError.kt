package com.arnyminerz.escalaralcoiaicomtat.core.data

enum class NearbyZonesError(val message: String) {
    NOT_LOADED("MapHelper is not loaded"),
    CONTEXT("Not showing fragment (context is null)"),
    NOT_ENABLED("Nearby Zones not enabled"),
    PERMISSION("Location permission not granted"),
    RESUMED("Not showing fragment (not resumed)"),
    EMPTY("AREAS is empty"),
    GPS_DISABLED("The device's GPS is turned off")
}
