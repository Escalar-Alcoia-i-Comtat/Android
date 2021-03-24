package com.arnyminerz.escalaralcoiaicomtat.generic

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_LANGUAGE_PREF
import timber.log.Timber
import java.util.Locale

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

fun Context.toast(@StringRes text: Int, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

fun Context.onUiThread(call: (context: Context) -> Unit) {
    if (this is Activity)
        runOnUiThread { call(this) }
    else
        Handler(Looper.getMainLooper()).post {
            call(this)
        }
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
    val langPref = SETTINGS_LANGUAGE_PREF.get()

    val config = resources.configuration
    val localeList = LocaleList(Locale(langPref))
    config.setLocales(localeList)

    Timber.v("Set app locale to $langPref")
    return ContextUtils(context.createConfigurationContext(config))
}

fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
