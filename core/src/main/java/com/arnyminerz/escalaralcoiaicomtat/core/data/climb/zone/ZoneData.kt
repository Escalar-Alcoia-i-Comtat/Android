package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import com.google.android.gms.maps.model.LatLng

@Document
data class ZoneData(
    @Document.Score var index: Int,
    @Document.Id var objectId: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var displayName: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.StringProperty var image: String,
    @Document.StringProperty var kmzReferenceUrl: String,
    @Document.DoubleProperty var latitude: Double,
    @Document.DoubleProperty var longitude: Double,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var documentPath: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var webUrl: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var parentObjectId: String,
) {
    @Document.Namespace
    var namespace: String = Zone.NAMESPACE

    fun zone() = Zone(
        objectId,
        displayName,
        timestamp,
        image,
        kmzReferenceUrl,
        LatLng(latitude, longitude),
        documentPath,
        webUrl.ifEmpty { null },
        parentObjectId
    )
}

fun Zone.data(index: Int): ZoneData {
    return ZoneData(
        index,
        objectId,
        displayName,
        timestampMillis,
        imageReferenceUrl,
        kmzReferenceUrl,
        position.latitude,
        position.longitude,
        metadata.documentPath,
        metadata.webURL ?: "",
        parentAreaId
    )
}
