package com.arnyminerz.escalaralcoiaicomtat.generic

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.preference.sharedPreferences
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_LANGUAGE_PREF
import timber.log.Timber
import java.util.*

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

fun Context.toast(@StringRes text: Int, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

fun Context.onUiThread(call: (context: Context) -> Unit) =
    Handler(Looper.getMainLooper()).post {
        call(this)
    }

fun toast(context: Context?, @StringRes text: Int) =
    context?.onUiThread { it.toast(text) }

fun toast(context: Context?, text: String) =
    context?.onUiThread { it.toast(text) }

class ContextUtils(base: Context) : ContextWrapper(base)
@ExperimentalUnsignedTypes
fun loadLocale(context: Context): ContextWrapper {
    Timber.v("Loading app language...")
    val resources = context.resources
    val sharedPreferences = context.sharedPreferences
    val appLanguagesValues = resources.getStringArray(R.array.app_languages_values)
    val langPref = SETTINGS_LANGUAGE_PREF.get(sharedPreferences)
    val newLang = appLanguagesValues[langPref]

    val config = resources.configuration
    val localeList = LocaleList(Locale(newLang))
    config.setLocales(localeList)

    Timber.v("Set app locale to $newLang")
    return ContextUtils(context.createConfigurationContext(config))
}

fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
