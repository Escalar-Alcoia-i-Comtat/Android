package com.arnyminerz.escalaralcoiaicomtat.shared

import android.app.Application
import android.content.Context
import android.util.Log
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber

private const val CRASHLYTICS_KEY_PRIORITY = "priority"
private const val CRASHLYTICS_KEY_TAG = "tag"

class App : Application(), ConnectivityProvider.ConnectivityStateListener {
    private val provider: ConnectivityProvider
        get() = appNetworkProvider

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashReportingTree())
        Timber.v("Planted Timber.")

        sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        Timber.v("Getting Firestore instance...")
        firestore = Firebase.firestore

        Timber.v("Initializing network provider...")
        appNetworkProvider = ConnectivityProvider.createProvider(this)
        Timber.v("Adding network listener...")
        provider.addListener(this)
    }

    override fun onTerminate() {
        Timber.v("Removing network listener...")
        provider.removeListener(this)
        super.onTerminate()
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {
        Timber.v("Network state updated: $state")
        Timber.v("Updating Firestore's network status according to new state.")
        if (state.hasInternet)
            firestore.enableNetwork()
        else
            firestore.disableNetwork()

        appNetworkState = state
    }

    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
        Timber.v("Network state updated asyncronously: $state")
    }

    private inner class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val crashlytics = Firebase.crashlytics

            crashlytics.setCustomKey(CRASHLYTICS_KEY_PRIORITY, priority)
            if (tag != null)
                crashlytics.setCustomKey(CRASHLYTICS_KEY_TAG, tag)

            if (priority == Log.DEBUG || priority == Log.VERBOSE || priority == Log.INFO || priority == Log.WARN)
                crashlytics.log(message)
            else if (t != null)
                crashlytics.recordException(t)
            else
                crashlytics.log(message)
        }
    }
}
