package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.SunTime
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassDisplayOptions
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.NO_SUN
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint

/**
 * Creates a new [Sector] instance.
 * @author Arnau Mora
 * @since 20210724
 */
@Parcelize
class Sector internal constructor(
    override val objectId: String,
    override val displayName: String,
    override val timestampMillis: Long,
    @SunTime val sunTime: String,
    val kidsApt: Boolean,
    val walkingTime: Long,
    override val location: GeoPoint?,
    val weight: String,
    override val imagePath: String,
    val webUrl: String?,
    private val parentZoneId: String,
) : DataClass<Path, Zone, SectorData>(
    displayName,
    timestampMillis,
    imagePath,
    null,
    location,
    DataClassMetadata(
        objectId,
        NAMESPACE,
        webUrl,
        parentZoneId
    ),
    DataClassDisplayOptions(
        R.drawable.ic_wide_placeholder,
        R.drawable.ic_wide_placeholder,
        1,
        downloadable = true,
        showLocation = location != null,
    )
) {

    /**
     * Creates a new [Sector] from the data of the Data Module.
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     * @param sectorId The ID of the Sector.
     */
    constructor(data: JSONObject, @ObjectId sectorId: String) : this(
        sectorId,
        data.getString("displayName"),
        data.getDate("last_edit")!!.time,
        data.getString("sunTime"),
        data.getBoolean("kidsApt"),
        data.getLong("walkingTime"),
        GeoPoint(
            data.getDouble("latitude"),
            data.getDouble("longitude")
        ),
        data.getString("weight"),
        data.getString("image"),
        try {
            data.getString("webURL")
        } catch (e: JSONException) {
            null
        },
        data.getString("zone")
    )

    @IgnoredOnParcel
    override val imageQuality: Int = IMAGE_QUALITY

    @IgnoredOnParcel
    override val hasParents: Boolean = true

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + sunTime.hashCode()
        result = 31 * result + kidsApt.hashCode()
        result = 31 * result + walkingTime.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + webUrl.hashCode()
        return result
    }

    override fun data(index: Int): SectorData {
        return SectorData(
            index,
            objectId,
            displayName,
            timestampMillis,
            sunTime,
            kidsApt,
            walkingTime,
            location?.latitude,
            location?.longitude,
            weight,
            imagePath,
            metadata.webURL ?: "",
            parentZoneId
        )
    }

    companion object {
        @Namespace
        const val NAMESPACE = "Sector"

        const val IMAGE_QUALITY = 100

        /**
         * A sample sector for debugging and placeholder.
         * @author Arnau Mora
         * @since 20220106
         */
        val SAMPLE_SECTOR = Sector(
            objectId = "B9zNqbw6REYVxGZxlYwh",
            displayName = "Mas de la Penya 3",
            timestampMillis = 1618153404000L,
            sunTime = NO_SUN,
            kidsApt = false,
            walkingTime = 12,
            location = GeoPoint(38.741649, -0.466173),
            weight = "aac",
            imagePath = "images/sectors/mas-de-la-penya-sector-3_croquis.jpg",
            webUrl = null,
            parentZoneId = "3DmHnKBlDRwqlH1KK85C"
        )
    }
}
