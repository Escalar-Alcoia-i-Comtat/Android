package com.arnyminerz.escalaralcoiaicomtat.data.climb.data.dataclass

import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.shared.DATA_FIX_LABEL

abstract class DataClassImpl(open val objectId: String, open val namespace: String) : Parcelable {
    protected val pin: String
        get() = "${DATA_FIX_LABEL}_${namespace}_$objectId"

    companion object {
        fun find(list: List<DataClassImpl>, objectId: String): Int {
            for ((i, item) in list.withIndex())
                if (item.objectId == objectId)
                    return i
            return -1
        }
    }
}
