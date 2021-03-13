package com.arnyminerz.escalaralcoiaicomtat.fragment.model

import android.content.Context
import androidx.preference.PreferenceFragmentCompat
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivityInterface
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider.NetworkState

abstract class NetworkChangeListenerPreferenceFragment : PreferenceFragmentCompat(),
    ConnectivityProvider.ConnectivityStateListener, NetworkChangeListenerActivityInterface {

    override var networkState: NetworkState = NetworkState.NOT_CONNECTED

    override var provider: ConnectivityProvider? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        provider = ConnectivityProvider.createProvider(context)
    }

    override fun onStateChange(state: NetworkState) {
        networkState = state
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
