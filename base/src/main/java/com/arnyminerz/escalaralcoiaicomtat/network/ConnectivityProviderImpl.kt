package com.arnyminerz.escalaralcoiaicomtat.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProviderBaseImpl

class ConnectivityProviderImpl(private val cm: ConnectivityManager) :
    ConnectivityProviderBaseImpl() {

    private val networkCallback = ConnectivityCallback()

    override fun subscribe() {
        cm.registerDefaultNetworkCallback(networkCallback)
    }

    override fun unsubscribe() {
        cm.unregisterNetworkCallback(networkCallback)
    }

    override fun getNetworkState(): ConnectivityProvider.NetworkState {
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return ConnectivityProvider.NetworkState(capabilities)
    }

    private inner class ConnectivityCallback : ConnectivityManager.NetworkCallback() {

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            dispatchChange(ConnectivityProvider.NetworkState(capabilities))
        }

        override fun onLost(network: Network) {
            dispatchChange(ConnectivityProvider.NetworkState.NOT_CONNECTED)
        }
    }
}
