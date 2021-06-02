package com.arnyminerz.escalaralcoiaicomtat.generic

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.preferences.SETTINGS_LANGUAGE_PREF
import timber.log.Timber
import java.util.Locale

@UiThread
fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

@UiThread
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

@UiThread
fun toast(context: Context?, @StringRes text: Int) =
    context?.toast(text)

@UiThread
fun toast(context: Context?, text: String) =
    context?.toast(text)

@UiThread
fun Fragment.toast(@StringRes text: Int) =
    context?.toast(text)

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

fun Activity?.finishActivityWithResult(resultCode: Int, data: Intent?) =
    this?.also {
        if (data == null)
            setResult(resultCode)
        else
            setResult(resultCode, data)
        finish()
    }

fun Context?.isLocationPermissionGranted(): Boolean =
    if (this == null)
        false
    else ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
