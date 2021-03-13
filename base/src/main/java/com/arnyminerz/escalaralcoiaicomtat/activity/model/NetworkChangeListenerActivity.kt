package com.arnyminerz.escalaralcoiaicomtat.activity.model

import com.arnyminerz.escalaralcoiaicomtat.appNetworkProvider
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider

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
}
