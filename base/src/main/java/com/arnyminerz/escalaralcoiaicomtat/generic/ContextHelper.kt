package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_LANGUAGE_PREF
import timber.log.Timber
import java.util.*

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

fun Context.toast(@StringRes text: Int, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

fun Context.runOnUiThread(call: (context: Context) -> Unit) =
    Handler(Looper.getMainLooper()).post {
        call(this)
    }

fun toast(context: Context?, @StringRes text: Int) =
    context?.runOnUiThread { it.toast(text) }

fun toast(context: Context?, text: String) =
    context?.runOnUiThread { it.toast(text) }

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
    if (SETTINGS_LANGUAGE_PREF.isSet(sharedPreferences!!)) {
        val appLanguagesValues = resources.getStringArray(R.array.app_languages_values)
        val langPref = SETTINGS_LANGUAGE_PREF.get(sharedPreferences!!)
        val newLang = appLanguagesValues[langPref]
        setAppLocale(newLang)
        Timber.v("Set app locale to $newLang")
    } else
        Timber.v("    No language was set in settings, using system default.")
}

fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
