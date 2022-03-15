package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import org.osmdroid.util.GeoPoint

@Document
data class ZoneData(
    @Document.Score var index: Int,
    @Document.Id var objectId: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES) var displayName: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.StringProperty var image: String,
    @Document.StringProperty var kmzPath: String?,
    @Document.DoubleProperty var latitude: Double,
    @Document.DoubleProperty var longitude: Double,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var webUrl: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var parentObjectId: String,
) : DataRoot<Zone> {
    @Document.Namespace
    var namespace: String = Zone.NAMESPACE.namespace

    override fun data() = Zone(
        objectId,
        displayName,
        timestamp,
        image,
        kmzPath,
        GeoPoint(latitude, longitude),
        webUrl.ifEmpty { null },
        parentObjectId
    )
}
