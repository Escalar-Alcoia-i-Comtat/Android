package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.accounts.AccountManager
import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.appsearch.app.AppSearchSession
import androidx.lifecycle.AndroidViewModel
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.utils.*
import com.arnyminerz.escalaralcoiaicomtat.core.utils.auth.loggedIn
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class App : Application(), ConnectivityProvider.ConnectivityStateListener {
    private val provider: ConnectivityProvider
        get() = appNetworkProvider

    /**
     * The [FirebaseFirestore] instance reference for requesting data to the server.
     * @author Arnau Mora
     * @since 20210617
     */
    private lateinit var firestore: FirebaseFirestore

    /**
     * The [FirebaseAnalytics] instance reference for analyzing the user actions.
     * @author Arnau Mora
     * @since 20210826
     */
    private lateinit var analytics: FirebaseAnalytics

    val authStateListener: FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener {
        val loggedIn = it.loggedIn
        Timber.v("Auth State updated. Logged in: $loggedIn. Anonymous: ${it.currentUser?.isAnonymous ?: "N/A"}")
        if (!loggedIn) {
            val am = AccountManager.get(this)
            for (account in am.accounts) {
                Timber.v("Removing account \"${account.name}\"...")
                try {
                    am.removeAccountExplicitly(account)
                } catch (e: SecurityException) {
                    Timber.w(e, "Could not remove account.")
                }
            }
        } else it.currentUser?.let { user ->
            analytics.setUserId(user.uid)
        }
    }

    /**
     * The session for doing search-related operations.
     * @author Arnau Mora
     * @since 20210817
     */
    @Inject
    lateinit var searchSession: AppSearchSession

    override fun onCreate() {
        super.onCreate()

        searchSession = runBlocking { createSearchSession(applicationContext) }

        sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        Timber.v("Getting Firestore instance...")
        firestore = Firebase.firestore
        Timber.v("Getting Analytics instance...")
        analytics = Firebase.analytics

        Timber.v("Initializing network provider...")
        appNetworkProvider = ConnectivityProvider.createProvider(this)
        Timber.v("Adding network listener...")
        provider.addListener(this)

        Timber.v("Adding auth state listener...")
        Firebase.auth.addAuthStateListener(authStateListener)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
    }

    override fun onTerminate() {
        Timber.i("Terminating app...")
        Timber.v("Removing network listener...")
        provider.removeListener(this)
        Timber.v("Closing AppSearch...")
        searchSession.close()
        super.onTerminate()
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        Timber.v("Network state updated: $state")

        appNetworkState = state
    }

    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
        Timber.v("Network state updated asyncronously: $state")
        Timber.v("Updating Firestore's network status according to new state.")
        if (state.hasInternet)
            firestore.enableNetwork().await()
        else
            firestore.disableNetwork().await()
    }

    /**
     * Gets all the [Area]s available in [searchSession].
     * @author Arnau Mora
     * @since 20210818
     */
    @WorkerThread
    suspend fun getAreas(): List<Area> = searchSession.getAreas()

    /**
     * Searches for the specified [Zone] in [searchSession].
     * Serves for a shortcut to [AppSearchSession.getZone].
     * @author Arnau Mora
     * @since 20210820
     * @see AppSearchSession.getZone
     */
    @WorkerThread
    suspend fun getArea(areaId: String): Area? = searchSession.getArea(areaId)

    /**
     * Searches for the specified [Zone] in [searchSession].
     * Serves for a shortcut to [AppSearchSession.getZone].
     * @author Arnau Mora
     * @since 20210820
     * @see AppSearchSession.getZone
     */
    @WorkerThread
    suspend fun getZone(zoneId: String): Zone? = searchSession.getZone(zoneId)

    /**
     * Searches for the specified [Sector] in [searchSession].
     * Serves for a shortcut to [AppSearchSession.getSector].
     * @author Arnau Mora
     * @since 20210820
     * @see AppSearchSession.getSector
     */
    @WorkerThread
    suspend fun getSector(sectorId: String): Sector? = searchSession.getSector(sectorId)

    /**
     * Searches for the specified [Path] in [searchSession].
     * Serves for a shortcut to [AppSearchSession.getPath].
     * @author Arnau Mora
     * @since 20210820
     * @see AppSearchSession.getSector
     */
    @WorkerThread
    suspend fun getPath(pathId: String): Path? = searchSession.getPath(pathId)

    /**
     * Searches for the specified [Path]s in [searchSession].
     * Serves for a shortcut to [AppSearchSession.getPaths].
     * @author Arnau Mora
     * @since 20210820
     * @see AppSearchSession.getSector
     */
    @WorkerThread
    suspend fun getPaths(zoneId: String): List<Path> = searchSession.getPaths(zoneId)
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
