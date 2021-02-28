package com.arnyminerz.escalaralcoiaicomtat.activity.model

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.generic.loadLocale

abstract class LanguageAppCompatActivity: AppCompatActivity() {
    @ExperimentalUnsignedTypes
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(loadLocale(newBase))
    }
}