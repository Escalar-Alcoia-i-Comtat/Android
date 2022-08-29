package com.arnyminerz.escalaralcoiaicomtat.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import androidx.annotation.RequiresPermission

class ConnectivityProvider private constructor(context: Context) {
    companion object {
        @Volatile
        private var provider: ConnectivityProvider? = null

        fun getInstance(context: Context) =
            provider ?: synchronized(this) {
                provider ?: ConnectivityProvider(context).also {
                    provider = it
                }
            }
    }

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    /**
     * Adds a new listener that observes the current network state.
     * @author Arnau Mora
     * @since 20220829
     * @param callback Will get called whenever the current network state is updated.
     */
    @RequiresPermission(value = "android.permission.ACCESS_NETWORK_STATE")
    fun registerNetworkCallback(callback: NetworkCallback) =
        connectivityManager.registerDefaultNetworkCallback(callback)

    /**
     * Removes a callback from listening for network updates.
     * @author Arnau Mora
     * @since 20220829
     */
    fun unregisterNetworkCallback(callback: NetworkCallback) =
        connectivityManager.unregisterNetworkCallback(callback)
}
