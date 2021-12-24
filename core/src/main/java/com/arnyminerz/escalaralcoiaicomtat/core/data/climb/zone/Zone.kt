package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassDisplayOptions
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getLatLng
import com.arnyminerz.escalaralcoiaicomtat.core.utils.toLatLng
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

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
    override val imageReferenceUrl: String,
    override val kmzReferenceUrl: String,
    val position: LatLng,
    override val documentPath: String,
    val webUrl: String?,
    val parentAreaId: String,
) : DataClass<Sector, Area>(
    displayName,
    timestampMillis,
    imageReferenceUrl,
    kmzReferenceUrl,
    position,
    DataClassMetadata(
        objectId,
        NAMESPACE,
        Area.NAMESPACE,
        Sector.NAMESPACE,
        documentPath,
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
     * Creates a new [Zone] from the data of a [DocumentSnapshot].
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     */
    @Deprecated("Use data module")
    constructor(data: DocumentSnapshot) : this(
        data.id,
        data.getString("displayName")!!,
        data.getDate("created")!!.time,
        data.getString("image")!!,
        data.getString("kmz")!!,
        data.getGeoPoint("location")!!.toLatLng(),
        documentPath = data.reference.path,
        data.getString("webURL"),
        data.reference.parent.parent!!.id
    )

    /**
     * Creates a new [Zone] from the data from the Data Module.
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     */
    constructor(data: JSONObject, path: String) : this(
        path.split('/').last(),
        data.getString("displayName"),
        data.getDate("created")!!.time,
        data.getString("image"),
        data.getString("kmz"),
        data.getLatLng("location")!!,
        documentPath = path,
        data.getString("webURL"),
        path.split("/").let { it[it.size - 3] }
    )

    @IgnoredOnParcel
    override val imageQuality: Int = IMAGE_QUALITY

    companion object {
        const val NAMESPACE = "Zone"

        const val IMAGE_QUALITY = 65

        val SAMPLE_ZONE = Zone(
            objectId = "LtYZWlzTPwqHsWbYIDTt",
            displayName = "Barranquet de Ferri",
            timestampMillis = 1618160538000L,
            imageReferenceUrl = "gs://escalaralcoiaicomtat.appspot.com/images/BarranquetDeFerriAPP.jpg",
            kmzReferenceUrl = "gs://escalaralcoiaicomtat.appspot.com/kmz/Barranquet de Ferri.kmz",
            position = LatLng(38.705581, -0.498946),
            documentPath = "/Areas/WWQME983XhriXVhtVxFu/Zones/LtYZWlzTPwqHsWbYIDTt",
            webUrl = "https://escalaralcoiaicomtat.centrexcursionistalcoi.org/barranquet-de-ferri.html",
            parentAreaId = "WWQME983XhriXVhtVxFu"
        )
    }
}
