package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import org.osmdroid.util.GeoPoint
import java.util.Date

@Entity(tableName = "Zones")
data class ZoneData(
    @PrimaryKey var objectId: String,
    @ColumnInfo(name = "last_edit") val lastEdit: Date,
    @ColumnInfo(name = "displayName") val displayName: String,
    @ColumnInfo(name = "image") val image: String,
    @ColumnInfo(name = "kmz") val kmz: String?,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "webURL") val webUrl: String?,
    @ColumnInfo(name = "area") val area: String,
    @ColumnInfo(name = "childrenCount") var childrenCount: Long,
) : DataRoot<Zone> {
    override fun data(): Zone = Zone(
        objectId,
        displayName,
        lastEdit.time,
        image,
        kmz,
        GeoPoint(latitude, longitude),
        webUrl,
        area,
        childrenCount,
    )
}
