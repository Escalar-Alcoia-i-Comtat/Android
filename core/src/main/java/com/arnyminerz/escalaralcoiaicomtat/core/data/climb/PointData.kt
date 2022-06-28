package com.arnyminerz.escalaralcoiaicomtat.core.data.climb

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.osmdroid.util.GeoPoint

/**
 * Represents a point with a description label.
 * @author Arnau Mora
 * @since 20220627
 * @param position The position of the point.
 * @param label The label of the point.
 */
@Parcelize
data class PointData(
    val position: GeoPoint,
    val label: String,
) : Parcelable {
    override fun toString(): String =
        "$label (${position.latitude}, ${position.longitude})"
}
