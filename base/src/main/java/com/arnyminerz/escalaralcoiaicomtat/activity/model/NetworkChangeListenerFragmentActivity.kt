package com.arnyminerz.escalaralcoiaicomtat.activity.model

import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider.NetworkState

@ExperimentalUnsignedTypes
abstract class NetworkChangeListenerFragmentActivity : LanguageAppCompatActivity(),
    ConnectivityProvider.ConnectivityStateListener, NetworkChangeListenerActivityInterface {

    override var networkState: NetworkState = NetworkState.NOT_CONNECTED

    override var provider: ConnectivityProvider? = null

    override fun onStateChange(state: NetworkState) {
        networkState = state
    }

    override fun onStart() {
        super.onStart()
        provider = ConnectivityProvider.createProvider(this)
    }

    override fun onResume() {
        super.onResume()
        provider?.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        provider?.removeListener(this)
    }
}
