package com.arnyminerz.escalaralcoiaicomtat.location

import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.LatLngBounds
import java.io.Serializable

class SerializableLatLng(val latitude: Double, val longitude: Double) : Serializable {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

fun LatLng.serializable(): SerializableLatLng = SerializableLatLng(latitude, longitude)

fun LatLngBounds.Builder.include(latLng: SerializableLatLng) {
    include(latLng.toLatLng())
}