package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema

@Document
data class BlockingData(
    @Document.Id var pathId: String,
    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS) val rawBlockingType: String,
) {
    companion object {
        const val NAMESPACE = "PathBlockingData"
    }

    @Document.Namespace
    var namespace: String = NAMESPACE

    /**
     * Returns [rawBlockingType] as a valid [BlockingType].
     * Uses [BlockingType.find] under the hood.
     * @author Arnau Mora
     * @since 20210824
     */
    val blockingType: BlockingType = BlockingType.find(rawBlockingType)
}
