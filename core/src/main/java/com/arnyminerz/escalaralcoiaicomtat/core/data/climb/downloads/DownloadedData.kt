package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId

/**
 * Used for indexing the downloaded dataclasses.
 * @author Arnau Mora
 * @since 20211230
 */
@Document
data class DownloadedData(
    @Document.Id var downloadId: String,
    @ObjectId
    @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) var objectId: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.Namespace var namespace: String,
    @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) var displayName: String,
    @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) var path: String,
    @Document.LongProperty var childrenCount: Long,
    @ObjectId
    @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) var parentId: String,
    @Document.LongProperty var sizeBytes: Long
)
