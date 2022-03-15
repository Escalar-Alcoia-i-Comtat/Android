package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider

const val PREFERENCES_NAME = "EAICPreferences"

@Deprecated("DataStore is the new recommended method for storing key-value data.")
lateinit var sharedPreferences: SharedPreferences

var appNetworkState: ConnectivityProvider.NetworkState =
    ConnectivityProvider.NetworkState.NOT_CONNECTED

lateinit var appNetworkProvider: ConnectivityProvider

val currentUrl: MutableLiveData<String> by lazy {
    MutableLiveData<String>("https://escalaralcoiaicomtat.org/inici.html")
}
