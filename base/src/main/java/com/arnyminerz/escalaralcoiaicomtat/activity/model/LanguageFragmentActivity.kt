package com.arnyminerz.escalaralcoiaicomtat.activity.model

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.generic.loadLocale

abstract class LanguageFragmentActivity : FragmentActivity() {
    @ExperimentalUnsignedTypes
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(loadLocale(newBase))
    }
}
