package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import com.arnyminerz.escalaralcoiaicomtat.core.R
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.UIMetadata
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

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
    UIMetadata(
        R.drawable.ic_wide_placeholder,
        R.drawable.ic_wide_placeholder,
    ),
    DataClassMetadata(
        objectId,
        NAMESPACE,
        documentPath,
        webUrl
    )
) {
    /**
     * Creates a new [Area] from the data of a [DocumentSnapshot].
     * Note: This doesn't add children
     * @author Arnau Mora
     * @since 20210411
     * @param data The object to get data from
     */
    constructor(data: DocumentSnapshot) : this(
        data.id,
        data.getString("displayName")!!,
        data.getDate("created")!!.time,
        data.getString("image")!!,
        data.getString("kmz")!!,
        documentPath = data.reference.path,
        data.getString("webURL")
    )

    @IgnoredOnParcel
    override val imageQuality: Int = 65

    companion object {
        const val NAMESPACE = "Area"

        /**
         * Checks if [data] contains the valid data for creating an instance of [Area].
         * @author Arnau Mora
         * @since 20210422
         * @param data The data to check.
         * @return True if the [data] contents are valid, false otherwise.
         */
        fun validate(data: DocumentSnapshot): Boolean =
            data.contains("displayName") && data.contains("created") && data.contains("image") &&
                    data.contains("kmz")
    }
}
