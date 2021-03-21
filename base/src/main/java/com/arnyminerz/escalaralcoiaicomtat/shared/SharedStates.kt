package com.arnyminerz.escalaralcoiaicomtat.shared

import android.content.SharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider

var appNetworkState: ConnectivityProvider.NetworkState =
    ConnectivityProvider.NetworkState.NOT_CONNECTED

lateinit var appNetworkProvider: ConnectivityProvider

private const val PREFERENCES_NAME = "EAICPreferences"
private var sharedPreferencesStorage: SharedPreferences? = null
