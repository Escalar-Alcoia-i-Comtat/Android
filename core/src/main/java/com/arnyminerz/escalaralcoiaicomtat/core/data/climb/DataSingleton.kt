package com.arnyminerz.escalaralcoiaicomtat.core.data.climb

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl

class DataSingleton {
    companion object {
        @Volatile
        private var INSTANCE: DataSingleton? = null

        /**
         * Get the current [DataSingleton] or initializes a new one if necessary.
         * @author Arnau Mora
         * @since 20220315
         */
        fun getInstance() =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataSingleton()
                    .also { INSTANCE = it }
            }
    }

    var areas by mutableStateOf(emptyList<Area>())

    var children by mutableStateOf(emptyList<DataClassImpl>())
}