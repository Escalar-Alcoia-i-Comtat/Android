package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.Namespace
import com.arnyminerz.escalaralcoiaicomtat.core.annotations.ObjectId
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataSingleton
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import timber.log.Timber

class App : Application() {
    private lateinit var dataSingleton: DataSingleton

    override fun onCreate() {
        super.onCreate()

        dataSingleton = DataSingleton.getInstance(this)

        // TODO: Shared preferences will be removed
        @Suppress("DEPRECATION")
        sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun onTerminate() {
        Timber.i("Terminating app...")

        Timber.v("Closing database connection...")
        dataSingleton.close()

        super.onTerminate()
    }

    /**
     * Gets all the [Area]s available.
     * @author Arnau Mora
     * @since 20210818
     */
    @WorkerThread
    suspend fun getAreas(): List<Area> = dataSingleton.repository.getAreas()

    /**
     * Searches for the specified [Zone].
     * @author Arnau Mora
     * @since 20210820
     */
    @WorkerThread
    suspend fun getArea(@ObjectId areaId: String): Area? =
        dataSingleton.repository.getArea(areaId)

    /**
     * Searches for the specified [Zone].
     * @author Arnau Mora
     * @since 20210820
     */
    @WorkerThread
    suspend fun getZone(@ObjectId zoneId: String): Zone? =
        dataSingleton.repository.getZone(zoneId)

    /**
     * Searches for the specified [Sector].
     * @author Arnau Mora
     * @since 20210820
     */
    @WorkerThread
    suspend fun getSector(@ObjectId sectorId: String): Sector? =
        dataSingleton.repository.getSector(sectorId)

    /**
     * Searches for the specified [Path].
     * @author Arnau Mora
     * @since 20210820
     */
    @WorkerThread
    suspend fun getPath(@ObjectId pathId: String): Path? =
        dataSingleton.repository.getPath(pathId)

    /**
     * Searches for the specified [Path]s.
     * @author Arnau Mora
     * @since 20210820
     */
    @WorkerThread
    suspend fun getPaths(@ObjectId zoneId: String): List<Path> =
        dataSingleton
            .repository
            .getChildren(Namespace.PATH, zoneId)
            ?.map { it as Path }
            ?: emptyList()
}

/**
 * Returns the [Activity.getApplication] casted as [App].
 * @author Arnau Mora
 * @since 20210818
 */
val Activity.app: App
    get() = application as App

/**
 * Returns the [AndroidViewModel.getApplication] casted as [App].
 * @author Arnau Mora
 * @since 20210818
 */
val AndroidViewModel.app: App
    get() = getApplication()

/**
 * Returns the application's context attached to the view model.
 * @author Arnau Mora
 * @since 20211229
 */
val AndroidViewModel.context: Context
    get() = getApplication()
