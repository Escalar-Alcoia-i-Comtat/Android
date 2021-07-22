package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import android.os.Parcelable
import androidx.appsearch.annotation.Document
import kotlinx.parcelize.Parcelize
import java.util.Date

@Document
@Parcelize
open class DataClassImpl(
    @Document.Id open val objectId: String,
    @Document.Namespace val namespace: String,
    @Document.CreationTimestampMillis val timestampMillis: Long
) : Parcelable {
    companion object {
        fun find(list: List<DataClassImpl>, objectId: String): Int {
            for ((i, item) in list.withIndex())
                if (item.objectId == objectId)
                    return i
            return -1
        }
    }

    open val timestamp: Date
        get() = Date(timestampMillis)
}
