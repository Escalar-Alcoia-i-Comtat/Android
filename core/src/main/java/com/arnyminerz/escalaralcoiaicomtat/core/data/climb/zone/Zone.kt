package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassDisplayOptions
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.osmdroid.util.GeoPoint

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
    constructor(data: JSONObject, @ObjectId zoneId: String) : this(
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
        data.getString("area")
    )

    @IgnoredOnParcel
    override val imageQuality: Int = IMAGE_QUALITY

    @IgnoredOnParcel
    override val hasParents: Boolean = true

    override fun data(index: Int) = ZoneData(
        index,
        objectId,
        displayName,
        timestampMillis,
        imagePath,
        kmzPath,
        position.latitude,
        position.longitude,
        metadata.webURL ?: "",
        parentAreaId
    )

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + webUrl.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + parentAreaId.hashCode()
        return result
    }

    companion object {
        @Namespace
        const val NAMESPACE = "Zone"

        const val IMAGE_QUALITY = 65

        val SAMPLE_ZONE = Zone(
            objectId = "LtYZWlzTPwqHsWbYIDTt",
            displayName = "Barranquet de Ferri",
            timestampMillis = 1618160538000L,
            imagePath = "gs://escalaralcoiaicomtat.appspot.com/images/BarranquetDeFerriAPP.jpg",
            kmzPath = "gs://escalaralcoiaicomtat.appspot.com/kmz/Barranquet de Ferri.kmz",
            position = GeoPoint(38.705581, -0.498946),
            webUrl = "https://escalaralcoiaicomtat.centrexcursionistalcoi.org/barranquet-de-ferri.html",
            parentAreaId = "WWQME983XhriXVhtVxFu"
        )
    }
}
