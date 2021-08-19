package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.EndingType
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData

@Document
data class PathData(
    @Document.Id var objectId: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.LongProperty var sketchId: Long,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var displayName: String,
    @Document.StringProperty var grades: String,
    @Document.StringProperty var heights: String,
    @Document.StringProperty var endings: String,
    @Document.StringProperty var pitches: String?,
    @Document.LongProperty val stringCount: Long,
    @Document.LongProperty val paraboltCount: Long,
    @Document.LongProperty val spitCount: Long,
    @Document.LongProperty val tensorCount: Long,
    @Document.LongProperty val pitonCount: Long,
    @Document.LongProperty val burilCount: Long,
    @Document.BooleanProperty val lanyardRequired: Boolean,
    @Document.BooleanProperty val crackerRequired: Boolean,
    @Document.BooleanProperty val friendRequired: Boolean,
    @Document.BooleanProperty val stripsRequired: Boolean,
    @Document.BooleanProperty val pitonRequired: Boolean,
    @Document.BooleanProperty val nailRequired: Boolean,
    @Document.StringProperty var description: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var builtBy: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var rebuiltBy: String,
    @Document.BooleanProperty var downloaded: Boolean,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var documentPath: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var parentSectorId: String,
) : DataRoot<Path> {
    @Document.Namespace
    var namespace: String = Path.NAMESPACE

    override fun data(): Path {
        val heightsArray = heights.split(",")
        val heights = arrayListOf<Long>()
        for (h in heightsArray)
            h.toLongOrNull()?.let { heights.add(it) }

        val endingsArray = endings.split(",")
        val endings = arrayListOf<@EndingType String>()
        for (e in endingsArray)
            endings.add(e)

        return Path(
            objectId,
            timestamp,
            sketchId,
            displayName,
            grades,
            heights,
            endings,
            pitches,
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
            documentPath,
            parentSectorId,
        )
    }
}

fun Path.data(): PathData {
    var heights = ""
    for (height in this.heights)
        heights += "$height,"
    heights = heights.substringBeforeLast(',')

    var endings = ""
    for (ending in this.endings)
        endings += "$ending,"
    endings = endings.substringBeforeLast(',')

    return PathData(
        objectId,
        timestampMillis,
        sketchId,
        displayName,
        rawGrades,
        heights,
        endings,
        rawPitches,
        fixedSafesData.stringCount,
        fixedSafesData.paraboltCount,
        fixedSafesData.spitCount,
        fixedSafesData.tensorCount,
        fixedSafesData.pitonCount,
        fixedSafesData.burilCount,
        requiredSafesData.lanyardRequired,
        requiredSafesData.crackerRequired,
        requiredSafesData.friendRequired,
        requiredSafesData.stripsRequired,
        requiredSafesData.pitonRequired,
        requiredSafesData.nailRequired,
        description ?: "",
        builtBy ?: "",
        rebuiltBy ?: "",
        downloaded,
        documentPath,
        parentSectorId,
    )
}
