package com.arnyminerz.escalaralcoiaicomtat.core.data.climb

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.database.DataClassDatabase
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.db.repository.DataClassRepository

class DataSingleton private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: DataSingleton? = null

        /**
         * Get the current [DataSingleton] or initializes a new one if necessary.
         * @author Arnau Mora
         * @since 20220315
         * @param context The context that is requesting the instance. Used for initializing the
         * database.
         */
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataSingleton(context)
                    .also { INSTANCE = it }
            }
    }

    private val database = DataClassDatabase.getInstance(context)

    val repository = DataClassRepository(
        database.areasDao(),
        database.zonesDao(),
        database.sectorsDao(),
        database.pathsDao(),
    )

    val areas = mutableStateOf(emptyList<Area>())

    val children = mutableStateOf(emptyList<DataClassImpl>())

    /**
     * Closes the connection with the local database.
     * @author Arnau Mora
     * @since 20220317
     */
    fun close() {
        database.close()
    }
}