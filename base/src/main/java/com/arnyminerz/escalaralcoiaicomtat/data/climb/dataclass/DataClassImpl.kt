package com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass

import android.os.Parcelable
import java.util.Date

abstract class DataClassImpl(
    val objectId: String,
    val namespace: String,
    val timestamp: Date
) : Parcelable {
    companion object {
        fun find(list: List<DataClassImpl>, objectId: String): Int {
            for ((i, item) in list.withIndex())
                if (item.objectId == objectId)
                    return i
            return -1
        }
    }
}
