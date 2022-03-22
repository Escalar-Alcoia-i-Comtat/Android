package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import org.osmdroid.util.GeoPoint
import java.util.Date

@Entity(tableName = "Sectors")
data class SectorData(
    @PrimaryKey var objectId: String,
    @ColumnInfo(name = "last_edit") val lastEdit: Date,
    @ColumnInfo(name = "displayName") val displayName: String,
    @ColumnInfo(name = "image") val image: String,
    @ColumnInfo(name = "kidsApt") val kidsApt: Boolean,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
    @ColumnInfo(name = "sunTime") val sunTime: String,
    @ColumnInfo(name = "walkingTime") val walkingTime: Long,
    @ColumnInfo(name = "weight") val weight: String,
    @ColumnInfo(name = "zone") val zone: String,
    @ColumnInfo(name = "downloaded") var downloaded: Boolean = false,
    @ColumnInfo(name = "downloadSize") var downloadSize: Long?,
    @ColumnInfo(name = "childrenCount") var childrenCount: Long,
) : DataRoot<Sector> {
    override fun data(): Sector = Sector(
        objectId,
        displayName,
        lastEdit.time,
        sunTime,
        kidsApt,
        walkingTime,
        if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null,
        weight,
        image,
        null,
        zone,
        downloaded,
        downloadSize,
        childrenCount,
    )
}
