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
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.preferences.PreferencesModule
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class App : Application(), ConnectivityProvider.ConnectivityStateListener {
    private val provider: ConnectivityProvider
        get() = appNetworkProvider

    /**
     * The [FirebaseAnalytics] instance reference for analyzing the user actions.
     * @author Arnau Mora
     * @since 20210826
     */
    private lateinit var analytics: FirebaseAnalytics

    private lateinit var dataSingleton: DataSingleton

    override fun onCreate() {
        super.onCreate()

        dataSingleton = DataSingleton.getInstance(this)

        PreferencesModule.initWith(this)

        // TODO: Shared preferences will be removed
        @Suppress("DEPRECATION")
        sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        Timber.v("Getting Analytics instance...")
        analytics = Firebase.analytics

        Timber.v("Initializing network provider...")
        appNetworkProvider = ConnectivityProvider.createProvider(this)
        Timber.v("Adding network listener...")
        provider.addListener(this)
    }

    override fun onTerminate() {
        Timber.i("Terminating app...")

        Timber.v("Removing network listener...")
        provider.removeListener(this)

        Timber.v("Closing database connection...")
        dataSingleton.close()

        super.onTerminate()
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        Timber.v("Network state updated: $state")

        appNetworkState = state
    }

    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {}

    /**
     * Gets all the [Area]s available.
     * @author Arnau Mora
     * @since 20210818
     */
    @WorkerThread
    suspend fun getAreas(): List<Area> = dataSingleton.repository.getAreas().map { it.data() }

    /**
     * Searches for the specified [Zone].
     * @author Arnau Mora
     * @since 20210820
     */
    @WorkerThread
    suspend fun getArea(@ObjectId areaId: String): Area? =
        dataSingleton.repository.getArea(areaId)?.data()

    /**
     * Searches for the specified [Zone].
     * @author Arnau Mora
     * @since 20210820
     */
    @WorkerThread
    suspend fun getZone(@ObjectId zoneId: String): Zone? =
        dataSingleton.repository.getZone(zoneId)?.data()

    /**
     * Searches for the specified [Sector].
     * @author Arnau Mora
     * @since 20210820
     */
    @WorkerThread
    suspend fun getSector(@ObjectId sectorId: String): Sector? =
        dataSingleton.repository.getSector(sectorId)?.data()

    /**
     * Searches for the specified [Path].
     * @author Arnau Mora
     * @since 20210820
     */
    @WorkerThread
    suspend fun getPath(@ObjectId pathId: String): Path? =
        dataSingleton.repository.getPath(pathId)?.data()

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
            ?.map { it.data() as Path }
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
    get() = getApplication<App>()

/**
 * Returns the application's context attached to the view model.
 * @author Arnau Mora
 * @since 20211229
 */
val AndroidViewModel.context: Context
    get() = getApplication()
