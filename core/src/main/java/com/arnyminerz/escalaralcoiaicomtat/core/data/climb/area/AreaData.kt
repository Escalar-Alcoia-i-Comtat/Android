package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import java.util.Date

@Entity(tableName = "Areas")
data class AreaData(
    @PrimaryKey var objectId: String,
    @ColumnInfo(name = "last_edit") val lastEdit: Date,
    @ColumnInfo(name = "displayName") val displayName: String,
    @ColumnInfo(name = "image") val image: String,
    @ColumnInfo(name = "kmz") val kmz: String?,
    @ColumnInfo(name = "webURL") val webUrl: String?,
    @ColumnInfo(name = "childrenCount") var childrenCount: Long,
) : DataRoot<Area> {
    override fun data(): Area = Area(
        objectId, displayName, lastEdit.time, image, kmz, webUrl, childrenCount,
    )
}
