package com.arnyminerz.escalaralcoiaicomtat.core.data.climb

import android.content.Context
import com.arnyminerz.escalaralcoiaicomtat.core.utils.createSearchSession
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class SearchSingleton private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: SearchSingleton? = null

        /**
         * Get the current [SearchSingleton] or initializes a new one if necessary.
         * @author Arnau Mora
         * @since 20220315
         */
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SearchSingleton(context)
                    .also { INSTANCE = it }
            }
    }

    @Inject
    val searchSession = runBlocking { createSearchSession(context) }
}