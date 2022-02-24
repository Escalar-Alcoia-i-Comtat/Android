package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.google.android.gms.maps.model.LatLng

@Document
data class SectorData(
    @Document.Score var index: Int,
    @Document.Id var objectId: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var displayName: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var sunTime: String,
    @Document.BooleanProperty var kidsApt: Boolean,
    @Document.LongProperty var walkingTime: Long,
    @Document.DoubleProperty var latitude: Double?,
    @Document.DoubleProperty var longitude: Double?,
    @Document.StringProperty var weight: String,
    @Document.StringProperty var image: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var webUrl: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var parentObjectId: String,
) : DataRoot<Sector> {
    @Document.Namespace
    var namespace: String = Sector.NAMESPACE

    override fun data() = Sector(
        objectId,
        displayName,
        timestamp,
        sunTime,
        kidsApt,
        walkingTime,
        if (latitude == null || longitude == null) null else LatLng(latitude!!, longitude!!),
        weight,
        image,
        webUrl.ifEmpty { null },
        parentObjectId
    )
}
