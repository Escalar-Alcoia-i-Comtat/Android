package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone

import android.content.Context
import android.content.Intent
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.PointData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.PointType
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassCompanion
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassDisplayOptions
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.dataClassExploreActivity
import com.arnyminerz.escalaralcoiaicomtat.core.shared.EXTRA_DATACLASS
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import com.arnyminerz.escalaralcoiaicomtat.core.utils.putExtra
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.Serializable

/**
 * Creates a new [Zone] instance.
 * @author Arnau Mora
 * @since 20210724
 */
@Parcelize
@Entity(tableName = "Zones")
class Zone(
    @PrimaryKey override val objectId: String,
    @ColumnInfo(name = "displayName") override val displayName: String,
    @ColumnInfo(name = "last_edit") override val timestampMillis: Long,
    @ColumnInfo(name = "image") override val imagePath: String,
    @ColumnInfo(name = "kmz") override val kmzPath: String?,
    @Ignore val position: GeoPoint,
    @ColumnInfo(name = "points", defaultValue = "") val pointsString: String,
    @ColumnInfo(name = "webURL") val webUrl: String?,
    @ColumnInfo(name = "area") val parentAreaId: String,
    @ColumnInfo(name = "childrenCount") val childrenCount: Long,
) : DataClass<Sector, Area>(
    displayName,
    timestampMillis,
    imagePath,
    kmzPath,
    position,
    DataClassMetadata(
        objectId,
        NAMESPACE,
        webUrl,
        parentAreaId,
        childrenCount,
    ),
    DataClassDisplayOptions(
        R.drawable.ic_tall_placeholder,
        R.drawable.ic_tall_placeholder,
        2,
        vertical = true,
        showLocation = true
    ),
) {
    /**
     * Creates a new [Zone] from the data from the Data Module.
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     * @param zoneId The ID of the Zone.
     */
    constructor(data: JSONObject, @ObjectId zoneId: String, childrenCount: Long) : this(
        zoneId,
        data.getString("displayName"),
        data.getDate("last_edit")!!.time,
        data.getString("image"),
        data.getString("kmz"),
        GeoPoint(
            data.getDouble("latitude"),
            data.getDouble("longitude")
        ),
        data.getString("points"),
        data.getString("webURL"),
        parentAreaId = data.getString("area"),
        childrenCount = childrenCount,
    )

    constructor(
        objectId: String,
        displayName: String,
        timestampMillis: Long,
        imagePath: String,
        kmzPath: String?,
        latitude: Double,
        longitude: Double,
        pointsString: String,
        webUrl: String?,
        parentAreaId: String,
        childrenCount: Long,
    ): this(
        objectId,
        displayName,
        timestampMillis,
        imagePath,
        kmzPath,
        GeoPoint(latitude, longitude),
        pointsString,
        webUrl,
        parentAreaId,
        childrenCount,
    )

    @Ignore
    @IgnoredOnParcel
    override val imageQuality: Int = IMAGE_QUALITY

    @Ignore
    @IgnoredOnParcel
    override val hasParents: Boolean = true

    @Ignore
    @IgnoredOnParcel
    val points = pointsString
        .takeIf { it.isNotEmpty() && it.isNotBlank() }
        ?.apply { replace("\r", "") }
        ?.split("\n")
        ?.mapNotNull { line ->
            val pieces = line.split(";")
            if (pieces.size < 3)
                return@mapNotNull null
            val lat = pieces[0].toDoubleOrNull() ?: return@mapNotNull null
            val lon = pieces[1].toDoubleOrNull() ?: return@mapNotNull null
            val type = pieces.takeIf { it.size > 3 }?.get(3)?.let { PointType.fromString(it) }

            PointData(
                GeoPoint(lat, lon),
                pieces[2],
                type ?: PointType.DEFAULT,
            )
        }
        ?: emptyList()

    @IgnoredOnParcel
    @ColumnInfo(name = "latitude")
    val latitude: Double = position.latitude

    @IgnoredOnParcel
    @ColumnInfo(name = "longitude")
    val longitude: Double = position.longitude

    /**
     * Fetches the [Intent] that launches [dataClassExploreActivity] with [EXTRA_DATACLASS] as
     * `this`.
     * @author Arnau Mora
     * @since 20220406
     * @param context The context launching from.
     */
    fun intent(context: Context) =
        Intent(context, dataClassExploreActivity)
            .putExtra(EXTRA_DATACLASS, this)

    override fun displayMap(): Map<String, Serializable?> = mapOf(
        "objectId" to objectId,
        "displayName" to displayName,
        "timestampMillis" to timestampMillis,
        "imagePath" to imagePath,
        "kmzPath" to kmzPath,
        "position" to position,
        "points" to "[" + points.joinToString(", ") + "]",
        "webUrl" to webUrl,
        "parentAreaId" to parentAreaId,
    )

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + webUrl.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + pointsString.hashCode()
        result = 31 * result + parentAreaId.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Zone

        if (objectId != other.objectId) return false
        if (displayName != other.displayName) return false
        if (timestampMillis != other.timestampMillis) return false
        if (imagePath != other.imagePath) return false
        if (kmzPath != other.kmzPath) return false
        if (position != other.position) return false
        if (pointsString != other.pointsString) return false
        if (webUrl != other.webUrl) return false
        if (parentAreaId != other.parentAreaId) return false
        if (imageQuality != other.imageQuality) return false
        if (hasParents != other.hasParents) return false

        return true
    }

    companion object : DataClassCompanion<Zone>() {
        override val NAMESPACE = Namespace.ZONE

        override val IMAGE_QUALITY = 30

        override val CONSTRUCTOR: (data: JSONObject, objectId: String, childrenCount: Long) -> Zone =
            { data, objectId, childrenCount -> Zone(data, objectId, childrenCount) }

        const val SAMPLE_OBJECT_ID = "LtYZWlzTPwqHsWbYIDTt"

        override val SAMPLE = Zone(
            objectId = SAMPLE_OBJECT_ID,
            displayName = "Barranquet de Ferri",
            timestampMillis = 1618160538000L,
            imagePath = "gs://escalaralcoiaicomtat.appspot.com/images/BarranquetDeFerriAPP.jpg",
            kmzPath = "gs://escalaralcoiaicomtat.appspot.com/kmz/Barranquet de Ferri.kmz",
            position = GeoPoint(38.705581, -0.498946),
            pointsString = "38.705581;-0.498946;Example Point\n38.706039;-0.498811;Cova",
            webUrl = "https://escalaralcoiaicomtat.centrexcursionistalcoi.org/barranquet-de-ferri.html",
            parentAreaId = "WWQME983XhriXVhtVxFu",
            0L,
        )
    }
}
