package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

@UiThread
fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

@UiThread
fun Context.toast(@StringRes text: Int, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

@UiThread
fun toast(context: Context?, @StringRes text: Int) =
    context?.toast(text)

@UiThread
fun toast(context: Context?, text: String) =
    context?.toast(text)

@UiThread
fun Fragment.toast(@StringRes text: Int) =
    context?.toast(text)

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
