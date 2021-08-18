package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema

@Document
data class AreaData(
    @Document.Score var index: Int,
    @Document.Id var objectId: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var displayName: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.StringProperty var image: String,
    @Document.StringProperty var kmzReferenceUrl: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var documentPath: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var webUrl: String,
) {
    @Document.Namespace
    var namespace: String = Area.NAMESPACE

    fun area() = Area(
        objectId,
        displayName,
        timestamp,
        image,
        kmzReferenceUrl,
        documentPath,
        webUrl.ifEmpty { null }
    )
}

fun Area.data(index: Int): AreaData {
    return AreaData(
        index,
        objectId,
        displayName,
        timestampMillis,
        imageReferenceUrl,
        kmzReferenceUrl,
        metadata.documentPath,
        metadata.webURL ?: ""
    )
}
