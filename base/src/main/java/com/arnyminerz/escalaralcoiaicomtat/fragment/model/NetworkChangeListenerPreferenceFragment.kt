package com.arnyminerz.escalaralcoiaicomtat.fragment.model

import androidx.preference.PreferenceFragmentCompat
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.shared.appNetworkProvider

abstract class NetworkChangeListenerPreferenceFragment : PreferenceFragmentCompat(),
    ConnectivityProvider.ConnectivityStateListener {

    override fun onResume() {
        super.onResume()
        appNetworkProvider.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        appNetworkProvider.removeListener(this)
    }
}
