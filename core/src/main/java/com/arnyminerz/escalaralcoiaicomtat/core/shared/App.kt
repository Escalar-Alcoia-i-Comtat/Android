package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import androidx.appsearch.app.AppSearchSession
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
}
