package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector

import androidx.appsearch.annotation.Document
import com.google.android.gms.maps.model.LatLng

@Document
data class SectorData(
    @Document.Id var objectId: String,
    @Document.StringProperty var displayName: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.LongProperty var sunTime: Int,
    @Document.BooleanProperty var kidsApt: Boolean,
    @Document.LongProperty var walkingTime: Long,
    @Document.DoubleProperty var latitude: Double,
    @Document.DoubleProperty var longitude: Double,
    @Document.StringProperty var weight: String,
    @Document.StringProperty var image: String,
    @Document.StringProperty var kmzReferenceUrl: String,
    @Document.StringProperty var documentPath: String,
    @Document.StringProperty var webUrl: String,
) {
    @Document.Namespace
    var namespace: String = Sector.NAMESPACE

    fun sector() = Sector(
        objectId,
        displayName,
        timestamp,
        sunTime,
        kidsApt,
        walkingTime,
        if (latitude < 0 || longitude < 0) null else LatLng(latitude, longitude),
        weight,
        image,
        documentPath,
        webUrl.ifEmpty { null })
}

fun Sector.data(): SectorData {
    return SectorData(
        objectId,
        displayName,
        timestampMillis,
        sunTime,
        kidsApt,
        walkingTime,
        location?.latitude ?: -1.0,
        location?.longitude ?: -1.0,
        weight,
        imageReferenceUrl,
        kmzReferenceUrl ?: "",
        metadata.documentPath,
        metadata.webURL ?: ""
    )
}
