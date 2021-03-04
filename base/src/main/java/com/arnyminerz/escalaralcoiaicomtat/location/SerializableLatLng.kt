package com.arnyminerz.escalaralcoiaicomtat.location

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import java.io.Serializable

class SerializableLatLng(val latitude: Double, val longitude: Double) : Serializable {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

fun LatLng.serializable(): SerializableLatLng = SerializableLatLng(latitude, longitude)

fun LatLngBounds.Builder.include(latLng: SerializableLatLng) {
    include(latLng.toLatLng())
}