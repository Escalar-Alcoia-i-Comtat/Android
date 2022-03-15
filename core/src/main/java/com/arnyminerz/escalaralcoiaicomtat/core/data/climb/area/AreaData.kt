package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot

@Document
data class AreaData(
    @Document.Score var index: Int,
    @Document.Id var objectId: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES) var displayName: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.StringProperty var image: String,
    @Document.StringProperty var kmzPath: String?,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var webUrl: String,
) : DataRoot<Area> {
    @Document.Namespace
    var namespace: String = Area.NAMESPACE.namespace

    override fun data() = Area(
        objectId,
        displayName,
        timestamp,
        image,
        kmzPath,
        webUrl.ifEmpty { null }
    )
}
