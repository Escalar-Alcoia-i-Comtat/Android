package com.arnyminerz.escalaralcoiaicomtat.activity.model

import android.content.Context
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.core.network.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.network.ConnectivityStateListener

/**
 * A Component Activity that adapts to the user chosen language, and listens for device connectivity
 * changes. Override [onStateChange] for adding listeners.
 * @author Arnau Mora
 * @since 20220102
 */
abstract class NetworkAwareActivity : AppCompatActivity(), ConnectivityStateListener {

    private lateinit var connectivityProvider: ConnectivityProvider

    private val networkCallback = object : NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)

            val state = ConnectivityStateListener.NetworkState(networkCapabilities)
            onStateChange(state)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectivityProvider = ConnectivityProvider.getInstance(this as Context)
    }

    override fun onResume() {
        super.onResume()
        connectivityProvider.registerNetworkCallback(networkCallback)
    }

    override fun onPause() {
        super.onPause()
        connectivityProvider.unregisterNetworkCallback(networkCallback)
    }

    /**
     * This will get called when the connectivity state of the device is updated.
     * @author Arnau Mora
     * @since 20210818
     * @param state The new [ConnectivityStateListener.NetworkState].
     */
    override fun onStateChange(state: ConnectivityStateListener.NetworkState) {}
}
