package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.shared.App

/**
 * Used for indexing the downloaded dataclasses.
 * @author Arnau Mora
 * @since 20211230
 */
@Document
data class DownloadedData(
    @Document.Id var objectId: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.Namespace var namespace: String,
    @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) var displayName: String,
    @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) var path: String,
    @Document.LongProperty var childrenCount: Long,
    @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) var parentId: String,
    @Document.LongProperty var sizeBytes: Long
) {
    /**
     * Converts the [DownloadedData] into a [DataClass].
     * @author Arnau Mora
     * @since 20211230
     * @param app The [App] instance for fetching the values.
     */
    suspend fun export(app: App): DataClass<*, *> {
        return when (namespace) {
            Zone.NAMESPACE -> app.getZone(objectId)
            Sector.NAMESPACE -> app.getSector(objectId)
            else -> throw IllegalStateException("Only Zones and Sectors can be downloaded.")
        } ?: throw ClassNotFoundException("Could not find a dataclass at $namespace:$objectId")
    }
}
