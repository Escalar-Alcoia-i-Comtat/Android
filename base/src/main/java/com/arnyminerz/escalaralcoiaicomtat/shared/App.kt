package com.arnyminerz.escalaralcoiaicomtat.shared

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber

private const val CRASHLYTICS_KEY_PRIORITY = "priority"
private const val CRASHLYTICS_KEY_TAG = "tag"

class App : Application(), ConnectivityProvider.ConnectivityStateListener {
    companion object {
        /**
         * Stores the [dataClassChildrenCache] usage so multiple threads won't collide.
         * @author Arnau Mora
         * @since 20210510
         */
        var usingChildren = false
    }

    private val provider: ConnectivityProvider
        get() = appNetworkProvider

    private lateinit var firestore: FirebaseFirestore

    /**
     * Stores while the application is running the data class' children data.
     * @author Arnau Mora
     * @since 20210430
     */
    val dataClassChildrenCache = arrayMapOf<String, List<DataClassImpl>>()

    /**
     * Stores while the application is running the path's blocked status.
     * @author Arnau Mora
     * @since 20210503
     * @see BlockingType
     */
    val blockStatuses = arrayMapOf<String, BlockingType>()

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
        Timber.v("Clearing DataClass children cache...")
        dataClassChildrenCache.clear()
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
