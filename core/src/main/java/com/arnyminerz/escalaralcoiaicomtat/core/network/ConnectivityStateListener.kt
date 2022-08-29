package com.arnyminerz.escalaralcoiaicomtat.core.network

import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import androidx.annotation.WorkerThread
import java.net.InetAddress
import java.net.UnknownHostException

interface ConnectivityStateListener {
    class NetworkState(private val capabilities: NetworkCapabilities) {
        val shouldHaveInternet: Boolean
            get() = capabilities.hasCapability(NET_CAPABILITY_INTERNET)

        val isInternetAvailable: Boolean
            @WorkerThread
            get() = if (!shouldHaveInternet)
                false
            else try {
                val address = InetAddress.getByName("www.google.com")
                !address.equals("")
            } catch (e: UnknownHostException) {
                false
            }
    }

    fun onStateChange(state: NetworkState)
}