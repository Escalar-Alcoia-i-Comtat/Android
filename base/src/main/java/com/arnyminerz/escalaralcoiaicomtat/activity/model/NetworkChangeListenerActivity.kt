package com.arnyminerz.escalaralcoiaicomtat.activity.model

import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkProvider

abstract class NetworkChangeListenerActivity : LanguageAppCompatActivity(),
    ConnectivityProvider.ConnectivityStateListener {

    override fun onResume() {
        super.onResume()
        appNetworkProvider.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        appNetworkProvider.removeListener(this)
    }

    override fun onStateChange(state: ConnectivityProvider.NetworkState) {}
    override suspend fun onStateChangeAsync(state: ConnectivityProvider.NetworkState) {}
}
