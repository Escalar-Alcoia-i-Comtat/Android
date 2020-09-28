package com.arnyminerz.escalaralcoiaicomtat.wrapper

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.*

class MyContextWrapper(context: Context) : ContextWrapper(context) {
    companion object {
        fun wrapContext(context: Context, language: String): ContextWrapper {
            val config = context.resources.configuration
            val sysLocale = config.getSystemLocale()
            if (language != "" && sysLocale.language != language) {
                val locale = Locale(language)
                Locale.setDefault(locale)
                config.setLocale(locale)
            }
            val newContext = context.createConfigurationContext(config)
            return MyContextWrapper(newContext)
        }

        fun Configuration.getSystemLocale(): Locale = locales.get(0)
    }
}