package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.content.Context
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.localstorage.LocalStorage
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SEARCH_DATABASE_NAME

/**
 * Creates a new search session, using the database name [SEARCH_DATABASE_NAME].
 * @author Arnau Mora
 * @since 20210824
 * @param context The context that is requesting the search session.
 */
suspend fun createSearchSession(context: Context): AppSearchSession =
    LocalStorage.createSearchSession(
        LocalStorage.SearchContext.Builder(context, SEARCH_DATABASE_NAME)
            .build()
    ).await()
