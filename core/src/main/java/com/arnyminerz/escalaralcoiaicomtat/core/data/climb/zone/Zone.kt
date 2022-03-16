package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassCompanion
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassDisplayOptions
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.Serializable
import java.util.Date

/**
 * Creates a new [Zone] instance.
 * @author Arnau Mora
 * @since 20210724
 */
@Parcelize
class Zone internal constructor(
    override val objectId: String,
    override val displayName: String,
    override val timestampMillis: Long,
    override val imagePath: String,
    override val kmzPath: String?,
    val position: GeoPoint,
    val webUrl: String?,
    private val parentAreaId: String,
    var downloaded: Boolean = false,
    var downloadSize: Long?,
    val childrenCount: Long,
) : DataClass<Sector, Area, ZoneData>(
    displayName,
    timestampMillis,
    imagePath,
    kmzPath,
    position,
    DataClassMetadata(
        objectId,
        NAMESPACE,
        webUrl,
        parentAreaId
    ),
    DataClassDisplayOptions(
        R.drawable.ic_tall_placeholder,
        R.drawable.ic_tall_placeholder,
        2,
        downloadable = true,
        showLocation = true
    )
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
        data.getString("webURL"),
        data.getString("area"),
        downloaded = false,
        downloadSize = null,
        childrenCount = childrenCount,
    )

    @IgnoredOnParcel
    override val imageQuality: Int = IMAGE_QUALITY

    @IgnoredOnParcel
    override val hasParents: Boolean = true

    override fun data() = ZoneData(
        objectId,
        Date(timestampMillis),
        displayName,
        imagePath,
        kmzPath,
        position.latitude,
        position.longitude,
        metadata.webURL ?: "",
        parentAreaId,
        childrenCount,
        downloaded,
        downloadSize,
    )

    override fun displayMap(): Map<String, Serializable?> = mapOf(
        "objectId" to objectId,
        "displayName" to displayName,
        "timestampMillis" to timestampMillis,
        "imagePath" to imagePath,
        "kmzPath" to kmzPath,
        "position" to position,
        "webUrl" to webUrl,
        "parentAreaId" to parentAreaId,
    )

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + webUrl.hashCode()
        result = 31 * result + position.hashCode()
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
            webUrl = "https://escalaralcoiaicomtat.centrexcursionistalcoi.org/barranquet-de-ferri.html",
            parentAreaId = "WWQME983XhriXVhtVxFu",
            false, null, 0L,
        )
    }
}
