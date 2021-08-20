package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.accounts.AccountManager
import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE
import androidx.appsearch.exceptions.AppSearchException
import androidx.collection.arrayMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.work.await
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.AreaData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.Path
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.PathData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.Sector
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.sector.SectorData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.Zone
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.zone.ZoneData
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), ConnectivityProvider.ConnectivityStateListener {
    private val provider: ConnectivityProvider
        get() = appNetworkProvider

    private lateinit var firestore: FirebaseFirestore

    private var areas: List<Area> = listOf()

    val authStateListener: FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener {
        val user = it.currentUser
        Timber.v("Auth State updated. Logged in: ${user != null}")
        if (user == null) {
            val am = AccountManager.get(this)
            for (account in am.accounts) {
                Timber.v("Removing account \"${account.name}\"...")
                try {
                    am.removeAccountExplicitly(account)
                } catch (e: SecurityException) {
                    Timber.w(e, "Could not remove account.")
                }
            }
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

        sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        Timber.v("Getting Firestore instance...")
        firestore = Firebase.firestore

        Timber.v("Initializing network provider...")
        appNetworkProvider = ConnectivityProvider.createProvider(this)
        Timber.v("Adding network listener...")
        provider.addListener(this)

        Timber.v("Adding auth state listener...")
        Firebase.auth.addAuthStateListener(authStateListener)
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

val appSearchSessionAreas = arrayMapOf<Int, List<Area>>()

private var AppSearchSession.areas: List<Area>
    get() = appSearchSessionAreas[hashCode()] ?: listOf()
    set(value) = appSearchSessionAreas.set(hashCode(), value)

/**
 * Gets all the stored [Area]s in the [AppSearchSession].
 * @author Arnau Mora
 * @since 20210818
 * @return A list of the [Area]s that have been found.
 */
@WorkerThread
suspend fun AppSearchSession.getAreas(): List<Area> {
    if (areas.isEmpty()) {
        val areasSearchSpec = SearchSpec.Builder()
            .addFilterNamespaces(Area.NAMESPACE)
            .setOrder(SearchSpec.ORDER_ASCENDING)
            .setRankingStrategy(RANKING_STRATEGY_DOCUMENT_SCORE)
            .build()
        val searchResult = search("", areasSearchSpec)
        val searchPage = searchResult.nextPage.await()
        areas = arrayListOf<Area>().apply {
            for (page in searchPage) {
                val genericDocument = page.genericDocument
                Timber.v("Got generic document ${genericDocument.namespace}: ${genericDocument.id}")
                val areaData = try {
                    genericDocument.toDocumentClass(AreaData::class.java)
                } catch (e: AppSearchException) {
                    Timber.e("Could not convert GenericDocument to AreaData!")
                    continue
                }
                val area = areaData.data()
                add(area)
            }
        }
    }
    return areas
}

/**
 * Searches for a [DataClassImpl] parent with Class name [R], and [DataRoot] T.
 * @author Arnau Mora
 * @since 20210820
 * @param query What to search for.
 * @param namespace The namespace of the query.
 * @return The [R], or null if not found.
 */
@WorkerThread
suspend inline fun <R : DataClassImpl, reified T : DataRoot<R>> AppSearchSession.getData(
    query: String,
    namespace: String
): R? {
    val searchSpec = SearchSpec.Builder()
        .addFilterNamespaces(namespace)
        .setOrder(SearchSpec.ORDER_ASCENDING)
        .setRankingStrategy(RANKING_STRATEGY_DOCUMENT_SCORE)
        .setResultCountPerPage(1)
        .build()
    val searchResult = search(query, searchSpec)
    val searchPage = searchResult.nextPage.await().ifEmpty { return null }

    // If reached here, searchPage is not empty.
    val page = searchPage[0]

    val genericDocument = page.genericDocument
    Timber.v("Got generic document ${genericDocument.namespace}: ${genericDocument.id}")
    val data: T = try {
        genericDocument.toDocumentClass(T::class.java)
    } catch (e: AppSearchException) {
        Timber.e("Could not convert GenericDocument to ${T::class.java.simpleName}!")
        return null
    }
    return data.data()
}

/**
 * Searches for all the [DataClassImpl] typed [R] that are indexed with [query].
 * @author Arnau Mora
 * @since 20210820
 * @param query What to search for.
 * @param namespace The namespace of the query.
 * @param max The maximum amount of items to fetch.
 * @return A [List] of [R] with the found items.
 */
@WorkerThread
suspend inline fun <R : DataClassImpl, reified T : DataRoot<R>> AppSearchSession.getList(
    query: String,
    namespace: String,
    max: Int = 100,
): List<R> {
    val searchSpec = SearchSpec.Builder()
        .addFilterNamespaces(namespace)
        .setOrder(SearchSpec.ORDER_ASCENDING)
        .setRankingStrategy(RANKING_STRATEGY_DOCUMENT_SCORE)
        .setResultCountPerPage(max)
        .build()
    val searchResult = search(query, searchSpec)
    val searchPage = searchResult.nextPage.await()

    return arrayListOf<R>().apply {
        for (page in searchPage) {
            val genericDocument = page.genericDocument
            Timber.v("Got generic document ${genericDocument.namespace}: ${genericDocument.id}")
            val data: T = try {
                genericDocument.toDocumentClass(T::class.java)
            } catch (e: AppSearchException) {
                Timber.e("Could not convert GenericDocument to ${T::class.java.simpleName}!")
                continue
            }
            val t = data.data()
            add(t)
        }
    }
}

/**
 * Searches for a [Area] with id [areaId] stored in the [AppSearchSession].
 * @author Arnau Mora
 * @since 20210820
 * @param areaId The ID of the [Area] to search for.
 * @return The [Area], or null if not found.
 */
@WorkerThread
suspend fun AppSearchSession.getArea(areaId: String): Area? =
    getData<Area, AreaData>(areaId, Area.NAMESPACE)

/**
 * Searches for a [Zone] with id [zoneId] stored in the [AppSearchSession].
 * @author Arnau Mora
 * @since 20210820
 * @param zoneId The ID of the [Zone] to search for.
 * @return The [Zone], or null if not found.
 */
@WorkerThread
suspend fun AppSearchSession.getZone(zoneId: String): Zone? =
    getData<Zone, ZoneData>(zoneId, Zone.NAMESPACE)

/**
 * Searches for a [Sector] with id [sectorId] stored in the [AppSearchSession].
 * @author Arnau Mora
 * @since 20210820
 * @param sectorId The ID of the sector to search for.
 * @return The [Sector], or null if not found.
 */
@WorkerThread
suspend fun AppSearchSession.getSector(sectorId: String): Sector? =
    getData<Sector, SectorData>(sectorId, Sector.NAMESPACE)

/**
 * Searches for all the [AppSearchSession] indexed [Zone]s that have as a parent an area with id [areaId].
 * @author Arnau Mora
 * @since 20210820
 * @param areaId The parent [Area] id of the [Zone]s to search for.
 * @return A [List] with the found [Zone]s.
 */
@WorkerThread
suspend fun AppSearchSession.getZones(areaId: String): List<Zone> =
    getList<Zone, ZoneData>(areaId, Zone.NAMESPACE)

/**
 * Searches for all the [AppSearchSession] indexed [Sector]s that have as a parent a zone with id
 * [zoneId].
 * @author Arnau Mora
 * @since 20210820
 * @param zoneId The parent [Zone] id of the [Sector]s to search for.
 * @return A [List] with the found [Sector]s.
 */
@WorkerThread
suspend fun AppSearchSession.getSectors(zoneId: String): List<Sector> =
    getList<Sector, SectorData>(zoneId, Sector.NAMESPACE)

/**
 * Searches for all the [AppSearchSession] indexed [Path]s that have as a parent a zone with id
 * [sectorId].
 * @author Arnau Mora
 * @since 20210820
 * @param sectorId The parent [Sector] id of the [Path]s to search for.
 * @return A [List] with the found [Path]s.
 */
@WorkerThread
suspend fun AppSearchSession.getPaths(sectorId: String): List<Path> =
    getList<Path, PathData>(sectorId, Path.NAMESPACE)
