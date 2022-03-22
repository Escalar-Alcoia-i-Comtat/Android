package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData

@Entity(tableName = "Paths")
data class PathData(
    @PrimaryKey var objectId: String,
    @ColumnInfo(name = "last_edit") val timestamp: Long,
    @ColumnInfo(name = "sketchId") val sketchId: Long,
    @ColumnInfo(name = "displayName") val displayName: String,
    @ColumnInfo(name = "grade") val rawGrades: String,
    @ColumnInfo(name = "height") val rawHeights: String?,
    @ColumnInfo(name = "ending") val rawEndings: String,
    @ColumnInfo(name = "pitch_info") val rawPitches: String,
    @ColumnInfo(name = "stringCount") val stringCount: Long,
    @ColumnInfo(name = "paraboltCount") val paraboltCount: Long,
    @ColumnInfo(name = "spitCount") val spitCount: Long,
    @ColumnInfo(name = "tensorCount") val tensorCount: Long,
    @ColumnInfo(name = "pitonCount") val pitonCount: Long,
    @ColumnInfo(name = "burilCount") val burilCount: Long,
    @ColumnInfo(name = "lanyardRequired") val lanyardRequired: Boolean,
    @ColumnInfo(name = "crackerRequired") val crackerRequired: Boolean,
    @ColumnInfo(name = "friendRequired") val friendRequired: Boolean,
    @ColumnInfo(name = "stripsRequired") val stripsRequired: Boolean,
    @ColumnInfo(name = "pitonRequired") val pitonRequired: Boolean,
    @ColumnInfo(name = "nailRequired") val nailRequired: Boolean,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "builtBy") val builtBy: String,
    @ColumnInfo(name = "rebuilders") val rebuiltBy: String,
    @ColumnInfo(name = "downloaded") val downloaded: Boolean,
    @ColumnInfo(name = "sector") val sector: String,
) : DataRoot<Path> {
    override fun data(): Path {
        return Path(
            objectId,
            timestamp,
            sketchId,
            displayName,
            rawGrades,
            rawHeights ?: "",
            rawEndings,
            rawPitches,
            FixedSafesData(
                stringCount,
                paraboltCount,
                spitCount,
                tensorCount,
                pitonCount,
                burilCount
            ),
            RequiredSafesData(
                lanyardRequired,
                crackerRequired,
                friendRequired,
                stripsRequired,
                pitonRequired,
                nailRequired
            ),
            description,
            builtBy.ifEmpty { null },
            rebuiltBy.ifEmpty { null },
            downloaded,
            sector,
        )
    }
}
