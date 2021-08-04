package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.area.Area
import com.arnyminerz.escalaralcoiaicomtat.core.network.base.ConnectivityProvider

val AREAS = arrayListOf<Area>()

const val PREFERENCES_NAME = "EAICPreferences"
lateinit var sharedPreferences: SharedPreferences

var appNetworkState: ConnectivityProvider.NetworkState =
    ConnectivityProvider.NetworkState.NOT_CONNECTED

lateinit var appNetworkProvider: ConnectivityProvider

val currentUrl: MutableLiveData<String> by lazy {
    MutableLiveData<String>("https://escalaralcoiaicomtat.org/inici.html")
}
