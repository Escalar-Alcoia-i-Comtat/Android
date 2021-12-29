package com.arnyminerz.escalaralcoiaicomtat.activity.model

import android.content.Context
import androidx.activity.ComponentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.utils.loadLocale

abstract class LanguageComponentActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(loadLocale(newBase))
    }
}