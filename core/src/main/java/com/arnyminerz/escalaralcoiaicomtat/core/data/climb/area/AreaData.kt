package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import androidx.appsearch.annotation.Document
import java.util.Date

@Document
data class AreaData(
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

    fun area() = Area(
        objectId,
        displayName,
        Date(timestamp),
        image,
        kmzReferenceUrl,
        documentPath,
        webUrl.ifEmpty { null }
    )
}

fun Area.data(): AreaData {
    return AreaData(
        objectId,
        displayName,
        timestampMillis,
        imageReferenceUrl,
        kmzReferenceUrl ?: "",
        metadata.documentPath,
        metadata.webURL ?: ""
    )
}
