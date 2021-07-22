package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone

import androidx.appsearch.annotation.Document
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area

@Document
data class ZoneData(
    @Document.Id var objectId: String,
    @Document.StringProperty var displayName: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.StringProperty var image: String,
    @Document.StringProperty var kmzReferenceUrl: String,
    @Document.StringProperty var documentPath: String,
    @Document.StringProperty var webUrl: String,
) {
    @Document.Namespace
    var namespace: String = Area.NAMESPACE
}

fun Zone.data(): ZoneData {
    return ZoneData(
        objectId,
        displayName,
        timestampMillis,
        imageReferenceUrl,
        kmzReferenceUrl ?: "",
        metadata.documentPath,
        metadata.webURL ?: ""
    )
}
