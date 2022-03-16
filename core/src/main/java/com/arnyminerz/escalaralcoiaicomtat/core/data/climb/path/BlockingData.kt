package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Blocking")
data class BlockingData(
    @PrimaryKey var id: String,
    @ColumnInfo(name = "path") val pathId: String,
    @ColumnInfo(name = "type") val rawBlockingType: String,
    @ColumnInfo(name = "end_date") val endDate: Date?,
) {
    companion object {
        const val NAMESPACE = "PathBlockingData"
    }

    /**
     * Returns [rawBlockingType] as a valid [BlockingType].
     * Uses [BlockingType.find] under the hood.
     * @author Arnau Mora
     * @since 20210824
     */
    @Ignore
    val blockingType: BlockingType = BlockingType.find(rawBlockingType)
}
