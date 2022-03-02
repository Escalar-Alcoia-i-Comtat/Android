package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.FixedSafesData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.safes.RequiredSafesData

@Document
data class PathData(
    @Document.Id var objectId: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.LongProperty var sketchId: Long,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES) var displayName: String,
    @Document.StringProperty var rawGrades: String,
    @Document.StringProperty var rawHeights: String,
    @Document.StringProperty var rawEndings: String,
    @Document.StringProperty var rawPitches: String,
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
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) var parentSectorId: String,
) : DataRoot<Path> {
    @Document.Namespace
    var namespace: String = Path.NAMESPACE

    override fun data(): Path {
        return Path(
            objectId,
            timestamp,
            sketchId,
            displayName,
            rawGrades,
            rawHeights,
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
            parentSectorId,
        )
    }
}
