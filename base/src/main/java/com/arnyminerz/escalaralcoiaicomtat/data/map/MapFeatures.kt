package com.arnyminerz.escalaralcoiaicomtat.data.map

import java.io.Serializable

data class MapFeatures(
    val markers: ArrayList<GeoMarker>,
    val polylines: ArrayList<GeoGeometry>,
    val polygons: ArrayList<GeoGeometry>
) : Serializable
