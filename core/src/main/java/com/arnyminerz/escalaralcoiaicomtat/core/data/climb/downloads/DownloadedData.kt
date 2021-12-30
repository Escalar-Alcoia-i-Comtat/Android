package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.downloads

import androidx.appsearch.annotation.Document
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
