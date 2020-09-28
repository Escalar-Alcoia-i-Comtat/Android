package com.arnyminerz.escalaralcoiaicomtat.data.map

import android.graphics.Color
import com.google.android.libraries.maps.model.Cap
import java.io.Serializable

data class GeoStyle(
    val fillColor: String?,
    val strokeColor: String?,
    val lineWidth: Float?,
    val lineCap: Cap?,
    val lineJoint: Int?
) : Serializable {
    private fun colorIsNull(color: String?) =
        color == null || !color.startsWith("#") || color.contains("null")

    fun fillColor(): Int? = if (!colorIsNull(fillColor)) try {
        Color.parseColor(fillColor)
    } catch (ex: NumberFormatException) {
        null
    } else null

    fun strokeColor(): Int? = if (!colorIsNull(strokeColor)) try {
        Color.parseColor(strokeColor)
    } catch (ex: NumberFormatException) {
        null
    } else null
}