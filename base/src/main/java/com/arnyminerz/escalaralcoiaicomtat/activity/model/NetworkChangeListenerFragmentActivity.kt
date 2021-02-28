package com.arnyminerz.escalaralcoiaicomtat.activity.model

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_LANGUAGE_PREF
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider.NetworkState
import com.arnyminerz.escalaralcoiaicomtat.wrapper.MyContextWrapper
import timber.log.Timber

@ExperimentalUnsignedTypes
abstract class NetworkChangeListenerFragmentActivity : AppCompatActivity(),
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

    override fun attachBaseContext(newBase: Context) {
        Timber.v("  Loading Shared Preferences...")
        val sharedPreferences = newBase.sharedPreferences

        if (SETTINGS_LANGUAGE_PREF.isSet(sharedPreferences)) {
            val appLanguagesValues = arrayOf("en", "ca", "es")
            val langPref = SETTINGS_LANGUAGE_PREF.get(sharedPreferences)
            val newLang = appLanguagesValues[langPref]
            super.attachBaseContext(MyContextWrapper.wrapContext(newBase, newLang))
        } else
            super.attachBaseContext(newBase)
    }
}