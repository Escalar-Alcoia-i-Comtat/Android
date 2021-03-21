package com.arnyminerz.escalaralcoiaicomtat.shared

import android.content.SharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider

var appNetworkState: ConnectivityProvider.NetworkState =
    ConnectivityProvider.NetworkState.NOT_CONNECTED

lateinit var appNetworkProvider: ConnectivityProvider

const val PREFERENCES_NAME = "EAICPreferences"
lateinit var sharedPreferences: SharedPreferences
