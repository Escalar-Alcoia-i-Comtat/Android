package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import android.os.Parcelable
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.Date

@Parcelize
open class DataClassImpl(
    @ObjectId
    open val objectId: String,
    val namespace: Namespace,
    open val timestampMillis: Long,
    open val displayName: String,
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

    /**
     * Returns a map used to display the stored data to the user. Keys should be the parameter name,
     * and the value the value to display. Should be overridden by target class.
     * @author Arnau Mora
     * @since 20220315
     */
    open fun displayMap(): Map<String, Serializable?> = emptyMap()
}
