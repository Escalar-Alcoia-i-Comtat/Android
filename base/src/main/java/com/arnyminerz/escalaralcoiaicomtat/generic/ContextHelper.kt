package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_LANGUAGE_PREF
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import timber.log.Timber
import java.util.*

fun toast(context: Context?, @StringRes text: Int) =
    context?.runOnUiThread { toast(text) }

fun toast(context: Context?, text: String) =
    context?.runOnUiThread { toast(text) }

fun Activity.setAppLocale(localeCode: String) {
    val config = baseContext.resources.configuration
    val locale = Locale(localeCode)
    Locale.setDefault(locale)
    config.setLocales(LocaleList(locale))
    baseContext.createConfigurationContext(config)
}

@ExperimentalUnsignedTypes
fun Activity.loadLocale() {
    Timber.v("  Loading default language...")
    if (SETTINGS_LANGUAGE_PREF.isSet(MainActivity.sharedPreferences!!)) {
        val appLanguagesValues = resources.getStringArray(R.array.app_languages_values)
        val langPref = SETTINGS_LANGUAGE_PREF.get(MainActivity.sharedPreferences!!)
        val newLang = appLanguagesValues[langPref]
        setAppLocale(newLang)
        Timber.v("Set app locale to $newLang")
    } else
        Timber.v("    No language was set in settings, using system default.")
}

fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
