package com.arnyminerz.escalaralcoiaicomtat.core.utils

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
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

class ContextUtils(base: Context) : ContextWrapper(base)

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

/**
 * Returns the fragment's [Activity] as an [AppCompatActivity] and makes sure it's non-null.
 * @author Arnau Mora
 * @since 20211121
 * @throws IllegalStateException If not currently associated with an activity or if associated only
 * with a context.
 * @throws ClassCastException If the holding activity is not an [AppCompatActivity].
 */
@Throws(IllegalStateException::class, ClassCastException::class)
fun Fragment.requireActivityCompat() = requireActivity() as AppCompatActivity

/**
 * Returns the fragment's [Activity] as an [AppCompatActivity] with null-check.
 * @author Arnau Mora
 * @since 20211121
 */
val Fragment.activityCompat: AppCompatActivity?
    get() = activity as? AppCompatActivity?

/**
 * Gets a resource's [Uri].
 * @author Arnau Mora
 * @since 20220102
 * @param resourceId
 */
fun Context.resourceUri(resourceId: Int): Uri =
    with(resources) {
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(getResourcePackageName(resourceId))
            .appendPath(getResourceTypeName(resourceId))
            .appendPath(getResourceEntryName(resourceId))
            .build()
    }
