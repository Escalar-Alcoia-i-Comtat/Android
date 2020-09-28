package com.arnyminerz.escalaralcoiaicomtat.activity.model

import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider

interface NetworkChangeListenerActivityInterface {
    var networkState: ConnectivityProvider.NetworkState
    var provider: ConnectivityProvider?
}