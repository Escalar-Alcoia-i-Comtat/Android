package com.arnyminerz.escalaralcoiaicomtat.activity.model

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arnyminerz.escalaralcoiaicomtat.core.utils.context.LocaleHelper

abstract class LanguageAppCompatActivity : AppCompatActivity() {
    private var initialLocale: String? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialLocale = LocaleHelper.getPersistedLocale()
    }

    override fun onResume() {
        super.onResume()
        if (initialLocale != null && initialLocale != LocaleHelper.getPersistedLocale())
            recreate()
    }
}
