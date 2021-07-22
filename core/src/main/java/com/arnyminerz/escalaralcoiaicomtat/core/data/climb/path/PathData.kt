package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.appsearch.annotation.Document
import org.json.JSONArray

@Document
data class PathData(
    @Document.Id var objectId: String,
    @Document.CreationTimestampMillis var timestamp: Long,
    @Document.LongProperty var sketchId: Long,
    @Document.StringProperty var displayName: String,
    @Document.StringProperty var grades: String,
    @Document.StringProperty var heights: String,
    @Document.StringProperty var endings: String,
    @Document.StringProperty var pitches: String,
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
    @Document.StringProperty var builtBy: String,
    @Document.StringProperty var rebuiltBy: String,
    @Document.BooleanProperty var downloaded: Boolean,
    @Document.StringProperty var documentPath: String,
) {
    @Document.Namespace
    var namespace: String = Path.NAMESPACE
}

fun Path.data(): PathData {
    val heights = JSONArray()
    for (height in this.heights)
        heights.put(height)

    val endings = JSONArray()
    for (ending in this.endings)
        endings.put(ending.idName)

    val pitches = JSONArray()
    for (pitch in this.pitches)
        pitches.put(pitch.toJSON())

    return PathData(
        objectId,
        timestampMillis,
        sketchId,
        displayName,
        grades.toJSONStringArray(),
        heights.toString(),
        endings.toString(),
        pitches.toString(),
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
        documentPath
    )
}
