package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
open class DataClassImpl(
    open val objectId: String,
    val namespace: String,
    open val timestampMillis: Long,
    open val displayName: String,
    open val documentPath: String
) : Parcelable {
    companion object {
        fun find(list: List<DataClassImpl>, objectId: String): Int {
            for ((i, item) in list.withIndex())
                if (item.objectId == objectId)
                    return i
            return -1
        }
    }

    val timestamp: Date
        get() = Date(timestampMillis)

    override fun toString(): String = "$namespace/$objectId"
}
