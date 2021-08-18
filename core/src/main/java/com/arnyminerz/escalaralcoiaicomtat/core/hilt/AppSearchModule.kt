package com.arnyminerz.escalaralcoiaicomtat.core.hilt

import android.content.Context
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.localstorage.LocalStorage
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.shared.SEARCH_DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppSearchModule {
    @Singleton
    @Provides
    fun provideAppSearchSession(
        @ApplicationContext appContext: Context
    ): AppSearchSession {
        return runBlocking {
            LocalStorage.createSearchSession(
                LocalStorage.SearchContext.Builder(appContext, SEARCH_DATABASE_NAME)
                    .build()
            ).await()
        }
    }
}
