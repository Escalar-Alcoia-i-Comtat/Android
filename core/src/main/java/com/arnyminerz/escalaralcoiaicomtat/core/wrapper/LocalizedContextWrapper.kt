package com.arnyminerz.escalaralcoiaicomtat.core.wrapper

import android.content.Context
import android.content.ContextWrapper
import java.util.*

class LocalizedContextWrapper(context: Context) : ContextWrapper(context) {
    companion object {
        fun wrap(context: Context, language: String): ContextWrapper {
            val config = context.resources.configuration
            val sysLocale = config.locales[0]
            if (language.isNotEmpty() && sysLocale.language != language) {
                val locale = Locale(language)
                Locale.setDefault(locale)
                config.setLocale(locale)
            }
            return LocalizedContextWrapper(
                context.createConfigurationContext(config)
            )
        }
    }
}