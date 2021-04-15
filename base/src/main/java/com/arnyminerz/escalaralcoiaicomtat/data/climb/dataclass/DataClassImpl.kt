package com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass

import android.os.Parcelable

abstract class DataClassImpl(val objectId: String, val namespace: String) : Parcelable {
    companion object {
        fun find(list: List<DataClassImpl>, objectId: String): Int {
            for ((i, item) in list.withIndex())
                if (item.objectId == objectId)
                    return i
            return -1
        }
    }
}
