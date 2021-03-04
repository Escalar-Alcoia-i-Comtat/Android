package com.arnyminerz.escalaralcoiaicomtat.network.base

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.*
import com.arnyminerz.escalaralcoiaicomtat.network.ConnectivityProviderImpl

interface ConnectivityProvider {
    interface ConnectivityStateListener {
        fun onStateChange(state: NetworkState)
    }

    fun addListener(listener: ConnectivityStateListener)
    fun removeListener(listener: ConnectivityStateListener)

    fun getNetworkState(): NetworkState

    class NetworkState private constructor(
        private val networkCapabilities: NetworkCapabilities?,
        private val forceInternetStatus: Boolean?,
        private val forceWifiStatus: Boolean?
    ) {
        constructor(networkCapabilities: NetworkCapabilities?) : this(
            networkCapabilities,
            null,
            null
        )

        companion object {
            val NOT_CONNECTED = NetworkState(null)
            val CONNECTED_NO_WIFI =
                NetworkState(null, forceInternetStatus = true, forceWifiStatus = false)
        }

        override fun toString(): String =
            "[hasInternet()=%s,wifiConnected()=%s]".format(hasInternet, wifiConnected)

        val hasInternet: Boolean
            get() = forceInternetStatus ?: ((networkCapabilities?.hasCapability(
                NET_CAPABILITY_INTERNET
            ) ?: false) && isConnectionFast)

        val wifiConnected: Boolean
            get() = forceWifiStatus ?: (networkCapabilities?.hasTransport(TRANSPORT_WIFI) ?: false)

        val usingMobileData: Boolean
            get() = forceWifiStatus
                ?: (networkCapabilities?.hasTransport(
                    TRANSPORT_CELLULAR
                ) ?: false)

        val usingEthernet: Boolean
            get() = networkCapabilities?.hasTransport(TRANSPORT_ETHERNET) ?: false

        val isConnectionFast: Boolean =
            networkCapabilities?.linkDownstreamBandwidthKbps?.let { it >= 400 }
                ?: false // Connection is fast with at least 400kbps
    }

    companion object {
        fun createProvider(context: Context): ConnectivityProvider {
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            return ConnectivityProviderImpl(cm)
        }
    }
}

fun ConnectivityProvider.NetworkState?.hasInternet(): Boolean = this?.hasInternet() ?: false