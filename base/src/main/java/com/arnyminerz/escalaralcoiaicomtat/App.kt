package com.arnyminerz.escalaralcoiaicomtat

import android.app.Application
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.parse.Parse
import com.parse.Parse.Configuration
import timber.log.Timber

var appNetworkState: ConnectivityProvider.NetworkState =
    ConnectivityProvider.NetworkState.NOT_CONNECTED
    private set

lateinit var appNetworkProvider: ConnectivityProvider

class App : Application(), ConnectivityProvider.ConnectivityStateListener {
    private val provider: ConnectivityProvider
        get() = appNetworkProvider

    override fun onCreate() {
        super.onCreate()
        Parse.initialize(
            Configuration.Builder(this)
                .applicationId(BuildConfig.PARSE_APPLICATION_ID) // if defined
                .clientKey(BuildConfig.PARSE_KEY)
                .server(BuildConfig.PARSE_SERVER)
                .enableLocalDataStore()
                .build()
        )

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
        appNetworkState = state
    }
}
