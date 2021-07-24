package com.arnyminerz.escalaralcoiaicomtat.core.network.base

import android.os.Handler
import android.os.Looper
import com.arnyminerz.escalaralcoiaicomtat.core.utils.doAsync

abstract class ConnectivityProviderBaseImpl : ConnectivityProvider {
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<ConnectivityProvider.ConnectivityStateListener>()
    private var subscribed = false

    override fun addListener(listener: ConnectivityProvider.ConnectivityStateListener) {
        listeners.add(listener)
        val networkState = getNetworkState()
        listener.onStateChange(networkState) // propagate an initial state
        doAsync { listener.onStateChangeAsync(networkState) }
        verifySubscription()
    }

    override fun removeListener(listener: ConnectivityProvider.ConnectivityStateListener) {
        listeners.remove(listener)
        verifySubscription()
    }

    private fun verifySubscription() {
        if (!subscribed && listeners.isNotEmpty()) {
            subscribe()
            subscribed = true
        } else if (subscribed && listeners.isEmpty()) {
            unsubscribe()
            subscribed = false
        }
    }

    protected fun dispatchChange(state: ConnectivityProvider.NetworkState) {
        handler.post {
            for (listener in listeners) {
                listener.onStateChange(state) // propagate an initial state
                doAsync { listener.onStateChangeAsync(state) }
            }
        }
    }

    protected abstract fun subscribe()
    protected abstract fun unsubscribe()
}
