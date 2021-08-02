package com.arnyminerz.escalaralcoiaicomtat.fragment.model

import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.core.shared.appNetworkProvider

abstract class NetworkChangeListenerFragment : Fragment(),
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
