package com.arnyminerz.escalaralcoiaicomtat.shared

import android.app.Application
import android.content.Context
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import timber.log.Timber

class App : Application(), ConnectivityProvider.ConnectivityStateListener {
    private val provider: ConnectivityProvider
        get() = appNetworkProvider

    override fun onCreate() {
        super.onCreate()

        sharedPreferences =
            applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

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

    override fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {
        Timber.v("Network state updated asyncronously: $state")
    }
}
