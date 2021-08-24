package com.arnyminerz.escalaralcoiaicomtat.core.hilt

import android.content.Context
import androidx.appsearch.app.AppSearchSession
import com.arnyminerz.escalaralcoiaicomtat.core.utils.createSearchSession
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
            createSearchSession(appContext)
        }
    }
}
