package com.arnyminerz.escalaralcoiaicomtat.shared

import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider

var appNetworkState: ConnectivityProvider.NetworkState =
    ConnectivityProvider.NetworkState.NOT_CONNECTED

lateinit var appNetworkProvider: ConnectivityProvider
