package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassDisplayOptions
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.utils.getDate
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Creates a new Area instance.
 * @author Arnau Mora
 * @since 20210416
 * @param objectId The id of the object
 * @param displayName The Area's display name
 * @param timestampMillis The update date of the Area
 * @param kmzReferenceUrl The reference url from Firebase Storage for the Area's KMZ file
 * @param documentPath The path in Firebase Firestore of the Area
 * @param webUrl The url for the Area on the website
 */
@Parcelize
class Area internal constructor(
    override val objectId: String,
    override val displayName: String,
    override val timestampMillis: Long,
    override val imageReferenceUrl: String,
    override val kmzReferenceUrl: String,
    override val documentPath: String,
    val webUrl: String?,
) : DataClass<Zone, DataClassImpl>(
    displayName,
    timestampMillis,
    imageReferenceUrl,
    kmzReferenceUrl,
    null,
    DataClassMetadata(
        objectId,
        NAMESPACE,
        null,
        Zone.NAMESPACE,
        documentPath,
        webUrl,
        null
    ),
    DataClassDisplayOptions(
        R.drawable.ic_wide_placeholder,
        R.drawable.ic_wide_placeholder,
        1,
        downloadable = false,
        showLocation = false,
    )
) {
    /**
     * Creates a new [Area] from the data of a [DocumentSnapshot].
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
        documentPath = data.reference.path,
        try {
            data.getString("webURL")
        } catch (e: JSONException) {
            null
        }
    )

    /**
     * Creates a new [Area] from the data from the Data module.
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
        documentPath = path,
        data.getString("webURL")
    )

    @IgnoredOnParcel
    override val imageQuality: Int = IMAGE_QUALITY

    @IgnoredOnParcel
    override val hasParents: Boolean = false

    companion object {
        @Namespace
        const val NAMESPACE = "Area"

        const val IMAGE_QUALITY = 65

        const val SAMPLE_AREA_OBJECT_ID = "PL5j43cBRP7F24ecXGOR"
        const val SAMPLE_AREA_DISPLAY_NAME = "Cocentaina"
        const val SAMPLE_AREA_TIMESTAMP = 1618160598000L
        const val SAMPLE_AREA_IMAGE_REF =
            "gs://escalaralcoiaicomtat.appspot.com/images/areas/Vista panoramica Cocentaina-app.jpg"
        const val SAMPLE_AREA_KMZ_REF =
            "gs://escalaralcoiaicomtat.appspot.com/kmz/Zones Cocentaina.kmz"
        const val SAMPLE_AREA_DOC_PATH = "/Areas/PL5j43cBRP7F24ecXGOR"
        const val SAMPLE_AREA_WEB_URL =
            "https://escalaralcoiaicomtat.org/zones-escalada-cocentaina.html"
        val SAMPLE_AREA = Area(
            objectId = SAMPLE_AREA_OBJECT_ID,
            displayName = SAMPLE_AREA_DISPLAY_NAME,
            timestampMillis = SAMPLE_AREA_TIMESTAMP,
            imageReferenceUrl = SAMPLE_AREA_IMAGE_REF,
            kmzReferenceUrl = SAMPLE_AREA_KMZ_REF,
            documentPath = SAMPLE_AREA_DOC_PATH,
            webUrl = SAMPLE_AREA_WEB_URL,
        )
    }
}
