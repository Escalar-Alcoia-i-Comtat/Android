package com.arnyminerz.escalaralcoiaicomtat.core.shared

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData

const val PREFERENCES_NAME = "EAICPreferences"

@Deprecated("DataStore is the new recommended method for storing key-value data.")
lateinit var sharedPreferences: SharedPreferences

val currentUrl: MutableLiveData<String> by lazy {
    MutableLiveData<String>("https://escalaralcoiaicomtat.org/inici.html")
}
