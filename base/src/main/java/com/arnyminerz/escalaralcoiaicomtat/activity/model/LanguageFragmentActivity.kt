package com.arnyminerz.escalaralcoiaicomtat.activity.model

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.arnyminerz.escalaralcoiaicomtat.core.utils.loadLocale

abstract class LanguageFragmentActivity : FragmentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(loadLocale(newBase))
    }
}
