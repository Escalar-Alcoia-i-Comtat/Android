package com.arnyminerz.escalaralcoiaicomtat.activity.model

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.core.utils.loadLocale

abstract class LanguageAppCompatActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(loadLocale(newBase))
    }
}
