package com.arnyminerz.escalaralcoiaicomtat.core.data.climb

import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.Place
import androidx.compose.ui.graphics.vector.ImageVector
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
    val type: PointType = PointType.DEFAULT,
) : Parcelable {
    override fun toString(): String =
        "$label (${position.latitude}, ${position.longitude}) - $type"
}

enum class PointType(val icon: ImageVector) {
    DEFAULT(Icons.Rounded.Place),
    PARKING(Icons.Rounded.LocalParking),
    DRINK(Icons.Rounded.LocalDrink);

    companion object {
        fun fromString(text: String) =
            when (text) {
                "parking" -> PARKING
                "drink" -> DRINK
                else -> DEFAULT
            }
    }
}
