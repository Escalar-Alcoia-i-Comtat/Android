package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector

import androidx.appsearch.annotation.Document

@Document
data class SectorData(
    @Document.Id var objectId: String,
    @Document.StringProperty var displayName: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.StringProperty var image: String,
    @Document.StringProperty var kmzReferenceUrl: String,
    @Document.StringProperty var documentPath: String,
    @Document.StringProperty var webUrl: String,
) {
    @Document.Namespace
    var namespace: String = Sector.NAMESPACE
}

fun Sector.data(): SectorData {
    return SectorData(
        objectId,
        displayName,
        timestampMillis,
        imageReferenceUrl,
        kmzReferenceUrl ?: "",
        metadata.documentPath,
        metadata.webURL ?: ""
    )
}
